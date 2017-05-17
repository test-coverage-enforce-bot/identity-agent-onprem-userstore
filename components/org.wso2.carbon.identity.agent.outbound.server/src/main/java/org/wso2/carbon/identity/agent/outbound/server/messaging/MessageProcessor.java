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
package org.wso2.carbon.identity.agent.outbound.server.messaging;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.SessionHandler;
import org.wso2.carbon.identity.user.store.common.MessageRequestUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.ServerOperation;
import org.wso2.carbon.identity.user.store.common.model.UserOperation;

import java.io.IOException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.websocket.Session;

/**
 * Message process worker
 */
public class MessageProcessor implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSMessageReceiver.class);
    private Message message;
    private SessionHandler serverHandler;

    public MessageProcessor(Message message, SessionHandler serverHandler) {
        this.message = message;
        this.serverHandler = serverHandler;
    }

    @Override
    public void run() {
        try {
            processOperation(message);
        } catch (JMSException e) {
            LOGGER.error("Error occurred while processing message", e);
        }
    }

    public void processOperation(Message message) throws JMSException {
        if (((ObjectMessage) message).getObject() instanceof UserOperation) {
            UserOperation userOperation = (UserOperation) ((ObjectMessage) message).getObject();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Message received for user operation : " + userOperation.getRequestType()
                        + " correlation Id : "
                        + userOperation.getCorrelationId());
            }
            processUserOperation(userOperation);
        } else if (((ObjectMessage) message).getObject() instanceof ServerOperation) {
            ServerOperation serverOperation = (ServerOperation) ((ObjectMessage) message).getObject();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Message received for server operation : " + serverOperation.getOperationType());
            }
            processServerOperation(serverOperation);
        }
    }

    /**
     * Process user operation in a separate thread.
     * @param serverOperation Server operation message
     */
    public void processServerOperation(ServerOperation serverOperation) {
        if (serverOperation.getOperationType().equals(UserStoreConstants.SERVER_OPERATION_TYPE_KILL_AGENTS)) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Performing server operation: " + UserStoreConstants.SERVER_OPERATION_TYPE_KILL_AGENTS
                            + " for tenant: " + serverOperation.getTenantDomain());
                }
                serverHandler.removeAndKillSessions(serverOperation.getTenantDomain(), serverOperation.getDomain());
            } catch (IOException | JSONException e) {
                LOGGER.error(
                        "Error occurred while performing server operation: " + serverOperation.getOperationType()
                                + " for tenant: " + serverOperation.getTenantDomain(), e);
            }
        }
    }

    /**
     * Process User operation in a separate thread
     * @param userOperation User operation message
     */
    public void processUserOperation(UserOperation userOperation) {

        try {
            Session session = serverHandler.getSession(userOperation.getTenant(), userOperation.getDomain());
            if (session != null) {
                //TODO Check getBasicRemove is thread safe
                session.getBasicRemote().sendText(MessageRequestUtil.getUserOperationJSONMessage(userOperation));
            }
        } catch (IOException ex) {
            LOGGER.error("Error occurred while sending messaging to client. correlationId: " + userOperation
                    .getCorrelationId() + " tenant: " + userOperation.getTenant() + " type: " + userOperation
                    .getRequestType(), ex);
        }
    }

}
