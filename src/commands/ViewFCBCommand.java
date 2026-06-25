package commands;

import filesystem.DirectoryEntry;
import filesystem.FCB;
import terminal.Terminal;

import java.text.SimpleDateFormat;

/**
 * viewFCB - Display all attributes of a File Control Block.
 *
 * Stallings "Operating Systems" Table 12.1 — FCB fields:
 *   "File identification: name, unique identifier, location
 *    File type, address of data blocks, file size, timestamps,
 *    access control (owner, group, permissions), open/closed flag."
 *
 * Usage: viewFCB <archivo>
 */
public class ViewFCBCommand implements Command {
    private static final SimpleDateFormat SDF =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override public String getName()        { return "viewfcb"; }
    @Override public String getDescription() { return "viewFCB <archivo> — muestra el FCB completo"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: viewFCB <archivo>"); return; }

        String path = terminal.resolvePath(args[1]);
        FCB fcb = terminal.getDisk().getFCBByPath(path);

        if (fcb == null) { System.out.println("viewFCB: '" + args[1] + "': No existe"); return; }

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║         FILE CONTROL BLOCK (FCB/Inode)       ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.printf("  %-22s: %s%n", "ID (inode)",     fcb.getId());
        System.out.printf("  %-22s: %s%n", "Nombre",         fcb.getName());
        System.out.printf("  %-22s: %s%n", "Ruta",           fcb.getPath());
        System.out.printf("  %-22s: %s%n", "Tipo",
            fcb.isDirectory() ? "Directorio" : (fcb.isSymlink() ? "Enlace Simbólico" : "Archivo Regular"));
        System.out.printf("  %-22s: %s%n", "Propietario",    fcb.getOwner());
        System.out.printf("  %-22s: %s%n", "Grupo",          fcb.getGroup());
        System.out.printf("  %-22s: %d  (%s)%n", "Permisos",
            fcb.getPermissions(), fcb.getPermissionsString());
        System.out.printf("  %-22s: %d bytes%n", "Tamaño",   fcb.getSize());
        System.out.printf("  %-22s: %s%n", "Fecha creación", SDF.format(fcb.getCreationDate()));
        System.out.printf("  %-22s: %s%n", "Última modificación", SDF.format(fcb.getModificationDate()));
        System.out.printf("  %-22s: %s%n", "Estado",
            fcb.isOpen() ? "ABIERTO (en OFT)" : "cerrado");
        System.out.printf("  %-22s: %d bloques %s%n", "Bloques asignados",
            fcb.getBlocks().size(), fcb.getBlocks().isEmpty() ? "" : fcb.getBlocks().toString());

        if (fcb.isSymlink())
            System.out.printf("  %-22s: %s%n", "Destino enlace", fcb.getLinkTarget());

        if (fcb.isDirectory() && fcb.getEntries() != null) {
            System.out.printf("  %-22s: %d entradas%n", "Entradas dir", fcb.getEntries().size());
            for (DirectoryEntry e : fcb.getEntries())
                System.out.printf("      → %-20s [%s]%n", e.getName(), e.getFcbId());
        }
        System.out.println("╚══════════════════════════════════════════════╝");
    }
}
