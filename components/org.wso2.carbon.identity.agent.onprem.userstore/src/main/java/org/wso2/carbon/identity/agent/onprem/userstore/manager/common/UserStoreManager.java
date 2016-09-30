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


/*
 * base userstore manager
 */
public interface UserStoreManager {

    Map<String, String> getUserPropertyValues(String userName, String[] propertyNames) throws UserStoreException;

    boolean doAuthenticate(String userName, Object credential) throws UserStoreException;

    String[] doListUsers(String filter, int maxItemLimit) throws UserStoreException;

    String[] doGetRoleNames(String filter, int maxItemLimit) throws UserStoreException;

    String[] doGetExternalRoleListOfUser(String userName) throws UserStoreException;

    boolean getConnectionStatus();
}
