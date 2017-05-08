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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

/**
 * Listener that listens to the problem in the jms connection.
 */
public class JMSExceptionListener implements ExceptionListener {

    private static final Logger logger = LoggerFactory.getLogger(JMSExceptionListener.class);
    private static final int RETRY_TIMEOUT = 4000;

    private JMSMessageReceiver receiver;

    public JMSExceptionListener(JMSMessageReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void onException(JMSException exception) {
        logger.error("Error in the JMS connection. ", exception);
        while (!receiver.startReceive()) {
            try {
                Thread.sleep(RETRY_TIMEOUT);
            } catch (InterruptedException e) {
                logger.error("Error while wait for reconnect listener.", e);
            }
        }
    }
}
