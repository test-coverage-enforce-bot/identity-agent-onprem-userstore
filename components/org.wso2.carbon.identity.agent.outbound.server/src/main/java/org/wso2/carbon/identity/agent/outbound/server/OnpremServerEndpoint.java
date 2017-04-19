/*
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.agent.outbound.server;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.dao.TokenMgtDao;
import org.wso2.carbon.identity.agent.outbound.server.messaging.JMSConnectionException;
import org.wso2.carbon.identity.agent.outbound.server.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.agent.outbound.server.messaging.MessageBrokerConfigUtil;
import org.wso2.carbon.identity.agent.outbound.server.model.AgentConnection;
import org.wso2.carbon.identity.agent.outbound.server.model.MessageBrokerConfig;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConstants;
import org.wso2.carbon.identity.user.store.outbound.model.AccessToken;
import org.wso2.carbon.identity.user.store.outbound.model.UserOperation;

import java.io.IOException;
import java.util.ArrayList;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * Server endpoint
 */
@ServerEndpoint(value = "/server/{token}/{node}")
public class OnpremServerEndpoint {

    private static final Logger log = LoggerFactory.getLogger(OnpremServerEndpoint.class);
    private static final String QUEUE_NAME_RESPONSE = "responseQueue";
    private static final long QUEUE_MESSAGE_LIFETIME = 5 * 60 * 1000;
    private static final ArrayList<String> NODES_LIST = new ArrayList();
    private ServerHandler serverHandler;
    private String serverNode;

    public OnpremServerEndpoint(ServerHandler serverHandler, String serverNode) {
        this.serverHandler = serverHandler;
        this.serverNode = serverNode;
        initNodeList();
        initializeConnections();
        log.info("############## OnPremise managed server started. : " + serverNode);
    }

    private void initializeConnections() {
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        tokenMgtDao.closeAllConnection(serverNode);
    }

    //TODO improve this
    private void initNodeList() {
        NODES_LIST.add("1");
        NODES_LIST.add("2");
    }

    //TODO consider concurrency
    private void addSession(String tenant, Session session) {
        serverHandler.addSession(tenant, session);
    }

    private void removeSession(String tenant, Session session) {
        serverHandler.removeSession(tenant, session);
    }

    //TODO consider concurrency
    private Session getSession(String tenant) {
        return serverHandler.getSession(tenant);
    }

    public void processOperation(UserOperation userOperation) {
        Thread loop = new Thread(new Runnable() {

            public void run() {
                try {
                    getSession(userOperation.getTenant()).getBasicRemote()
                            .sendText(convertToJson(userOperation));
                } catch (IOException ex) {
                    log.error("Error occurred while sending messaging to client", ex);
                }
            }
        });
        loop.start();
    }

    private String convertToJson(UserOperation userOperation) {

        return String
                .format("{correlationId : '%s', requestType : '%s', requestData : %s}",
                        userOperation.getCorrelationId(),
                        userOperation.getRequestType(), userOperation.getRequestData());
    }

    private void processResponse(String tenant, String message) {

        JMSConnectionFactory connectionFactory = new JMSConnectionFactory();
        Connection connection = null;
        MessageProducer producer;
        try {
            MessageBrokerConfig conf = MessageBrokerConfigUtil.build();
            connectionFactory.createActiveMQConnectionFactory(conf.getUrl());
            connection = connectionFactory.createConnection();
            connectionFactory.start(connection);
            javax.jms.Session session = connectionFactory.createSession(connection);
            Destination responseQueue = connectionFactory.createQueueDestination(session, QUEUE_NAME_RESPONSE);
            producer = connectionFactory.createMessageProducer(session, responseQueue, DeliveryMode.NON_PERSISTENT);

            JSONObject resultObj = new JSONObject(message);
            String responseData = (String) resultObj.get("responseData");
            String correlationId = (String) resultObj.get("correlationId");

            UserOperation requestOperation = new UserOperation();
            requestOperation.setCorrelationId(correlationId);
            requestOperation.setResponseData(responseData);

            ObjectMessage requestMessage = session.createObjectMessage();
            requestMessage.setObject(requestOperation);
            requestMessage.setJMSExpiration(QUEUE_MESSAGE_LIFETIME);
            requestMessage.setJMSCorrelationID(correlationId);
            producer.send(requestMessage);

        } catch (JMSException e) {
            log.error("Error occurred while sending message", e);
        } catch (JSONException e) {
            log.error("Error occurred while reading json payload", e);
        } catch (JMSConnectionException e) {
            log.error("Error occurred while sending message", e);
        } finally {
            try {
                connectionFactory.closeConnection(connection);
            } catch (JMSConnectionException e) {
                log.error("Error occurred while closing JMS connection", e);
            }
        }
    }

    private AccessToken validateAccessToken(String accessToken) {
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        return tokenMgtDao.validateAccessToken(accessToken);
    }

    @OnOpen
    public void onOpen(@PathParam("token") String token, @PathParam("node") String node, Session session) {

        log.info("########### Client connected token : " + token + " Node : " + node); //TODO remove log
        AccessToken accessToken = validateAccessToken(token);
        log.info("########### Client connected token accessToken : " + accessToken); //TODO remove log
        if (accessToken == null) {
            try {
                String message = "Closing session due to send invalid access token.";
                log.error(message);
                sendErrorMessage(session, message);
            } catch (IOException e) {
                log.error("Error occurred while closing session.");
            }
        } else if (!isValidNode(node)) {
            try {
                String message = "Closing session due to invalid node.";
                log.error(message);
                sendErrorMessage(session, message);
            } catch (IOException e) {
                log.error("Error occurred while closing session.");
            }
        } else if (isNodeConnected(accessToken, node)) {
            try {
                String message = "Node " + node + " already connected";
                log.error(message);
                sendErrorMessage(session, message);
            } catch (IOException e) {
                log.error("Error occurred while closing session.");
            }
        } else {
            log.info("############### serverNode :  " + serverNode);
            addConnection(accessToken, node);
            addSession(accessToken.getTenant(), session);
            String msg = accessToken.getTenant() + " connected to server";
            log.info(msg);
        }
    }

    private void addConnection(AccessToken accessToken, String node) {
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        if (tokenMgtDao.isConnectionExist(accessToken, node)) {
            tokenMgtDao.updateConnection(accessToken.getAccessToken(), node, serverNode,
                    ServerConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
        } else {
            log.info("############### addConnection serverNode :  " + serverNode);
            AgentConnection connection = new AgentConnection();
            connection.setAccessToken(accessToken.getAccessToken());
            connection.setStatus(ServerConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
            connection.setNode(node);
            connection.setServerNode(serverNode);
            tokenMgtDao.addAgentConnection(connection);
        }
    }

    private boolean isNodeConnected(AccessToken accessToken, String node) {
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        return tokenMgtDao.isNodeConnected(accessToken, node);
    }

    private boolean isValidNode(String node) {
        return NODES_LIST.contains(node);
    }

    private void sendErrorMessage(Session session, String message) throws IOException {
        UserOperation userOperation = new UserOperation();
        userOperation.setRequestType("error");
        userOperation.setRequestData(message);
        session.getBasicRemote().sendText(convertToJson(userOperation));
        session.close();
    }

    @OnMessage
    public void onTextMessage(@PathParam("token") String token, String text, Session session) throws IOException {
        Thread loop = new Thread(new Runnable() {

            public void run() {
                processResponse(token, text);
            }
        });
        loop.start();
    }

    @OnMessage
    public void onBinaryMessage(byte[] bytes, Session session) {
        log.info("Reading binary Message");
        log.info(bytes.toString());
    }

    @OnClose
    public void onClose(@PathParam("token") String token, @PathParam("node") String node, CloseReason closeReason,
            Session session) {
        log.info("Connection is closed with status code : " + closeReason.getCloseCode().getCode()
                + " On reason " + closeReason.getReasonPhrase());
        AccessToken accessToken = validateAccessToken(token);
        log.info("########## onClose 1");
        removeSession(accessToken.getTenant(), session);
        log.info("########## onClose 2");
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        log.info("########## onClose 3");
        tokenMgtDao
                .updateConnection(token, node, serverNode, ServerConstants.CLIENT_CONNECTION_STATUS_CONNECTION_FAILED);
    }

    @OnError
    public void onError(Throwable throwable, Session session) {
        log.error("Error found in method : " + throwable.toString());
    }

}
