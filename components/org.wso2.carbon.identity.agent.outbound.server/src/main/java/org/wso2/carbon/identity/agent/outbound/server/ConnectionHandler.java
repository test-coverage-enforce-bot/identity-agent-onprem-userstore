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

import org.wso2.carbon.identity.agent.outbound.server.dao.AgentMgtDao;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConfigurationBuilder;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.AccessToken;
import org.wso2.carbon.identity.user.store.common.model.AgentConnection;

import java.util.List;

/**
 * Agent connection handler
 */
public class ConnectionHandler {

    /**
     * Add connection
     * @param accessToken access token
     * @param node Client node
     * @param serverNode Server node
     */
    public void addConnection(AccessToken accessToken, String node, String serverNode) {
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

    /**
     * Check weather node is already connected
     * @param accessToken Access token
     * @param node Client node
     * @return result
     */
    public boolean isNodeConnected(AccessToken accessToken, String node) {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        return agentMgtDao.isNodeConnected(accessToken.getId(), node);
    }

    /**
     * Get server ip connected with the given node.
     *
     * @param accessToken Access token
     * @param node Node host name
     * @returnConnected Server Hostname / IP
     */
    public String getConnectedServer(AccessToken accessToken, String node) {

        AgentMgtDao agentMgtDao = new AgentMgtDao();
        return agentMgtDao.getConnectedServer(accessToken.getId(), node);

    }

    /**
     * Check is connection limit exceed
     * @param tenantDomain Tenant domain
     * @param domain User store domain
     * @return result
     */
    public boolean isConnectionLimitExceed(String tenantDomain, String domain) {
        AgentMgtDao agentMgtDao = new AgentMgtDao();
        List<AgentConnection> agentConnections = agentMgtDao
                .getAgentConnections(tenantDomain, domain, UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
        if (agentConnections.size() >= ServerConfigurationBuilder.build().getServer().getConnectionlimit()) {
            return true;
        }
        return false;
    }

}
