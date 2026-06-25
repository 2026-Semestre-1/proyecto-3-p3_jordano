package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import filesystem.VirtualDisk;
import security.AccessControl;
import terminal.Terminal;

/**
 * touch - Create an empty file, or update its modification timestamp if it exists.
 *
 * Silberschatz Ch.13: "create() creates a new file with no contents and
 * adds it to the directory. The FCB is initialized with metadata."
 *
 * Usage: touch <file> [file2 ...]
 */
public class TouchCommand implements Command {
    @Override public String getName()        { return "touch"; }
    @Override public String getDescription() { return "touch <archivo> — crea un archivo vacío"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: touch <archivo> [archivo2 ...]"); return; }

        VirtualDisk disk = terminal.getDisk();
        String user = terminal.getCurrentUser();

        for (int i = 1; i < args.length; i++) {
            String path   = terminal.resolvePath(args[i]);
            String name   = path.substring(path.lastIndexOf('/') + 1);
            FCB parent    = disk.getParentFCB(path);

            if (parent == null || !parent.isDirectory()) {
                System.out.println("touch: ruta padre no existe: " + args[i]); continue;
            }

            FCB existing = parent.findEntry(name) != null
                ? disk.getFCB(parent.findEntry(name).getFcbId()) : null;

            if (existing != null) {
                // Update modification timestamp
                if (!AccessControl.checkWrite(existing, user, disk)) continue;
                existing.setModificationDate(new java.util.Date());
                System.out.println("touch: actualizado: " + path);
            } else {
                if (!AccessControl.checkWrite(parent, user, disk)) continue;
                String group = disk.getUserManager().getUser(user).getPrimaryGroup();
                FCB file = new FCB(name, user, group, 77, false, path);
                disk.putFCB(file);
                parent.addEntry(new DirectoryEntry(name, file.getId()));
            }
        }
        terminal.saveDisk();
    }
}
