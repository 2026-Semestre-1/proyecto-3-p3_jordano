package commands;

import terminal.Terminal;
import users.UserManager;

import java.util.Scanner;

/**
 * passwd - Change a user's password.
 *
 * Silberschatz Ch.17: "Authentication involves proving identity.
 * The most common technique is a password stored as a one-way hash."
 *
 * Usage: passwd [username]
 * Without argument: changes the current user's own password.
 * Root can change any user's password.
 */
public class PasswdCommand implements Command {
    @Override public String getName()        { return "passwd"; }
    @Override public String getDescription() { return "passwd [usuario] — cambia contraseña"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        UserManager um   = terminal.getDisk().getUserManager();
        String self      = terminal.getCurrentUser();
        String target    = args.length >= 2 ? args[1] : self;

        // Only root can change another user's password
        if (!target.equals(self) && !"root".equals(self)) {
            System.out.println("Permiso denegado."); return;
        }
        if (!um.userExists(target)) {
            System.out.println("Usuario '" + target + "' no existe."); return;
        }

        Scanner sc = terminal.getScanner();

        // Non-root must verify current password
        if (!"root".equals(self)) {
            System.out.print("Contraseña actual: ");
            String current = sc.nextLine().trim();
            if (!um.authenticate(self, current)) {
                System.out.println("Contraseña incorrecta."); return;
            }
        }

        System.out.print("Nueva contraseña: ");
        String p1 = sc.nextLine().trim();
        System.out.print("Confirmar contraseña: ");
        String p2 = sc.nextLine().trim();

        if (!p1.equals(p2)) { System.out.println("Las contraseñas no coinciden."); return; }
        if (p1.isEmpty())    { System.out.println("La contraseña no puede estar vacía."); return; }

        um.changePassword(target, p1);
        terminal.saveDisk();
        System.out.println("Contraseña de '" + target + "' actualizada.");
    }
}
