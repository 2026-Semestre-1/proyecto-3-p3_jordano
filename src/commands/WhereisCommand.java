package commands;

import filesystem.FCB;
import terminal.Terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * whereis - Locate a file by name anywhere in the virtual filesystem.
 *
 * Searches the FCB table for all FCBs whose name matches the query.
 * This mimics Unix 'find / -name <pattern>' behavior.
 *
 * Usage: whereis <nombre>
 */
public class WhereisCommand implements Command {
    @Override public String getName()        { return "whereis"; }
    @Override public String getDescription() { return "whereis <nombre> — busca un archivo en el FS"; }

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length < 2) { System.out.println("Uso: whereis <nombre>"); return; }

        String query = args[1].toLowerCase();
        boolean wildcard = query.contains("*");
        String regex = wildcard
            ? query.replace(".", "\\.").replace("*", ".*")
            : null;

        List<String> found = new ArrayList<>();
        for (FCB fcb : terminal.getDisk().getFcbTable().values()) {
            String name = fcb.getName().toLowerCase();
            boolean match = wildcard ? name.matches(regex) : name.equals(query);
            if (match) found.add(fcb.getPath());
        }

        if (found.isEmpty()) {
            System.out.println("whereis: '" + args[1] + "': no encontrado");
        } else {
            found.forEach(System.out::println);
        }
    }
}
