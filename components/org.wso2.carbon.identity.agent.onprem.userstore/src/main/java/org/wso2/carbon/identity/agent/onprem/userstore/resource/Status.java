/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.agent.onprem.userstore.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManagerBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *  Connection health check endpoint.
 *  This will be available at https://localhost:8888/status
 */
@Api(value = "status")
@SwaggerDefinition(
        info = @Info(
                title = "Status Endpoint Swagger Definition", version = "1.0",
                description = "The endpoint which is used to check the user store connection health.",
                license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0")
        )
)
@Path("/status")
public class Status {
    private static Logger log = LoggerFactory.getLogger(UserResource.class);

    /**
     * @return 200 OK if the connection is healthy,
     * 500 INTERNAL SERVER ERROR otherwise.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Return HTTP 200 if the conection is healthy. ",
            notes = "Returns HTTP 500 if couldn't conect to user store.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "No message"),
            @ApiResponse(code = 500, message = "No message")})
    public Response checkConnectionStatus() {
        try {
            UserStoreManager userStoreManager = UserStoreManagerBuilder.getUserStoreManager();
            if (!userStoreManager.getConnectionStatus()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Response.Status.OK).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
