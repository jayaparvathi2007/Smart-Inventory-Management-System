package com.smartinventory.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    public static Gson getGson() {
        return GSON;
    }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * Reads complete JSON body from HttpServletRequest.
     */
    public static String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Send standard JSON response.
     */
    public static void sendResponse(HttpServletResponse response, int statusCode, boolean success, String message, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", success);
        payload.put("message", message != null ? message : "");
        if (data != null) {
            payload.put("data", data);
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(GSON.toJson(payload));
            out.flush();
        }
    }

    public static void sendSuccess(HttpServletResponse response, String message, Object data) throws IOException {
        sendResponse(response, HttpServletResponse.SC_OK, true, message, data);
    }

    public static void sendSuccess(HttpServletResponse response, String message) throws IOException {
        sendResponse(response, HttpServletResponse.SC_OK, true, message, null);
    }

    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        sendResponse(response, statusCode, false, message, null);
    }
}
