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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.server.util.DatabaseUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.AccessToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Token data access object
 */
public class TokenMgtDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenMgtDao.class);

    public AccessToken validateAccessToken(String accessToken) {
        Connection dbConnection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        try {
            dbConnection = DatabaseUtil.getDBConnection();
            prepStmt = dbConnection.prepareStatement(
                    "SELECT UM_ID,UM_TOKEN,UM_TENANT,UM_DOMAIN FROM UM_ACCESS_TOKEN WHERE UM_TOKEN = ? AND " +
                            "UM_STATUS = ?");
            prepStmt.setString(1, accessToken);
            prepStmt.setString(2, UserStoreConstants.ACCESS_TOKEN_STATUS_ACTIVE);
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                AccessToken token = new AccessToken();
                token.setAccessToken(resultSet.getString("UM_TOKEN"));
                token.setId(resultSet.getInt("UM_ID"));
                token.setTenant(resultSet.getString("UM_TENANT"));
                token.setDomain(resultSet.getString("UM_DOMAIN"));
                return token;
            }
        } catch (SQLException e) {
            String errorMessage = "Error occurred while getting reading data";
            LOGGER.error(errorMessage, e);
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, resultSet, prepStmt);
        }
        return null;
    }




}
