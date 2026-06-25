package commands;

import terminal.Terminal;
import java.util.*;

/**
 * help - Display detailed help for all commands or a specific one.
 *
 * Usage:
 *   help           — lista todos los comandos con descripción y ejemplo
 *   help <comando> — muestra ayuda detallada de un comando específico
 */
public class HelpCommand implements Command {

    @Override public String getName()        { return "help"; }
    @Override public String getDescription() { return "help [comando] — muestra ayuda de comandos"; }

    // ── Command catalog ────────────────────────────────────────────────────
    // Each entry: { name, description, usage example }
    private static final String[][] CATALOG = {
        // ── Disco y sesión ─────────────────────────────────────────────────
        { "format",
          "Inicializa (formatea) el disco virtual. Crea miDiscoDuro.fs con MBR,\n" +
          "  SuperBlock, bitmap, jerarquía UNIX (/ /root /home) y usuario root.",
          "format" },

        { "exit",
          "Guarda el disco y cierra la sesión actual del terminal.",
          "exit" },

        // ── Autenticación y usuarios ────────────────────────────────────────
        { "su",
          "Cambia de usuario (substitute user). Pide contraseña y mueve el\n" +
          "  directorio actual al home del nuevo usuario.",
          "su root\nsu alice" },

        { "whoami",
          "Muestra el nombre de usuario activo, su nombre completo, grupo\n" +
          "  principal y directorio home.",
          "whoami" },

        { "useradd",
          "Crea un nuevo usuario (solo root). Solicita nombre completo,\n" +
          "  contraseña y grupo. Crea automáticamente /home/users/<usuario>.",
          "useradd alice" },

        { "groupadd",
          "Crea un nuevo grupo de usuarios (solo root).",
          "groupadd developers" },

        { "passwd",
          "Cambia la contraseña. Sin argumento cambia la del usuario actual;\n" +
          "  root puede cambiar la de cualquier usuario.",
          "passwd\npasswd alice" },

        // ── Navegación ──────────────────────────────────────────────────────
        { "pwd",
          "Imprime el directorio de trabajo actual (Print Working Directory).",
          "pwd" },

        { "cd",
          "Cambia el directorio actual. Acepta rutas absolutas, relativas,\n" +
          "  '..' para subir y '~' para ir al home del usuario.",
          "cd /home/users/alice\ncd ..\ncd ~\ncd proyectos" },

        // ── Directorios y archivos ──────────────────────────────────────────
        { "ls",
          "Lista el contenido de un directorio mostrando tipo ([D]/[F]/[L]),\n" +
          "  permisos, propietario, grupo, tamaño y nombre.\n" +
          "  -R lista recursivamente todos los subdirectorios.",
          "ls\nls /home\nls -R /\nls -R /home/users" },

        { "mkdir",
          "Crea uno o más directorios. Admite rutas absolutas y relativas.\n" +
          "  El directorio padre debe existir.",
          "mkdir documentos\nmkdir /home/users/alice/proyectos\nmkdir dir1 dir2 dir3" },

        { "touch",
          "Crea un archivo vacío. Si el archivo ya existe, actualiza su\n" +
          "  timestamp de modificación sin alterar el contenido.",
          "touch notas.txt\ntouch /root/archivo.txt" },

        { "rm",
          "Elimina archivos o directorios. Libera los bloques en el bitmap.\n" +
          "  -R elimina un directorio y todo su contenido recursivamente.\n" +
          "  Acepta patrones wildcard (* y *.ext).",
          "rm notas.txt\nrm -R /home/users/alice/temp\nrm *.txt\nrm *" },

        { "mv",
          "Mueve o renombra un archivo/directorio. Si el destino es un\n" +
          "  directorio existente, mueve el origen dentro de él.",
          "mv viejo.txt nuevo.txt\nmv archivo.txt /home/users/alice/\nmv carpeta /root/" },

        // ── Contenido de archivos ───────────────────────────────────────────
        { "cat",
          "Muestra el contenido completo de un archivo en pantalla.\n" +
          "  Registra el archivo en la Open File Table durante la lectura.\n" +
          "  Sigue enlaces simbólicos automáticamente.",
          "cat notas.txt\ncat /home/users/alice/docs/readme.txt" },

        { "less",
          "Paginador: muestra el archivo de 20 líneas por página.\n" +
          "  Enter = siguiente página,  q = salir.",
          "less manual.txt\nless /root/log.txt" },

        { "note",
          "Editor de texto en línea. Permite crear y editar archivos.\n" +
          "  Escribe línea por línea. Comandos dentro del editor:\n" +
          "    :x  = terminar y preguntar si guardar\n" +
          "    :q  = salir SIN guardar\n" +
          "    :l  = listar las líneas actuales",
          "note diario.txt\nnote /home/users/alice/notas.txt" },

        // ── Permisos ────────────────────────────────────────────────────────
        { "chmod",
          "Cambia los permisos de un archivo o directorio.\n" +
          "  Formato: 2 dígitos decimales [dueño][grupo]\n" +
          "  Cada dígito: 4=leer, 2=escribir, 1=ejecutar (son aditivos)\n" +
          "  Solo el dueño o root pueden usar este comando.\n\n" +
          "  Tabla rápida:\n" +
          "    7 = rwx (leer+escribir+ejecutar)\n" +
          "    6 = rw-\n" +
          "    5 = r-x\n" +
          "    4 = r--\n" +
          "    0 = --- (sin acceso)",
          "chmod 77 archivo.txt   (dueño=rwx, grupo=rwx)\n" +
          "chmod 75 script.sh     (dueño=rwx, grupo=r-x)\n" +
          "chmod 44 privado.txt   (dueño=r--, grupo=r--)\n" +
          "chmod 70 secreto.txt   (solo dueño puede leer/escribir/ejecutar)" },

        { "chown",
          "Cambia el propietario de un archivo o directorio.\n" +
          "  Solo el dueño actual o root pueden usar este comando.",
          "chown alice notas.txt\nchown root /home/users/alice/config" },

        { "chgrp",
          "Cambia el grupo propietario de un archivo o directorio.\n" +
          "  Solo el dueño o root pueden usar este comando.",
          "chgrp developers proyecto.java\nchgrp root /root/config.txt" },

        // ── Búsqueda y enlaces ──────────────────────────────────────────────
        { "whereis",
          "Busca archivos por nombre en todo el sistema de archivos.\n" +
          "  Acepta patrones wildcard (* y *.ext).",
          "whereis notas.txt\nwhereis *.java\nwhereis config" },

        { "ln",
          "Crea un enlace simbólico (symbolic link). El enlace apunta al\n" +
          "  archivo destino; cat y less lo siguen automáticamente.",
          "ln /home/users/alice/notas.txt /root/enlace_notas\nln /root/config.txt /home/config" },

        // ── Información del sistema ─────────────────────────────────────────
        { "viewfcb",
          "Muestra todos los atributos del FCB (File Control Block / inode)\n" +
          "  de un archivo: ID, tipo, propietario, grupo, permisos, tamaño,\n" +
          "  fechas, estado abierto/cerrado y lista de bloques asignados.",
          "viewFCB notas.txt\nviewFCB /home/users/alice/docs/readme.txt" },

        { "viewfilesopen",
          "Muestra la tabla global de archivos abiertos (Open File Table).\n" +
          "  Indica: ruta, usuario que abrió, fecha de apertura y referencias.",
          "viewFilesOpen" },

        { "infofs",
          "Muestra estadísticas completas del sistema de archivos:\n" +
          "  nombre, tamaño total/usado/libre, bloques, usuarios, grupos,\n" +
          "  archivos abiertos, bitmap visual, datos del MBR y SuperBlock.",
          "infoFS" },

        // ── Terminal ────────────────────────────────────────────────────────
        { "clear",
          "Limpia la pantalla del terminal (ANSI escape).",
          "clear" },

        { "help",
          "Muestra esta ayuda. Con un nombre de comando muestra su\n" +
          "  descripción completa y ejemplo de uso.",
          "help\nhelp chmod\nhelp note\nhelp ls" },
    };

    // ── Execute ────────────────────────────────────────────────────────────

    @Override
    public void execute(String[] args, Terminal terminal) {
        if (args.length >= 2) {
            showSpecific(args[1].toLowerCase());
        } else {
            showAll();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void showAll() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║          VFS — Sistema de Archivos Virtual  |  Comandos             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════╣");
        System.out.println("║  Uso: help <comando>  para ver descripción detallada y ejemplo       ║");
        System.out.println("╠══════════╦═══════════════════════════════════════════════════════════╣");
        System.out.printf( "║  %-8s ║  %-53s  ║%n", "COMANDO", "DESCRIPCIÓN BREVE");
        System.out.println("╠══════════╬═══════════════════════════════════════════════════════════╣");

        // Group headers
        printSection("── Disco y sesión ──────────────────────────────────────────");
        printRow("format",        "Inicializa el disco virtual (crea miDiscoDuro.fs)");
        printRow("exit",          "Guarda el disco y cierra el terminal");

        printSection("── Usuarios y autenticación ────────────────────────────────");
        printRow("su",            "Cambia de usuario  (su root / su alice)");
        printRow("whoami",        "Muestra el usuario activo y su información");
        printRow("useradd",       "Crea un nuevo usuario  [solo root]");
        printRow("groupadd",      "Crea un nuevo grupo    [solo root]");
        printRow("passwd",        "Cambia contraseña propia o de otro usuario");

        printSection("── Navegación ──────────────────────────────────────────────");
        printRow("pwd",           "Imprime el directorio actual");
        printRow("cd",            "Cambia de directorio  (cd ~  cd ..  cd /ruta)");

        printSection("── Archivos y directorios ──────────────────────────────────");
        printRow("ls",            "Lista directorio  (ls -R para recursivo)");
        printRow("mkdir",         "Crea directorios");
        printRow("touch",         "Crea archivo vacío o actualiza timestamp");
        printRow("rm",            "Elimina archivo/dir  (rm -R  rm *.txt)");
        printRow("mv",            "Mueve o renombra archivo/directorio");

        printSection("── Contenido de archivos ───────────────────────────────────");
        printRow("cat",           "Muestra el contenido completo de un archivo");
        printRow("less",          "Paginador: 20 líneas por pantalla");
        printRow("note",          "Editor de texto en línea (:x guardar, :q salir)");

        printSection("── Permisos y propiedad ────────────────────────────────────");
        printRow("chmod",         "Cambia permisos  (chmod 75 archivo)");
        printRow("chown",         "Cambia propietario");
        printRow("chgrp",         "Cambia grupo");

        printSection("── Búsqueda y enlaces ──────────────────────────────────────");
        printRow("whereis",       "Busca archivos por nombre  (whereis *.java)");
        printRow("ln",            "Crea enlace simbólico");

        printSection("── Información del sistema ─────────────────────────────────");
        printRow("viewFCB",       "Muestra el FCB completo de un archivo");
        printRow("viewFilesOpen", "Tabla global de archivos abiertos (OFT)");
        printRow("infoFS",        "Estadísticas del FS: tamaño, bitmap, MBR...");
        printRow("clear",         "Limpia la pantalla");
        printRow("help",          "Esta ayuda  (help <comando> para detalle)");

        System.out.println("╠══════════╩═══════════════════════════════════════════════════════════╣");
        System.out.println("║  Multi-terminal: newterminal | switcht N | terminals | closeterminal ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void showSpecific(String name) {
        for (String[] entry : CATALOG) {
            if (entry[0].equalsIgnoreCase(name)) {
                String cmd  = entry[0];
                String desc = entry[1];
                String ex   = entry[2];

                System.out.println();
                System.out.println("┌──────────────────────────────────────────────────────────┐");
                System.out.printf( "│  COMANDO: %-48s│%n", cmd.toUpperCase());
                System.out.println("├──────────────────────────────────────────────────────────┤");
                System.out.println("│  DESCRIPCIÓN:                                            │");
                for (String line : desc.split("\n"))
                    System.out.printf("│    %-54s│%n", line);
                System.out.println("├──────────────────────────────────────────────────────────┤");
                System.out.println("│  EJEMPLO DE USO:                                         │");
                for (String line : ex.split("\n"))
                    System.out.printf("│    %-54s│%n", line);
                System.out.println("└──────────────────────────────────────────────────────────┘");
                System.out.println();
                return;
            }
        }
        System.out.println("help: comando '" + name + "' no reconocido. Escribe 'help' para ver la lista completa.");
    }

    private void printSection(String title) {
        System.out.printf("║  %-68s║%n", title);
    }

    private void printRow(String cmd, String desc) {
        System.out.printf("║  %-8s   %-57s║%n", cmd, desc);
    }
}
