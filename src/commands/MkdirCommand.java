package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import filesystem.VirtualDisk;
import security.AccessControl;
import terminal.Terminal;

/**
 * mkdir - Create a new directory.
 *
 * Silberschatz Ch.13: "The mkdir system call creates a new directory.
 * The new directory receives an empty entry list (just . and ..)."
 *
 * Usage: mkdir <dir> [dir2 ...]
 */
public class MkdirCommand implements Command {
    @Override public String getName()        { return "mkdir"; }
    @Override public String getDescription() { return "mkdir <dir> — crea uno o más directorios"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: mkdir <directorio> [dir2 ...]"); return; }

        VirtualDisk disk = terminal.getDisk();
        String user = terminal.getCurrentUser();

        for (int i = 1; i < args.length; i++) {
            String path    = terminal.resolvePath(args[i]);
            String name    = lastName(path);
            FCB parent     = disk.getParentFCB(path);

            if (parent == null || !parent.isDirectory()) {
                System.out.println("mkdir: ruta padre no existe: " + args[i]); continue;
            }
            if (!AccessControl.checkWrite(parent, user, disk)) continue;
            if (parent.findEntry(name) != null) {
                System.out.println("mkdir: '" + args[i] + "': ya existe"); continue;
            }

            FCB dir = new FCB(name, user,
                disk.getUserManager().getUser(user).getPrimaryGroup(),
                77, true, path);
            disk.putFCB(dir);
            parent.addEntry(new DirectoryEntry(name, dir.getId()));
        }
        terminal.saveDisk();
    }

    private String lastName(String path) {
        if (path.equals("/")) return "/";
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }
}
