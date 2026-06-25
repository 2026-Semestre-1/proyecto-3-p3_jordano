package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import filesystem.VirtualDisk;
import security.AccessControl;
import storage.BlockManager;
import terminal.Terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * rm - Remove files or directories.
 *
 * Stallings 12.4: "When a file is deleted, its blocks must be returned to the
 * free-space list (bitmap), and its FCB must be removed from the directory."
 *
 * Usage:
 *   rm <file>          — remove file
 *   rm -R <dir>        — remove directory recursively
 *   rm *.txt           — wildcard pattern
 */
public class RmCommand implements Command {
    @Override public String getName()        { return "rm"; }
    @Override public String getDescription() {
        return "rm [-R] <archivo|dir|patrón> — elimina archivos o directorios";
    }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: rm [-R] <archivo|directorio>"); return; }

        boolean recursive = false;
        List<String> targets = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if ("-R".equalsIgnoreCase(args[i]) || "-r".equals(args[i])) recursive = true;
            else targets.add(args[i]);
        }

        if (targets.isEmpty()) { System.out.println("rm: falta el operando"); return; }

        VirtualDisk disk = terminal.getDisk();
        String user = terminal.getCurrentUser();

        for (String t : targets) {
            // Wildcard support: *.ext or *
            if (t.contains("*")) {
                expandWildcard(terminal, t, recursive, user, disk);
            } else {
                String path = terminal.resolvePath(t);
                removePath(terminal, path, recursive, user, disk);
            }
        }
        terminal.saveDisk();
    }

    private void expandWildcard(Terminal terminal, String pattern, boolean recursive,
                                String user, VirtualDisk disk) {
        String dir = terminal.getCurrentDirectory();
        FCB dirFCB = disk.getFCBByPath(dir);
        if (dirFCB == null || !dirFCB.isDirectory()) return;

        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        List<DirectoryEntry> toRemove = new ArrayList<>();
        for (DirectoryEntry e : new ArrayList<>(dirFCB.getEntries())) {
            if (e.getName().matches(regex)) toRemove.add(e);
        }
        for (DirectoryEntry e : toRemove) {
            removePath(terminal, dir + "/" + e.getName(), recursive, user, disk);
        }
    }

    private void removePath(Terminal terminal, String path, boolean recursive,
                            String user, VirtualDisk disk) {
        FCB fcb    = disk.getFCBByPath(path);
        FCB parent = disk.getParentFCB(path);

        if (fcb == null)   { System.out.println("rm: '" + path + "': No existe"); return; }
        if (parent == null){ System.out.println("rm: no se puede eliminar '/'");  return; }
        if (!AccessControl.checkWrite(parent, user, disk)) return;

        if (fcb.isDirectory()) {
            if (!recursive) {
                System.out.println("rm: '" + path + "': es un directorio (use -R)"); return;
            }
            removeDirectoryRecursive(fcb, disk);
        } else {
            // Remove open-file-table entry if open
            disk.getOpenFileTable().forceClose(fcb.getId(), fcb);
            BlockManager.releaseBlocks(disk, fcb);
            disk.removeFCB(fcb.getId());
        }

        parent.removeEntry(fcb.getName());
        System.out.println("Eliminado: " + path);
    }

    private void removeDirectoryRecursive(FCB dir, VirtualDisk disk) {
        if (dir.getEntries() == null) return;
        for (DirectoryEntry e : new ArrayList<>(dir.getEntries())) {
            FCB child = disk.getFCB(e.getFcbId());
            if (child == null) continue;
            if (child.isDirectory()) removeDirectoryRecursive(child, disk);
            else {
                disk.getOpenFileTable().forceClose(child.getId(), child);
                BlockManager.releaseBlocks(disk, child);
            }
            disk.removeFCB(child.getId());
        }
        disk.removeFCB(dir.getId());
    }
}
