package commands;

import terminal.Terminal;

/**
 * Command - Strategy interface for all file system commands.
 *
 * Applying SOLID Open/Closed Principle: the CommandRegistry is closed for
 * modification but open for extension — add a new command by implementing
 * this interface and registering it.
 *
 * Stallings / Silberschatz analogy: each command corresponds to a system call
 * in the file-system interface layer (Silberschatz Ch.13 "File-System Interface").
 */
public interface Command {
    /** The keyword the user types to invoke this command (e.g., "ls", "mkdir"). */
    String getName();

    /** One-line usage description shown by 'help'. */
    String getDescription();

    /**
     * Execute the command.
     *
     * @param args    tokenized arguments (args[0] is the command name itself)
     * @param terminal the terminal context (current user, cwd, disk access)
     */
    void execute(String[] args, Terminal terminal);
}
