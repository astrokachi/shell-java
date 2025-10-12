import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        Set<String> builtin = Set.of("echo", "exit 0", "type");

        while (true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            if (input.equals("exit 0")) {
                break;
            }

            if (input.contains("echo ")) {
                String message = extractCmdStrings(input)[1];
                System.out.println(message);
            }

            if (input.contains("type ")) {
                String message = extractCmdStrings(input)[1];
                if (builtin.contains(message)) {
                    System.out.println(message + " is a shell builtin");
                } else {
                    System.out.println(message + ": not found");
                }
            }

            else {
                System.out.println(input + ": command not found");
            }
        }

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
