package commands;

import filesystem.FCB;
import security.AccessControl;
import storage.BlockManager;
import terminal.Terminal;

/**
 * cat - Display the full content of a file.
 *
 * Silberschatz Ch.13: "read() reads from the file into a user-space buffer.
 * The OS locates the file's blocks via the FCB and copies them to the buffer."
 *
 * Stallings: "Sequential access reads from the beginning to the end.
 * The OS uses the block list in the FCB to service the read."
 *
 * Also opens/closes the file in the Open File Table (OFT).
 *
 * Usage: cat <archivo>
 */
public class CatCommand implements Command {
    @Override public String getName()        { return "cat"; }
    @Override public String getDescription() { return "cat <archivo> — muestra el contenido de un archivo"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: cat <archivo>"); return; }

        String path = terminal.resolvePath(args[1]);
        FCB fcb = terminal.getDisk().getFCBByPath(path);

        if (fcb == null)          { System.out.println("cat: '" + args[1] + "': No existe"); return; }
        if (fcb.isDirectory())    { System.out.println("cat: '" + args[1] + "': Es un directorio"); return; }
        if (!AccessControl.checkRead(fcb, terminal.getCurrentUser(), terminal.getDisk())) return;

        // Follow symlink
        if (fcb.isSymlink()) {
            path = terminal.resolvePath(fcb.getLinkTarget());
            fcb  = terminal.getDisk().getFCBByPath(path);
            if (fcb == null) { System.out.println("cat: enlace roto"); return; }
        }

        // Register in Open File Table
        terminal.getDisk().getOpenFileTable().open(fcb, terminal.getCurrentUser());

        String content = BlockManager.readContent(terminal.getDisk(), fcb);
        System.out.println(content.isEmpty() ? "(archivo vacío)" : content);

        // Close from OFT
        terminal.getDisk().getOpenFileTable().close(
            fcb.getId(), terminal.getCurrentUser(), fcb);
        terminal.saveDisk();
    }
}
