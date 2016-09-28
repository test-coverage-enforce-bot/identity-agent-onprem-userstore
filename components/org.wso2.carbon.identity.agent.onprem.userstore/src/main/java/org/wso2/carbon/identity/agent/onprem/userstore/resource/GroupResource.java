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

package org.wso2.carbon.identity.agent.onprem.userstore.resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.config.UserStoreConfiguration;
import org.wso2.carbon.identity.agent.onprem.userstore.constant.CommonConstants;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.ldap.LDAPUserStoreManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/groups")
public class GroupResource {
    private static Logger log = LoggerFactory.getLogger(GroupResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllRoleNames(@QueryParam("limit") String limit){
        try {
            UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            if(limit==null ||limit.isEmpty()){
                limit = String.valueOf(CommonConstants.MAX_USER_LIST);
            }
            String[] usernames = ldapUserStoreManager.doGetRoleNames("*", Integer.parseInt(limit));
            JSONObject jsonObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(usernames);
            jsonObject.put("roles", usernameArray);
            return jsonObject.toString();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return "";
        }
    }
}
