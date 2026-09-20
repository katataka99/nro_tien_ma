/*     */
package network.server;

/*     */
/*     */

import network.session.ISession;
/*     */ import network.session.Session;
/*     */ import network.session.SessionFactory;
/*     */ import java.io.IOException;
/*     */ import java.net.ServerSocket;
/*     */ import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;

/*     */
/*     */
/*     */ public class EMTIServer
        /*     */ implements InEMTIServer /*     */ {

    /*     */ private static EMTIServer I;
    /*     */    private int port;
    /*     */    private ServerSocket serverListen;
    /*     */    private Class sessionClone;

    /*     */
    /*     */
    public static EMTIServer gI() {
        /*  22 */
        if (I == null) {
            /*  23 */
            I = new EMTIServer();
            /*     */
        }
        /*  25 */
        return I;
        /*     */
    }

    /*     */    private boolean start;
    private boolean randomKey;
    private IServerClose serverClose;
    /*     */    private ISessionAcceptHandler acceptHandler;
    /*     */    private Thread loopServer;

    /*     */
    /*     */
    private EMTIServer() {
        /*  32 */
        this.port = -1;
        /*     */
        this.sessionClone = Session.class;
    }

    public static final ConcurrentHashMap<String, Integer> firewall = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> deviceFirewall = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> firewallDownDataGame = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> dataDownloadWindowStartedAt = new ConcurrentHashMap<>();
    public static int maxConnectionsPerIp = 5;
    public static int maxConnectionsPerDevice = 5;
    private static final int MAX_DATA_DOWNLOAD_REQUESTS_PER_WINDOW = 22;
    private static final long DATA_DOWNLOAD_WINDOW_MS = 60_000L;

    private static boolean acquireIpSlot(String ip) {
        AtomicBoolean acquired = new AtomicBoolean(false);
        firewall.compute(ip, (key, current) -> {
            int count = current == null ? 0 : current;
            if (count >= maxConnectionsPerIp) {
                return count;
            }
            acquired.set(true);
            return count + 1;
        });
        return acquired.get();
    }

    public static void releaseIpSlot(String ip) {
        if (ip == null) {
            return;
        }
        firewall.computeIfPresent(ip, (key, current) -> current <= 1 ? null : current - 1);
    }

    public static boolean allowDataDownload(String ip) {
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);
        firewallDownDataGame.compute(ip, (key, current) -> {
            Long startedAt = dataDownloadWindowStartedAt.get(key);
            if (current == null || startedAt == null || now - startedAt >= DATA_DOWNLOAD_WINDOW_MS) {
                dataDownloadWindowStartedAt.put(key, now);
                allowed.set(true);
                return 1;
            }
            int next = current + 1;
            allowed.set(next <= MAX_DATA_DOWNLOAD_REQUESTS_PER_WINDOW);
            return next;
        });
        return allowed.get();
    }

    /*     */
    /*     */
    /*     */
    /*     */
    /*     */
    public InEMTIServer init() {
        /*  44 */
        this.loopServer = new Thread(this);
        /*  45 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public InEMTIServer start(int port) throws Exception {
        /*  50 */
        if (port < 0) {
            /*  51 */
            throw new Exception("Vui lòng khởi tạo port server!");
            /*     */
        }
        /*  53 */
        if (this.acceptHandler == null) {
            /*  54 */
            throw new Exception("AcceptHandler chưa được khởi tạo!");
            /*     */
        }
        /*  56 */
        if (!ISession.class.isAssignableFrom(this.sessionClone)) {
            /*  57 */
            throw new Exception("Type session clone không hợp lệ!");
            /*     */
        }
        /*     */
        try {
            /*  60 */
            this.port = port;
            /*  61 */
            this.serverListen = new ServerSocket(port);
            /*  62 */
        } catch (IOException ex) {
            /*  63 */
            System.out.println("Lỗi khởi tạo server tại port " + port);
            /*  64 */
            System.exit(0);
            /*     */
        }
        /*  66 */
        this.start = true;
        /*  67 */
        this.loopServer.start();
        /*  68 */
        System.out.println("Server Girlkun đang chạy tại port " + this.port);
        /*  69 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public InEMTIServer close() {
        /*  74 */
        this.start = false;
        /*  75 */
        if (this.serverListen != null) {
            /*     */
            try {
                /*  77 */
                this.serverListen.close();
                /*  78 */
            } catch (IOException ex) {
                /*  79 */
                ex.printStackTrace();
                /*     */
            }
            /*     */
        }
        /*  82 */
        if (this.serverClose != null) {
            /*  83 */
            this.serverClose.serverClose();
            /*     */
        }
        /*  85 */
        System.out.println("Server Girlkun đã đóng!");
        /*  86 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public InEMTIServer dispose() {
        /*  91 */
        this.acceptHandler = null;
        /*  92 */
        this.loopServer = null;
        /*  93 */
        this.serverListen = null;
        /*  94 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public InEMTIServer setAcceptHandler(ISessionAcceptHandler handler) {
        /*  99 */
        this.acceptHandler = handler;
        /* 100 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    public void run() {
        while (this.start) {
            try {
                Socket socket = this.serverListen.accept();
                String ip = socket.getInetAddress().getHostAddress();

                // === Layer 1: Anti-DDoS - Rate limit & Blacklist ===
                if (!AntiLoginDDoS.checkNewConnection(ip)) {
                    socket.close();
                    continue;
                }

                // Reserve the IP slot before starting the session threads. If a client
                // disconnects during initialization, Session.disconnect() releases the
                // same slot instead of leaving a stale counter behind.
                if (!acquireIpSlot(ip)) {
                    System.out.println("[Anti-Clone] Blocked IP: " + ip + " - Đã đạt " + maxConnectionsPerIp + " kết nối!");
                    socket.close();
                } else {
                    try {
                        ISession session = SessionFactory.gI().cloneSession(this.sessionClone, socket);
                        this.acceptHandler.sessionInit(session);
                        EmtiSessionManager.gI().putSession(session);
                    } catch (Exception ex) {
                        releaseIpSlot(ip);
                        try {
                            socket.close();
                        } catch (IOException ignored) {
                        }
                        throw ex;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                Logger.getLogger(EMTIServer.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }
//    /*     */ public void run() {
//        /* 105 */ while (this.start) {
//            /*     */ try {
//                /* 107 */ Socket socket = this.serverListen.accept();
//                /* 108 */ ISession session = SessionFactory.gI().cloneSession(this.sessionClone, socket);
//                /* 109 */ this.acceptHandler.sessionInit(session);
//                /* 110 */ GirlkunSessionManager.gI().putSession(session);
//                /* 111 */            } catch (IOException ex) {
//                /* 112 */ ex.printStackTrace();
//                /* 113 */            } catch (Exception ex) {
//                /* 114 */ Logger.getLogger(EMTIServer.class.getName()).log(Level.SEVERE, (String) null, ex);
//                /*     */            }
//            /*     */        }
//        /*     */    }

    /*     */
    /*     */
    public InEMTIServer setDoSomeThingWhenClose(IServerClose serverClose) {
        /* 121 */
        this.serverClose = serverClose;
        /* 122 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public InEMTIServer randomKey(boolean isRandom) {
        /* 127 */
        this.randomKey = isRandom;
        /* 128 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public boolean isRandomKey() {
        /* 133 */
        return this.randomKey;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public InEMTIServer setTypeSessioClone(Class clazz) throws Exception {
        /* 138 */
        this.sessionClone = clazz;
        /* 139 */
        return this;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public ISessionAcceptHandler getAcceptHandler() throws Exception {
        /* 144 */
        if (this.acceptHandler == null) {
            /* 145 */
            throw new Exception("AcceptHandler chưa được khởi tạo!");
            /*     */
        }
        /* 147 */
        return this.acceptHandler;
        /*     */
    }

    /*     */
    /*     */
    /*     */
    public void stopConnect() {
        /* 152 */
        this.start = false;
        /*     */
    }
    /*     */
}


/* Location:              C:\Users\VoHoangKiet\Downloads\TEA_V5\lib\GirlkunNetwork.jar!\com\girlkun\network\server\EMTIServer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */
