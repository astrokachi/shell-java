import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        Set<String> builtin = Set.of("echo", "exit", "type");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String input = scanner.nextLine();
            String pathEnv = System.getenv("PATH");

            if (input.isEmpty()) {
                continue;
            }

            String message = extractCmdStrings(input)[1];
            String command = extractCmdStrings(input)[0];

            if (command.equals("exit")) {
                break;
            }

            if (command.equals("echo")) {
                System.out.println(message);
                continue;
            }

            if (command.equals("type")) {
                Path exec = findExecutable(message, pathEnv).orElse(null);
                if (builtin.contains(message)) {
                    System.out.println(message + " is a shell builtin");
                    continue;
                }
                if (exec == null) {
                    System.out.println(message + ": not found");
                    continue;
                }

                System.out.println(message + " is " + exec);
                continue;
            }

            Path extCmd = findExecutable(command, pathEnv).orElse(null);

            if (extCmd != null) {
                ProcessBuilder pb = new ProcessBuilder(splitByQuote(input));
                // pb.directory(new File(System.getProperty("user.dir")));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }

                    process.waitFor();
                } catch (IOException e) {
                    System.out.println("Error running command: " + e.getMessage());
                }
            } else {
                System.out.println(input + ": command not found");
            }
        }

        scanner.close();
    }

    static String[] splitByQuote(String input) {
        String[] splitStrings = input.split("\"");
        if (splitStrings.length <= 1) {
            return input.split(" ");
        }

        String[] arguments = splitStrings[0].split(" ");
        String literal = splitStrings[1];

        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(arguments));
        list.add(literal);

        return list.toArray(new String[0]);
    }

    static Optional<Path> findExecutable(String message, String pathEnv) {
        if (pathEnv == null || message.isEmpty()) {
            return Optional.empty();
        }

        String[] paths = pathEnv.split(File.pathSeparator);

        for (String path : paths) {
            Path filePath = Path.of(path, message);
            if (Files.exists(filePath) && Files.isExecutable(filePath)) {
                return Optional.of(filePath.toAbsolutePath());
            }
        }

        return Optional.empty();
    }

    static String[] extractCmdStrings(String input) {
        String[] inputArray = input.split(" ");
        String command = inputArray[0];
        String[] messageArray = new String[inputArray.length - 1];
        System.arraycopy(inputArray, 1, messageArray, 0, inputArray.length - 1);
        String message = String.join(" ", messageArray);
        return new String[] { command, message };
    }
}
