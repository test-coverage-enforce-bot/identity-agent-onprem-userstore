/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.agent.userstore;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLException;

/**
 * WebSocket client class for test
 */
public class WebSocketClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);
    private static boolean isRetryStarted = false;

    private final String url;
    private boolean shutdownFlag;
    private Channel channel = null;
    private WebSocketClientHandler handler;
    private EventLoopGroup group;
    private String accessToken;
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public WebSocketClient(String url, String accessToken) {
        this.url = System.getProperty("url", url);
        this.accessToken = accessToken;
    }

    public static boolean isRetryStarted() {
        return isRetryStarted;
    }

    public static void setIsRetryStarted(boolean isRetryStartedFlag) {
        isRetryStarted = isRetryStartedFlag;
    }

    /**
     * @return true if the handshake is done properly.
     * @throws java.net.URISyntaxException throws if there is an error in the URI syntax.
     * @throws InterruptedException throws if the connecting the server is interrupted.
     */
    public boolean handhshake() throws InterruptedException, URISyntaxException, SSLException {
        boolean isDone;
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            LOGGER.error("Only WS(S) is supported.");
            return false;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        group = new NioEventLoopGroup();

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
        try {
            handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null,
                                    true, new DefaultHttpHeaders().add(AUTHORIZATION_HEADER, "Bearer " + accessToken)),
                            this);

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            channel = b.connect(uri.getHost(), port).sync().channel();
            isDone = handler.handshakeFuture().sync().isSuccess();
        } catch (Exception ex) {
            LOGGER.error("Fail to connect Identity Cloud.", ex);
            isDone = false;
        }
        LOGGER.info("Connecting to server... Please wait.");
        Thread.sleep(5000);
        return isDone;
    }


    /**
     * @return the text received from the server.
     */
    public String getTextReceived() {
        return handler.getTextReceived();
    }

    /**
     * @return the binary data received from the server.
     */
    public ByteBuffer getBufferReceived() {
        return handler.getBufferReceived();
    }

    /**
     * Shutdown the WebSocke Client.
     */
    public void shutDown() throws InterruptedException {
        /**
         * Checking shutdown flag, when server close the connection channel.closeFuture().sync() hang and need to
         * check shutdownFlag is false
         */
        if (!shutdownFlag) {
            channel.writeAndFlush(new CloseWebSocketFrame());
            channel.closeFuture().sync();
            group.shutdownGracefully();
        }
    }

    public void setShutdownFlag(boolean shutdownFlag) {
        this.shutdownFlag = shutdownFlag;
    }

}
