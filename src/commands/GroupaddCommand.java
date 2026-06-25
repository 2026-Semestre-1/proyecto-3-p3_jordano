package commands;

import terminal.Terminal;

/** groupadd - Create a new group. Usage: groupadd <groupname> */
public class GroupaddCommand implements Command {
    @Override public String getName()        { return "groupadd"; }
    @Override public String getDescription() { return "groupadd <grupo> — crea un nuevo grupo"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (!"root".equals(terminal.getCurrentUser())) {
            System.out.println("Permiso denegado: solo root puede crear grupos."); return;
        }
        if (args.length < 2) { System.out.println("Uso: groupadd <grupo>"); return; }
        String grp = args[1];
        if (terminal.getDisk().getUserManager().addGroup(grp)) {
            terminal.saveDisk();
            System.out.println("Grupo '" + grp + "' creado.");
        } else {
            System.out.println("Error: el grupo '" + grp + "' ya existe.");
        }
    }
}
