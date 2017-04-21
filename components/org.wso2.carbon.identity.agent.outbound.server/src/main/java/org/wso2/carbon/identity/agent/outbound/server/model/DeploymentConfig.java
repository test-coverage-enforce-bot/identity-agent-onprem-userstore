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
package org.wso2.carbon.identity.agent.outbound.server.model;

/**
 * Deployment configurations
 */
public class DeploymentConfig {

    private MessageBrokerConfig messagebroker;
    private DatabaseConfig database;
    private ServerConfig server;

    public MessageBrokerConfig getMessagebroker() {
        return messagebroker;
    }

    public void setMessagebroker(MessageBrokerConfig messagebroker) {
        this.messagebroker = messagebroker;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }
}
