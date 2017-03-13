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

package org.wso2.carbon.identity.agent.outbound.util;

import org.wso2.carbon.identity.agent.outbound.constant.CommonConstants;

/**
 * Contains Functionalities common to all types of UserStoreManagers.
 */
public class UserStoreUtils {

    /**
     * @param userName Username of the User
     * @param displayName Display Name if provided
     * @return Combined User Name
     */
    public static String getCombinedName(String userName, String displayName) {
    /*
     * get the name in combined format if two different values are there for userName &
     * displayName format: userName|displayName
     */
        String combinedName = null;
        if (!userName.equals(displayName) && displayName != null) {
            combinedName = userName + CommonConstants.NAME_COMBINER + displayName;
        } else {
            combinedName = userName;
        }
        return combinedName;
    }
}
