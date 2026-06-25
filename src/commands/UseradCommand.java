package commands;

import filesystem.FCB;
import filesystem.DirectoryEntry;
import filesystem.VirtualDisk;
import terminal.Terminal;
import users.UserManager;

import java.util.Scanner;

/**
 * useradd - Create a new user account.
 *
 * Silberschatz Ch.17: "User identities (UIDs) are used by the OS to determine
 * which user is doing what operation and which files they may access."
 *
 * Usage: useradd <username>
 * Prompts for: full name, password, group.
 * Creates home directory at /home/users/<username>.
 */
public class UseradCommand implements Command {
    @Override public String getName()        { return "useradd"; }
    @Override public String getDescription() { return "useradd <usuario> — crea un nuevo usuario"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (!"root".equals(terminal.getCurrentUser())) {
            System.out.println("Permiso denegado: solo root puede crear usuarios."); return;
        }
        if (args.length < 2) { System.out.println("Uso: useradd <usuario>"); return; }

        VirtualDisk disk = terminal.getDisk();
        UserManager um   = disk.getUserManager();
        String username  = args[1];

        if (um.userExists(username)) {
            System.out.println("Error: el usuario '" + username + "' ya existe."); return;
        }

        Scanner sc = terminal.getScanner();
        System.out.print("Nombre completo: ");
        String fullName = sc.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = sc.nextLine().trim();
        System.out.print("Grupo principal [" + username + "]: ");
        String group = sc.nextLine().trim();
        if (group.isEmpty()) group = username;

        // Ensure group exists (create if needed)
        if (!um.groupExists(group)) {
            um.addGroup(group);
            System.out.println("  Grupo '" + group + "' creado.");
        }

        String homeDir = "/home/users/" + username;
        if (!um.addUser(username, fullName, password, group, homeDir)) {
            System.out.println("Error: no se pudo crear el usuario."); return;
        }

        // Create home directory
        FCB parentDir = disk.getFCBByPath("/home/users");
        if (parentDir != null) {
            FCB userHome = new FCB(username, username, group, 77, true, homeDir);
            disk.putFCB(userHome);
            parentDir.addEntry(new DirectoryEntry(username, userHome.getId()));
        }

        terminal.saveDisk();
        System.out.println("Usuario '" + username + "' creado. Home: " + homeDir);
    }
}
