package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import filesystem.VirtualDisk;
import security.AccessControl;
import terminal.Terminal;

/**
 * mv - Move or rename a file/directory.
 *
 * Silberschatz Ch.13: "A rename operation updates the directory entry to
 * point to the same FCB under a new name. A move operation removes the entry
 * from the source directory and adds it to the destination directory."
 *
 * Usage: mv <origen> <destino>
 */
public class MvCommand implements Command {
    @Override public String getName()        { return "mv"; }
    @Override public String getDescription() { return "mv <origen> <destino> — mueve o renombra"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 3) { System.out.println("Uso: mv <origen> <destino>"); return; }

        VirtualDisk disk = terminal.getDisk();
        String user = terminal.getCurrentUser();
        String srcPath  = terminal.resolvePath(args[1]);
        String destPath = terminal.resolvePath(args[2]);

        FCB srcFCB     = disk.getFCBByPath(srcPath);
        FCB srcParent  = disk.getParentFCB(srcPath);

        if (srcFCB == null)  { System.out.println("mv: '" + args[1] + "': No existe"); return; }
        if (srcParent == null){ System.out.println("mv: no se puede mover '/'"); return; }
        if (!AccessControl.checkWrite(srcParent, user, disk)) return;

        // Determine if destination is a directory (move into it) or a new name
        FCB destFCB = disk.getFCBByPath(destPath);
        FCB destParent;
        String newName;

        if (destFCB != null && destFCB.isDirectory()) {
            // mv src dir/ → place src inside dir
            destParent = destFCB;
            newName    = srcFCB.getName();
            destPath   = destPath + "/" + newName;
        } else {
            destParent = disk.getParentFCB(destPath);
            newName    = destPath.substring(destPath.lastIndexOf('/') + 1);
        }

        if (destParent == null || !destParent.isDirectory()) {
            System.out.println("mv: destino no válido: " + args[2]); return;
        }
        if (!AccessControl.checkWrite(destParent, user, disk)) return;

        // Check for name conflict at destination
        if (destParent.findEntry(newName) != null) {
            System.out.println("mv: '" + destPath + "': ya existe"); return;
        }

        // Remove from source directory
        srcParent.removeEntry(srcFCB.getName());

        // Update FCB name and path
        String oldPath = srcFCB.getPath();
        srcFCB.setName(newName);
        srcFCB.setPath(VirtualDisk.normalizePath(destParent.getPath() + "/" + newName));
        srcFCB.setModificationDate(new java.util.Date());

        // Update child paths if it's a directory
        if (srcFCB.isDirectory()) {
            updateChildPaths(srcFCB, oldPath, srcFCB.getPath(), disk);
        }

        // Add to destination directory
        destParent.addEntry(new DirectoryEntry(newName, srcFCB.getId()));
        terminal.saveDisk();
        System.out.println("Movido: " + srcPath + " → " + srcFCB.getPath());
    }

    private void updateChildPaths(FCB dir, String oldBase, String newBase, VirtualDisk disk) {
        if (dir.getEntries() == null) return;
        for (DirectoryEntry e : dir.getEntries()) {
            FCB child = disk.getFCB(e.getFcbId());
            if (child != null) {
                child.setPath(child.getPath().replace(oldBase, newBase));
                if (child.isDirectory()) updateChildPaths(child, oldBase, newBase, disk);
            }
        }
    }
}
