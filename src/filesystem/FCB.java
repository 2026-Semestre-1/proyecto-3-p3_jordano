package filesystem;

import java.io.Serializable;
import java.util.*;

/**
 * FCB (File Control Block) - Core metadata structure for every file and directory.
 *
 * Stallings "Operating Systems" 12.2 — "File Attributes":
 *   "The FCB (inode in Unix) contains all information the OS needs to manage a file:
 *    owner, permissions, size, timestamps, and the location of data blocks."
 *
 * Silberschatz Ch.14 "File-System Implementation":
 *   "Each file is represented by an inode (index node). The inode contains all
 *    metadata about the file except its name (the name lives in the directory entry)."
 *
 * ─── INDEXED ALLOCATION ───────────────────────────────────────────────────────
 * Choice: Indexed Allocation (cf. Stallings 12.4, Silberschatz 14.4).
 *
 *   Advantages over contiguous allocation:
 *     - No external fragmentation.
 *     - File can grow dynamically without reservation.
 *   Advantages over linked allocation:
 *     - O(1) random-block access (no traversal of a pointer chain).
 *     - No wasted space for next-block pointers inside data blocks.
 *   Trade-off: index block overhead (negligible for small files).
 *
 *   Each FCB stores `List<Integer> blocks`, an explicit list of block indices.
 *   This mirrors the Unix inode's direct block pointer array.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class FCB implements Serializable {
    private static final long serialVersionUID = 1L;

    // ── Identity ──────────────────────────────────────────────────────────
    private String id;              // UUID — analogous to inode number
    private String name;            // File/dir name (kept here for convenience)
    private String path;            // Absolute virtual path

    // ── Ownership & Security (Silberschatz Ch.17) ─────────────────────────
    private String owner;           // Owning username
    private String group;           // Owning group name
    /**
     * Unix-style 2-digit octal permissions:
     *   tens digit = owner (4=r, 2=w, 1=x)
     *   units digit = group (4=r, 2=w, 1=x)
     * E.g., 77 = rwxrwx,  75 = rwxr-x,  44 = r--r--
     */
    private int permissions;

    // ── Attributes (Stallings Table 12.1) ────────────────────────────────
    private long size;              // Content size in bytes
    private Date creationDate;
    private Date modificationDate;
    private boolean isOpen;         // Currently held in Open File Table?
    private boolean isDirectory;    // true → directory; false → regular file
    private boolean isSymlink;      // Symbolic link?
    private String  linkTarget;     // Symlink target path (null if not symlink)

    // ── Indexed Allocation (Stallings 12.4) ──────────────────────────────
    /** List of data block indices belonging to this file (indexed allocation). */
    private List<Integer> blocks;

    // ── Directory Contents ───────────────────────────────────────────────
    /** Child entries — valid only when isDirectory == true. */
    private List<DirectoryEntry> entries;

    // ── Constructor ──────────────────────────────────────────────────────
    public FCB(String name, String owner, String group, int permissions,
               boolean isDirectory, String path) {
        this.id               = UUID.randomUUID().toString();
        this.name             = name;
        this.owner            = owner;
        this.group            = group;
        this.permissions      = permissions;
        this.size             = 0;
        this.creationDate     = new Date();
        this.modificationDate = new Date();
        this.isOpen           = false;
        this.isDirectory      = isDirectory;
        this.isSymlink        = false;
        this.linkTarget       = null;
        this.path             = path;
        this.blocks           = new ArrayList<>();
        this.entries          = isDirectory ? new ArrayList<>() : null;
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public String  getId()               { return id; }
    public String  getName()             { return name; }
    public String  getPath()             { return path; }
    public String  getOwner()            { return owner; }
    public String  getGroup()            { return group; }
    public int     getPermissions()      { return permissions; }
    public long    getSize()             { return size; }
    public Date    getCreationDate()     { return creationDate; }
    public Date    getModificationDate() { return modificationDate; }
    public boolean isOpen()              { return isOpen; }
    public boolean isDirectory()         { return isDirectory; }
    public boolean isSymlink()           { return isSymlink; }
    public String  getLinkTarget()       { return linkTarget; }
    public List<Integer>       getBlocks()  { return blocks; }
    public List<DirectoryEntry> getEntries(){ return entries; }

    // ── Setters ───────────────────────────────────────────────────────────
    public void setName(String n)              { this.name = n; }
    public void setPath(String p)              { this.path = p; }
    public void setOwner(String o)             { this.owner = o; }
    public void setGroup(String g)             { this.group = g; }
    public void setPermissions(int p)          { this.permissions = p; }
    public void setSize(long s)                { this.size = s; }
    public void setModificationDate(Date d)    { this.modificationDate = d; }
    public void setOpen(boolean b)             { this.isOpen = b; }
    public void setSymlink(boolean b)          { this.isSymlink = b; }
    public void setLinkTarget(String t)        { this.linkTarget = t; }
    public void setBlocks(List<Integer> bl)    { this.blocks = bl; }

    // ── Directory helpers ─────────────────────────────────────────────────
    public void addEntry(DirectoryEntry e) {
        if (isDirectory && entries != null) entries.add(e);
    }

    public void removeEntry(String entryName) {
        if (isDirectory && entries != null)
            entries.removeIf(e -> e.getName().equals(entryName));
    }

    public DirectoryEntry findEntry(String entryName) {
        if (!isDirectory || entries == null) return null;
        return entries.stream()
            .filter(e -> e.getName().equals(entryName))
            .findFirst().orElse(null);
    }

    // ── Permission display ────────────────────────────────────────────────
    /**
     * Formats permissions as "rwxrwx"-style string.
     * E.g., permissions=75 → "rwxr-x"
     */
    public String getPermissionsString() {
        int owner = permissions / 10;
        int grp   = permissions % 10;
        return permBits(owner) + permBits(grp);
    }

    private static String permBits(int p) {
        return ((p & 4) != 0 ? "r" : "-")
             + ((p & 2) != 0 ? "w" : "-")
             + ((p & 1) != 0 ? "x" : "-");
    }

    // ── Type indicator for ls ─────────────────────────────────────────────
    public String typeTag() {
        if (isSymlink)   return "[L]";
        if (isDirectory) return "[D]";
        return "[F]";
    }

    @Override
    public String toString() {
        return String.format("%s %s  %-12s  %-12s  %8d bytes  %s  %s",
            typeTag(), getPermissionsString(), owner, group,
            size, modificationDate, name);
    }
}
