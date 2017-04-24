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
package org.wso2.carbon.identity.agent.outbound.server.dao;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.model.DatabaseConfig;
import org.wso2.carbon.identity.agent.outbound.server.util.DatabaseUtil;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConfigUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.AccessToken;
import org.wso2.carbon.identity.user.store.common.model.AgentConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * Token data access object
 */
public class AgentMgtDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentMgtDao.class);

    public boolean updateConnection(int accessTokenId, String node, String serverNode, String status) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        boolean result = true;
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            prepStmt = dbConnection.prepareStatement("UPDATE UM_AGENT_CONNECTIONS SET UM_STATUS=?,UM_SERVER_NODE=? " +
                    "WHERE UM_ACCESS_TOKEN_ID=? AND UM_NODE=?");
            prepStmt.setString(1, status);
            prepStmt.setString(2, serverNode);
            prepStmt.setInt(3, accessTokenId);
            prepStmt.setString(4, node);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String errorMessage = "Error occurred while updating connection";
            LOGGER.error(errorMessage, e);
            result = false;
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, prepStmt);
        }
        return result;
    }

    public boolean closeAllConnection(String serverNode) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        boolean result = true;
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            prepStmt = dbConnection
                    .prepareStatement("UPDATE UM_AGENT_CONNECTIONS SET UM_STATUS=? WHERE UM_SERVER_NODE=?");
            prepStmt.setString(1, UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTION_FAILED);
            prepStmt.setString(2, serverNode);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String errorMessage = "Error occurred while updating connection";
            LOGGER.error(errorMessage, e);
            result = false;
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, prepStmt);
        }
        return result;
    }

    public boolean isNodeConnected(int accessTokenId, String node) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        boolean isValid = false;
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            prepStmt = dbConnection.prepareStatement("SELECT UM_ID FROM UM_AGENT_CONNECTIONS WHERE " +
                    "UM_ACCESS_TOKEN_ID = ? AND UM_NODE = ? AND UM_STATUS = ?");
            prepStmt.setInt(1, accessTokenId);
            prepStmt.setString(2, node);
            prepStmt.setString(3, UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                isValid = true;
            }
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting reading data";
            LOGGER.error(errorMessage, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, resultSet, prepStmt);
        }
        return isValid;
    }

    public boolean isConnectionExist(int accessTokenId, String node) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        boolean isExist = false;
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            prepStmt = dbConnection.prepareStatement("SELECT UM_ID FROM UM_AGENT_CONNECTIONS WHERE " +
                    "UM_ACCESS_TOKEN_ID = ? AND UM_NODE = ?");
            prepStmt.setInt(1, accessTokenId);
            prepStmt.setString(2, node);
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                isExist = true;
            }
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting connection data";
            LOGGER.error(errorMessage, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, resultSet, prepStmt);
        }
        return isExist;
    }

    public boolean addAgentConnection(AgentConnection connection) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        boolean result = true;
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            prepStmt = dbConnection.prepareStatement("INSERT INTO UM_AGENT_CONNECTIONS(UM_ACCESS_TOKEN_ID,UM_NODE," +
                    "UM_STATUS,UM_SERVER_NODE)VALUES(?,?,?,?)");
            prepStmt.setInt(1, connection.getAccessTokenId());
            prepStmt.setString(2, connection.getNode());
            prepStmt.setString(3, connection.getStatus());
            prepStmt.setString(4, connection.getServerNode());
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting reading data";
            LOGGER.error(errorMessage, e);
            result = false;
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, prepStmt);
        }
        return result;
    }

    public List<AgentConnection> getAgentConnections(String tenantDomain, String domain, String status)  {
        Connection dbConnection = null;
        PreparedStatement insertTokenPrepStmt = null;
        ResultSet resultSet = null;
        List<AgentConnection> agentConnections = new ArrayList<>();
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            insertTokenPrepStmt = dbConnection.prepareStatement("SELECT UM_STATUS,UM_NODE,UM_ACCESS_TOKEN_ID," +
                    "UM_SERVER_NODE FROM UM_AGENT_CONNECTIONS WHERE UM_STATUS =? AND UM_ACCESS_TOKEN_ID IN " +
                    "(SELECT A.UM_ID FROM UM_ACCESS_TOKEN A WHERE A.UM_DOMAIN=? AND A.UM_TENANT=?);");
            insertTokenPrepStmt.setString(1, status);
            insertTokenPrepStmt.setString(2, domain);
            insertTokenPrepStmt.setString(3, tenantDomain);
            resultSet = insertTokenPrepStmt.executeQuery();
            while (resultSet.next()) {
                AgentConnection agentConnection = new AgentConnection();
                agentConnection.setStatus(resultSet.getString("UM_STATUS"));
                agentConnection.setNode(resultSet.getString("UM_NODE"));
                agentConnection.setAccessTokenId(resultSet.getInt("UM_ACCESS_TOKEN_ID"));
                agentConnection.setServerNode(resultSet.getString("UM_SERVER_NODE"));
                agentConnections.add(agentConnection);
            }
        } catch (SQLException e) {
            LOGGER.error("Error occurred while reading agent information", e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, resultSet, insertTokenPrepStmt);
        }
        return agentConnections;
    }


}
