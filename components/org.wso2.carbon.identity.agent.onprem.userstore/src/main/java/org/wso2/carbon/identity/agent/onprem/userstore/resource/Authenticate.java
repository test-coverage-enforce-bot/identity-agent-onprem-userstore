/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.agent.onprem.userstore.resource;

import org.json.JSONObject;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.config.UserStoreConfiguration;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.ldap.LDAPUserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.model.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/authenticate")
public class Authenticate {
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public String authenticate(User user) {
            Boolean isAuthenticated = false;
            Map<String , Boolean> returnMap = new HashMap<>();
            try {
                UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
                isAuthenticated = ldapUserStoreManager.doAuthenticate(user.getUsername(),user.getPassword());
            } catch (UserStoreException e) {
                e.printStackTrace();
            }
            returnMap.put("authenticated", isAuthenticated);
            return new JSONObject(returnMap).toString();
        }
}
