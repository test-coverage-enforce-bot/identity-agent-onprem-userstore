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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.config.UserStoreConfiguration;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.carbon.identity.agent.onprem.userstore.manager.ldap.LDAPUserStoreManager;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/status")
public class Status {
    private static Logger log = LoggerFactory.getLogger(UserResource.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAttributes() {
        Map<String , Boolean> returnMap = new HashMap<>();
        try {
            UserStoreManager ldapUserStoreManager =
                    new LDAPUserStoreManager(UserStoreConfiguration.getConfiguration().getUserStoreProperties());
            ldapUserStoreManager.getConnectionStatus();
            returnMap.put("active", true);
            return Response.status(Response.Status.OK).entity(new JSONObject(returnMap).toString()).build();
        } catch (UserStoreException e) {
            log.error(e.getMessage());
            returnMap.put("active", false);
            return Response.status(Response.Status.OK).entity(new JSONObject(returnMap).toString()).build();
        }
    }

}
