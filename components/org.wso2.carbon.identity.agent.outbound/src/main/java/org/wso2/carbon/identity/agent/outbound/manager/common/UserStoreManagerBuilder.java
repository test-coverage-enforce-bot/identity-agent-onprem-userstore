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

package org.wso2.carbon.identity.agent.outbound.manager.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.config.UserStoreConfiguration;
import org.wso2.carbon.identity.agent.outbound.constant.XMLConfigurationConstants;
import org.wso2.carbon.identity.agent.outbound.exception.UserStoreException;

import java.util.Map;

/**
 *  Creates an instance of the Relevant UserStoreManager.
 */
public class UserStoreManagerBuilder {
    private static Logger log = LoggerFactory.getLogger(UserStoreManagerBuilder.class);

    /**
     * @return An instance of the UserStoreManager mentioned in the userstore-mgt.xml file.
     * @throws UserStoreException If an error occurs while loading the class or instantiating it.
     */
    public static UserStoreManager getUserStoreManager() throws UserStoreException {
        Map<String, String> userStoreProperties = UserStoreConfiguration.getConfiguration().getUserStoreProperties();
        try {
            Class managerClass = UserStoreManagerBuilder.class.getClassLoader().
                    loadClass(userStoreProperties.get(XMLConfigurationConstants.LOCAL_NAME_CLASS));
            UserStoreManager userStoreManager = (UserStoreManager) managerClass.newInstance();
            userStoreManager.setUserStoreProperties(userStoreProperties);
            return userStoreManager;
        } catch (ClassNotFoundException e) {
            String message = "Error while loading the UserStoreManager";
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
            throw new UserStoreException(message, e);
        } catch (InstantiationException e) {
            String message = "Error instantiating the UserStoreManager because, " +
                    "Class represents an abstract class, an interface, an array class, " +
                    "a primitive type, or void; or the class has no nullary constructor; " +
                    "or for some other reason.";
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
            throw new UserStoreException(message, e);
        } catch (IllegalAccessException e) {
            String message = userStoreProperties.get(XMLConfigurationConstants.LOCAL_NAME_CLASS) +
                    " Class or its nullary constructor is not accessible";
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
            throw new UserStoreException(message, e);
        }
    }
}
