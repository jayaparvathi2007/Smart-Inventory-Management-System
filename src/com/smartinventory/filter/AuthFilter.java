package com.smartinventory.filter;

import com.smartinventory.util.JsonUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // Set UTF-8 encoding
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Allow public static resources and authentication endpoint
        if (isPublicPath(path)) {
            chain.doFilter(req, resp);
            return;
        }

        // Verify session
        HttpSession session = request.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);

        if (isLoggedIn) {
            chain.doFilter(req, resp);
        } else {
            if (path.startsWith("/api/")) {
                JsonUtil.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Session expired or unauthorized. Please log in.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login.html");
            }
        }
    }

    private boolean isPublicPath(String path) {
        if (path.equals("") || path.equals("/") || path.equals("/login.html") || path.equals("/login")) {
            return true;
        }
        if (path.startsWith("/api/auth")) {
            return true;
        }
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.endsWith(".ico") || path.endsWith(".png") || path.endsWith(".svg")) {
            return true;
        }
        return false;
    }

    @Override
    public void destroy() {}
}
