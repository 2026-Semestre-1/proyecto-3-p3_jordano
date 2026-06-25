package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import filesystem.VirtualDisk;
import security.AccessControl;
import terminal.Terminal;

/**
 * ln - Create a symbolic link.
 *
 * Silberschatz Ch.13 "File-System Interface":
 *   "A symbolic link is a directory entry that simply contains the name of
 *    another file. The OS transparently follows the link to the target."
 *
 * Stallings: "Symbolic links allow a file to be referenced by multiple path names."
 *
 * Usage: ln <target> <linkname>
 */
public class LnCommand implements Command {
    @Override public String getName()        { return "ln"; }
    @Override public String getDescription() { return "ln <destino> <enlace> — crea enlace simbólico"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 3) { System.out.println("Uso: ln <destino> <nombre_enlace>"); return; }

        VirtualDisk disk  = terminal.getDisk();
        String user       = terminal.getCurrentUser();
        String targetPath = terminal.resolvePath(args[1]);
        String linkPath   = terminal.resolvePath(args[2]);
        String linkName   = linkPath.substring(linkPath.lastIndexOf('/') + 1);

        FCB targetFCB = disk.getFCBByPath(targetPath);
        if (targetFCB == null) {
            System.out.println("ln: destino '" + args[1] + "': No existe"); return;
        }
        if (!AccessControl.checkRead(targetFCB, user, disk)) return;

        FCB linkParent = disk.getParentFCB(linkPath);
        if (linkParent == null || !linkParent.isDirectory()) {
            System.out.println("ln: directorio padre del enlace no existe"); return;
        }
        if (!AccessControl.checkWrite(linkParent, user, disk)) return;
        if (linkParent.findEntry(linkName) != null) {
            System.out.println("ln: '" + linkPath + "': ya existe"); return;
        }

        String group = disk.getUserManager().getUser(user).getPrimaryGroup();
        FCB link = new FCB(linkName, user, group, 77, false, linkPath);
        link.setSymlink(true);
        link.setLinkTarget(targetPath);

        disk.putFCB(link);
        linkParent.addEntry(new DirectoryEntry(linkName, link.getId()));
        terminal.saveDisk();
        System.out.println("Enlace simbólico: " + linkPath + " → " + targetPath);
    }
}
