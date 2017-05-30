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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.SessionHandler;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConfigurationBuilder;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Topic;

/**
 * JMS Message receiver
 */
public class JMSMessageReceiver implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JMSMessageReceiver.class);
    private SessionHandler serverHandler;
    private boolean transacted = false;
    private ThreadPoolExecutor executor;

    public JMSMessageReceiver(SessionHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    /**
     * Start JMS listening in a separate thread.
     */
    public void start() {
        executor = (ThreadPoolExecutor) Executors
                .newFixedThreadPool(ServerConfigurationBuilder.build().getServer().getMaxthreadpoolsize());
        Thread loop = new Thread(this::startReceive);
        loop.start();
    }

    /**
     * Start receiving message from request topic.
     * @return result of the operation
     */
    public boolean startReceive() {
        boolean started = true;
        String messageBrokerURL = ServerConfigurationBuilder.build().getMessagebroker().getUrl();
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerURL);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            javax.jms.Session session = connection.createSession(transacted, javax.jms.Session.AUTO_ACKNOWLEDGE);
            Topic requestTopic = session.createTopic(UserStoreConstants.TOPIC_NAME_REQUEST);
            MessageConsumer requestConsumer = session.createConsumer(requestTopic);
            requestConsumer.setMessageListener(this);
            JMSExceptionListener exceptionListener = new JMSExceptionListener(this);
            connection.setExceptionListener(exceptionListener);
            LOGGER.info("Message listener successfully started.");
        } catch (JMSException e) {
            LOGGER.error(
                    "Error occurred while start listening message from topic: " + UserStoreConstants.TOPIC_NAME_REQUEST,
                    e);
            started = false;
        }
        return started;
    }

    @Override
    public void onMessage(Message message) {
        addMessageToThreadPool(message);
    }

    private void addMessageToThreadPool(Message message) {
        MessageProcessor messageProcessor = new MessageProcessor(message, serverHandler);
        executor.execute(messageProcessor);
    }
}
