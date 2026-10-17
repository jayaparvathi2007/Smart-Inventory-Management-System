package com.smartinventory.server;

import com.smartinventory.filter.AuthFilter;
import com.smartinventory.servlet.*;
import com.smartinventory.util.DBUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Built-in Lightweight HTTP & Servlet Server.
 * Allows running the entire application with pure Java without requiring external Tomcat setup,
 * while maintaining 100% compatibility with standard Java Servlets and web.xml.
 */
public class EmbeddedServer {

    private static final Logger LOGGER = Logger.getLogger(EmbeddedServer.class.getName());
    private static final Map<String, SimpleSession> SESSIONS = new ConcurrentHashMap<>();
    private static final String COOKIE_NAME = "JSESSIONID";

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("==================================================================");
        System.out.println("     SMART INVENTORY MANAGEMENT SYSTEM - APPLICATION SERVER       ");
        System.out.println("==================================================================");

        // Test database connectivity
        System.out.println("[*] Checking Database connection...");
        if (DBUtil.testConnection()) {
            System.out.println("[+] Database connected successfully to: " + DBUtil.getJdbcUrl());
        } else {
            System.err.println("[-] Warning: Could not connect to MySQL database at: " + DBUtil.getJdbcUrl());
            System.err.println("[-] Please ensure MySQL is running and credentials in 'config/db.properties' are correct.");
            System.err.println("[-] If you haven't created the database yet, run: mysql -u root -p < database/schema.sql");
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // Initialize servlets
            AuthServlet authServlet = new AuthServlet();
            DashboardServlet dashboardServlet = new DashboardServlet();
            ProductServlet productServlet = new ProductServlet();
            CategoryServlet categoryServlet = new CategoryServlet();
            SupplierServlet supplierServlet = new SupplierServlet();
            StockServlet stockServlet = new StockServlet();
            ReportServlet reportServlet = new ReportServlet();
            AuthFilter authFilter = new AuthFilter();

            // Locate webapp directory
            File webappDir = findWebappDir();
            System.out.println("[*] Web resources located at: " + webappDir.getAbsolutePath());

            // Universal Handler delegating to Servlets and Static Files
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    try {
                        dispatch(exchange, webappDir, authFilter, authServlet, dashboardServlet,
                                productServlet, categoryServlet, supplierServlet, stockServlet, reportServlet);
                    } catch (Exception e) {
                        Throwable cause = e;
                        while (cause.getCause() != null && cause.getCause() != cause) {
                            cause = cause.getCause();
                        }
                        LOGGER.severe("Request error: " + cause.getMessage());
                        cause.printStackTrace();
                        byte[] msg = ("500 Internal Server Error: " + cause.getMessage()).getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(500, msg.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(msg);
                        }
                    }
                }
            });

            server.start();
            System.out.println("------------------------------------------------------------------");
            System.out.println("[+] Server is running at: http://localhost:" + port + "/");
            System.out.println("[+] Default Login:  admin / admin123  (or manager / manager123)");
            System.out.println("------------------------------------------------------------------");
            System.out.println("[*] Press Ctrl+C in terminal to stop the server.");
        } catch (Exception e) {
            System.err.println("[-] Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static File findWebappDir() {
        File dir = new File("webapp");
        if (dir.exists() && dir.isDirectory()) return dir;
        dir = new File("../webapp");
        if (dir.exists() && dir.isDirectory()) return dir;
        dir = new File("src/main/webapp");
        if (dir.exists() && dir.isDirectory()) return dir;
        return new File(".");
    }

    private static void dispatch(HttpExchange exchange, File webappDir, AuthFilter authFilter,
                                 AuthServlet authServlet, DashboardServlet dashboardServlet,
                                 ProductServlet productServlet, CategoryServlet categoryServlet,
                                 SupplierServlet supplierServlet, StockServlet stockServlet,
                                 ReportServlet reportServlet) throws Exception {

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if (path == null || path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }

        // Identify session
        String sessionId = extractCookie(exchange, COOKIE_NAME);
        SimpleSession session = null;
        boolean isNewSession = false;
        if (sessionId != null && SESSIONS.containsKey(sessionId)) {
            session = SESSIONS.get(sessionId);
            if (session.isExpired()) {
                SESSIONS.remove(sessionId);
                session = null;
            }
        }
        if (session == null) {
            sessionId = UUID.randomUUID().toString();
            session = new SimpleSession(sessionId);
            SESSIONS.put(sessionId, session);
            isNewSession = true;
        }
        session.access();

        final String finalSessionId = sessionId;
        final boolean setCookieHeader = isNewSession;
        final SimpleSession currentSession = session;

        // Parse query parameters
        Map<String, String[]> queryParams = parseQueryParams(uri.getRawQuery());

        // Read request body
        byte[] bodyBytes;
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            bodyBytes = baos.toByteArray();
        }

        // Build Mock HttpServletRequest & HttpServletResponse Proxies
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        SimpleResponseState respState = new SimpleResponseState(exchange, responseBody);
        if (setCookieHeader) {
            respState.setHeader("Set-Cookie", COOKIE_NAME + "=" + finalSessionId + "; Path=/; HttpOnly");
        }

        HttpServletRequest reqProxy = createRequestProxy(exchange, path, queryParams, bodyBytes, currentSession);
        HttpServletResponse respProxy = createResponseProxy(exchange, respState, finalSessionId, setCookieHeader);

        // Filter Chain
        final String targetPath = path;
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse resp = (HttpServletResponse) response;

                try {
                    if (targetPath.startsWith("/api/auth")) {
                        invokeServlet(authServlet, req, resp);
                    } else if (targetPath.startsWith("/api/dashboard")) {
                        invokeServlet(dashboardServlet, req, resp);
                    } else if (targetPath.startsWith("/api/products")) {
                        invokeServlet(productServlet, req, resp);
                    } else if (targetPath.startsWith("/api/categories")) {
                        invokeServlet(categoryServlet, req, resp);
                    } else if (targetPath.startsWith("/api/suppliers")) {
                        invokeServlet(supplierServlet, req, resp);
                    } else if (targetPath.startsWith("/api/stock")) {
                        invokeServlet(stockServlet, req, resp);
                    } else if (targetPath.startsWith("/api/reports")) {
                        invokeServlet(reportServlet, req, resp);
                    } else {
                        // Serve static file
                        serveStaticFile(exchange, webappDir, targetPath, respState);
                    }
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            }
        };

        authFilter.doFilter(reqProxy, respProxy, chain);

        // Finish response if not already sent by static file
        if (!respState.isCommitted()) {
            respState.commit();
        }
    }

    private static void invokeServlet(HttpServlet servlet, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        servlet.service(req, resp);
    }

    private static void serveStaticFile(HttpExchange exchange, File webappDir, String path, SimpleResponseState respState) throws IOException {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        File file = new File(webappDir, cleanPath).getCanonicalFile();

        // Safety check to avoid path traversal
        if (!file.getPath().startsWith(webappDir.getCanonicalPath()) || !file.exists() || file.isDirectory()) {
            respState.setStatus(404);
            respState.setContentType("text/html;charset=UTF-8");
            respState.getWriter().write("<h3>404 Not Found</h3><p>Resource: " + path + " does not exist.</p>");
            return;
        }

        String mimeType = getContentType(file.getName());
        respState.setContentType(mimeType);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        respState.getOutputStream().write(fileBytes);
    }

    private static String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".csv")) return "text/csv; charset=UTF-8";
        return "application/octet-stream";
    }

    private static Map<String, String[]> parseQueryParams(String query) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
                String val = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : "";
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
            }
        }
        Map<String, String[]> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
        return result;
    }

    private static String extractCookie(HttpExchange exchange, String name) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders != null) {
            for (String header : cookieHeaders) {
                String[] cookies = header.split(";");
                for (String c : cookies) {
                    String[] parts = c.trim().split("=", 2);
                    if (parts.length == 2 && parts[0].trim().equals(name)) {
                        return parts[1].trim();
                    }
                }
            }
        }
        return null;
    }

    private static HttpServletRequest createRequestProxy(HttpExchange exchange, String path,
                                                         Map<String, String[]> queryParams,
                                                         byte[] bodyBytes, SimpleSession session) {

        return (HttpServletRequest) Proxy.newProxyInstance(
                EmbeddedServer.class.getClassLoader(),
                new Class[]{HttpServletRequest.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        switch (name) {
                            case "getMethod":
                                return exchange.getRequestMethod();
                            case "getRequestURI":
                                return path;
                            case "getRequestURL":
                                return new StringBuffer("http://localhost:8080" + path);
                            case "getContextPath":
                                return "";
                            case "getServletPath":
                                return path;
                            case "getPathInfo":
                                return null;
                            case "getQueryString":
                                return exchange.getRequestURI().getRawQuery();
                            case "getParameter":
                                String pKey = (String) args[0];
                                String[] vals = queryParams.get(pKey);
                                return (vals != null && vals.length > 0) ? vals[0] : null;
                            case "getParameterValues":
                                return queryParams.get((String) args[0]);
                            case "getParameterMap":
                                return queryParams;
                            case "getParameterNames":
                                return Collections.enumeration(queryParams.keySet());
                            case "getReader":
                                return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyBytes), StandardCharsets.UTF_8));
                            case "getInputStream":
                                return new ServletInputStream() {
                                    private final ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
                                    @Override public boolean isFinished() { return bais.available() == 0; }
                                    @Override public boolean isReady() { return true; }
                                    @Override public void setReadListener(ReadListener readListener) {}
                                    @Override public int read() { return bais.read(); }
                                };
                            case "getSession":
                                boolean create = args == null || args.length == 0 || (Boolean) args[0];
                                return session;
                            case "getContentType":
                                return exchange.getRequestHeaders().getFirst("Content-Type");
                            case "getHeader":
                                return exchange.getRequestHeaders().getFirst((String) args[0]);
                            case "getHeaders":
                                List<String> h = exchange.getRequestHeaders().get((String) args[0]);
                                return h != null ? Collections.enumeration(h) : Collections.emptyEnumeration();
                            case "getHeaderNames":
                                return Collections.enumeration(exchange.getRequestHeaders().keySet());
                            case "setCharacterEncoding":
                                return null;
                            case "getCookies":
                                return new Cookie[]{new Cookie(COOKIE_NAME, session.getId())};
                            default:
                                return getDefaultValue(method.getReturnType());
                        }
                    }
                }
        );
    }

    private static HttpServletResponse createResponseProxy(HttpExchange exchange, SimpleResponseState state,
                                                           String sessionId, boolean setCookieHeader) {

        return (HttpServletResponse) Proxy.newProxyInstance(
                EmbeddedServer.class.getClassLoader(),
                new Class[]{HttpServletResponse.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        switch (name) {
                            case "setStatus":
                                state.setStatus((Integer) args[0]);
                                return null;
                            case "getStatus":
                                return state.getStatus();
                            case "setContentType":
                                state.setContentType((String) args[0]);
                                return null;
                            case "setHeader":
                                state.setHeader((String) args[0], (String) args[1]);
                                return null;
                            case "addHeader":
                                state.addHeader((String) args[0], (String) args[1]);
                                return null;
                            case "getWriter":
                                return state.getWriter();
                            case "getOutputStream":
                                return state.getOutputStream();
                            case "sendRedirect":
                                String loc = (String) args[0];
                                state.setStatus(HttpServletResponse.SC_FOUND);
                                state.setHeader("Location", loc);
                                state.commit();
                                return null;
                            case "sendError":
                                int errCode = (Integer) args[0];
                                String errMsg = args.length > 1 ? (String) args[1] : "";
                                state.setStatus(errCode);
                                state.getWriter().write("{\"success\":false,\"message\":\"" + errMsg + "\"}");
                                return null;
                            case "setCharacterEncoding":
                                return null;
                            default:
                                return getDefaultValue(method.getReturnType());
                        }
                    }
                }
        );
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        return null;
    }

    private static class SimpleResponseState {
        private final HttpExchange exchange;
        private final ByteArrayOutputStream buffer;
        private int status = 200;
        private String contentType = "text/html; charset=UTF-8";
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private PrintWriter writer;
        private ServletOutputStream sos;
        private boolean committed = false;

        public SimpleResponseState(HttpExchange exchange, ByteArrayOutputStream buffer) {
            this.exchange = exchange;
            this.buffer = buffer;
        }

        public void setStatus(int status) { this.status = status; }
        public int getStatus() { return status; }
        public void setContentType(String contentType) {
            this.contentType = contentType;
            setHeader("Content-Type", contentType);
        }
        public void setHeader(String name, String value) {
            List<String> list = new ArrayList<>();
            list.add(value);
            headers.put(name, list);
        }
        public void addHeader(String name, String value) {
            headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }

        public PrintWriter getWriter() {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8), true);
            }
            return writer;
        }

        public ServletOutputStream getOutputStream() {
            if (sos == null) {
                sos = new ServletOutputStream() {
                    @Override public boolean isReady() { return true; }
                    @Override public void setWriteListener(WriteListener writeListener) {}
                    @Override public void write(int b) { buffer.write(b); }
                    @Override public void write(byte[] b, int off, int len) { buffer.write(b, off, len); }
                };
            }
            return sos;
        }

        public boolean isCommitted() { return committed; }

        public void commit() throws IOException {
            if (committed) return;
            committed = true;
            if (writer != null) {
                writer.flush();
            }

            // Copy custom headers
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                exchange.getResponseHeaders().put(entry.getKey(), entry.getValue());
            }

            if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }

            byte[] bytes = buffer.toByteArray();
            exchange.sendResponseHeaders(status, bytes.length > 0 ? bytes.length : -1);
            if (bytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }

    private static class SimpleSession implements HttpSession {
        private final String id;
        private final long creationTime = System.currentTimeMillis();
        private long lastAccessedTime = System.currentTimeMillis();
        private int maxInactiveInterval = 3600; // 1 hour
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private boolean valid = true;

        public SimpleSession(String id) { this.id = id; }
        public void access() { this.lastAccessedTime = System.currentTimeMillis(); }
        public boolean isExpired() {
            return !valid || (System.currentTimeMillis() - lastAccessedTime > maxInactiveInterval * 1000L);
        }

        @Override public long getCreationTime() { return creationTime; }
        @Override public String getId() { return id; }
        @Override public long getLastAccessedTime() { return lastAccessedTime; }
        @Override public ServletContext getServletContext() { return null; }
        @Override public void setMaxInactiveInterval(int interval) { this.maxInactiveInterval = interval; }
        @Override public int getMaxInactiveInterval() { return maxInactiveInterval; }
        @Override public HttpSessionContext getSessionContext() { return null; }
        @Override public Object getAttribute(String name) { return attributes.get(name); }
        @Override public Object getValue(String name) { return attributes.get(name); }
        @Override public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributes.keySet()); }
        @Override public String[] getValueNames() { return attributes.keySet().toArray(new String[0]); }
        @Override public void setAttribute(String name, Object value) {
            if (value == null) attributes.remove(name);
            else attributes.put(name, value);
        }
        @Override public void putValue(String name, Object value) { setAttribute(name, value); }
        @Override public void removeAttribute(String name) { attributes.remove(name); }
        @Override public void removeValue(String name) { attributes.remove(name); }
        @Override public void invalidate() { valid = false; attributes.clear(); }
        @Override public boolean isNew() { return false; }
    }
}
