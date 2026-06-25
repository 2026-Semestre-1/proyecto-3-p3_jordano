package commands;

import filesystem.OpenFileEntry;
import terminal.Terminal;

import java.util.List;

/**
 * viewFilesOpen - Display the global Open File Table.
 *
 * Silberschatz Ch.14 "Open Files":
 *   "Several pieces of data are needed to manage open files:
 *    file-position pointer, file-open count, disk location, access rights."
 *   "The system-wide open-file table contains a copy of the FCB of each open file."
 *
 * Stallings "Operating Systems" 12.5:
 *   "When a process opens a file, the OS adds an entry to the system open-file table
 *    and the count for that file is incremented."
 *
 * Usage: viewFilesOpen
 */
public class ViewFilesOpenCommand implements Command {
    @Override public String getName()        { return "viewfilesopen"; }
    @Override public String getDescription() { return "viewFilesOpen — muestra archivos abiertos (OFT)"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        List<OpenFileEntry> entries = terminal.getDisk().getOpenFileTable().getEntries();

        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              TABLA DE ARCHIVOS ABIERTOS (Open File Table)        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.printf("  Total archivos abiertos: %d%n%n", entries.size());

        if (entries.isEmpty()) {
            System.out.println("  (ningún archivo abierto)");
            return;
        }

        System.out.printf("  %-40s  %-10s  %-25s  %s%n",
            "Ruta", "Usuario", "Fecha apertura", "Refs");
        System.out.println("  " + "─".repeat(85));
        for (OpenFileEntry e : entries) {
            System.out.printf("  %-40s  %-10s  %-25s  %d%n",
                e.getFilepath(), e.getOpenedByUser(), e.getOpenDate(), e.getReferenceCount());
        }
    }
}
