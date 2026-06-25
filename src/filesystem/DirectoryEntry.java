package filesystem;

import java.io.Serializable;

/**
 * DirectoryEntry - Represents a single entry within a directory.
 *
 * Stallings "Operating Systems" 12.3:
 *   "A directory entry provides the mapping from a file name to the file itself."
 *   The entry contains the file name and a pointer to the FCB (or inode number).
 *
 * Silberschatz Ch.14:
 *   "Directory structure can be implemented as a linear list of file names
 *    with pointers to the data blocks."
 *
 * Each directory FCB contains a List<DirectoryEntry> mapping names to FCB IDs.
 * This separates naming (in directory) from metadata (in FCB), mirroring UNIX inodes.
 */
public class DirectoryEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Display name of the file/directory in this directory. */
    private String name;

    /** UUID of the FCB that describes this file/directory. */
    private String fcbId;

    public DirectoryEntry(String name, String fcbId) {
        this.name = name;
        this.fcbId = fcbId;
    }

    public String getName()  { return name; }
    public String getFcbId() { return fcbId; }
    public void setName(String name) { this.name = name; }
}
