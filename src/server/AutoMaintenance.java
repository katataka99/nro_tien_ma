package server;

/*
 *
 *
 * @author EMTI
 */

import EMTI.Functions;
import java.time.LocalTime;
import java.util.Properties;
import utils.Logger;

public class AutoMaintenance extends Thread {

    public static boolean AutoMaintenance = false;
    public static int hours = 4;
    public static int mins = 0;
    private static AutoMaintenance instance;
    public static boolean isRunning;

    public static AutoMaintenance gI() {
        if (instance == null) {
            instance = new AutoMaintenance();
        }
        return instance;
    }

    public static void configure(Properties properties) {
        AutoMaintenance = Boolean.parseBoolean(properties.getProperty("server.autorestart", "false"));
        hours = parseTimePart(properties, "server.maintenance.hour", 4, 0, 23);
        mins = parseTimePart(properties, "server.maintenance.min", 0, 0, 59);

        if (AutoMaintenance) {
            Logger.success(String.format("Tự động bảo trì hằng ngày lúc %02d:%02d\n", hours, mins));
        } else {
            Logger.warning("Tự động bảo trì hằng ngày đang tắt\n");
        }
    }

    private static int parseTimePart(Properties properties, String key, int defaultValue, int min, int max) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
            return value >= min && value <= max ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && !isRunning) {
            try {
                if (AutoMaintenance) {
                    LocalTime currentTime = LocalTime.now();
                    if (currentTime.getHour() == hours && currentTime.getMinute() == mins) {
                        Logger.log(Logger.PURPLE, "Đang tiến hành quá trình bảo trì tự động\n");
                        Maintenance.gI().start(60);
                        isRunning = true;
                    }
                }
                Functions.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

}
