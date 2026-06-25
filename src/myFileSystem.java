import filesystem.VirtualDisk;
import storage.DiskPersistence;
import terminal.TerminalManager;

/**
 * myFileSystem - Entry point for the Virtual File System.
 *
 * Usage:
 *   java myFileSystem [diskfile]
 *   java myFileSystem miDiscoDuro.fs
 *
 * On startup:
 *   - If the disk file exists, load it (restoring all files, users, permissions).
 *   - If not, start without a disk (user must run 'format' first).
 *
 * Stallings "Operating Systems" 12.1:
 *   "The file system is mounted at startup. All on-disk structures are read
 *    into memory, and the file system is ready for use."
 *
 * Silberschatz Ch.15:
 *   "When a file system is mounted, the OS reads the super block and validates
 *    its magic number before trusting the structure."
 */
public class myFileSystem {

    public static void main(String[] args) {
        String diskFile = args.length > 0 ? args[0] : "miDiscoDuro.fs";

        VirtualDisk disk = null;

        if (DiskPersistence.diskExists(diskFile)) {
            System.out.println("Cargando disco: " + diskFile +
                " (" + DiskPersistence.fileSize(diskFile) + " bytes)");
            disk = DiskPersistence.load(diskFile);

            if (disk == null) {
                System.out.println("[WARN] No se pudo leer el disco. Iniciando sin disco.");
            } else if (!disk.getSuperBlock().isValid()) {
                System.out.println("[WARN] SuperBlock inválido. ¿Archivo corrompido?");
                disk = null;
            } else {
                System.out.println("Disco montado: " + disk.getSuperBlock().getFsName());
            }
        }

        TerminalManager manager = new TerminalManager(disk, diskFile);
        manager.start();
    }
}
