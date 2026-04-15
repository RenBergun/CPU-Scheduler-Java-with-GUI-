import java.util.Scanner;

public class InputHelper {

    /**
     * Reads an integer from the console, repeating the prompt
     * until the user enters a valid integer within [min, max].
     *
     * @param sc   Shared Scanner reading from System.in
     * @param min  Minimum acceptable value (inclusive)
     * @param max  Maximum acceptable value (inclusive)
     * @return     A validated integer in [min, max]
     */
    public static int readInt(Scanner sc, int min, int max) {
        while (true) {
            try {
                String line = sc.nextLine().trim();
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
                System.out.printf("  ✗ Please enter a value between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("  ✗ Invalid input. Enter an integer: ");
            }
        }
    }
}
