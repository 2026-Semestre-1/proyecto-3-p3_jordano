package filesystem;

import java.io.Serializable;
import java.util.Date;

/**
 * MBR (Master Boot Record) - First logical structure of the virtual disk.
 *
 * Stallings "Operating Systems":
 *   "The first sector of a disk typically contains the MBR which identifies
 *    the partition table and the boot sector for the active partition."
 *
 * Silberschatz Ch.15:
 *   "The MBR (sector 0) is used to boot the computer. The end of the MBR
 *    contains a partition table. One partition is marked as active."
 *
 * In our virtual disk, the MBR is the first metadata structure.
 * It holds the volume label, total size, creation date, and a magic signature.
 * It points to the SuperBlock which contains detailed FS layout information.
 */
public class MBR implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Boot signature — analogous to the 0x55AA at the end of a real MBR. */
    public static final String SIGNATURE = "MBR-VFS-1.0";

    private String volumeName;    // Human-readable volume name
    private long   totalDiskSize; // Total capacity of the virtual disk in bytes
    private Date   creationDate;  // Timestamp when the disk was formatted
    private String signature;     // Integrity check string
    private int    partitionStart;// Starting block of the main partition (always 0 here)
    private int    partitionSize; // Number of blocks in the partition

    public MBR(String volumeName, long totalDiskSize, int totalBlocks) {
        this.volumeName     = volumeName;
        this.totalDiskSize  = totalDiskSize;
        this.creationDate   = new Date();
        this.signature      = SIGNATURE;
        this.partitionStart = 0;
        this.partitionSize  = totalBlocks;
    }

    public boolean isValid()         { return SIGNATURE.equals(signature); }

    public String getVolumeName()    { return volumeName; }
    public long   getTotalDiskSize() { return totalDiskSize; }
    public Date   getCreationDate()  { return creationDate; }
    public String getSignature()     { return signature; }
    public int    getPartitionStart(){ return partitionStart; }
    public int    getPartitionSize() { return partitionSize; }

    @Override
    public String toString() {
        return String.format(
            "MBR{\n  Volumen=%s\n  Tamaño=%d bytes\n  Partición=[%d, %d bloques]\n" +
            "  Firma=%s\n  Creado=%s\n}",
            volumeName, totalDiskSize, partitionStart, partitionSize,
            signature, creationDate);
    }
}
