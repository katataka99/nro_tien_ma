import services.func.AdminItemOptions;
import java.util.Map;

public class AdminItemOptionsTest {
    public static void main(String[] args) {
        Map<Integer, Integer> options = AdminItemOptions.parse("1-20;5-20;36-20");
        if (!options.equals(Map.of(1, 20, 5, 20, 36, 20))) {
            throw new AssertionError("Example options parsed incorrectly");
        }
        if (!AdminItemOptions.parse(" 1 - 0 ; 5 - 20 ").equals(Map.of(1, 0, 5, 20))) {
            throw new AssertionError("Whitespace or zero value parsed incorrectly");
        }
        String[] invalid = {"", "1", "1-20;", "1-20;;5-20", "x-20", "1-x", "1--20",
                "-1-20", "1-20;1-30", "1-2147483648", "1-20-30"};
        for (String input : invalid) {
            try {
                AdminItemOptions.parse(input);
                throw new AssertionError("Accepted invalid input: " + input);
            } catch (IllegalArgumentException expected) {
                if (expected.getMessage() == null || expected.getMessage().isBlank()) {
                    throw new AssertionError("Missing validation message");
                }
            }
        }
        System.out.println("PASS: multi-option example, whitespace, zero and invalid inputs");
    }
}
