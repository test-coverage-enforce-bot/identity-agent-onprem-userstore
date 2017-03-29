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
    private final static int SOCKET_RETRY_INTERVAL = 2000; //Two seconds

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
                LOGGER.info("Trying to reconnect the server...");
                result = client.handhshake();
                Thread.sleep(SOCKET_RETRY_INTERVAL);
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

    private void writeResponse(Channel ch, String correlationId, String result) {
        ch.writeAndFlush(new TextWebSocketFrame(
                String.format("{correlationId : '%s', responseData: '%s'}", correlationId, result)));
    }

    private void processAuthenticationRequest(Channel ch, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject("requestData");
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
        boolean isAuthenticated = userStoreManager.doAuthenticate(requestData.getString("username"),
                requestData.getString("password"));
        String authenticationResult = OperationConstants.UM_OPERATION_AUTHENTICATE_RESULT_FAIL;
        if (isAuthenticated) {
            authenticationResult = OperationConstants.UM_OPERATION_AUTHENTICATE_RESULT_SUCCESS;
        }
        writeResponse(ch, (String) requestObj.get("correlationId"), authenticationResult);
    }

    private void processGetClaimsRequest(Channel ch, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject("requestData");
        String username = requestData.getString("username");
        String attributes = requestData.getString("attributes");
        String[] attributeArray = attributes.split(CommonConstants.ATTRIBUTE_LIST_SEPERATOR);
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();

        Map<String, String> propertyMap = userStoreManager.getUserPropertyValues(username, attributeArray);
        JSONObject returnObject = new JSONObject(propertyMap);

        writeResponse(ch, (String) requestObj.get("correlationId"), returnObject.toString());
    }

    private void processGetUserRolesRequest(Channel ch, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject("requestData");
        String username = requestData.getString("username");

        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
        String[] roles = userStoreManager.doGetExternalRoleListOfUser(username);
        JSONObject jsonObject = new JSONObject();
        JSONArray usernameArray = new JSONArray(roles);
        jsonObject.put("groups", usernameArray);

        writeResponse(ch, (String) requestObj.get("correlationId"), jsonObject.toString());
    }

    private void processGetRolesRequest(Channel ch, JSONObject requestObj) throws UserStoreException {
        JSONObject requestData = requestObj.getJSONObject("requestData");
        String limit = requestData.getString("limit");

        if (limit == null || limit.isEmpty()) {
            limit = String.valueOf(CommonConstants.MAX_USER_LIST);
        }
        UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
        String[] roleNames = userStoreManager.doGetRoleNames("*", Integer.parseInt(limit));
        JSONObject returnObject = new JSONObject();
        JSONArray usernameArray = new JSONArray(roleNames);
        returnObject.put("groups", usernameArray);

        writeResponse(ch, (String) requestObj.get("correlationId"), returnObject.toString());
    }

    private void processUserOperationRequest(Channel ch, JSONObject requestObj) throws UserStoreException {

        String type = (String) requestObj.get("requestType");

        switch (type) {
        case OperationConstants.UM_OPERATION_TYPE_AUTHENTICATE:
            processAuthenticationRequest(ch, requestObj);
            break;
        case OperationConstants.UM_OPERATION_TYPE_GET_CLAIMS:
            processGetClaimsRequest(ch, requestObj);
            break;
        case OperationConstants.UM_OPERATION_TYPE_GET_USER_ROLES:
            processGetUserRolesRequest(ch, requestObj);
            break;
        case OperationConstants.UM_OPERATION_TYPE_GET_ROLES:
            processGetRolesRequest(ch, requestObj);
            break;
        default:
            LOGGER.error("Invalid user operation request type : " + type + " received.");
            break;
        }
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

            LOGGER.info("WebSocket Client received text message: " + textFrame.text());
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

