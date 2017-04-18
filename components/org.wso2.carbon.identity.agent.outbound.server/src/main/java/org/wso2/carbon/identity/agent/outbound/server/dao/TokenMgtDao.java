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
import org.wso2.carbon.identity.agent.outbound.server.model.AgentConnection;
import org.wso2.carbon.identity.agent.outbound.server.util.DatabaseUtil;
import org.wso2.carbon.identity.agent.outbound.server.util.ServerConstants;
import org.wso2.carbon.identity.user.store.outbound.model.AccessToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Token data access object
 */
public class TokenMgtDao {

    private static final Logger log = LoggerFactory.getLogger(TokenMgtDao.class);
    protected DataSource jdbcds = null;

    public AccessToken validateAccessToken(String accessToken) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        try {
            dbConnection = getDBConnection();
            prepStmt = dbConnection.prepareStatement(
                    "SELECT UM_TOKEN, UM_TENANT FROM UM_ACCESS_TOKEN WHERE UM_TOKEN = ? AND UM_STATUS = ?");
            prepStmt.setString(1, accessToken);
            prepStmt.setString(2, ServerConstants.CLIENT_CONNECTION_STATUS_ACTIVE);
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                log.info("########### TokenMgtDao.validateAccessToken true"); //TODO remove log
                AccessToken token = new AccessToken();
                token.setAccessToken(resultSet.getString("UM_TOKEN"));
                token.setTenant(resultSet.getString("UM_TENANT"));
                log.info("########### TokenMgtDao.validateAccessToken token :" + token); //TODO remove log
                return token;
            }
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting reading data";
            log.error(errorMessage, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, resultSet, prepStmt);
        }
        return null;
    }

    public boolean updateConnection(String accessToken, String node, String serverNode, String status) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        boolean result = true;
        try {
            dbConnection = getDBConnection();
            prepStmt = dbConnection.prepareStatement("UPDATE UM_AGENT_CONNECTIONS SET UM_STATUS=?,UM_SERVER_NODE=? " +
                    "WHERE UM_ACCESS_TOKEN=? AND UM_NODE=?");
            prepStmt.setString(1, status);
            prepStmt.setString(2, serverNode);
            prepStmt.setString(3, accessToken);
            prepStmt.setString(4, node);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String errorMessage = "Error occurred while updating connection";
            log.error(errorMessage, e);
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
            dbConnection = getDBConnection();
            prepStmt = dbConnection
                    .prepareStatement("UPDATE UM_AGENT_CONNECTIONS SET UM_STATUS=? WHERE UM_SERVER_NODE=?");
            prepStmt.setString(1, ServerConstants.CLIENT_CONNECTION_STATUS_CLOSED);
            prepStmt.setString(2, serverNode);
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String errorMessage = "Error occurred while updating connection";
            log.error(errorMessage, e);
            result = false;
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, prepStmt);
        }
        return result;
    }

    public boolean isNodeConnected(AccessToken accessToken, String node) {
        java.sql.Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        boolean isValid = false;
        try {
            dbConnection = getDBConnection();
            prepStmt = dbConnection.prepareStatement("SELECT UM_ID FROM UM_AGENT_CONNECTIONS WHERE " +
                    "UM_ACCESS_TOKEN = ? AND UM_NODE = ? AND UM_STATUS = ?");
            prepStmt.setString(1, accessToken.getAccessToken());
            prepStmt.setString(2, node);
            prepStmt.setString(3, ServerConstants.CLIENT_CONNECTION_STATUS_ACTIVE);
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                isValid = true;
            }
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting reading data";
            log.error(errorMessage, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, resultSet, prepStmt);
        }
        return isValid;
    }

    public boolean isConnectionExist(AccessToken accessToken, String node) {
        java.sql.Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        boolean isExist = false;
        try {
            dbConnection = getDBConnection();
            prepStmt = dbConnection.prepareStatement("SELECT UM_ID FROM UM_AGENT_CONNECTIONS WHERE " +
                    "UM_ACCESS_TOKEN = ? AND UM_NODE = ?");
            prepStmt.setString(1, accessToken.getAccessToken());
            prepStmt.setString(2, node);
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                isExist = true;
            }
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting connection data";
            log.error(errorMessage, e);
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
            dbConnection = getDBConnection();
            prepStmt = dbConnection.prepareStatement("INSERT INTO UM_AGENT_CONNECTIONS(UM_ACCESS_TOKEN,UM_NODE," +
                    "UM_STATUS,UM_SERVER_NODE)VALUES(?,?,?,?)");
            prepStmt.setString(1, connection.getAccessToken());
            prepStmt.setString(2, connection.getNode());
            prepStmt.setString(3, connection.getStatus());
            prepStmt.setString(4, connection.getServerNode());
            prepStmt.executeUpdate();
            dbConnection.commit();
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting reading data";
            log.error(errorMessage, e);
            result = false;
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, prepStmt);
        }
        return result;
    }

    protected Connection getDBConnection() throws SQLException {
        Connection dbConnection = getJDBCDataSource().getConnection();
        dbConnection.setAutoCommit(false);
        if (dbConnection.getTransactionIsolation() != java.sql.Connection.TRANSACTION_READ_COMMITTED) {
            dbConnection.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
        }
        return dbConnection;
    }

    private DataSource getJDBCDataSource() {
        if (jdbcds == null) {
            jdbcds = loadUserStoreSpacificDataSoruce();
        }
        return jdbcds;
    }

    private DataSource loadUserStoreSpacificDataSoruce() {
        PoolProperties poolProperties = new PoolProperties();
        poolProperties.setDriverClassName("com.mysql.jdbc.Driver");
        poolProperties.setUrl("jdbc:mysql://localhost:3306/sampleuserstoredb");
        poolProperties.setUsername("root");
        poolProperties.setPassword("root");

        return new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);

    }

}
