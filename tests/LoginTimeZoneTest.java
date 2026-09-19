import java.sql.*;
import java.util.TimeZone;
import jdbc.DBConnecter;

// Run against a disposable database configured in data/config/config.properties.
// Uses a connection-local temporary table; no game accounts are modified.
public class LoginTimeZoneTest {
    public static void main(String[] args) throws Exception {
        try (Connection c = DBConnecter.getConnectionServer(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT @@session.time_zone")) {
                r.next();
                if (!"+07:00".equals(r.getString(1))) throw new AssertionError("Unexpected session timezone");
            }
            s.execute("CREATE TEMPORARY TABLE login_clock_test (last_time_login TIMESTAMP, last_time_logout TIMESTAMP)");
            s.executeUpdate("INSERT INTO login_clock_test VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            s.executeUpdate("UPDATE login_clock_test SET last_time_login = CURRENT_TIMESTAMP, last_time_logout = CURRENT_TIMESTAMP");
            try (ResultSet r = s.executeQuery("SELECT last_time_login, last_time_logout, UNIX_TIMESTAMP(last_time_login) FROM login_clock_test")) {
                r.next();
                long now = System.currentTimeMillis();
                for (int column = 1; column <= 2; column++) {
                    long epoch = r.getTimestamp(column).getTime();
                    if (Math.abs(now - epoch) > 5000) throw new AssertionError("Login timestamp skew: " + (epoch - now));
                    if (Math.abs(epoch / 1000 - r.getLong(3)) > 1) throw new AssertionError("JDBC/SQL epoch mismatch");
                }
            }
            System.out.println("PASS: correct login/logout instants with JVM zone " + TimeZone.getDefault().getID());
        } finally {
            DBConnecter.close();
        }
    }
}
