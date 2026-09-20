import item.Item;
import java.util.ArrayList;
import java.util.List;
import models.Template;
import services.InventoryService;

public class EventItemStackingTest {

    private static final Template.ItemOptionTemplate EMPTY_OPTION =
            new Template.ItemOptionTemplate(73, "", 0);

    public static void main(String[] args) {
        Template.ItemTemplate eventTemplate = template((short) 1217, (byte) 27, false);
        List<Item> bag = new ArrayList<>();
        bag.add(item(eventTemplate, 2, 0));
        bag.add(item(eventTemplate, 3, 0));
        bag.add(item(eventTemplate, 4, 1));
        bag.add(new Item());
        bag.add(new Item());

        Item received = item(eventTemplate, 1, 0);
        if (!InventoryService.gI().addItemList(bag, received)) {
            throw new AssertionError("Event item was not added");
        }
        if (bag.get(0).quantity != 6 || bag.get(1).isNotNullItem()) {
            throw new AssertionError("Matching event item stacks were not merged");
        }
        if (bag.get(2).quantity != 4) {
            throw new AssertionError("Event items with different options were merged");
        }

        Template.ItemTemplate regularTemplate = template((short) 999, (byte) 27, false);
        List<Item> regularBag = new ArrayList<>();
        regularBag.add(item(regularTemplate, 1, 0));
        regularBag.add(new Item());
        Item regularReceived = item(regularTemplate, 1, 0);
        InventoryService.gI().addItemList(regularBag, regularReceived);
        if (regularBag.get(0).quantity != 1 || regularBag.get(1).quantity != 1) {
            throw new AssertionError("A regular non-stackable item was merged");
        }

        System.out.println("PASS: event stacks merge while options and regular items stay separate");
    }

    private static Template.ItemTemplate template(short id, byte type, boolean stackable) {
        Template.ItemTemplate template = new Template.ItemTemplate();
        template.id = id;
        template.type = type;
        template.isUpToUp = stackable;
        return template;
    }

    private static Item item(Template.ItemTemplate template, int quantity, int optionParam) {
        Item item = new Item();
        item.template = template;
        item.quantity = quantity;
        item.itemOptions.add(new Item.ItemOption(EMPTY_OPTION, optionParam));
        return item;
    }
}
