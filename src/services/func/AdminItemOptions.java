package services.func;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parser shared by both admin item forms. */
public final class AdminItemOptions {
    private AdminItemOptions() {}

    public static Map<Integer, Integer> parse(String input) {
        Map<Integer, Integer> options = new LinkedHashMap<>();
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Nhập option dạng 1-20;5-20;36-20");
        }
        for (String entry : input.split(";", -1)) {
            String[] pair = entry.trim().split("-", -1);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Sai định dạng option: " + entry + ". Ví dụ: 1-20;5-20;36-20");
            }
            int id;
            int value;
            try {
                id = Integer.parseInt(pair[0].trim());
                value = Integer.parseInt(pair[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ID option và giá trị phải là số nguyên: " + entry);
            }
            if (id < 0 || value < 0) {
                throw new IllegalArgumentException("ID option và giá trị không được âm");
            }
            if (options.putIfAbsent(id, value) != null) {
                throw new IllegalArgumentException("ID option bị trùng: " + id);
            }
        }
        return options;
    }
}
