package users;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Group - Represents a group of users for access control.
 *
 * Silberschatz Ch.17 "Protection":
 *   "Groups allow a set of users to share files. Each file is associated with a
 *    GID (Group Identifier) and permissions specify group-level access rights."
 *
 * Stallings "Operating Systems" 12.7:
 *   "Access classes can be: owner, specific user, group, world."
 *
 * A file's group field in the FCB identifies which group has group-level access.
 * A user is a member of exactly one primary group, and may belong to others.
 */
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String       name;    // Group name (e.g., "root", "developers")
    private List<String> members; // Usernames belonging to this group

    public Group(String name) {
        this.name    = name;
        this.members = new ArrayList<>();
    }

    public String getName() { return name; }
    public List<String> getMembers() { return members; }

    public void addMember(String username) {
        if (!members.contains(username)) members.add(username);
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean hasMember(String username) {
        return members.contains(username);
    }

    @Override
    public String toString() {
        return String.format("Group{name=%s, members=%s}", name, members);
    }
}
