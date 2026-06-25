package storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FreeSpaceManager - Tracks free/occupied disk blocks using a BITMAP.
 *
 * Stallings "Operating Systems" 12.4 — "Free Space Management":
 *   "A bit map or bit vector uses one bit per block. If the block is free the
 *    bit is 0; if allocated, the bit is 1.
 *    With a bit map: finding the first free block requires only scanning the
 *    bitmap. The advantage of the bit map is its simplicity and efficiency
 *    in finding free blocks."
 *
 * Silberschatz Ch.15 "Free-Space Management":
 *   "The file system maintains a free-space list to track all free disk blocks.
 *    The bit vector technique: each bit represents one block.
 *    bit[i] = 0 → block i is free
 *    bit[i] = 1 → block i is occupied"
 *
 * ─── BITMAP EXAMPLE ──────────────────────────────────────────────────────────
 *
 *   Block:  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14
 *   Bit:    1  1  1  1  1  0  0  0  0  0  1  1  1  1  1
 *              ^^^^^^^^^^^^^ FREE REGION ^^^^^^^^^^^^^^
 *
 *   1 = occupied (allocated to a file or reserved)
 *   0 = free
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class FreeSpaceManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean[] bitmap;    // false = free, true = occupied
    private int totalBlocks;
    private int freeCount;

    public FreeSpaceManager(int totalBlocks) {
        this.totalBlocks = totalBlocks;
        this.bitmap      = new boolean[totalBlocks]; // all false (free)
        this.freeCount   = totalBlocks;
    }

    /**
     * Allocate `count` free blocks (not necessarily contiguous).
     *
     * Returns a list of block indices, or null if not enough free blocks.
     * Non-contiguous allocation is acceptable with indexed allocation since
     * the FCB tracks each block individually (Stallings 12.4).
     */
    public List<Integer> allocate(int count) {
        if (freeCount < count) return null;
        List<Integer> allocated = new ArrayList<>(count);
        for (int i = 0; i < totalBlocks && allocated.size() < count; i++) {
            if (!bitmap[i]) {
                bitmap[i] = true;
                allocated.add(i);
                freeCount--;
            }
        }
        return allocated;
    }

    /**
     * Allocate exactly one free block.
     * @return block index, or -1 if disk is full
     */
    public int allocateOne() {
        for (int i = 0; i < totalBlocks; i++) {
            if (!bitmap[i]) {
                bitmap[i] = true;
                freeCount--;
                return i;
            }
        }
        return -1;
    }

    /**
     * Free (release) a list of blocks back to the free pool.
     * Silberschatz: "A file is deleted by returning its blocks to the free-space list."
     */
    public void free(List<Integer> blocks) {
        for (int idx : blocks) {
            if (idx >= 0 && idx < totalBlocks && bitmap[idx]) {
                bitmap[idx] = false;
                freeCount++;
            }
        }
    }

    /** Free a single block. */
    public void freeOne(int idx) {
        if (idx >= 0 && idx < totalBlocks && bitmap[idx]) {
            bitmap[idx] = false;
            freeCount++;
        }
    }

    public boolean isFree(int idx) {
        return idx >= 0 && idx < totalBlocks && !bitmap[idx];
    }

    public int getFreeCount()  { return freeCount; }
    public int getTotalBlocks(){ return totalBlocks; }
    public int getUsedBlocks() { return totalBlocks - freeCount; }

    /**
     * Returns a visual bitmap string (up to 80 bits shown).
     * '0' = free, '1' = occupied.
     * Mirrors the Stallings/Silberschatz textbook notation.
     */
    public String getBitmapString() {
        int show = Math.min(totalBlocks, 80);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < show; i++) sb.append(bitmap[i] ? '1' : '0');
        if (totalBlocks > show) sb.append("...(+" + (totalBlocks - show) + " más)");
        return sb.toString();
    }
}
