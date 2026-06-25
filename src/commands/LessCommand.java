package commands;

import filesystem.FCB;
import security.AccessControl;
import storage.BlockManager;
import terminal.Terminal;

import java.util.Scanner;

/**
 * less - Page through file content (20 lines per page).
 *
 * Usage: less <archivo>
 * Press Enter for next page, 'q' to quit.
 */
public class LessCommand implements Command {
    private static final int PAGE_SIZE = 20;

    @Override public String getName()        { return "less"; }
    @Override public String getDescription() { return "less <archivo> — paginador de archivos"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: less <archivo>"); return; }

        String path = terminal.resolvePath(args[1]);
        FCB fcb = terminal.getDisk().getFCBByPath(path);

        if (fcb == null)       { System.out.println("less: '" + args[1] + "': No existe"); return; }
        if (fcb.isDirectory()) { System.out.println("less: '" + args[1] + "': Es un directorio"); return; }
        if (!AccessControl.checkRead(fcb, terminal.getCurrentUser(), terminal.getDisk())) return;

        if (fcb.isSymlink()) {
            path = terminal.resolvePath(fcb.getLinkTarget());
            fcb  = terminal.getDisk().getFCBByPath(path);
            if (fcb == null) { System.out.println("less: enlace roto"); return; }
        }

        terminal.getDisk().getOpenFileTable().open(fcb, terminal.getCurrentUser());
        String content = BlockManager.readContent(terminal.getDisk(), fcb);
        terminal.getDisk().getOpenFileTable().close(fcb.getId(), terminal.getCurrentUser(), fcb);

        if (content.isEmpty()) { System.out.println("(archivo vacío)"); return; }

        String[] lines = content.split("\n", -1);
        Scanner sc = terminal.getScanner();
        int page = 0;

        while (page * PAGE_SIZE < lines.length) {
            int start = page * PAGE_SIZE;
            int end   = Math.min(start + PAGE_SIZE, lines.length);
            for (int i = start; i < end; i++) System.out.println(lines[i]);

            if (end < lines.length) {
                System.out.print("─── Líneas " + (start+1) + "-" + end +
                    " de " + lines.length + " ─── [Enter=siguiente, q=salir]: ");
                String in = sc.nextLine().trim().toLowerCase();
                if (in.equals("q")) break;
                page++;
            } else {
                System.out.println("─── Fin del archivo (" + lines.length + " líneas) ───");
                break;
            }
        }
    }
}
