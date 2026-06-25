package commands;

import filesystem.FCB;
import terminal.Terminal;

/**
 * chgrp - Change file group.
 *
 * Silberschatz Ch.17: "The group identifier (GID) in the FCB determines
 * which group has group-level access to the file."
 *
 * Usage: chgrp <grupo> <archivo>
 */
public class ChgrpCommand implements Command {
    @Override public String getName()        { return "chgrp"; }
    @Override public String getDescription() { return "chgrp <grupo> <archivo> — cambia el grupo"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 3) { System.out.println("Uso: chgrp <grupo> <archivo>"); return; }

        String newGroup = args[1];
        String path     = terminal.resolvePath(args[2]);
        FCB fcb         = terminal.getDisk().getFCBByPath(path);
        String user     = terminal.getCurrentUser();

        if (fcb == null) { System.out.println("chgrp: '" + args[2] + "': No existe"); return; }
        if (!terminal.getDisk().getUserManager().groupExists(newGroup)) {
            System.out.println("chgrp: grupo '" + newGroup + "' no existe"); return;
        }
        if (!"root".equals(user) && !user.equals(fcb.getOwner())) {
            System.out.println("Permiso denegado"); return;
        }

        fcb.setGroup(newGroup);
        fcb.setModificationDate(new java.util.Date());
        terminal.saveDisk();
        System.out.println("Grupo de '" + fcb.getName() + "' cambiado a '" + newGroup + "'");
    }
}
