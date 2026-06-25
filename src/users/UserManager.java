package users;

import security.PasswordHasher;
import java.io.Serializable;
import java.util.*;

/**
 * UserManager - Manages users and groups for the virtual file system.
 *
 * Silberschatz Ch.13 "File-System Interface" — "Access Control":
 *   "The most general scheme to implement identity-dependent access is to associate
 *    with each file and directory an access-control list (ACL) specifying user names
 *    and the types of access allowed for each user."
 *
 * Stallings "Operating Systems" 12.7:
 *   "The most common approach for access control uses three classes:
 *    owner, specific group, all others."
 *
 * Passwords are always stored as SHA-256 hashes (see PasswordHasher).
 */
public class UserManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, User>  users  = new LinkedHashMap<>();
    private Map<String, Group> groups = new LinkedHashMap<>();

    // ── User operations ───────────────────────────────────────────────────

    public boolean addUser(String username, String fullName, String plainPassword,
                           String primaryGroup, String homeDir) {
        if (users.containsKey(username)) return false;
        String hash = PasswordHasher.hash(plainPassword);
        users.put(username, new User(username, fullName, hash, primaryGroup, homeDir));
        // Add to primary group
        Group g = groups.get(primaryGroup);
        if (g != null) g.addMember(username);
        return true;
    }

    public boolean removeUser(String username) {
        if (!users.containsKey(username)) return false;
        users.remove(username);
        for (Group g : groups.values()) g.removeMember(username);
        return true;
    }

    public User getUser(String username) { return users.get(username); }

    public boolean userExists(String username) { return users.containsKey(username); }

    public Collection<User> allUsers() { return users.values(); }

    /**
     * Authenticate a user.
     * @return true if username exists and password matches the stored hash
     */
    public boolean authenticate(String username, String plainPassword) {
        User u = users.get(username);
        if (u == null) return false;
        return PasswordHasher.verify(plainPassword, u.getPasswordHash());
    }

    public boolean changePassword(String username, String newPlainPassword) {
        User u = users.get(username);
        if (u == null) return false;
        u.setPasswordHash(PasswordHasher.hash(newPlainPassword));
        return true;
    }

    // ── Group operations ──────────────────────────────────────────────────

    public boolean addGroup(String groupName) {
        if (groups.containsKey(groupName)) return false;
        groups.put(groupName, new Group(groupName));
        return true;
    }

    public boolean removeGroup(String groupName) {
        return groups.remove(groupName) != null;
    }

    public Group getGroup(String groupName) { return groups.get(groupName); }

    public boolean groupExists(String groupName) { return groups.containsKey(groupName); }

    public Collection<Group> allGroups() { return groups.values(); }

    /** Returns true if user is a member of the given group (primary or secondary). */
    public boolean isMemberOf(String username, String groupName) {
        User u = users.get(username);
        if (u == null) return false;
        if (u.getPrimaryGroup().equals(groupName)) return true;
        Group g = groups.get(groupName);
        return g != null && g.hasMember(username);
    }

    public int userCount()  { return users.size(); }
    public int groupCount() { return groups.size(); }
}
