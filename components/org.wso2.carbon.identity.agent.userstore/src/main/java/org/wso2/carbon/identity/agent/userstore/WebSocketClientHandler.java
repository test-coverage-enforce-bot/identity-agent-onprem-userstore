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

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.net.ssl.SSLException;

/**
 * WebSocket Client Handler for Testing.
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private static final int SOCKET_RETRY_INTERVAL = 3000;

    private String textReceived = "";
    private ByteBuffer bufferReceived = null;
    private WebSocketClient client;

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
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("Socket Client disconnected!");

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
        boolean isAuthenticated = userStoreManager.doAuthenticate(
                requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_NAME),
                requestData.getString(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_USER_PASSWORD));
        String authenticationResult = UserAgentConstants.UM_OPERATION_AUTHENTICATE_RESULT_FAIL;
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
     * Process user operation request
     * @param channel
     * @param requestObj
     * @throws UserStoreException
     */
    private void processUserOperationRequest(Channel channel, JSONObject requestObj) throws UserStoreException {

        String type = (String) requestObj.get(UserAgentConstants.UM_JSON_ELEMENT_REQUEST_DATA_TYPE);

        switch (type) {
        case UserAgentConstants.UM_OPERATION_TYPE_AUTHENTICATE:
            processAuthenticationRequest(channel, requestObj);
            break;
        case UserAgentConstants.UM_OPERATION_TYPE_GET_CLAIMS:
            processGetClaimsRequest(channel, requestObj);
            break;
        case UserAgentConstants.UM_OPERATION_TYPE_GET_USER_ROLES:
            processGetUserRolesRequest(channel, requestObj);
            break;
        case UserAgentConstants.UM_OPERATION_TYPE_GET_ROLES:
            processGetRolesRequest(channel, requestObj);
            break;
        case UserAgentConstants.UM_OPERATION_TYPE_ERROR:
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
            LOGGER.info("WebSocket Client connected!");
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
            LOGGER.info("Message received : " + textReceived);
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

