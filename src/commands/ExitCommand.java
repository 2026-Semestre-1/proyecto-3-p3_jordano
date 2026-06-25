package commands;

import terminal.Terminal;

/** exit - Close this terminal session. */
public class ExitCommand implements Command {
    @Override public String getName()        { return "exit"; }
    @Override public String getDescription() { return "Cierra la sesión actual del terminal"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (terminal.getDisk() != null) {
            terminal.saveDisk();
            System.out.println("Disco guardado.");
        }
        System.out.println("Sesión cerrada. ¡Hasta luego!");
        terminal.stop();
    }
}
