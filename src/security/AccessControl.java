package security;

import filesystem.FCB;
import filesystem.VirtualDisk;

/**
 * AccessControl - Convenience wrapper that prints errors on denied access.
 *
 * Silberschatz Ch.17: "If the desired access is not allowed, the operation
 * is denied and an error message is generated."
 */
public class AccessControl {

    private AccessControl() {}

    public static boolean checkRead(FCB fcb, String user, VirtualDisk disk) {
        if (PermissionManager.canRead(fcb, user, disk.getUserManager())) return true;
        System.out.println("Permiso denegado: " + user + " no tiene permiso de lectura en '"
            + fcb.getPath() + "'");
        return false;
    }

    public static boolean checkWrite(FCB fcb, String user, VirtualDisk disk) {
        if (PermissionManager.canWrite(fcb, user, disk.getUserManager())) return true;
        System.out.println("Permiso denegado: " + user + " no tiene permiso de escritura en '"
            + fcb.getPath() + "'");
        return false;
    }

    public static boolean checkExecute(FCB fcb, String user, VirtualDisk disk) {
        if (PermissionManager.canExecute(fcb, user, disk.getUserManager())) return true;
        System.out.println("Permiso denegado: " + user + " no tiene permiso de ejecución en '"
            + fcb.getPath() + "'");
        return false;
    }
}
