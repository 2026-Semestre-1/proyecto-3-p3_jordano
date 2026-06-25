package commands;

import filesystem.FCB;
import filesystem.VirtualDisk;
import storage.DiskPersistence;
import terminal.Terminal;

import java.util.Scanner;

/**
 * format - Initialize (or reinitialize) the virtual disk.
 *
 * Steps performed (Stallings 12.1 + Silberschatz Ch.15):
 *  1. Ask for disk size.
 *  2. Create VirtualDisk (allocates byte[][] blocks).
 *  3. Build MBR and SuperBlock.
 *  4. Initialize bitmap (all blocks free).
 *  5. Create root "/" directory FCB.
 *  6. Create root user (password: root), group "root".
 *  7. Create /root home directory.
 *  8. Create /home and /home/users directories.
 *  9. Persist to miDiscoDuro.fs.
 */
public class FormatCommand implements Command {

    @Override public String getName()        { return "format"; }
    @Override public String getDescription() {
        return "Inicializa el disco virtual (crea miDiscoDuro.fs)";
    }

    @Override
    public void execute(String[] args, Terminal terminal) {
        Scanner sc = terminal.getScanner();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      FORMATO DE DISCO VIRTUAL VFS        ║");
        System.out.println("╚══════════════════════════════════════════╝");

        if (terminal.getDisk() != null) {
            System.out.print("¡ADVERTENCIA! Ya existe un disco. ¿Desea reformatear? (Y/N): ");
            String resp = sc.nextLine().trim().toUpperCase();
            if (!resp.equals("Y")) { System.out.println("Operación cancelada."); return; }
        }

        // ── 1. Disk size ──────────────────────────────────────────────────
        long totalSize = 0;
        while (totalSize < VirtualDisk.BLOCK_SIZE) {
            System.out.print("Ingrese tamaño del disco (ej: 10MB, 5MB, 1MB): ");
            String input = sc.nextLine().trim().toUpperCase();
            totalSize = parseSize(input);
            if (totalSize < VirtualDisk.BLOCK_SIZE)
                System.out.println("  Tamaño mínimo: " + VirtualDisk.BLOCK_SIZE + " bytes.");
        }

        // ── 2. Volume name ─────────────────────────────────────────────────
        System.out.print("Nombre del volumen [MiVFS]: ");
        String volName = sc.nextLine().trim();
        if (volName.isEmpty()) volName = "MiVFS";

        System.out.println("\nFormateando disco...");

        // ── 3. Create VirtualDisk ─────────────────────────────────────────
        VirtualDisk disk = new VirtualDisk(volName, totalSize);

        // ── 4. Create group "root" ─────────────────────────────────────────
        disk.getUserManager().addGroup("root");

        // ── 5. Create root user (password "root") ─────────────────────────
        disk.getUserManager().addUser("root", "Root User", "root", "root", "/root");

        // ── 6. Build directory tree: /  /root  /home  /home/users ──────────
        FCB rootDir = new FCB("/", "root", "root", 77, true, "/");
        disk.putFCB(rootDir);
        disk.setRootFcbId(rootDir.getId());

        FCB rootHome = mkDir(disk, rootDir, "root", "root", "root", "/root");
        FCB home     = mkDir(disk, rootDir, "home", "root", "root", "/home");
        mkDir(disk, home, "users", "root", "root", "/home/users");

        // Update root user's home dir (already "/root" — consistent)
        System.out.println("  [OK] Árbol de directorios creado: / /root /home /home/users");

        // ── 7. Persist ────────────────────────────────────────────────────
        disk.syncSuperBlock();
        terminal.setDisk(disk);

        String diskFile = terminal.getDiskFile() != null
            ? terminal.getDiskFile() : "miDiscoDuro.fs";
        terminal.setDiskFile(diskFile);

        if (DiskPersistence.save(disk, diskFile)) {
            System.out.println("  [OK] Disco guardado en: " + diskFile);
        } else {
            System.out.println("  [ERROR] No se pudo guardar el disco.");
            return;
        }

        // ── 8. Auto-login as root ─────────────────────────────────────────
        terminal.setCurrentUser("root");
        terminal.setCurrentDirectory("/root");

        int blocks = disk.getSuperBlock().getTotalBlocks();
        System.out.println("\n══════════════════════════════════════════");
        System.out.printf("  Disco: %-30s%n", volName);
        System.out.printf("  Tamaño: %d bytes (%d KB)%n", totalSize, totalSize/1024);
        System.out.printf("  Bloques: %d x %d bytes%n", blocks, VirtualDisk.BLOCK_SIZE);
        System.out.println("  Usuario: root  |  Contraseña: root");
        System.out.println("══════════════════════════════════════════\n");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private FCB mkDir(VirtualDisk disk, FCB parent, String name,
                      String owner, String group, String path) {
        FCB dir = new FCB(name, owner, group, 77, true, path);
        disk.putFCB(dir);
        parent.addEntry(new filesystem.DirectoryEntry(name, dir.getId()));
        return dir;
    }

    /** Parse human-readable size strings like "10MB", "512KB", "1048576". */
    private long parseSize(String s) {
        try {
            s = s.trim().toUpperCase().replace(" ", "");
            // Check suffixes BEFORE stripping any characters
            if (s.endsWith("GB")) return Long.parseLong(s.substring(0, s.length()-2)) * 1024L * 1024 * 1024;
            if (s.endsWith("MB")) return Long.parseLong(s.substring(0, s.length()-2)) * 1024L * 1024;
            if (s.endsWith("KB")) return Long.parseLong(s.substring(0, s.length()-2)) * 1024L;
            if (s.endsWith("B"))  return Long.parseLong(s.substring(0, s.length()-1));
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
