package storage;

import filesystem.FCB;
import filesystem.VirtualDisk;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * BlockManager - High-level helpers for reading/writing file content via blocks.
 *
 * Stallings "Operating Systems" 12.4 — "Indexed Allocation":
 *   "Each file has its own index block, which is an array of disk-block addresses.
 *    The i-th entry in the index block points to the i-th block of the file."
 *
 * Silberschatz Ch.15 — "Indexed Allocation":
 *   "With indexed allocation, each file has an index block which is a collection
 *    of disk-block pointers. To access block i of a file, use the i-th pointer
 *    in the index block."
 *
 * Here the index is the FCB's `blocks` list (List<Integer>).
 * This class translates between "file content as String/bytes" and
 * "raw content scattered across physical data blocks".
 */
public class BlockManager {

    private BlockManager() {}

    /**
     * Read the entire text content of a file from its allocated blocks.
     *
     * Uses the FCB's block list as the index (indexed allocation).
     * Reads exactly fcb.getSize() bytes to avoid reading garbage padding.
     */
    public static String readContent(VirtualDisk disk, FCB fcb) {
        if (fcb.getSize() == 0 || fcb.getBlocks().isEmpty()) return "";
        byte[] buffer = new byte[(int) fcb.getSize()];
        int cursor = 0;
        int blockSize = VirtualDisk.BLOCK_SIZE;

        for (int blockIdx : fcb.getBlocks()) {
            if (cursor >= fcb.getSize()) break;
            byte[] block = disk.readBlock(blockIdx);
            int toCopy = (int) Math.min(blockSize, fcb.getSize() - cursor);
            System.arraycopy(block, 0, buffer, cursor, toCopy);
            cursor += toCopy;
        }
        return new String(buffer, 0, cursor, StandardCharsets.UTF_8);
    }

    /**
     * Write text content to a file, updating its allocated blocks.
     *
     * Steps (Silberschatz indexed allocation write):
     *   1. Free all currently allocated blocks (return to bitmap).
     *   2. Compute how many new blocks are needed.
     *   3. Allocate new blocks from the free-space manager.
     *   4. Write content bytes across those blocks.
     *   5. Update FCB: size, block list, modificationDate.
     *
     * Returns false if the disk is full.
     */
    public static boolean writeContent(VirtualDisk disk, FCB fcb, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int blockSize = VirtualDisk.BLOCK_SIZE;

        // 1. Free existing blocks
        disk.getFreeSpaceManager().free(fcb.getBlocks());
        fcb.getBlocks().clear();

        if (bytes.length == 0) {
            fcb.setSize(0);
            fcb.setModificationDate(new java.util.Date());
            return true;
        }

        // 2. Calculate required blocks
        int numBlocks = (bytes.length + blockSize - 1) / blockSize;

        // 3. Allocate new blocks
        List<Integer> newBlocks = disk.getFreeSpaceManager().allocate(numBlocks);
        if (newBlocks == null) return false; // disk full

        // 4. Write bytes across blocks
        for (int i = 0; i < newBlocks.size(); i++) {
            byte[] block = new byte[blockSize];
            int srcPos = i * blockSize;
            int length = Math.min(blockSize, bytes.length - srcPos);
            System.arraycopy(bytes, srcPos, block, 0, length);
            disk.writeBlock(newBlocks.get(i), block);
        }

        // 5. Update FCB
        fcb.setBlocks(newBlocks);
        fcb.setSize(bytes.length);
        fcb.setModificationDate(new java.util.Date());
        disk.syncSuperBlock();
        return true;
    }

    /**
     * Release all blocks held by an FCB (used when deleting a file).
     */
    public static void releaseBlocks(VirtualDisk disk, FCB fcb) {
        disk.getFreeSpaceManager().free(fcb.getBlocks());
        fcb.getBlocks().clear();
        fcb.setSize(0);
        disk.syncSuperBlock();
    }

    /**
     * Create an empty file (zero blocks allocated, size 0).
     */
    public static void initEmpty(FCB fcb) {
        fcb.getBlocks().clear();
        fcb.setSize(0);
    }
}
