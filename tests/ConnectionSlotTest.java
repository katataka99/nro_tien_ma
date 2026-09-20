import java.net.ServerSocket;
import java.net.Socket;
import network.server.EMTIServer;
import server.Manager;
import server.ServerManager;
import server.io.MySession;

public class ConnectionSlotTest {

    public static void main(String[] args) throws Exception {
        int oldMaxPerIp = Manager.MAX_PER_IP;
        Manager.MAX_PER_IP = 2;
        String testIp = "connection-slot-test";
        assertTrue(ServerManager.tryAcquireClientIpSlot(testIp), "first IP slot");
        assertTrue(ServerManager.tryAcquireClientIpSlot(testIp), "second IP slot");
        assertFalse(ServerManager.tryAcquireClientIpSlot(testIp), "IP limit");
        ServerManager.releaseClientIpSlot(testIp);
        ServerManager.releaseClientIpSlot(testIp);
        ServerManager.releaseClientIpSlot(testIp);
        assertEquals(0, ServerManager.getClientIpSlotCount(testIp), "IP slot cleanup");
        Manager.MAX_PER_IP = oldMaxPerIp;

        EMTIServer.deviceFirewall.clear();
        EMTIServer.maxConnectionsPerDevice = 1;
        assertTrue(EMTIServer.acquireDeviceSlot("device-1"), "first device slot");
        assertFalse(EMTIServer.acquireDeviceSlot("device-1"), "device limit");
        EMTIServer.releaseDeviceSlot("device-1");
        EMTIServer.releaseDeviceSlot("device-1");
        assertFalse(EMTIServer.deviceFirewall.containsKey("device-1"), "device slot cleanup");

        String sessionDevice = "connection-slot-device";
        try (ServerSocket listener = new ServerSocket(0);
             Socket client = new Socket("127.0.0.1", listener.getLocalPort());
             Socket server = listener.accept()) {
            MySession session = new MySession(server);
            assertTrue(session.acquireIpSlot(), "session IP slot");
            assertTrue(session.acquireDeviceSlot(sessionDevice), "session device slot");
            session.disconnect();
            session.disconnect();
            assertEquals(0, ServerManager.getClientIpSlotCount(session.ipAddress),
                    "idempotent session IP cleanup");
            assertFalse(EMTIServer.deviceFirewall.containsKey(sessionDevice),
                    "idempotent session device cleanup");
        }

        System.out.println("PASS: IP and device connection slots release without stale counters");
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError("Expected true: " + message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError("Expected false: " + message);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
