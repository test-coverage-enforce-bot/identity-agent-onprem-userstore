/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.identity.agent.onprem.userstore.manager.common;

import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;

import java.util.Map;


/**
 * Base userstore manager.
 */
public interface UserStoreManager {

    /**
     * @param userName - username of the user
     * @param propertyNames - array of required attributes' names
     * @return - Map containing the name value pairs of required attributes
     * @throws UserStoreException - if an error occurs while retrieving data.
     */
    Map<String, String> getUserPropertyValues(String userName, String[] propertyNames) throws UserStoreException;

    /**
     * @param userName - username of the user
     * @param credential password of the user
     * @return - true if the users credentials are valid
     * - false otherwise.
     * @throws UserStoreException - if an error occurs while retrieving data.
     */
    boolean doAuthenticate(String userName, Object credential) throws UserStoreException;

    /**
     * @param filter - username filter String.
     * @param maxItemLimit - maximum size of the username list.
     * @return the list of usernames.
     * @throws UserStoreException - if an error occurs while retrieving data.
     */
    String[] doListUsers(String filter, int maxItemLimit) throws UserStoreException;

    /**
     * @param filter - group filter string
     * @param maxItemLimit - maximum size of the return group list
     * @return - the array of all the group names
     * @throws UserStoreException - if an error occurs while retrieving data.
     */
    String[] doGetRoleNames(String filter, int maxItemLimit) throws UserStoreException;

    /**
     * @param userName - username of the user whose role list is required.
     * @return - the array of roles of the given user.
     * @throws UserStoreException - if an error occurs while retrieving data.
     */
    String[] doGetExternalRoleListOfUser(String userName) throws UserStoreException;

    /**
     * @return - true if the connection to the userstore is healthy.
     * -false otherwise.
     */
    boolean getConnectionStatus();

    void setUserStoreProperties(Map<String, String> userStoreProperties) throws UserStoreException;
}
