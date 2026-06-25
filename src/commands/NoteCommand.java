package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import filesystem.VirtualDisk;
import security.AccessControl;
import storage.BlockManager;
import terminal.Terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * note - Simple line-by-line text editor.
 *
 * Silberschatz Ch.13: "write() appends to or overwrites the file content.
 * The OS allocates new blocks as needed and updates the FCB."
 *
 * Stallings 12.2: "When a file is modified the FCB fields: size,
 * modification date, and block list are updated."
 *
 * Usage: note <archivo>
 *
 * Editor controls:
 *   :x  — finish editing, ask to save
 *   :q  — quit without saving
 *   :l  — list current lines
 */
public class NoteCommand implements Command {
    @Override public String getName()        { return "note"; }
    @Override public String getDescription() { return "note <archivo> — editor de texto en línea"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: note <archivo>"); return; }

        VirtualDisk disk = terminal.getDisk();
        String user = terminal.getCurrentUser();
        String path = terminal.resolvePath(args[1]);
        String name = path.substring(path.lastIndexOf('/') + 1);

        FCB fcb = disk.getFCBByPath(path);

        if (fcb != null && fcb.isDirectory()) {
            System.out.println("note: '" + args[1] + "': Es un directorio"); return;
        }

        boolean isNew = (fcb == null);

        if (isNew) {
            // Create new file
            FCB parent = disk.getParentFCB(path);
            if (parent == null || !parent.isDirectory()) {
                System.out.println("note: ruta padre no existe"); return;
            }
            if (!AccessControl.checkWrite(parent, user, disk)) return;
            String group = disk.getUserManager().getUser(user).getPrimaryGroup();
            fcb = new FCB(name, user, group, 77, false, path);
            disk.putFCB(fcb);
            parent.addEntry(new DirectoryEntry(name, fcb.getId()));
        } else {
            if (!AccessControl.checkWrite(fcb, user, disk)) return;
        }

        // Open in OFT
        disk.getOpenFileTable().open(fcb, user);

        // Load existing content
        List<String> lines = new ArrayList<>();
        if (!isNew) {
            String existing = BlockManager.readContent(disk, fcb);
            if (!existing.isEmpty()) {
                for (String l : existing.split("\n", -1)) lines.add(l);
            }
        }

        // ── Editor UI ────────────────────────────────────────────────────
        System.out.println("┌─────────────────────────────────────────────────┐");
        System.out.println("│  EDITOR DE TEXTO — " + name);
        System.out.println("│  Comandos: :x = guardar y salir  :q = salir sin guardar  :l = listar");
        System.out.println("└─────────────────────────────────────────────────┘");
        if (!lines.isEmpty()) {
            System.out.println("  (contenido actual: " + lines.size() + " líneas)");
        }

        Scanner sc = terminal.getScanner();
        boolean modified = false;

        while (true) {
            System.out.print("  > ");
            String input = sc.nextLine();

            if (input.equals(":x")) break;
            if (input.equals(":q")) {
                disk.getOpenFileTable().close(fcb.getId(), user, fcb);
                terminal.saveDisk();
                System.out.println("Saliendo sin guardar.");
                return;
            }
            if (input.equals(":l")) {
                System.out.println("  ──── Contenido actual ────");
                for (int i = 0; i < lines.size(); i++)
                    System.out.printf("  %3d │ %s%n", i+1, lines.get(i));
                System.out.println("  ──────────────────────────");
                continue;
            }
            lines.add(input);
            modified = true;
        }

        // ── Save decision ─────────────────────────────────────────────────
        if (!modified && !isNew) {
            System.out.println("Sin cambios.");
        } else {
            System.out.print("¿Guardar cambios? (Y/N): ");
            String ans = sc.nextLine().trim().toUpperCase();
            if (ans.equals("Y")) {
                String content = String.join("\n", lines);
                if (BlockManager.writeContent(disk, fcb, content)) {
                    System.out.printf("Guardado: %s (%d bytes, %d líneas, %d bloques)%n",
                        path, fcb.getSize(), lines.size(), fcb.getBlocks().size());
                } else {
                    System.out.println("Error: disco lleno.");
                }
            } else {
                System.out.println("Cambios descartados.");
            }
        }

        disk.getOpenFileTable().close(fcb.getId(), user, fcb);
        terminal.saveDisk();
    }
}
