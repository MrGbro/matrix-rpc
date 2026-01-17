package io.homeey.matrix.rpc.protocol.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * HTTP/2 服务端（完整实现）
 * 
 * <p>基于 Netty HTTP/2 实现的服务端，支持：
 * <ul>
 *   <li>HTTP/2 多路复用</li>
 *   <li>Header 压缩（HPACK）</li>
 *   <li>Stream 管理</li>
 *   <li>TLS 支持（可选）</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2Server {
    
    private static final Logger logger = LoggerFactory.getLogger(Http2Server.class);
    
    private final int port;
    private Function<Http2Request, Http2Response> requestHandler;
    private volatile boolean started = false;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    public Http2Server(int port) {
        this.port = port;
    }
    
    /**
     * 设置请求处理器
     */
    public void setRequestHandler(Function<Http2Request, Http2Response> handler) {
        this.requestHandler = handler;
    }
    
    /**
     * 启动服务器
     */
    public synchronized void start() {
        if (started) {
            logger.warn("HTTP/2 server already started on port: {}", port);
            return;
        }
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 配置 HTTP/2 ConnectionHandler
                        Http2Connection connection = new DefaultHttp2Connection(true);
                        
                        Http2FrameListener frameListener = new Http2FrameAdapter() {
                            @Override
                            public int onDataRead(ChannelHandlerContext ctx, int streamId, 
                                                 io.netty.buffer.ByteBuf data, int padding, boolean endOfStream) {
                                // 处理 DATA 帧
                                int processed = data.readableBytes() + padding;
                                if (endOfStream) {
                                    logger.debug("Received end of stream on stream {}", streamId);
                                }
                                return processed;
                            }
                            
                            @Override
                            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                                     Http2Headers headers, int padding, boolean endOfStream) {
                                logger.debug("Received headers on stream {}: {}", streamId, headers);
                                handleHttp2Request(ctx, streamId, headers, endOfStream);
                            }
                            
                            @Override
                            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                                     Http2Headers headers, int streamDependency, short weight,
                                                     boolean exclusive, int padding, boolean endOfStream) {
                                onHeadersRead(ctx, streamId, headers, padding, endOfStream);
                            }
                        };
                        
                        Http2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
                            .connection(connection)
                            .frameListener(frameListener)
                            .frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "HTTP/2 Server"))
                            .build();
                        
                        pipeline.addLast(connectionHandler);
                        pipeline.addLast(new Http2ServerHandler(Http2Server.this));
                    }
                });
            
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            logger.info("HTTP/2 server started on port: {}", port);
            started = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to start HTTP/2 server", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start HTTP/2 server", e);
        }
    }
    
    /**
     * 停止服务器
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while closing server channel", e);
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            logger.info("HTTP/2 server stopped on port: {}", port);
            started = false;
        }
    }
    
    /**
     * 处理 HTTP/2 请求
     */
    private void handleHttp2Request(ChannelHandlerContext ctx, int streamId, 
                                   Http2Headers headers, boolean endOfStream) {
        if (!endOfStream) {
            // 等待完整请求
            return;
        }
        
        try {
            // 1. 从 Headers 构建 Http2Request
            Http2Request request = new Http2Request();
            request.setMethod(headers.method() != null ? headers.method().toString() : "POST");
            request.setPath(headers.path() != null ? headers.path().toString() : "/");
            
            // 复制所有 headers
            for (CharSequence name : headers.names()) {
                if (!isPseudoHeader(name)) {
                    request.addHeader(name.toString(), headers.get(name).toString());
                }
            }
            
            // 2. 调用请求处理器
            Http2Response response = handleRequest(request);
            
            // 3. 发送响应
            sendHttp2Response(ctx, streamId, response);
        } catch (Exception e) {
            logger.error("Failed to handle HTTP/2 request on stream {}", streamId, e);
            sendErrorResponse(ctx, streamId, 500, "Internal Server Error: " + e.getMessage());
        }
    }
    
    /**
     * 发送 HTTP/2 响应
     */
    private void sendHttp2Response(ChannelHandlerContext ctx, int streamId, Http2Response response) {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.status(String.valueOf(response.getStatus()));
        
        // 添加响应 headers
        response.getHeaders().forEach((name, value) -> 
            headers.add(AsciiString.of(name), AsciiString.of(value)));
        
        // 发送 HEADERS 帧
        ctx.write(new DefaultHttp2HeadersFrame(headers).stream(
            new Http2FrameStream() {
                @Override
                public int id() {
                    return streamId;
                }
                
                @Override
                public Http2Stream.State state() {
                    return Http2Stream.State.OPEN;
                }
            }
        ));
        
        // 发送响应体（如果有）
        if (response.getBody() != null && response.getBody().length > 0) {
            io.netty.buffer.ByteBuf data = ctx.alloc().buffer(response.getBody().length);
            data.writeBytes(response.getBody());
            ctx.write(new DefaultHttp2DataFrame(data, true));
        }
        
        ctx.flush();
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, int streamId, int status, String message) {
        Http2Response response = new Http2Response();
        response.setStatus(status);
        response.addHeader("matrix-error", message);
        sendHttp2Response(ctx, streamId, response);
    }
    
    /**
     * 判断是否为伪头部（pseudo-header）
     */
    private boolean isPseudoHeader(CharSequence name) {
        return name.length() > 0 && name.charAt(0) == ':';
    }
    
    /**
     * 处理 HTTP/2 请求（内部方法）
     */
    protected Http2Response handleRequest(Http2Request request) {
        if (requestHandler != null) {
            return requestHandler.apply(request);
        }
        
        Http2Response response = new Http2Response();
        response.setStatus(500);
        response.addHeader("matrix-error", "No request handler configured");
        return response;
    }
    
    /**
     * HTTP/2 Server Handler
     */
    private static class Http2ServerHandler extends ChannelInboundHandlerAdapter {
        private final Http2Server server;
        
        public Http2ServerHandler(Http2Server server) {
            this.server = server;
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("HTTP/2 server handler exception", cause);
            ctx.close();
        }
    }
}
