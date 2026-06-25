package filesystem;

import java.io.Serializable;
import java.util.Date;

/**
 * OpenFileEntry - A single record in the global Open File Table.
 *
 * Silberschatz Ch.14 "Open Files":
 *   "The system-wide open-file table contains a copy of the FCB of each open file,
 *    as well as other information: the file-open count."
 *
 * Stallings "Operating Systems" 12.5:
 *   "The system open-file table maintains per-open-file state: which process
 *    opened it, whether the file is locked, and the current file position."
 *
 * Here we track: which FCB, which user opened it, when it was opened,
 * and a reference counter to handle the case where multiple terminals/processes
 * have the same file open simultaneously.
 */
public class OpenFileEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fcbId;           // UUID of the open FCB
    private String filename;        // Cached name for display
    private String filepath;        // Full virtual path
    private String openedByUser;    // Username who opened the file
    private Date   openDate;        // When it was opened
    private int    referenceCount;  // How many terminals currently have it open

    public OpenFileEntry(String fcbId, String filename, String filepath, String user) {
        this.fcbId          = fcbId;
        this.filename       = filename;
        this.filepath       = filepath;
        this.openedByUser   = user;
        this.openDate       = new Date();
        this.referenceCount = 1;
    }

    public String getFcbId()         { return fcbId; }
    public String getFilename()      { return filename; }
    public String getFilepath()      { return filepath; }
    public String getOpenedByUser()  { return openedByUser; }
    public Date   getOpenDate()      { return openDate; }
    public int    getReferenceCount(){ return referenceCount; }

    public void incrementRef()       { referenceCount++; }
    public void decrementRef()       { if (referenceCount > 0) referenceCount--; }

    @Override
    public String toString() {
        return String.format("%-40s  %-10s  %s  (refs=%d)",
            filepath, openedByUser, openDate, referenceCount);
    }
}
