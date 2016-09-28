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
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.ldap.LDAPUserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.security.SecretCallbackHandlerService;
import org.wso2.carbon.identity.agent.onprem.userstore.security.SecretManagerInitializer;
import org.wso2.carbon.identity.agent.onprem.userstore.util.UserStoreUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/users")
public class UserResource {
    private static Logger log = LoggerFactory.getLogger(UserResource.class);

    public UserResource() {
        System.setProperty("carbon.home", UserStoreUtils.getProductHomePath());
        SecretManagerInitializer secretManagerInitializer = new SecretManagerInitializer();
        SecretCallbackHandlerService secretCallbackHandlerService = secretManagerInitializer.init();
    }

    @GET
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserAttributes(@PathParam("username") String username, @QueryParam("attributes") String attributes){
        try {
            String[] attributeArray = attributes.split(",");
            UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            Map<String,String> propertyMap = ldapUserStoreManager.getUserPropertyValues(username,attributeArray);
            JSONObject returnObject = new JSONObject(propertyMap);
            return returnObject.toString();
        } catch (Exception e) {
            log.error(e.getMessage());//TODo log with a message / specialize exceptions
            return ""; //TODO Error code and error message
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllUserNames(@QueryParam("limit") String limit){

        try {
            UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            String[] usernames = ldapUserStoreManager.doListUsers("*", Integer.parseInt(limit));
            JSONObject jsonObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(usernames);
            jsonObject.put("usernames", usernameArray);
            return jsonObject.toString();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return "";
        }
    }

    @GET
    @Path("{username}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserRoles(@PathParam("username") String username){
        try {
            UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            String[]  roles = ldapUserStoreManager.doGetExternalRoleListOfUser(username);
            JSONObject jsonObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(roles);
            jsonObject.put("groups", usernameArray);
            return jsonObject.toString();
        } catch (Exception e) {
            log.error(e.getMessage());//TODo log with a message / specialize exceptions
            return ""; //TODO Error code and error message
        }
    }
}