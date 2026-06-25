package commands;

import terminal.Terminal;

/** pwd - Print working directory. */
public class PwdCommand implements Command {
    @Override public String getName()        { return "pwd"; }
    @Override public String getDescription() { return "pwd — muestra el directorio actual"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        System.out.println(terminal.getCurrentDirectory());
    }
}
