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
import org.wso2.carbon.identity.agent.onprem.userstore.constant.CommonConstants;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManagerBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for user groups.
 */
@Path("/groups")
public class GroupResource {
    private static Logger log = LoggerFactory.getLogger(GroupResource.class);

    /**
     * @param limit - maximum number of the role names that should be returned.
     * @return - list of the role names in the userstore.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRoleNames(@QueryParam("limit") String limit) {
        try {
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
            if (limit == null || limit.isEmpty()) {
                limit = String.valueOf(CommonConstants.MAX_USER_LIST);
            }
            String[] usernames = userStoreManager.doGetRoleNames("*", Integer.parseInt(limit));
            JSONObject returnObject = new JSONObject();
            JSONArray usernameArray = new JSONArray(usernames);
            returnObject.put("roles", usernameArray);
            return Response.status(Response.Status.OK).entity(returnObject.toString()).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
