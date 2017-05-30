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

/**
 * SQL queries constants
 */
public class SQLQueries {

    public static final String QUERY_UPDATE_AGENT_CONNECTIONS = "UPDATE UM_AGENT_CONNECTIONS SET UM_STATUS=?," +
            "UM_SERVER_NODE=? WHERE UM_ACCESS_TOKEN_ID=? AND UM_NODE=?";
    public static final String QUERY_UPDATE_AGENT_CONNECTIONS_STATUS = "UPDATE UM_AGENT_CONNECTIONS SET UM_STATUS=? " +
            "WHERE UM_SERVER_NODE=?";
    public static final String QUERY_IS_AGENT_NODE_CONNECTED = "SELECT UM_ID FROM UM_AGENT_CONNECTIONS WHERE " +
            "UM_ACCESS_TOKEN_ID = ? AND UM_NODE = ? AND UM_STATUS = ?";
    public static final String QUERY_IS_AGENT_CONNECTION_EXIST = "SELECT UM_ID FROM UM_AGENT_CONNECTIONS WHERE " +
            "UM_ACCESS_TOKEN_ID = ? AND UM_NODE = ?";
    public static final String QUERY_ADD_CONNECTION = "INSERT INTO UM_AGENT_CONNECTIONS(UM_ACCESS_TOKEN_ID,UM_NODE," +
            "UM_STATUS,UM_SERVER_NODE)VALUES(?,?,?,?)";
    public static final String QUERY_GET_ALL_CONNECTIONS = "SELECT UM_STATUS,UM_NODE,UM_ACCESS_TOKEN_ID," +
            "UM_SERVER_NODE FROM UM_AGENT_CONNECTIONS WHERE UM_STATUS =? AND UM_ACCESS_TOKEN_ID IN " +
            "(SELECT A.UM_ID FROM UM_ACCESS_TOKEN A WHERE A.UM_DOMAIN=? AND A.UM_TENANT=?);";
    public static final String QUERY_GET_ACCESS_TOKEN = "SELECT UM_ID,UM_TOKEN,UM_TENANT,UM_DOMAIN,UM_STATUS FROM " +
            "UM_ACCESS_TOKEN WHERE UM_TOKEN = ?";
}
