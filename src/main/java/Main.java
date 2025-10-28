import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        Set<String> builtInCommands = Set.of("echo", "exit", "type", "pwd", "cd");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print(System.getProperty("user.dir") + "$ ");
            String userInput = scanner.nextLine();
            String pathVariable = System.getenv("PATH");
            String currentDirectory = System.getProperty("user.dir");

            if (userInput.isEmpty()) {
                continue;
            }

            String[] parsedInput = parseCommandAndArgs(userInput);
            String command = parsedInput[0];
            String arguments = parsedInput[1];

            ProcessBuilder processBuilder = new ProcessBuilder(tokenizeCommand(userInput));

            // --- Built-in Commands ---
            if (command.equals("exit")) {
                break;
            }

            if (command.equals("echo")) {
                System.out.println(arguments);
                continue;
            }

            if (command.equals("type")) {
                Path executablePath = locateExecutable(arguments, pathVariable).orElse(null);

                if (builtInCommands.contains(arguments)) {
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

            if (command.equals("pwd")) {
                System.out.println(currentDirectory);
                continue;
            }

            if (command.equals("cd")) {
                String firstChar = (arguments != null && !arguments.isEmpty()) ? arguments.substring(0, 1) : "";
                String homeDirectory = System.getProperty("user.home");

                if (firstChar.isBlank()) {
                    System.out.println(homeDirectory);
                }

                if (firstChar.equals("/")) {
                    String targetPath = homeDirectory + arguments;
                    System.out.println(targetPath);
                }

                int dirDepth = arguments.split("/").length;
                continue;
            }

            // --- External Commands ---
            Path executableCommand = locateExecutable(command, pathVariable).orElse(null);

            if (executableCommand != null) {
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                try (BufferedReader outputReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String outputLine;
                    while ((outputLine = outputReader.readLine()) != null) {
                        System.out.println(outputLine);
                    }

                    process.waitFor();
                } catch (IOException e) {
                    System.out.println("Error running command: " + e.getMessage());
                }
            } else {
                System.out.println(userInput + ": command not found");
            }
        }

        scanner.close();
    }

    // Tokenizes input while respecting quoted strings
    static String[] tokenizeCommand(String input) {
        String[] splitByQuote = input.split("\"");
        if (splitByQuote.length <= 1) {
            return input.split(" ");
        }

        String[] preQuoteTokens = splitByQuote[0].split(" ");
        String quotedSection = splitByQuote[1];

        List<String> tokens = new ArrayList<>();
        tokens.addAll(Arrays.asList(preQuoteTokens));
        tokens.add(quotedSection);

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

    // Separates the command from its arguments
    static String[] parseCommandAndArgs(String input) {
        String[] parts = input.split(" ");
        String command = parts[0];

        String[] argsArray = new String[parts.length - 1];
        System.arraycopy(parts, 1, argsArray, 0, parts.length - 1);
        String arguments = String.join(" ", argsArray);

        return new String[] { command, arguments };
    }
}
