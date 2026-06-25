package commands;

import terminal.Terminal;

/** clear - Clear the terminal screen. */
public class ClearCommand implements Command {
    @Override public String getName()        { return "clear"; }
    @Override public String getDescription() { return "clear — limpia la pantalla"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        // ANSI escape: clear screen and move cursor to top-left
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
