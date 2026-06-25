package terminal;

import commands.*;
import filesystem.FCB;
import filesystem.VirtualDisk;
import storage.DiskPersistence;
import users.User;

import java.util.*;
import java.util.Scanner;

/**
 * Terminal - Represents one interactive shell session.
 *
 * Silberschatz Ch.13: "Users interact with the file system through a command
 * interpreter. Each session maintains its own working directory and credential."
 *
 * Each Terminal is independent: it has its own current user, current directory,
 * and command history. Multiple terminals share the same VirtualDisk.
 *
 * Multi-terminal support (Silberschatz Ch.4 — Processes):
 *   "Multiple processes (terminals) sharing file system resources must
 *    coordinate through the system's open-file table and permission mechanisms."
 */
public class Terminal {

    private final int    id;
    private VirtualDisk  disk;
    private String       diskFile;
    private String       currentUser;      // null = not logged in
    private String       currentDirectory; // absolute path, e.g. "/root"
    private boolean      running;

    /**
     * Shared Scanner for System.in — all commands use this instance.
     * Multiple Scanner objects on System.in cause buffering conflicts
     * (one buffers what the other needs). One shared instance avoids this.
     */
    private Scanner sharedScanner;

    private Map<String, Command> commands = new LinkedHashMap<>();

    public Terminal(int id, VirtualDisk disk, String diskFile) {
        this.id               = id;
        this.disk             = disk;
        this.diskFile         = diskFile;
        this.currentDirectory = "/";
        this.running          = true;
        registerCommands();
    }

    /** Inject the shared scanner (called by TerminalManager). */
    public void setScanner(Scanner sc) { this.sharedScanner = sc; }

    /** Returns the shared System.in scanner. Commands must use this instead of new Scanner(System.in). */
    public Scanner getScanner() { return sharedScanner; }

    // ── Command registration ───────────────────────────────────────────────

    private void registerCommands() {
        register(new FormatCommand());
        register(new ExitCommand());
        register(new UseradCommand());
        register(new GroupaddCommand());
        register(new PasswdCommand());
        register(new SuCommand());
        register(new WhoamiCommand());
        register(new PwdCommand());
        register(new MkdirCommand());
        register(new RmCommand());
        register(new MvCommand());
        register(new LsCommand());
        register(new ClearCommand());
        register(new CdCommand());
        register(new WhereisCommand());
        register(new LnCommand());
        register(new TouchCommand());
        register(new CatCommand());
        register(new LessCommand());
        register(new ChownCommand());
        register(new ChgrpCommand());
        register(new ChmodCommand());
        register(new ViewFilesOpenCommand());
        register(new ViewFCBCommand());
        register(new InfoFSCommand());
        register(new NoteCommand());
        register(new HelpCommand());
    }

    private void register(Command cmd) {
        commands.put(cmd.getName(), cmd);
    }

    // ── Command execution ──────────────────────────────────────────────────

    /**
     * Parse and execute a raw command line.
     * Returns false if the terminal should stop (exit command).
     */
    public boolean executeCommandLine(String line) {
        if (line == null || line.trim().isEmpty()) return true;
        String[] tokens = tokenize(line.trim());
        if (tokens.length == 0) return true;

        String name = tokens[0].toLowerCase();

        // Special alias
        if (name.equals("useradd")) name = "useradd";

        Command cmd = commands.get(name);
        if (cmd == null) {
            System.out.println(name + ": comando no encontrado. Escribe 'help' para ver comandos disponibles.");
            return true;
        }

        // Guard: require login for most commands
        if (requiresLogin(name) && currentUser == null) {
            System.out.println("Debe iniciar sesión primero. Use: su <usuario>");
            return true;
        }

        // Guard: require disk for most commands
        if (requiresDisk(name) && disk == null) {
            System.out.println("No hay disco activo. Use el comando 'format' primero.");
            return true;
        }

        cmd.execute(tokens, this);
        return running;
    }

    /** Simple tokenizer: splits on spaces but respects double-quoted strings. */
    private static String[] tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ' ' && !inQuotes) {
                if (cur.length() > 0) { tokens.add(cur.toString()); cur.setLength(0); }
            } else { cur.append(c); }
        }
        if (cur.length() > 0) tokens.add(cur.toString());
        return tokens.toArray(new String[0]);
    }

    private static final Set<String> NO_LOGIN_NEEDED = new HashSet<>(Arrays.asList(
        "format", "exit", "su", "help", "clear", "infofs"
    ));
    private static final Set<String> NO_DISK_NEEDED = new HashSet<>(Arrays.asList(
        "format", "exit", "help", "clear"
    ));

    private boolean requiresLogin(String cmd) { return !NO_LOGIN_NEEDED.contains(cmd); }
    private boolean requiresDisk(String cmd)  { return !NO_DISK_NEEDED.contains(cmd); }

    // ── Path resolution ────────────────────────────────────────────────────

    /**
     * Resolve a path (relative or absolute) to an absolute virtual path.
     * Handles: /, .., ., ~
     */
    public String resolvePath(String path) {
        if (path == null || path.isEmpty()) return currentDirectory;
        if (path.equals("~")) return homeDir();
        if (path.startsWith("~/")) return homeDir() + path.substring(1);
        if (path.startsWith("/")) return VirtualDisk.normalizePath(path);
        return VirtualDisk.normalizePath(currentDirectory + "/" + path);
    }

    private String homeDir() {
        if (currentUser == null) return "/";
        User u = disk.getUserManager().getUser(currentUser);
        return u != null ? u.getHomeDirectory() : "/";
    }

    // ── Disk save ─────────────────────────────────────────────────────────

    public boolean saveDisk() {
        if (disk == null || diskFile == null) return false;
        return DiskPersistence.save(disk, diskFile);
    }

    // ── Prompt ────────────────────────────────────────────────────────────

    public String getPrompt() {
        String user = currentUser != null ? currentUser : "guest";
        String cwd  = currentDirectory != null ? currentDirectory : "/";
        // Show short path: if cwd == home, show ~
        if (disk != null && currentUser != null) {
            User u = disk.getUserManager().getUser(currentUser);
            if (u != null && cwd.equals(u.getHomeDirectory())) cwd = "~";
        }
        char marker = "root".equals(currentUser) ? '#' : '$';
        return String.format("[T%d] %s@vfs:%s%c ", id, user, cwd, marker);
    }

    // ── Getters / setters ──────────────────────────────────────────────────

    public int         getId()               { return id; }
    public VirtualDisk getDisk()             { return disk; }
    public String      getDiskFile()         { return diskFile; }
    public String      getCurrentUser()      { return currentUser; }
    public String      getCurrentDirectory() { return currentDirectory; }
    public boolean     isRunning()           { return running; }
    public Map<String, Command> getCommands(){ return commands; }

    public void setDisk(VirtualDisk d)            { this.disk = d; }
    public void setDiskFile(String f)             { this.diskFile = f; }
    public void setCurrentUser(String u)          { this.currentUser = u; }
    public void setCurrentDirectory(String dir)   { this.currentDirectory = dir; }
    public void stop()                            { this.running = false; }

    // ── FCB shortcuts ──────────────────────────────────────────────────────

    public FCB getFCBByPath(String path) {
        if (disk == null) return null;
        return disk.getFCBByPath(resolvePath(path));
    }

    public FCB getParentFCB(String path) {
        if (disk == null) return null;
        return disk.getParentFCB(resolvePath(path));
    }

    @Override
    public String toString() {
        return String.format("Terminal#%d [user=%s, cwd=%s]", id, currentUser, currentDirectory);
    }
}
