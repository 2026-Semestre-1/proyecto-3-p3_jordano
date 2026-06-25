package commands;

import filesystem.FCB;
import security.PermissionManager;
import terminal.Terminal;

/**
 * chmod - Change file permissions.
 *
 * Stallings "Operating Systems" 12.7:
 *   "Permissions are encoded as bit fields per access class.
 *    chmod changes the permission bits stored in the FCB."
 *
 * Silberschatz Ch.17:
 *   "chmod modifies the protection bits associated with a file.
 *    Only the owner or root may change file permissions."
 *
 * Permission format: 2-digit decimal (owner digit, group digit).
 *   Each digit: 4=read, 2=write, 1=execute (additive).
 *   E.g.: chmod 77 file → owner=rwx, group=rwx
 *         chmod 75 file → owner=rwx, group=r-x
 *         chmod 44 file → owner=r--, group=r--
 *
 * Usage: chmod <permisos> <archivo>
 */
public class ChmodCommand implements Command {
    @Override public String getName()        { return "chmod"; }
    @Override public String getDescription() { return "chmod <permisos> <archivo> — cambia permisos (ej: 75)"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 3) { System.out.println("Uso: chmod <permisos> <archivo>"); return; }

        int perms;
        try {
            perms = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("chmod: permisos inválidos (use formato: 77, 75, 44, etc.)"); return;
        }

        if (!PermissionManager.isValidPermission(perms)) {
            System.out.println("chmod: permiso inválido '" + args[1] +
                "' (cada dígito debe ser 0-7)"); return;
        }

        String path = terminal.resolvePath(args[2]);
        FCB fcb = terminal.getDisk().getFCBByPath(path);
        String user = terminal.getCurrentUser();

        if (fcb == null) { System.out.println("chmod: '" + args[2] + "': No existe"); return; }
        if (!"root".equals(user) && !user.equals(fcb.getOwner())) {
            System.out.println("Permiso denegado: solo el dueño o root pueden usar chmod"); return;
        }

        int old = fcb.getPermissions();
        fcb.setPermissions(perms);
        fcb.setModificationDate(new java.util.Date());
        terminal.saveDisk();

        System.out.printf("Permisos de '%s' cambiados: %d (%s) → %d (%s)%n",
            fcb.getName(), old, permStr(old), perms, permStr(perms));
    }

    private String permStr(int p) {
        int o = p / 10, g = p % 10;
        return bits(o) + bits(g);
    }

    private String bits(int p) {
        return ((p&4)!=0?"r":"-") + ((p&2)!=0?"w":"-") + ((p&1)!=0?"x":"-");
    }
}
