package filesystem;

import storage.FreeSpaceManager;
import users.UserManager;
import java.io.Serializable;
import java.util.*;

/**
 * VirtualDisk - Top-level container for the entire virtual file system.
 *
 * This object represents "miDiscoDuro.fs" and is serialized/deserialized
 * directly to/from that file.
 *
 * Stallings "Operating Systems" 12.1 — "Overview of File Management":
 *   "The file system has several components:
 *    - a set of files, each storing related data
 *    - a directory structure, organizing and providing information about all files
 *    - a partition (volume) that is the portion of the disk handled by one FS"
 *
 * Silberschatz Ch.15 "File-System Structure":
 *   "The on-disk structure includes: a boot control block (MBR), a volume control
 *    block (super block), per-file FCBs (inodes), and data blocks."
 *
 * Architecture layers within VirtualDisk:
 *   1. MBR             – disk identification and partition info
 *   2. SuperBlock       – filesystem parameters
 *   3. FreeSpaceManager – bitmap of free/occupied blocks
 *   4. fcbTable         – all FCBs indexed by UUID
 *   5. blocks           – raw data blocks (byte[totalBlocks][BLOCK_SIZE])
 *   6. UserManager      – users and groups
 *   7. OpenFileTable    – currently-open files
 */
public class VirtualDisk implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Block size in bytes — standard sector size (Stallings recommends 512–4096). */
    public static final int BLOCK_SIZE = 512;

    // ── Disk structures ────────────────────────────────────────────────────
    private MBR              mbr;
    private SuperBlock       superBlock;
    private FreeSpaceManager freeSpaceManager;

    // ── FCB table: UUID → FCB (all files and directories) ─────────────────
    private Map<String, FCB> fcbTable;

    // ── Raw data blocks (indexed allocation) ─────────────────────────────
    private byte[][] blocks;

    // ── Supporting subsystems ─────────────────────────────────────────────
    private UserManager    userManager;
    private OpenFileTable  openFileTable;

    // ── Root directory FCB UUID ───────────────────────────────────────────
    private String rootFcbId;

    // ── Constructor ────────────────────────────────────────────────────────
    public VirtualDisk(String volumeName, long totalSize) {
        int totalBlocks = (int)(totalSize / BLOCK_SIZE);

        this.mbr              = new MBR(volumeName, totalSize, totalBlocks);
        this.superBlock       = new SuperBlock(volumeName, totalSize, BLOCK_SIZE);
        this.freeSpaceManager = new FreeSpaceManager(totalBlocks);
        this.fcbTable         = new LinkedHashMap<>();
        this.blocks           = new byte[totalBlocks][BLOCK_SIZE];
        this.userManager      = new UserManager();
        this.openFileTable    = new OpenFileTable();
    }

    // ── Block I/O ──────────────────────────────────────────────────────────

    public byte[] readBlock(int index) {
        if (index < 0 || index >= blocks.length) return new byte[BLOCK_SIZE];
        return Arrays.copyOf(blocks[index], BLOCK_SIZE);
    }

    public void writeBlock(int index, byte[] data) {
        if (index < 0 || index >= blocks.length) return;
        int len = Math.min(data.length, BLOCK_SIZE);
        System.arraycopy(data, 0, blocks[index], 0, len);
        // Zero-fill the rest of the block if data is shorter
        if (len < BLOCK_SIZE) Arrays.fill(blocks[index], len, BLOCK_SIZE, (byte)0);
    }

    // ── FCB table operations ───────────────────────────────────────────────

    public void putFCB(FCB fcb) {
        fcbTable.put(fcb.getId(), fcb);
    }

    public FCB getFCB(String id) {
        return fcbTable.get(id);
    }

    public boolean removeFCB(String id) {
        return fcbTable.remove(id) != null;
    }

    public Map<String, FCB> getFcbTable() { return fcbTable; }

    // ── Path resolution (walks directory tree) ─────────────────────────────

    /**
     * Resolve an absolute virtual path to an FCB.
     * @param path absolute path like "/home/alice/notes.txt"
     * @return the FCB at that path, or null if not found
     */
    public FCB getFCBByPath(String path) {
        if (path == null) return null;
        path = normalizePath(path);
        if (path.equals("/")) return fcbTable.get(rootFcbId);

        FCB current = fcbTable.get(rootFcbId);
        if (current == null) return null;

        String[] parts = path.substring(1).split("/");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!current.isDirectory()) return null;
            DirectoryEntry entry = current.findEntry(part);
            if (entry == null) return null;
            current = fcbTable.get(entry.getFcbId());
            if (current == null) return null;
        }
        return current;
    }

    /**
     * Get the parent directory FCB for a given absolute path.
     * E.g., "/home/alice/notes.txt" → FCB for "/home/alice"
     */
    public FCB getParentFCB(String path) {
        path = normalizePath(path);
        if (path.equals("/")) return null;
        int lastSlash = path.lastIndexOf('/');
        String parentPath = lastSlash == 0 ? "/" : path.substring(0, lastSlash);
        return getFCBByPath(parentPath);
    }

    /** Normalize path: collapse ".", "..", remove double slashes. */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        String[] parts = path.split("/");
        Deque<String> stack = new ArrayDeque<>();
        for (String p : parts) {
            if (p.isEmpty() || p.equals(".")) continue;
            if (p.equals("..")) { if (!stack.isEmpty()) stack.pop(); }
            else stack.push(p);
        }
        if (stack.isEmpty()) return "/";
        List<String> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return "/" + String.join("/", list);
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public MBR              getMbr()              { return mbr; }
    public SuperBlock       getSuperBlock()       { return superBlock; }
    public FreeSpaceManager getFreeSpaceManager() { return freeSpaceManager; }
    public UserManager      getUserManager()      { return userManager; }
    public OpenFileTable    getOpenFileTable()    { return openFileTable; }
    public String           getRootFcbId()        { return rootFcbId; }
    public int              getBlockCount()       { return blocks.length; }

    public void setRootFcbId(String id) {
        this.rootFcbId = id;
        superBlock.setRootFcbId(id);
    }

    /** Sync superblock free-block counter with the bitmap. */
    public void syncSuperBlock() {
        superBlock.setFreeBlocks(freeSpaceManager.getFreeCount());
    }
}
