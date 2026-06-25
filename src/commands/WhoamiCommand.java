package commands;

import terminal.Terminal;
import users.User;

/** whoami - Print the current user. */
public class WhoamiCommand implements Command {
    @Override public String getName()        { return "whoami"; }
    @Override public String getDescription() { return "whoami — muestra el usuario actual"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        String user = terminal.getCurrentUser();
        if (user == null) { System.out.println("No hay sesión activa."); return; }
        User u = terminal.getDisk().getUserManager().getUser(user);
        if (u != null)
            System.out.printf("%s (%s)  grupo=%s  home=%s%n",
                u.getUsername(), u.getFullName(), u.getPrimaryGroup(), u.getHomeDirectory());
        else
            System.out.println(user);
    }
}
