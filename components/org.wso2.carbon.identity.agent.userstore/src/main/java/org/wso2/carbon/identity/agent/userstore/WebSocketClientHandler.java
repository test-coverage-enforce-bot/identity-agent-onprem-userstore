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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.userstore.constant.CommonConstants;
import org.wso2.carbon.identity.agent.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.userstore.manager.common.UserStoreManagerBuilder;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Timer;
import javax.net.ssl.SSLException;

/**
 * WebSocket Client Handler for Testing.
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private static final int SOCKET_RETRY_INTERVAL = 5000;

    private String textReceived = "";
    private ByteBuffer bufferReceived = null;
    private WebSocketClient client;
    private HeatBeatTask heatBeatTask;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, WebSocketClient client) {
        this.handshaker = handshaker;
        this.client = client;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
        scheduleHeatBeatSendTask(ctx.channel());
    }

    /**
     * Schedule a tast to send an ping message in every 30 seconds, otherwise connection get lost.
     * @param channel
     */
    private void scheduleHeatBeatSendTask(Channel channel) {
        Timer time = new Timer();
        heatBeatTask = new HeatBeatTask(channel);
        time.schedule(heatBeatTask, 10 * 1000, 10 * 1000);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("Server connection Client disconnected!");
        if (!WebSocketClient.isRetryStarted) {
            startRetrying();
        }
    }

    private void startRetrying() {
        WebSocketClient.isRetryStarted = true;
        while (true) {
            boolean result = false;
            try {
                Thread.sleep(SOCKET_RETRY_INTERVAL);
                LOGGER.info("Trying to reconnect the server...");
                result = client.handhshake();
            } catch (InterruptedException e) {
                LOGGER.error("Error occurred while reconnecting to socket server", e);
            } catch (URISyntaxException e) {
                LOGGER.error("Error occurred while reconnecting to socket server", e);
            } catch (SSLException e) {
                LOGGER.error("Error occurred while reconnecting to socket server", e);
            }
            if (result) {
                WebSocketClient.isRetryStarted = false;
                LOGGER.info("Agent successfully reconnected to server.");
                break;
            }
        }
    }

    /**
     * Write response to server socket with correlationId
     * @param channel
     * @param correlationId
     * @param result
     */
    private void writeResponse(Channel channel, String correlationId, String result) {
        channel.writeAndFlush(new TextWebSocketFrame(
                String.format("{correlationId : '%s', responseData: '%s'}", correlationId, result)));
    }

    /**
     * Process authentication request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processAuthenticationRequest(Channel channel, JSONObject requestObj) throws UserStoreException {

        JSONObject requestData = requestObj.getJSONObject(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA);
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting to authenticate user " + requestData
                    .getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME));
        }

        boolean isAuthenticated = userStoreManager.doAuthenticate(
                requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME),
                requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_PASSWORD));
        String authenticationResult = UserAgentConstants.UM_OPERATION_AUTHENTICATE_RESULT_FAIL;

        LOGGER.info("Authenticating user " + requestData
                .getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME) + " result "
                + isAuthenticated);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Authenticating user " + requestData
                    .getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME) + " result "
                    + isAuthenticated);
        }

        if (isAuthenticated) {
            authenticationResult = UserAgentConstants.UM_OPERATION_AUTHENTICATE_RESULT_SUCCESS;
        }
        writeResponse(channel, (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_CORRELATION_ID),
                authenticationResult);
    }

    /**
     * Process Get claims request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processGetClaimsRequest(Channel channel, JSONObject requestObj) throws UserStoreException {

        JSONObject requestData = requestObj.getJSONObject(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA);
        String username = requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME);
        String attributes = requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_ATTRIBUTES);
        String[] attributeArray = attributes.split(CommonConstants.ATTRIBUTE_LIST_SEPERATOR);
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();

        Map<String, String> propertyMap = userStoreManager.getUserPropertyValues(username, attributeArray);
        JSONObject returnObject = new JSONObject(propertyMap);

        writeResponse(channel, (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_CORRELATION_ID),
                returnObject.toString());
    }

    /**
     * Process get user roles request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processGetUserRolesRequest(Channel channel, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA);
        String username = requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME);

        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
        String[] roles = userStoreManager.doGetExternalRoleListOfUser(username);
        JSONObject jsonObject = new JSONObject();
        JSONArray usernameArray = new JSONArray(roles);
        jsonObject.put("groups", usernameArray);

        writeResponse(channel, (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_CORRELATION_ID),
                jsonObject.toString());
    }

    /**
     * Process get roles request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processGetRolesRequest(Channel channel, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA);
        String limit = requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_GET_ROLE_LIMIT);

        if (limit == null || limit.isEmpty()) {
            limit = String.valueOf(CommonConstants.MAX_USER_LIST);
        }
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
        String[] roleNames = userStoreManager.doGetRoleNames("*", Integer.parseInt(limit));
        JSONObject returnObject = new JSONObject();
        JSONArray usernameArray = new JSONArray(roleNames);
        returnObject.put("groups", usernameArray);

        writeResponse(channel, (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_CORRELATION_ID),
                returnObject.toString());
    }

    /**
     * Process get roles request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processGetUsersListRequest(Channel channel, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA);
        String limit = requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_GET_USER_LIMIT);
        String filter = requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_GET_USER_FILTER);

        if (limit == null || limit.isEmpty()) {
            limit = String.valueOf(CommonConstants.MAX_USER_LIST);
        }
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
        String[] roleNames = userStoreManager.doListUsers(filter, Integer.parseInt(limit));
        JSONObject returnObject = new JSONObject();
        JSONArray usernameArray = new JSONArray(roleNames);
        returnObject.put("usernames", usernameArray);

        writeResponse(channel, (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_CORRELATION_ID),
                returnObject.toString());
    }

    /**
     * Process user operation request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processUserOperationRequest(Channel channel, JSONObject requestObj) throws UserStoreException {

        String type = (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_TYPE);
        LOGGER.info("Message receive for operation " + type);
        switch (type) {
        case UserStoreConstants.UM_OPERATION_TYPE_AUTHENTICATE:
            processAuthenticationRequest(channel, requestObj);
            break;
        case UserStoreConstants.UM_OPERATION_TYPE_GET_CLAIMS:
            processGetClaimsRequest(channel, requestObj);
            break;
        case UserStoreConstants.UM_OPERATION_TYPE_GET_USER_ROLES:
            processGetUserRolesRequest(channel, requestObj);
            break;
        case UserStoreConstants.UM_OPERATION_TYPE_GET_ROLES:
            processGetRolesRequest(channel, requestObj);
            break;
        case UserStoreConstants.UM_OPERATION_TYPE_GET_USER_LIST:
            processGetUsersListRequest(channel, requestObj);
            break;
        case UserStoreConstants.UM_OPERATION_TYPE_ERROR:
            logError(requestObj);
            client.setShutdownFlag(true);
            System.exit(0);
            break;
        default:
            LOGGER.error("Invalid user operation request type : " + type + " received.");
            break;
        }
    }

    private void logError(JSONObject requestObj) {
        String message = (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA);
        LOGGER.error(message);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            JSONObject requestObj = new JSONObject(textFrame.text());
            textReceived = textFrame.text();
            processUserOperationRequest(ch, requestObj);
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
            bufferReceived = binaryFrame.content().nioBuffer();
            LOGGER.info("WebSocket Client received  binary message: " + bufferReceived.toString());
        } else if (frame instanceof PongWebSocketFrame) {
            LOGGER.info("WebSocket Client received pong");
            PongWebSocketFrame pongFrame = (PongWebSocketFrame) frame;
            bufferReceived = pongFrame.content().nioBuffer();
        } else if (frame instanceof CloseWebSocketFrame) {
            LOGGER.info("WebSocket Client received closing");
            ch.close();
        }
    }

    /**
     * @return the text received from the server.
     */
    public String getTextReceived() {
        return textReceived;
    }

    /**
     * @return the binary data received from the server.
     */
    public ByteBuffer getBufferReceived() {
        return bufferReceived;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!handshakeFuture.isDone()) {
            LOGGER.error("Handshake failed : " + cause.getMessage(), cause);
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}

