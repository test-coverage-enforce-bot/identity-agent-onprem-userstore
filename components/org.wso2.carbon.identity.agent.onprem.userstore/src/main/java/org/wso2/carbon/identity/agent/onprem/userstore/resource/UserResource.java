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
import org.wso2.carbon.identity.agent.onprem.userstore.config.UserStoreConfiguration;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.ldap.LDAPUserStoreManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/users")
public class UserResource {

    @GET
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public String getUserAttributes(@PathParam("username") String username, @QueryParam("attributes") String attributes){
        try {
            UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            String[] attributeArray = attributes.split(",");
            Map<String,String> propertyMap = ldapUserStoreManager.getUserPropertyValues(username,attributeArray);
            JSONObject returnObject = new JSONObject(propertyMap);
            return returnObject.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String getAllUserNames(@QueryParam("limit") String limit){

        try {
            UserStoreManager ldapUserStoreManager = new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            String[] usernames = ldapUserStoreManager.doListUsers("*", Integer.parseInt(limit));
            JSONObject jsonObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(usernames);
            jsonObject.put("usernames", usernameArray);
            return jsonObject.toString();
        } catch (UserStoreException e) {
            e.printStackTrace();
            return "";
        }
    }
}