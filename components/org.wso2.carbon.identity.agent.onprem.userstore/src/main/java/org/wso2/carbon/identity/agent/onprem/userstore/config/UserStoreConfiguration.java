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
package org.wso2.carbon.identity.agent.onprem.userstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;

import java.util.Map;

/**
 *  Singleton to get the user store properties from userstore-mgt.xml.
 */
public class UserStoreConfiguration {
    private static Logger log = LoggerFactory.getLogger(UserStoreConfiguration.class);
    private static Map<String, String> userStoreProperties;
    private static UserStoreConfiguration instance = new UserStoreConfiguration();

    private UserStoreConfiguration() {
        try {
            init();
        } catch (UserStoreException e) {
            log.error("Error in configuring the UserStoreManager: " + e.getMessage());
        }
    }

    /**
     *  Initializes the User Store properties.
     */
    private void init() throws UserStoreException {
        UserStoreConfigurationXMLProcessor userStoreConfigurationXMLProcessor
                = new UserStoreConfigurationXMLProcessor();
        userStoreProperties = userStoreConfigurationXMLProcessor.buildUserStoreConfigurationFromFile();
    }

    /**
     * @return instance of UserStoreConfiguration with properties initialized.
     */
    public static UserStoreConfiguration getConfiguration() {
        return instance;
    }

    /**
     * @return the map of userStoreProperties from userstore-mgt.xml.
     */
    public Map<String, String> getUserStoreProperties() {
        return userStoreProperties;
    }
}
