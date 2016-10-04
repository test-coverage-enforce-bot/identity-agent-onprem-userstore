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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.constant.CommonConstants;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManagerBuilder;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *  Users REST endpoint.
 *  This will be available at https://localhost:8888/users
 */
@Api(value = "users")
@SwaggerDefinition(
        info = @Info(
                title = "Users Endpoint Swagger Definition", version = "1.0",
                description = "The endpoint which is used to manage users.",
                license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0")
        )
)
@Path("/users")
public class UserResource {
    private static Logger log = LoggerFactory.getLogger(UserResource.class);

    /**
     * @param username - username of the user whose attributes are required.
     * @param attributes - required attribute list separated by commas, as a QueryParam.
     * @return - Map with the requested attribute names mapped to their values.
     */
    @GET
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Return the requested attributes of the user. ",
            notes = "Returns HTTP 500 if an internal error occurs at the server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "{att1name:att1val, att2name:att2val, ...}"),
            @ApiResponse(code = 500, message = "Particular exception message")})
    public Response getUserAttributes(@ApiParam(value = "Username", required = true)
                                          @PathParam("username") String username,
                                      @ApiParam(value = "User Attributes", required = true)
                                      @QueryParam("attributes") String attributes) {
        try {
            if (attributes == null || attributes.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).
                        entity("Required User Attributes are not Specified!").build();
            }
            String[] attributeArray = attributes.split(CommonConstants.ATTRIBUTE_LIST_SEPERATOR);
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();

            Map<String, String> propertyMap = userStoreManager.getUserPropertyValues(username, attributeArray);
            JSONObject returnObject = new JSONObject(propertyMap);
            return Response.status(Response.Status.OK).entity(returnObject.toString()).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * @param limit - maximum number of usernames required. Deafult value will be taken if not specified.
     * @return - the list of usernames up to the given limit.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Return the usernames in the user store up to the limit. ",
            notes = "Returns HTTP 500 if an internal error occurs at the server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "{usernames:[username1, username2, ...]}"),
            @ApiResponse(code = 500, message = "Particular exception message")})
    public Response getAllUserNames(@ApiParam(value = "Limit", required = false) @QueryParam("limit") String limit) {
        try {
            if (limit == null || limit.isEmpty()) {
                limit = String.valueOf(CommonConstants.MAX_USER_LIST);
            }
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();

            String[] usernames = userStoreManager.
                    doListUsers(CommonConstants.WILD_CARD_FILTER, Integer.parseInt(limit));
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

    /**
     * @param username - username of the user whose role names are required.
     * @return - the list of role names of the given user.
     */
    @GET
    @Path("{username}/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Return the role names of the given user. ",
            notes = "Returns HTTP 500 if an internal error occurs at the server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "{groups:[group1, group2, ...]}"),
            @ApiResponse(code = 500, message = "Particular exception message")})
    public Response getUserRoles(@ApiParam(value = "Username", required = true)
                                     @PathParam("username") String username) {
        try {
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
            String[]  roles = userStoreManager.doGetExternalRoleListOfUser(username);
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
