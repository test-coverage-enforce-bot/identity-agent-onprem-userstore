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
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 *
 */
@Path("/users")
public class UserResource {
    private static Logger log = LoggerFactory.getLogger(UserResource.class);

    @GET
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAttributes(@PathParam("username") String username,
                                      @QueryParam("attributes") String attributes) {
        try {
            if (attributes == null || attributes.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).
                        entity("Required User Attributes are not Specified!").build();
            }
            String[] attributeArray = attributes.split(",");
            UserStoreManager ldapUserStoreManager =
                    new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            Map<String, String> propertyMap = ldapUserStoreManager.getUserPropertyValues(username, attributeArray);
            JSONObject returnObject = new JSONObject(propertyMap);
            return Response.status(Response.Status.OK).entity(returnObject.toString()).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUserNames(@QueryParam("limit") String limit) {
        try {
            if (limit == null || limit.isEmpty()) {
                limit = String.valueOf(CommonConstants.MAX_USER_LIST);
            }
            UserStoreManager ldapUserStoreManager =
                    new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            String[] usernames = ldapUserStoreManager.doListUsers("*", Integer.parseInt(limit));
            JSONObject jsonObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(usernames);
            jsonObject.put("usernames", usernameArray);
            return Response.status(Response.Status.OK).entity(jsonObject.toString()).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (NumberFormatException ex) {
            String errorMessage = "Limit Should be an integer: ";
            log.error(errorMessage + ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).
                    entity(JSONObject.stringToValue(errorMessage + ex.getMessage())).build();
        }
    }

    @GET
    @Path("{username}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserRoles(@PathParam("username") String username) {
        try {
            UserStoreManager ldapUserStoreManager =
                    new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            String[]  roles = ldapUserStoreManager.doGetExternalRoleListOfUser(username);
            JSONObject jsonObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(roles);
            jsonObject.put("groups", usernameArray);
            return Response.status(Response.Status.OK).entity(jsonObject.toString()).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
