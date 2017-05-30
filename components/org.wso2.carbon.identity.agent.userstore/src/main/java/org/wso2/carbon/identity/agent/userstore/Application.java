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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.userstore.config.AgentConfigUtil;
import org.wso2.carbon.identity.agent.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.userstore.manager.common.UserStoreManagerBuilder;
import org.wso2.carbon.identity.agent.userstore.security.AccessTokenHandler;
import org.wso2.carbon.identity.agent.userstore.security.SecretManagerInitializer;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/**
 * Application main class which initialize the socket connection with server
 *
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClient.class);
    private Thread shutdownHook;

    public static void main(String[] args)
            throws InterruptedException, SSLException, URISyntaxException, UnknownHostException {

        Application application = new Application();
        application.startAgent();
    }

    /**
     * Start agent which initialize security manager, check user store config and start socket connection with server
     * @throws UnknownHostException
     */
    private void startAgent() throws UnknownHostException {

        String accessToken = new AccessTokenHandler().getAccessToken();
        if (StringUtils.isEmpty(accessToken)) {
            LOGGER.error("Please enter valid access token.");
            System.exit(0);
        }
        new SecretManagerInitializer().init();
        try {
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
            LOGGER.info("Verifying user store.....");
            boolean connectionStatus = userStoreManager.getConnectionStatus();
            if (!connectionStatus) {
                LOGGER.error("User store verification failed. Please check the user store configurations in file conf/"
                        + UserAgentConstants.USERSTORE_CONFIG_FILE);
                System.exit(0);
            }
            LOGGER.info("User store verification success.");
        } catch (UserStoreException e) {
            LOGGER.error("User store verification failed. Please check the user store configurations in file conf/"
                    + UserAgentConstants.USERSTORE_CONFIG_FILE, e);
            System.exit(0);
        }

        String hostname = InetAddress.getLocalHost().getHostName();
        WebSocketClient webSocketClient = new WebSocketClient(
                AgentConfigUtil.build().getServerUrl() + "/" + hostname, accessToken);
        try {
            boolean result = webSocketClient.handhshake();
            if (result) {
                LOGGER.info("Agent successfully connected to Identity Cloud.");
            } else {
                LOGGER.info("Failed to connect Identity Cloud.");
                System.exit(0);
            }
        } catch (InterruptedException | URISyntaxException | SSLException e) {
            LOGGER.error("Error occurred while connecting to server.", e);
        }

        Application app = new Application();
        app.addShutdownHook(webSocketClient);
    }

    /**
     * Add shutdown hook
     * @param webSocketClient Websocket client
     */
    private void addShutdownHook(WebSocketClient webSocketClient) {
        if (shutdownHook != null) {
            return;
        }
        shutdownHook = new Thread() {

            public void run() {
                shutdownGracefully(webSocketClient);
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Shutdown agent gracefully.
     * @param webSocketClient Websocket client
     */
    private void shutdownGracefully(WebSocketClient webSocketClient) {
        try {
            LOGGER.info("Shutting down agent....");
            webSocketClient.shutDown();
        } catch (InterruptedException e) {
            LOGGER.error("Error occurred while sending shutdown signal", e);
        }
        LOGGER.info("Agent shutting down completed.");
    }
}
