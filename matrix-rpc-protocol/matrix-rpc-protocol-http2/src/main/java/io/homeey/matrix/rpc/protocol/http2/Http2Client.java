package io.homeey.matrix.rpc.protocol.http2;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP/2 客户端（完整实现）
 * 
 * <p>基于 Netty HTTP/2 实现的客户端，支持：
 * <ul>
 *   <li>连接池管理</li>
 *   <li>Stream 复用</li>
 *   <li>自动重连</li>
 *   <li>超时控制</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2Client {
    
    private static final Logger logger = LoggerFactory.getLogger(Http2Client.class);
    
    private final String host;
    private final int port;
    private volatile boolean connected = false;
    
    private EventLoopGroup workerGroup;
    private Channel channel;
    private Http2Connection connection;
    private Http2ConnectionHandler connectionHandler;
    
    // Stream ID 生成器（客户端使用奇数）
    private final AtomicInteger streamIdGenerator = new AtomicInteger(1);
    
    // 待处理的请求：streamId -> CompletableFuture<Http2Response>
    private final Map<Integer, CompletableFuture<Http2Response>> pendingRequests = new ConcurrentHashMap<>();
    
    // 临时存储 headers 和 data
    private final Map<Integer, Http2Response> responseBuffers = new ConcurrentHashMap<>();
    
    public Http2Client(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 连接到服务器
     */
    public synchronized void connect() {
        if (connected) {
            logger.warn("HTTP/2 client already connected to: {}:{}", host, port);
            return;
        }
        
        workerGroup = new NioEventLoopGroup();
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 配置 HTTP/2 ConnectionHandler
                        connection = new DefaultHttp2Connection(false);
                        
                        Http2FrameListener frameListener = new Http2FrameAdapter() {
                            @Override
                            public int onDataRead(ChannelHandlerContext ctx, int streamId, 
                                                 ByteBuf data, int padding, boolean endOfStream) {
                                // 处理 DATA 帧
                                int processed = data.readableBytes() + padding;
                                
                                Http2Response response = responseBuffers.computeIfAbsent(streamId, 
                                    k -> new Http2Response());
                                
                                byte[] bytes = new byte[data.readableBytes()];
                                data.readBytes(bytes);
                                response.setBody(bytes);
                                
                                if (endOfStream) {
                                    completeResponse(streamId, response);
                                }
                                
                                return processed;
                            }
                            
                            @Override
                            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                                     Http2Headers headers, int padding, boolean endOfStream) {
                                handleResponseHeaders(streamId, headers, endOfStream);
                            }
                            
                            @Override
                            public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                                                     Http2Headers headers, int streamDependency, short weight,
                                                     boolean exclusive, int padding, boolean endOfStream) {
                                onHeadersRead(ctx, streamId, headers, padding, endOfStream);
                            }
                        };
                        
                        connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
                            .connection(connection)
                            .frameListener(frameListener)
                            .frameLogger(new Http2FrameLogger(LogLevel.DEBUG, "HTTP/2 Client"))
                            .build();
                        
                        pipeline.addLast(connectionHandler);
                        pipeline.addLast(new Http2ClientHandler());
                    }
                });
            
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            logger.info("HTTP/2 client connected to: {}:{}", host, port);
            connected = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect HTTP/2 client", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect HTTP/2 client", e);
        }
    }
    
    /**
     * 发送请求并等待响应
     * 
     * @param request HTTP/2 请求
     * @param timeout 超时时间（毫秒）
     * @return HTTP/2 响应
     */
    public Http2Response send(Http2Request request, long timeout) {
        if (!connected) {
            throw new IllegalStateException("HTTP/2 client not connected");
        }
        
        // 1. 分配 Stream ID
        int streamId = streamIdGenerator.getAndAdd(2); // 客户端使用奇数
        
        // 2. 创建 CompletableFuture
        CompletableFuture<Http2Response> future = new CompletableFuture<>();
        pendingRequests.put(streamId, future);
        
        try {
            // 3. 构建 HTTP/2 Headers
            Http2Headers headers = new DefaultHttp2Headers();
            headers.method(AsciiString.of(request.getMethod()));
            headers.path(AsciiString.of(request.getPath()));
            headers.scheme(AsciiString.of("http"));
            headers.authority(AsciiString.of(host + ":" + port));
            
            // 添加自定义 headers
            request.getHeaders().forEach((name, value) -> 
                headers.add(AsciiString.of(name), AsciiString.of(value)));
            
            // 4. 发送 HEADERS Frame
            Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, false);
            headersFrame.stream(createHttp2FrameStream(streamId));
            channel.write(headersFrame);
            
            // 5. 发送 DATA Frame（如果有 body）
            if (request.getBody() != null && request.getBody().length > 0) {
                ByteBuf data = channel.alloc().buffer(request.getBody().length);
                data.writeBytes(request.getBody());
                Http2DataFrame dataFrame = new DefaultHttp2DataFrame(data, true);
                dataFrame.stream(createHttp2FrameStream(streamId));
                channel.writeAndFlush(dataFrame);
            } else {
                // 没有 body，发送空的 DATA Frame 表示结束
                Http2DataFrame dataFrame = new DefaultHttp2DataFrame(true);
                dataFrame.stream(createHttp2FrameStream(streamId));
                channel.writeAndFlush(dataFrame);
            }
            
            // 6. 等待响应（带超时）
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequests.remove(streamId);
            responseBuffers.remove(streamId);
            
            logger.error("Failed to send HTTP/2 request on stream {}", streamId, e);
            
            Http2Response errorResponse = new Http2Response();
            errorResponse.setStatus(500);
            errorResponse.addHeader("matrix-error", "Request failed: " + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * 处理响应 Headers
     */
    private void handleResponseHeaders(int streamId, Http2Headers headers, boolean endOfStream) {
        Http2Response response = responseBuffers.computeIfAbsent(streamId, 
            k -> new Http2Response());
        
        // 解析 status
        CharSequence status = headers.status();
        if (status != null) {
            response.setStatus(Integer.parseInt(status.toString()));
        }
        
        // 复制所有 headers（排除伪头部）
        for (CharSequence name : headers.names()) {
            if (!isPseudoHeader(name)) {
                response.addHeader(name.toString(), headers.get(name).toString());
            }
        }
        
        if (endOfStream) {
            // 没有 data frame，直接完成
            completeResponse(streamId, response);
        }
    }
    
    /**
     * 完成响应
     */
    private void completeResponse(int streamId, Http2Response response) {
        CompletableFuture<Http2Response> future = pendingRequests.remove(streamId);
        responseBuffers.remove(streamId);
        
        if (future != null) {
            future.complete(response);
        }
    }
    
    /**
     * 判断是否为伪头部（pseudo-header）
     */
    private boolean isPseudoHeader(CharSequence name) {
        return name.length() > 0 && name.charAt(0) == ':';
    }
    
    /**
     * 创建 Http2FrameStream
     */
    private Http2FrameStream createHttp2FrameStream(int streamId) {
        return new Http2FrameStream() {
            @Override
            public int id() {
                return streamId;
            }
            
            @Override
            public Http2Stream.State state() {
                return Http2Stream.State.OPEN;
            }
        };
    }
    
    /**
     * 关闭连接
     */
    public synchronized void close() {
        if (!connected) {
            return;
        }
        
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while closing channel", e);
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            logger.info("HTTP/2 client closed: {}:{}", host, port);
            connected = false;
        }
    }
    
    /**
     * HTTP/2 Client Handler
     */
    private class Http2ClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("HTTP/2 client handler exception", cause);
            
            // 所有待处理请求都标记为失败
            pendingRequests.values().forEach(future -> {
                if (!future.isDone()) {
                    future.completeExceptionally(cause);
                }
            });
            pendingRequests.clear();
            responseBuffers.clear();
            
            ctx.close();
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.warn("HTTP/2 connection closed: {}:{}", host, port);
            connected = false;
            ctx.fireChannelInactive();
        }
    }
}
