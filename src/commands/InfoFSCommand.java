package commands;

import filesystem.SuperBlock;
import filesystem.VirtualDisk;
import storage.FreeSpaceManager;
import terminal.Terminal;

/**
 * infoFS - Display filesystem statistics.
 *
 * Stallings "Operating Systems" 12.1:
 *   "The volume (partition) control block (super block) contains volume details:
 *    number of blocks, block size, free block count, free FCB count, etc."
 *
 * Usage: infoFS
 */
public class InfoFSCommand implements Command {
    @Override public String getName()        { return "infofs"; }
    @Override public String getDescription() { return "infoFS — información del sistema de archivos"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (terminal.getDisk() == null) {
            System.out.println("No hay disco activo. Use 'format' primero."); return;
        }

        VirtualDisk disk       = terminal.getDisk();
        SuperBlock   sb        = disk.getSuperBlock();
        FreeSpaceManager fsm   = disk.getFreeSpaceManager();

        long usedBytes = (long) fsm.getUsedBlocks() * VirtualDisk.BLOCK_SIZE;
        long freeBytes = (long) fsm.getFreeCount()  * VirtualDisk.BLOCK_SIZE;

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║         INFORMACIÓN DEL SISTEMA DE ARCHIVOS       ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("  %-28s: %s%n",  "Nombre FS",       sb.getFsName());
        System.out.printf("  %-28s: %d bytes (%d KB)%n", "Tamaño total",
            sb.getTotalSize(), sb.getTotalSize() / 1024);
        System.out.printf("  %-28s: %d bytes (%d KB)%n", "Espacio usado",   usedBytes, usedBytes/1024);
        System.out.printf("  %-28s: %d bytes (%d KB)%n", "Espacio libre",   freeBytes, freeBytes/1024);
        System.out.printf("  %-28s: %d%n",  "Bloques totales", sb.getTotalBlocks());
        System.out.printf("  %-28s: %d%n",  "Bloques libres",  fsm.getFreeCount());
        System.out.printf("  %-28s: %d bytes%n", "Tamaño de bloque", sb.getBlockSize());
        System.out.printf("  %-28s: %d%n",  "FCBs (archivos+dirs)", disk.getFcbTable().size());
        System.out.printf("  %-28s: %d%n",  "Usuarios",        disk.getUserManager().userCount());
        System.out.printf("  %-28s: %d%n",  "Grupos",          disk.getUserManager().groupCount());
        System.out.printf("  %-28s: %d%n",  "Archivos abiertos",disk.getOpenFileTable().totalOpen());
        System.out.printf("  %-28s: %s%n",  "Creado",          sb.getCreationDate());
        System.out.println();
        System.out.printf("  BITMAP (primeros bits): %s%n", disk.getFreeSpaceManager().getBitmapString());

        // MBR info
        System.out.println("\n  ── MBR ──");
        System.out.printf("  %-28s: %s%n", "Volumen",   disk.getMbr().getVolumeName());
        System.out.printf("  %-28s: %s%n", "Firma",     disk.getMbr().getSignature());
        System.out.printf("  %-28s: %s%n", "Creado",    disk.getMbr().getCreationDate());
        System.out.println("╚══════════════════════════════════════════════════╝");
    }
}
