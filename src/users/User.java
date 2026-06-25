package users;

import java.io.Serializable;

/**
 * User - Represents a system user.
 *
 * Silberschatz Ch.17 "Protection":
 *   "Each user in the system is identified by a unique UID (User Identifier).
 *    Files are owned by users; the owner field in the FCB stores this UID."
 *
 * Stallings "Operating Systems" 12.7:
 *   "User authentication involves verifying the identity of the user through
 *    passwords stored in a secured, hashed form."
 *
 * Passwords are NEVER stored in plain text; only SHA-256 hashes are kept.
 * This matches Unix /etc/shadow behavior.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;        // Login name (e.g., "root", "alice")
    private String fullName;        // Display name
    private String passwordHash;    // SHA-256 hash of the password
    private String primaryGroup;    // Primary group name
    private String homeDirectory;   // Absolute path to home dir (e.g., "/root")
    private boolean isRoot;         // true only for the superuser

    public User(String username, String fullName, String passwordHash,
                String primaryGroup, String homeDirectory) {
        this.username      = username;
        this.fullName      = fullName;
        this.passwordHash  = passwordHash;
        this.primaryGroup  = primaryGroup;
        this.homeDirectory = homeDirectory;
        this.isRoot        = "root".equals(username);
    }

    public String getUsername()       { return username; }
    public String getFullName()       { return fullName; }
    public String getPasswordHash()   { return passwordHash; }
    public String getPrimaryGroup()   { return primaryGroup; }
    public String getHomeDirectory()  { return homeDirectory; }
    public boolean isRoot()           { return isRoot; }

    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void setFullName(String name)     { this.fullName = name; }
    public void setPrimaryGroup(String g)    { this.primaryGroup = g; }
    public void setHomeDirectory(String h)   { this.homeDirectory = h; }

    @Override
    public String toString() {
        return String.format("User{username=%s, fullName=%s, group=%s, home=%s}",
            username, fullName, primaryGroup, homeDirectory);
    }
}
