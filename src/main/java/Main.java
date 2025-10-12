import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        while(true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            if (input.equals("exit 0")) {
                break;
            }
            
            if (input.contains("echo ")) {
                String[] inputArray = input.split(" ");
                String[] messageArray = new String[inputArray.length - 1];
                System.arraycopy(inputArray, 1, messageArray, 0, inputArray.length - 1);
                System.out.println(String.join(" ", messageArray));
            }
            
            

            else {
                System.out.println(input + ": command not found");
            }
        }
    }
}
