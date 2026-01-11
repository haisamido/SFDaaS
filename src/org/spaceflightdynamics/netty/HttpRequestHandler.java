package org.spaceflightdynamics.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Netty channel handler for HTTP requests.
 * Processes incoming HTTP requests and generates JSON responses.
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final SessionManager sessionManager;
    private final String contextPath;

    public HttpRequestHandler(SessionManager sessionManager, String contextPath) {
        this.sessionManager = sessionManager;
        this.contextPath = contextPath;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // Get remote address
            String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

            // Parse URI and remove context path
            String uri = request.uri();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(uri);
            String path = queryDecoder.path();

            // Remove context path prefix if present
            if (path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }

            // Check if this is a static file request
            if (path.equals("/") || path.equals("") || path.equals("/index.html")) {
                serveStaticFile(ctx, request, "WebContent/index.html", "text/html");
                return;
            } else if (path.startsWith("/static/") || path.endsWith(".html") || path.endsWith(".css") ||
                       path.endsWith(".js") || path.endsWith(".ico")) {
                // Serve other static files from WebContent
                String filePath = "WebContent" + path;
                String contentType = getContentType(path);
                serveStaticFile(ctx, request, filePath, contentType);
                return;
            }

            // API endpoints - JSON responses
            String responseJson;
            HttpResponseStatus status = HttpResponseStatus.OK;

            // Extract query parameters
            Map<String, String> params = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : queryDecoder.parameters().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    params.put(entry.getKey(), entry.getValue().get(0));
                }
            }

            // Handle session cookie
            HttpSession session = getOrCreateSession(request);

            // Update last accessed time
            session.updateLastAccessedTime();

            // Route based on path
            if (path.equals("/orekit/propagate/usage") || path.equals("/orekit/propagate/usage/")) {
                responseJson = RouteHandler.handleUsage(request, session, remoteAddress);
            } else if (path.equals("/orekit/propagate") || path.equals("/orekit/propagate/")) {
                responseJson = RouteHandler.handlePropagate(request, session, params, remoteAddress);
            } else {
                responseJson = RouteHandler.handle404(path);
                status = HttpResponseStatus.NOT_FOUND;
            }

            // Create JSON response
            sendJsonResponse(ctx, request, responseJson, status);

        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = JsonResponseBuilder.buildErrorResponse(
                    "Internal server error: " + e.getMessage(),
                    500);
            sendJsonResponse(ctx, request, errorJson, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                   String responseJson, HttpResponseStatus status) {
        ByteBuf content = Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                content);

        // Set response headers
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        // Handle keep-alive
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write response and close if needed
        if (keepAlive) {
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void serveStaticFile(ChannelHandlerContext ctx, FullHttpRequest request,
                                  String filePath, String contentType) {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            String errorJson = JsonResponseBuilder.buildErrorResponse(
                    "File not found: " + filePath,
                    404);
            sendJsonResponse(ctx, request, errorJson, HttpResponseStatus.NOT_FOUND);
            return;
        }

        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            ByteBuf content = Unpooled.wrappedBuffer(fileContent);

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content);

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            if (keepAlive) {
                ctx.writeAndFlush(response);
            } else {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }

        } catch (IOException e) {
            e.printStackTrace();
            String errorJson = JsonResponseBuilder.buildErrorResponse(
                    "Error reading file: " + e.getMessage(),
                    500);
            sendJsonResponse(ctx, request, errorJson, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    /**
     * Retrieves existing session from cookie or creates a new one.
     */
    private HttpSession getOrCreateSession(FullHttpRequest request) {
        String sessionId = null;

        // Try to extract JSESSIONID from cookies
        String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieHeader != null) {
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
            for (Cookie cookie : cookies) {
                if ("JSESSIONID".equals(cookie.name())) {
                    sessionId = cookie.value();
                    break;
                }
            }
        }

        // Get existing session or create new one
        HttpSession session = sessionManager.getSession(sessionId, true);

        return session;
    }

    /**
     * Creates a Set-Cookie header for JSESSIONID.
     */
    private String createSessionCookie(String sessionId) {
        io.netty.handler.codec.http.cookie.DefaultCookie cookie =
                new io.netty.handler.codec.http.cookie.DefaultCookie("JSESSIONID", sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return ServerCookieEncoder.STRICT.encode(cookie);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();

        String errorJson = JsonResponseBuilder.buildErrorResponse(
                "Internal server error: " + cause.getMessage(),
                500);

        ByteBuf content = Unpooled.copiedBuffer(errorJson, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                content);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
