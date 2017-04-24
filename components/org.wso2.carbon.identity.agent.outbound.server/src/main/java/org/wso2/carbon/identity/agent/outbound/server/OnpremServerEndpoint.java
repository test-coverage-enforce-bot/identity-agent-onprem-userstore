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
import org.wso2.carbon.identity.agent.outbound.server.dao.AgentMgtDao;
import org.wso2.carbon.identity.agent.outbound.server.dao.TokenMgtDao;
import org.wso2.carbon.identity.agent.outbound.server.model.MessageBrokerConfig;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConfigUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionException;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.user.store.common.model.AccessToken;
import org.wso2.carbon.identity.user.store.common.model.AgentConnection;
import org.wso2.carbon.identity.user.store.common.model.UserOperation;

import java.io.IOException;
import java.util.List;
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
    private static final long QUEUE_MESSAGE_LIFETIME = 5 * 60 * 1000;
    private ServerHandler serverHandler;
    private String serverNode;

    public OnpremServerEndpoint(ServerHandler serverHandler, String serverNode) {
        this.serverHandler = serverHandler;
        this.serverNode = serverNode;
        initializeConnections();
    }

    private void initializeConnections() {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        agentMgtDao.closeAllConnection(serverNode);
    }

    private void addSession(String tenantDomain, String userstoreDomain, Session session) {
        serverHandler.addSession(tenantDomain, userstoreDomain, session);
    }

    private void removeSession(String tenantDomain, String userstoreDomain, Session session) {
        serverHandler.removeSession(tenantDomain, userstoreDomain, session);
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
            MessageBrokerConfig conf = ServerConfigUtil.build().getMessagebroker();
            connectionFactory.createActiveMQConnectionFactory(conf.getUrl());
            connection = connectionFactory.createConnection();
            connectionFactory.start(connection);
            javax.jms.Session session = connectionFactory.createSession(connection);
            Destination responseQueue = connectionFactory
                    .createQueueDestination(session, UserStoreConstants.QUEUE_NAME_RESPONSE);
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

    private boolean isConnectionLimitExceed(String tenantDomain, String domain) {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        List<AgentConnection> agentConnections = agentMgtDao
                .getAgentConnections(tenantDomain, domain, UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
        if (agentConnections.size() >= ServerConfigUtil.build().getServer().getConnectionlimit()) {
            return true;
        }
        return false;
    }

    @OnOpen
    public void onOpen(@PathParam("token") String token, @PathParam("node") String node, Session session) {
        handleSession(token, node, session);
    }

    private void handleSession(String token, String node, Session session) {

        AccessToken accessToken = validateAccessToken(token);
        if (accessToken == null) {
            try {
                String message = "Closing session due to send invalid access token.";
                log.error(message);
                sendErrorMessage(session, message);
            } catch (IOException e) {
                log.error("Error occurred while clossing session.");
            }
        } else if (isNodeConnected(accessToken, node)) {
            try {
                String message = "Client " + node + " already connected";
                log.error(message);
                sendErrorMessage(session, message);
            } catch (IOException e) {
                log.error("Error occurred while closing session.");
            }
        } else if (isConnectionLimitExceed(accessToken.getTenant(), accessToken.getDomain())) {
            try {
                String message = "No of agent connections limit exceeded.";
                log.error(message);
                sendErrorMessage(session, message);
            } catch (IOException e) {
                log.error("Error occurred while closing session.");
            }
        } else {
            addConnection(accessToken, node);
            addSession(accessToken.getTenant(), accessToken.getDomain(), session);
            String msg = node + " from " + accessToken.getTenant()  + " connected to server";
            log.info(msg);
        }
    }

    private void addConnection(AccessToken accessToken, String node) {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        if (agentMgtDao.isConnectionExist(accessToken.getId(), node)) {
            agentMgtDao.updateConnection(accessToken.getId(), node, serverNode,
                    UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
        } else {
            AgentConnection connection = new AgentConnection();
            connection.setAccessTokenId(accessToken.getId());
            connection.setStatus(UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
            connection.setNode(node);
            connection.setServerNode(serverNode);
            agentMgtDao.addAgentConnection(connection);
        }
    }

    private boolean isNodeConnected(AccessToken accessToken, String node) {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        return agentMgtDao.isNodeConnected(accessToken.getId(), node);
    }

    private void sendErrorMessage(Session session, String message) throws IOException {
        UserOperation userOperation = new UserOperation();
        userOperation.setRequestType(UserStoreConstants.UM_OPERATION_TYPE_ERROR);
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
        removeSession(accessToken.getTenant(), accessToken.getDomain(), session);
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        agentMgtDao.updateConnection(accessToken.getId(), node, serverNode,
                UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTION_FAILED);
    }

    @OnError
    public void onError(Throwable throwable, Session session) {
        log.error("Error found in method : " + throwable.toString());
    }

}
