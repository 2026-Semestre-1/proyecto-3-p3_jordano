package commands;

import terminal.Terminal;
import users.User;
import users.UserManager;

import java.util.Scanner;

/**
 * su - Switch user (substitute user identity).
 *
 * Silberschatz Ch.13: "The user identifier (UID) with the process determines
 * which operations the process is permitted. su changes the running UID."
 *
 * Usage: su [username]  (default: root)
 */
public class SuCommand implements Command {
    @Override public String getName()        { return "su"; }
    @Override public String getDescription() { return "su [usuario] — cambia de usuario"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (terminal.getDisk() == null) {
            System.out.println("No hay disco activo. Use 'format' primero."); return;
        }
        UserManager um = terminal.getDisk().getUserManager();
        String target = args.length >= 2 ? args[1] : "root";

        if (!um.userExists(target)) {
            System.out.println("Usuario '" + target + "' no existe."); return;
        }

        Scanner sc = terminal.getScanner();
        System.out.print("Contraseña para " + target + ": ");
        String password = sc.nextLine().trim();

        if (!um.authenticate(target, password)) {
            System.out.println("Autenticación fallida."); return;
        }

        terminal.setCurrentUser(target);
        User u = um.getUser(target);
        terminal.setCurrentDirectory(u.getHomeDirectory());
        System.out.println("Bienvenido, " + u.getFullName() + " (" + target + ")");
    }
}
