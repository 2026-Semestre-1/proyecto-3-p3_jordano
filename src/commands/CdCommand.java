package commands;

import filesystem.FCB;
import security.AccessControl;
import terminal.Terminal;

/** cd - Change current working directory. */
public class CdCommand implements Command {
    @Override public String getName()        { return "cd"; }
    @Override public String getDescription() { return "cd [dir] — cambia el directorio actual"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        String target = args.length >= 2 ? args[1] : "~";
        String path   = terminal.resolvePath(target);
        FCB fcb       = terminal.getDisk().getFCBByPath(path);

        if (fcb == null) {
            System.out.println("cd: '" + target + "': No existe el directorio"); return;
        }
        if (!fcb.isDirectory()) {
            System.out.println("cd: '" + target + "': No es un directorio"); return;
        }
        if (!AccessControl.checkExecute(fcb, terminal.getCurrentUser(), terminal.getDisk())) return;

        terminal.setCurrentDirectory(path);
    }
}
