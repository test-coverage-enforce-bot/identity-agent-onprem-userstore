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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.userstore.config.AgentConfigUtil;
import org.wso2.carbon.identity.agent.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.userstore.manager.common.UserStoreManagerBuilder;
import org.wso2.carbon.identity.agent.userstore.security.SecretManagerInitializer;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.net.ssl.SSLException;

/**
 * org.wso2.carbon.identity.agent.outbound.Application entry point.
 *
 * @since 0.1
 */

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);
    private Thread shutdownHook;

    public static void main(String[] args)
            throws InterruptedException, SSLException, URISyntaxException, UnknownHostException {

        Application application = new Application();
        application.startAgent();
    }

    private void startAgent() throws InterruptedException, SSLException, URISyntaxException, UnknownHostException {
        new SecretManagerInitializer().init();
        try {
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
            boolean connectionStatus = userStoreManager.getConnectionStatus();
            if (!connectionStatus) {
                LOGGER.error("Cannot connect to user store, Please check the user store configurations.");
                System.exit(0);
            }
        } catch (UserStoreException e) {
            LOGGER.error("Cannot connect to user store, Please check the user store configurations.");
            System.exit(0);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Access token: ");
        String accessToken = scanner.next();
        String hostname = InetAddress.getLocalHost().getHostName();
        WebSocketClient echoClient = new WebSocketClient(
                AgentConfigUtil.build().getServerUrl() + accessToken + "/" + hostname);
        echoClient.handhshake();
        Application app = new Application();
        app.addShutdownHook(echoClient);
    }

    private void addShutdownHook(WebSocketClient echoClient) {
        if (shutdownHook != null) {
            return;
        }
        shutdownHook = new Thread() {

            public void run() {
                shutdownGracefully(echoClient);
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void shutdownGracefully(WebSocketClient echoClient) {
        try {
            echoClient.shutDown();
        } catch (InterruptedException e) {
            LOGGER.error("Error occurred while sending shutdown signal.");
        }
        LOGGER.info("shutdownGracefully Shutdown hook triggered....");
    }
}
