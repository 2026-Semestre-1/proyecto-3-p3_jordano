package storage;

import filesystem.VirtualDisk;
import java.io.*;

/**
 * DiskPersistence - Serializes and deserializes the VirtualDisk to/from a file.
 *
 * Stallings "Operating Systems" 12.1:
 *   "Permanence: file data persists even after the creating process terminates
 *    and is accessible to other processes according to access rights."
 *
 * Silberschatz Ch.15 "File-System Implementation":
 *   "On reboot or remount, the on-disk structures are read back into memory,
 *    restoring the file system to the state at last sync."
 *
 * We use Java Object Serialization as the "on-disk format". The entire VirtualDisk
 * object graph (SuperBlock, MBR, FCB table, data blocks, users, open-file table)
 * is written atomically to miDiscoDuro.fs.
 *
 * In a production system you would write raw binary blocks; here serialization
 * cleanly demonstrates the persistence concept for the academic context.
 */
public class DiskPersistence {

    private DiskPersistence() {}

    /**
     * Persist the disk to a file.
     * @param disk     the VirtualDisk instance to save
     * @param filePath path to the .fs file (e.g., "miDiscoDuro.fs")
     * @return true on success
     */
    public static boolean save(VirtualDisk disk, String filePath) {
        disk.syncSuperBlock();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                 new BufferedOutputStream(new FileOutputStream(filePath)))) {
            oos.writeObject(disk);
            return true;
        } catch (IOException e) {
            System.err.println("[DiskPersistence] Error al guardar disco: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load a VirtualDisk from a file.
     * @param filePath path to the .fs file
     * @return the loaded VirtualDisk, or null if loading fails
     */
    public static VirtualDisk load(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(
                 new BufferedInputStream(new FileInputStream(filePath)))) {
            return (VirtualDisk) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[DiskPersistence] Error al cargar disco: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check whether a disk file already exists.
     */
    public static boolean diskExists(String filePath) {
        return new File(filePath).exists();
    }

    /** Return the size of the .fs file on the host OS. */
    public static long fileSize(String filePath) {
        File f = new File(filePath);
        return f.exists() ? f.length() : 0L;
    }
}
