/*
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.agent.outbound.server;

import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.identity.user.store.common.MessageRequestUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.UserOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.websocket.Session;

/**
 * Server session handler
 */
public class SessionHandler {

    private Map<String, List<Session>> sessions = new HashMap<>();

    /**
     * Get key for session map.
     * @param tenantDomain Tenant domain
     * @param userstoreDomain User store domain
     * @return key
     */
    private String getKey(String tenantDomain, String userstoreDomain) {
        return userstoreDomain + "@" + tenantDomain;
    }

    /**
     * Add session into cache.
     * @param tenantDomain Tenant domain
     * @param userstoreDomain User store domain
     * @param session websocket session
     */
    public void addSession(String tenantDomain, String userstoreDomain, Session session) {
        List<Session> tenantSessions = sessions.get(getKey(tenantDomain, userstoreDomain));

        if (tenantSessions == null) {
            tenantSessions = new ArrayList<>();
            sessions.put(getKey(tenantDomain, userstoreDomain), tenantSessions);
        }
        tenantSessions.add(session);
    }

    /**
     * Get client session as round robin to send message.
     * @param userstoreDomain User store domain
     * @return websocket session
     */
    public Session getSession(String tenantDomain, String userstoreDomain) {

        String key = getKey(tenantDomain, userstoreDomain);
        List<Session> sessionList = sessions.get(key);

        if (sessionList != null && !sessionList.isEmpty()) {
            int noofSessions = sessionList.size();
            Random random = new Random();
            int randomIndex = Math.abs(random.nextInt()) % noofSessions;
            return sessionList.get(randomIndex);
        }
        return null;
    }

    /**
     * Remove session from cache.
     * @param tenantDomain Tenant domain
     * @param userstoreDomain User store domain
     * @param session websocket session
     */
    public void removeSession(String tenantDomain, String userstoreDomain, Session session) {

        if (sessions.get(getKey(tenantDomain, userstoreDomain)) != null) {
            Iterator<Session> iterator = sessions.get(getKey(tenantDomain, userstoreDomain)).iterator();
            while (iterator.hasNext()) {
                Session tmpSession = iterator.next();
                if (tmpSession.getId().equals(session.getId())) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * Remove websocket session from cache and kill sessions.
     * @param tenantDomain Tenant domain
     * @param userstoreDomain User store domain
     * @throws IOException
     */
    public void removeAndKillSessions(String tenantDomain, String userstoreDomain) throws IOException, JSONException {
        List<Session> sessionList = sessions.get(getKey(tenantDomain, userstoreDomain));
        if (sessionList != null) {
            for (Session session : sessionList) {
                UserOperation userOperation = new UserOperation();
                userOperation.setRequestType(UserStoreConstants.UM_OPERATION_TYPE_ERROR);
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("message", "Closing client connection from server.");
                userOperation.setRequestData(jsonMessage.toString());
                session.getBasicRemote().sendText(MessageRequestUtil.getUserOperationJSONMessage(userOperation));
            }
            sessions.remove(getKey(tenantDomain, userstoreDomain));
        }
    }
}
