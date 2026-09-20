import java.util.HashSet;
import java.util.Set;
import services.ItemService;

/** Regression coverage for the two-template SKH equipment roll. */
public class SkhDropDistributionTest {
    private static final int[][] EXPECTED = {
            {0, 33, 6, 35, 27, 30, 21, 24, 12, 57},
            {1, 41, 7, 43, 28, 47, 22, 46, 12, 57},
            {2, 49, 8, 51, 29, 55, 23, 53, 12, 57}
    };

    public static void main(String[] args) {
        ItemService service = ItemService.gI();
        for (int gender = 0; gender < EXPECTED.length; gender++) {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < 20_000; i++) {
                seen.add(service.randTempItemKichHoat(gender));
            }
            for (int itemId : EXPECTED[gender]) {
                if (!seen.contains(itemId)) {
                    throw new AssertionError("SKH item " + itemId
                            + " was never selected for gender " + gender + ": " + seen);
                }
            }
        }
        System.out.println("PASS: both SKH templates are reachable for every gender");
    }
}
