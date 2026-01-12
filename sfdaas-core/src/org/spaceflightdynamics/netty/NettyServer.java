package org.spaceflightdynamics.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * Main Netty-based HTTP server for Space Flight Dynamics as a Service (SFDaaS).
 * Standalone server that replaces the previous Tomcat servlet-based architecture.
 *
 * @author Haisam K. Ido <haisam.ido@gmail.com>
 * @license LGPL v3.0
 */
public class NettyServer {

    private final int port;
    private final String contextPath;
    private final SessionManager sessionManager;

    public NettyServer(int port, String contextPath) {
        this.port = port;
        this.contextPath = contextPath;
        this.sessionManager = new SessionManager();
    }

    public void start() throws Exception {
        // Event loop groups for handling connections
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(512 * 1024)) // 512KB max request size
                                    .addLast(new HttpRequestHandler(sessionManager, contextPath));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("======================================================================");
            System.out.println("Space Flight Dynamics as a Service (SFDaaS)");
            System.out.println("======================================================================");
            System.out.println();
            System.out.println("Starting Netty HTTP server...");
            System.out.println("  Port         : " + port);
            System.out.println("  Context Path : " + contextPath);
            System.out.println();

            // Print OreKit data path
            String orekitDataPath = System.getProperty("orekit.data.path", "./data");
            System.out.println("OreKit Configuration:");
            System.out.println("  Data Path    : " + orekitDataPath);
            System.out.println();

            // Bind and start to accept incoming connections
            ChannelFuture future = bootstrap.bind(port).sync();

            System.out.println("======================================================================");
            System.out.println("Server started successfully!");
            System.out.println("======================================================================");
            System.out.println();
            System.out.println("Available endpoints:");
            System.out.println("  Usage        : http://localhost:" + port + contextPath + "/orekit/propagate/usage");
            System.out.println("  Propagation  : http://localhost:" + port + contextPath + "/orekit/propagate");
            System.out.println();
            System.out.println("Press Ctrl+C to stop the server");
            System.out.println("======================================================================");
            System.out.println();

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                System.out.println("Shutting down server...");
                sessionManager.shutdown();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                System.out.println("Server stopped.");
            }));

            // Wait until the server socket is closed
            future.channel().closeFuture().sync();

        } finally {
            // Shutdown event loop groups
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        // Read configuration from system properties
        int port = Integer.parseInt(System.getProperty("server.port", "8080"));
        String contextPath = System.getProperty("server.contextPath", "/SFDaaS");

        // Ensure context path starts with /
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        try {
            NettyServer server = new NettyServer(port, contextPath);
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
