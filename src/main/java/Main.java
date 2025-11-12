import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        Set<String> builtInCommands = Set.of("echo", "exit", "type", "pwd", "cd");

        Scanner scanner = new Scanner(System.in);
        String pathVariable = System.getenv("PATH");
        Path currentDirectory = Path.of(System.getProperty("user.dir"));

        while (true) {
            System.out.print(currentDirectory + "$ ");
            String userInput = scanner.nextLine();

            if (userInput.isEmpty()) {
                continue;
            }

            String[] parsedInput = tokenizeInput(userInput);
            String command = parsedInput[0];
            String[] arguments = Arrays.copyOfRange(parsedInput, 1, parsedInput.length);

            // --- Built-in Commands ---
            if (command.equals("exit")) {
                break;
            }

            if (command.equals("echo")) {
                System.out.println(String.join(" ", arguments));
                continue;
            }

            if (command.equals("type")) {
                for (int i = 0; i < arguments.length; i++) {
                    Path executablePath = locateExecutable(arguments[i], pathVariable).orElse(null);

                    if (builtInCommands.contains(arguments[i])) {
                        System.out.println(arguments + " is a shell builtin");
                        continue;
                    }

                    if (executablePath == null) {
                        System.out.println(arguments + ": not found");
                        continue;
                    }

                    System.out.println(arguments + " is " + executablePath);
                    continue;
                }

                continue;
            }

            if (command.equals("pwd")) {
                System.out.println(currentDirectory);
                continue;
            }

            if (command.equals("cd")) {
                Path newDirectory;
                String home = System.getProperty("user.home");
                if (arguments.length == 0) {
                    newDirectory = Path.of(home);
                } else if (arguments[0].startsWith("~")) {
                    String replaced = arguments[1].replaceFirst("~", home);
                    newDirectory = Path.of(replaced).normalize();
                } else {
                    Path inputPath = Path.of(arguments[0]);
                    newDirectory = inputPath.isAbsolute() ? inputPath : currentDirectory.resolve(inputPath).normalize();
                }

                if (Files.exists(newDirectory) && Files.isDirectory(newDirectory)) {
                    currentDirectory = newDirectory;
                } else {
                    System.out.println("cd : No such file or directory " + arguments);
                }
                continue;
            }

            // ---External Command---
            executeExternalCommand(parsedInput, pathVariable, currentDirectory);
        }

        scanner.close();
    }

    static void executeExternalCommand(String[] parsedInput, String pathVariable, Path currentDirectory) {
        String command = parsedInput[0];
        Optional<Path> executable = locateExecutable(parsedInput[0], pathVariable);

        if (executable.isEmpty()) {
            System.out.println(command + ": command not found");
            return;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(parsedInput);
            processBuilder.directory(currentDirectory.toFile());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader outputReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String outputLine;
                while ((outputLine = outputReader.readLine()) != null) {
                    System.out.println(outputLine);
                }

                process.waitFor();
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Error running command" + e.getMessage());
        }

    }

    enum State {
        NORMAL,
        IN_SINGLE_QUOTE,
    }

    // Tokenizes input while respecting quoted strings
    static String[] tokenizeInput(String input) {
        State state = State.NORMAL;
        List<String> tokens = new ArrayList<String>();
        String currentToken = "";

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            // echo 'this is it'
            switch (state) {
                case NORMAL:
                    if (ch == '\'') { // 'hello''world'
                        state = state.IN_SINGLE_QUOTE;
                    } else if (Character.isWhitespace(ch)) {
                        if (currentToken.length() > 0) {
                            tokens.add(currentToken);
                            currentToken = "";
                        }
                    } else {
                        currentToken += ch;
                    }

                    break;

                case IN_SINGLE_QUOTE:
                    if (ch == '\'') {
                        state = state.NORMAL;
                    } else {
                        currentToken += ch;
                    }
            }

            if (i == input.length() - 1 && !currentToken.isEmpty()) {
                tokens.add(currentToken);
            }

        }

        return tokens.toArray(new String[0]);
    }

    // Finds a valid executable in PATH
    static Optional<Path> locateExecutable(String commandName, String pathVariable) {
        if (pathVariable == null || commandName.isEmpty()) {
            return Optional.empty();
        }

        String[] searchPaths = pathVariable.split(File.pathSeparator);

        for (String dir : searchPaths) {
            Path potentialPath = Path.of(dir, commandName);
            if (Files.exists(potentialPath) && Files.isExecutable(potentialPath)) {
                return Optional.of(potentialPath.toAbsolutePath());
            }
        }

        return Optional.empty();
    }
}
