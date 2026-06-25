package terminal;

import filesystem.VirtualDisk;
import storage.DiskPersistence;

import java.util.*;

/**
 * TerminalManager - Manages multiple independent terminal sessions.
 *
 * Silberschatz Ch.4 "Processes":
 *   "Multiple processes sharing the file system coordinate through the OS.
 *    Each process has its own working directory and effective UID."
 *
 * Multi-terminal architecture:
 *   - Each Terminal has its own state: currentUser, currentDirectory.
 *   - All terminals share the same VirtualDisk (and thus the same FCB table,
 *     open-file table, and user database).
 *   - The user can create new terminals and switch between them.
 *
 * Special TerminalManager-level commands (not delegated to Terminal):
 *   newterminal       — create a new terminal session
 *   switcht <n>       — switch to terminal number n
 *   terminals         — list all active terminals
 *   closeterminal <n> — close terminal n (if more than one exists)
 */
public class TerminalManager {

    private final List<Terminal>  terminals = new ArrayList<>();
    private int                   activeIdx = 0;
    private VirtualDisk           disk;
    private final String          diskFile;

    public TerminalManager(VirtualDisk disk, String diskFile) {
        this.disk     = disk;
        this.diskFile = diskFile;
        terminals.add(new Terminal(1, disk, diskFile));
    }

    /** Main loop: read input, dispatch to active terminal or handle meta-commands. */
    public void start() {
        printBanner();

        if (disk != null) {
            System.out.println("Disco cargado: " + diskFile);
            System.out.println("Use 'su root' para iniciar sesión (contraseña: root)");
        } else {
            System.out.println("No hay disco activo. Use 'format' para crear uno.");
        }
        System.out.println("Escriba 'help' para ver todos los comandos.\n");

        // Single shared Scanner for the entire session (avoids buffering conflicts)
        Scanner sc = new Scanner(System.in);
        for (Terminal t : terminals) t.setScanner(sc);

        try {
            while (true) {
                Terminal active = terminals.get(activeIdx);

                System.out.print(active.getPrompt());
                if (!sc.hasNextLine()) break;  // EOF
                String line = sc.nextLine();

                // ── Meta-commands handled by TerminalManager ──────────────
                if (isMetaCommand(line)) {
                    handleMeta(line, sc);
                    continue;
                }

                // ── Delegate to active terminal ───────────────────────────
                boolean cont = active.executeCommandLine(line);

                // Sync disk reference in case format was run
                syncDisk(active);

                if (!cont || !active.isRunning()) {
                    if (terminals.size() == 1) break; // last terminal → exit
                    terminals.remove(activeIdx);
                    activeIdx = Math.max(0, activeIdx - 1);
                    System.out.println("[TerminalManager] Terminal cerrada. " +
                        "Activa: T" + terminals.get(activeIdx).getId());
                }
            }
        } catch (Exception e) {
            // Ignore EOF / pipe close
        }
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║  VFS apagado. ¡Hasta luego!          ║");
        System.out.println("╚══════════════════════════════════════╝");
    }

    // ── Meta-command dispatch ─────────────────────────────────────────────

    private boolean isMetaCommand(String line) {
        String lower = line.trim().toLowerCase();
        return lower.equals("newterminal")
            || lower.startsWith("switcht ")
            || lower.equals("terminals")
            || lower.startsWith("closeterminal ");
    }

    private void handleMeta(String line, Scanner sc) {
        String[] parts = line.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "newterminal":
                int newId = terminals.stream().mapToInt(Terminal::getId).max().orElse(0) + 1;
                Terminal newT = new Terminal(newId, disk, diskFile);
                newT.setScanner(sc); // share the same scanner
                terminals.add(newT);
                activeIdx = terminals.size() - 1;
                System.out.println("[TerminalManager] Nueva terminal T" + newId + " creada y activa.");
                System.out.println("  Use 'su <usuario>' para iniciar sesión.");
                break;

            case "switcht":
                if (parts.length < 2) { System.out.println("Uso: switcht <número>"); break; }
                try {
                    int num = Integer.parseInt(parts[1].trim());
                    Optional<Terminal> found = terminals.stream()
                        .filter(t -> t.getId() == num).findFirst();
                    if (found.isPresent()) {
                        activeIdx = terminals.indexOf(found.get());
                        System.out.println("[TerminalManager] Cambiado a T" + num +
                            " [user=" + found.get().getCurrentUser() +
                            ", cwd=" + found.get().getCurrentDirectory() + "]");
                    } else {
                        System.out.println("Terminal T" + num + " no existe.");
                        listTerminals();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Uso: switcht <número>");
                }
                break;

            case "terminals":
                listTerminals();
                break;

            case "closeterminal":
                if (parts.length < 2) { System.out.println("Uso: closeterminal <número>"); break; }
                try {
                    int num = Integer.parseInt(parts[1].trim());
                    if (terminals.size() == 1) {
                        System.out.println("No se puede cerrar la única terminal. Use 'exit'."); break;
                    }
                    Optional<Terminal> toDel = terminals.stream()
                        .filter(t -> t.getId() == num).findFirst();
                    if (toDel.isPresent()) {
                        terminals.remove(toDel.get());
                        activeIdx = Math.max(0, Math.min(activeIdx, terminals.size() - 1));
                        System.out.println("[TerminalManager] Terminal T" + num + " cerrada.");
                        listTerminals();
                    } else {
                        System.out.println("Terminal T" + num + " no existe.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Uso: closeterminal <número>");
                }
                break;
        }
    }

    private void listTerminals() {
        System.out.println("\n  ── Terminales activas ──");
        for (int i = 0; i < terminals.size(); i++) {
            Terminal t = terminals.get(i);
            String marker = (i == activeIdx) ? " ◄ ACTIVA" : "";
            System.out.printf("  T%-3d  user=%-12s  cwd=%s%s%n",
                t.getId(),
                t.getCurrentUser() != null ? t.getCurrentUser() : "(no login)",
                t.getCurrentDirectory(),
                marker);
        }
        System.out.println();
    }

    /** Propagate a newly-formatted disk to all terminals. */
    private void syncDisk(Terminal origin) {
        if (origin.getDisk() != null && origin.getDisk() != disk) {
            disk = origin.getDisk();
            String df = origin.getDiskFile();
            for (Terminal t : terminals) {
                t.setDisk(disk);
                t.setDiskFile(df);
            }
        }
    }

    private void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║      VFS — Virtual File System  (Java Console)           ║");
        System.out.println("║  Basado en: Stallings & Silberschatz/Galvin/Gagne        ║");
        System.out.println("║  Comandos multi-terminal: newterminal, switcht N,        ║");
        System.out.println("║                           terminals, closeterminal N      ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
