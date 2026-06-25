package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import security.AccessControl;
import terminal.Terminal;

import java.util.List;

/**
 * ls - List directory contents.
 *
 * Silberschatz Ch.13: "The directory contains information about all files
 * within it. The ls command queries directory entries and displays file info."
 *
 * Usage: ls [path]     — list directory
 *        ls -R [path]  — recursive listing
 */
public class LsCommand implements Command {
    @Override public String getName()        { return "ls"; }
    @Override public String getDescription() { return "ls [-R] [dir] — lista el contenido de un directorio"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        boolean recursive = false;
        String  target    = null;

        for (int i = 1; i < args.length; i++) {
            if ("-R".equalsIgnoreCase(args[i])) recursive = true;
            else if (target == null) target = args[i];
        }

        String path = terminal.resolvePath(target != null ? target : ".");
        FCB fcb = terminal.getDisk().getFCBByPath(path);

        if (fcb == null) {
            System.out.println("ls: '" + path + "': No existe"); return;
        }
        if (!fcb.isDirectory()) {
            // List a single file
            printFCBLine(fcb);
            return;
        }
        if (!AccessControl.checkRead(fcb, terminal.getCurrentUser(), terminal.getDisk())) return;

        listDirectory(terminal, fcb, path, recursive, 0);
    }

    private void listDirectory(Terminal terminal, FCB dir, String path, boolean recursive, int depth) {
        if (depth > 0) System.out.println("\n" + path + ":");
        List<DirectoryEntry> entries = dir.getEntries();
        if (entries == null || entries.isEmpty()) {
            System.out.println("  (vacío)"); return;
        }

        System.out.printf("  %-6s  %-7s  %-12s  %-12s  %8s  %s%n",
            "Tipo", "Perms", "Propietario", "Grupo", "Tamaño", "Nombre");
        System.out.println("  " + "─".repeat(70));

        for (DirectoryEntry e : entries) {
            FCB child = terminal.getDisk().getFCB(e.getFcbId());
            if (child != null) printFCBLine(child);
        }

        if (recursive) {
            for (DirectoryEntry e : entries) {
                FCB child = terminal.getDisk().getFCB(e.getFcbId());
                if (child != null && child.isDirectory()) {
                    listDirectory(terminal, child, child.getPath(), true, depth + 1);
                }
            }
        }
    }

    private void printFCBLine(FCB fcb) {
        System.out.printf("  %-6s  %-7s  %-12s  %-12s  %8d  %s%n",
            fcb.typeTag(),
            fcb.getPermissionsString(),
            fcb.getOwner(),
            fcb.getGroup(),
            fcb.getSize(),
            fcb.getName());
    }
}
