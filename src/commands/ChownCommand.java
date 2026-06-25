package commands;

import filesystem.FCB;
import terminal.Terminal;

/**
 * chown - Change file owner.
 *
 * Silberschatz Ch.17: "File ownership is stored in the FCB.
 * Only the current owner or root may transfer ownership."
 *
 * Usage: chown <owner> <file>
 */
public class ChownCommand implements Command {
    @Override public String getName()        { return "chown"; }
    @Override public String getDescription() { return "chown <propietario> <archivo> — cambia el dueño"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 3) { System.out.println("Uso: chown <propietario> <archivo>"); return; }

        String newOwner = args[1];
        String path     = terminal.resolvePath(args[2]);
        FCB fcb         = terminal.getDisk().getFCBByPath(path);
        String user     = terminal.getCurrentUser();

        if (fcb == null) { System.out.println("chown: '" + args[2] + "': No existe"); return; }
        if (!terminal.getDisk().getUserManager().userExists(newOwner)) {
            System.out.println("chown: usuario '" + newOwner + "' no existe"); return;
        }
        if (!"root".equals(user) && !user.equals(fcb.getOwner())) {
            System.out.println("Permiso denegado: solo root o el dueño pueden usar chown"); return;
        }

        fcb.setOwner(newOwner);
        fcb.setModificationDate(new java.util.Date());
        terminal.saveDisk();
        System.out.println("Propietario de '" + fcb.getName() + "' cambiado a '" + newOwner + "'");
    }
}
