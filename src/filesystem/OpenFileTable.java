package filesystem;

import java.io.Serializable;
import java.util.*;

/**
 * OpenFileTable (OFT) - Global system-wide table of currently open files.
 *
 * Silberschatz Ch.14 "Open Files":
 *   "Several pieces of data are needed to manage open files:
 *    - File-position pointer: current position in the file (per-process).
 *    - File-open count: how many processes have this file open.
 *    - Disk location of the file (the FCB)."
 *   "When the file-open count reaches zero the entry is removed from the table."
 *
 * Stallings "Operating Systems" 12.5:
 *   "The system open-file table (as opposed to the per-process table) holds the
 *    FCB itself plus an open count. Closing the file decrements the count;
 *    only when it reaches zero are locks released."
 *
 * Implementation: a flat list is sufficient for a teaching system.
 * In production this would be a hash map keyed by (inode, mount-point) for O(1) lookup.
 */
public class OpenFileTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<OpenFileEntry> entries = new ArrayList<>();

    /** Open a file. If already open by the same user, increment reference count. */
    public void open(FCB fcb, String username) {
        for (OpenFileEntry e : entries) {
            if (e.getFcbId().equals(fcb.getId()) && e.getOpenedByUser().equals(username)) {
                e.incrementRef();
                fcb.setOpen(true);
                return;
            }
        }
        entries.add(new OpenFileEntry(fcb.getId(), fcb.getName(), fcb.getPath(), username));
        fcb.setOpen(true);
    }

    /** Close a file for a user. Decrements reference count; removes when it hits 0. */
    public void close(String fcbId, String username, FCB fcb) {
        Iterator<OpenFileEntry> it = entries.iterator();
        while (it.hasNext()) {
            OpenFileEntry e = it.next();
            if (e.getFcbId().equals(fcbId) && e.getOpenedByUser().equals(username)) {
                e.decrementRef();
                if (e.getReferenceCount() <= 0) {
                    it.remove();
                    if (fcb != null) fcb.setOpen(false);
                }
                return;
            }
        }
    }

    /** Close ALL entries for a given FCB (e.g., when the file is deleted). */
    public void forceClose(String fcbId, FCB fcb) {
        entries.removeIf(e -> e.getFcbId().equals(fcbId));
        if (fcb != null) fcb.setOpen(false);
    }

    public boolean isOpen(String fcbId) {
        return entries.stream().anyMatch(e -> e.getFcbId().equals(fcbId));
    }

    public List<OpenFileEntry> getEntries() { return Collections.unmodifiableList(entries); }

    public int totalOpen() { return entries.size(); }
}
