package security;

import filesystem.FCB;
import users.UserManager;

/**
 * PermissionManager - Evaluates Unix-style file access permissions.
 *
 * Silberschatz Ch.17 "Protection":
 *   "Access rights: read, write, execute.
 *    Access classes: owner, group, universe.
 *    To determine access, check: is the accessor the owner? a group member? other?"
 *
 * Stallings "Operating Systems" 12.7 — "Access Control":
 *   "UNIX uses a three-class protection scheme:
 *    owner, group, world — each with read/write/execute bits."
 *
 * Permission format used here (2-digit decimal):
 *   tens digit  → owner permissions (4=read, 2=write, 1=execute)
 *   units digit → group permissions
 *
 * Example: 75 → owner=rwx (7), group=r-x (5)
 *
 * The "root" user bypasses ALL permission checks (UNIX convention).
 * If a user is neither the owner nor in the file's group, access is denied.
 */
public class PermissionManager {

    public static final int READ    = 4;
    public static final int WRITE   = 2;
    public static final int EXECUTE = 1;

    private PermissionManager() {}

    /**
     * Generic access check.
     *
     * @param fcb         file/directory to access
     * @param username    requesting user
     * @param userManager user database (for group membership)
     * @param requiredBit READ, WRITE, or EXECUTE
     * @return true if access is granted
     */
    public static boolean hasAccess(FCB fcb, String username,
                                    UserManager userManager, int requiredBit) {
        if (username == null || fcb == null) return false;

        // root bypasses all permission checks (Silberschatz: superuser)
        if ("root".equals(username)) return true;

        int perms = fcb.getPermissions();
        int ownerBits = perms / 10;
        int groupBits = perms % 10;

        if (username.equals(fcb.getOwner())) {
            return (ownerBits & requiredBit) != 0;
        }

        if (userManager.isMemberOf(username, fcb.getGroup())) {
            return (groupBits & requiredBit) != 0;
        }

        // No matching class → deny (our FS has no "others" class)
        return false;
    }

    public static boolean canRead(FCB fcb, String username, UserManager um) {
        return hasAccess(fcb, username, um, READ);
    }

    public static boolean canWrite(FCB fcb, String username, UserManager um) {
        return hasAccess(fcb, username, um, WRITE);
    }

    public static boolean canExecute(FCB fcb, String username, UserManager um) {
        return hasAccess(fcb, username, um, EXECUTE);
    }

    /**
     * Validate that a permission value is a legal 2-digit decimal (00–77).
     * Each digit must be in [0..7] (octal single digit).
     */
    public static boolean isValidPermission(int perms) {
        if (perms < 0 || perms > 77) return false;
        int owner = perms / 10;
        int group = perms % 10;
        return owner <= 7 && group <= 7;
    }
}
