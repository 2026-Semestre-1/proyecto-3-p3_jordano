package filesystem;

import java.io.Serializable;
import java.util.Date;

/**
 * SuperBlock - Primary metadata structure of the virtual file system.
 *
 * Stallings "Operating Systems" 12.1:
 *   "The superblock contains key information about the structure of the file system:
 *    the number of blocks, the block size, and free block information."
 *
 * Silberschatz Ch.15 "File-System Structure":
 *   "The superblock stores information about the layout of the file system on disk,
 *    including how many inodes and data blocks there are."
 *
 * Located at the beginning of the file system (conceptually block 1 after MBR).
 * Loaded into memory on mount; written back on unmount or sync.
 */
public class SuperBlock implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Magic number to verify this is a valid FS image (like EXT2 magic = 0xEF53). */
    public static final int FS_MAGIC = 0xCAFE_BABE;

    private int    magic;           // FS signature for validation
    private String fsName;          // Volume label / filesystem name
    private long   totalSize;       // Total disk size in bytes
    private int    blockSize;       // Block size in bytes (512)
    private int    totalBlocks;     // Total number of data blocks
    private int    freeBlocks;      // Current count of free blocks (kept in sync)
    private Date   creationDate;    // When this FS was formatted
    private String rootFcbId;       // UUID of the root "/" FCB

    public SuperBlock(String fsName, long totalSize, int blockSize) {
        this.magic        = FS_MAGIC;
        this.fsName       = fsName;
        this.totalSize    = totalSize;
        this.blockSize    = blockSize;
        this.totalBlocks  = (int)(totalSize / blockSize);
        this.freeBlocks   = this.totalBlocks;
        this.creationDate = new Date();
    }

    public boolean isValid()          { return magic == FS_MAGIC; }

    public int    getMagic()          { return magic; }
    public String getFsName()         { return fsName; }
    public long   getTotalSize()      { return totalSize; }
    public int    getBlockSize()      { return blockSize; }
    public int    getTotalBlocks()    { return totalBlocks; }
    public int    getFreeBlocks()     { return freeBlocks; }
    public Date   getCreationDate()   { return creationDate; }
    public String getRootFcbId()      { return rootFcbId; }

    public void setFreeBlocks(int n)  { this.freeBlocks = n; }
    public void setRootFcbId(String id) { this.rootFcbId = id; }

    @Override
    public String toString() {
        return String.format(
            "SuperBlock{\n  FS=%s\n  Tamaño=%d bytes\n  Bloques=%d (libres=%d)\n" +
            "  BlockSize=%d bytes\n  Creado=%s\n}",
            fsName, totalSize, totalBlocks, freeBlocks, blockSize, creationDate);
    }
}
