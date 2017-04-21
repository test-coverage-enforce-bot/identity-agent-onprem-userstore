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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.UserOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.websocket.Session;

/**
 * Server session handler
 */
public class ServerHandler {

    //    private Map<String, Session> sessions = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);
    private Map<String, Integer> counter = new HashMap<>();
    private Map<String, List<Session>> sessions = new HashMap<>();

    private String getKey(String tenantDomain, String userstoreDomain) {
        return userstoreDomain + tenantDomain;
    }

    public void addSession(String tenantDomain, String userstoreDomain, Session session) {
        if (sessions.containsKey(getKey(tenantDomain, userstoreDomain))) {
            List<Session> tenantSessions = sessions.get(getKey(tenantDomain, userstoreDomain));
            tenantSessions.add(session);
            sessions.put(getKey(tenantDomain, userstoreDomain), tenantSessions);
        } else {
            List<Session> tenantSessions = new ArrayList<>();
            tenantSessions.add(session);
            sessions.put(getKey(tenantDomain, userstoreDomain), tenantSessions);
        }
        LOGGER.info("############# addSession sessions : " + sessions);
    }

    /**
     * Get client session as round robin to send message
     * @param userstoreDomain
     * @return
     */
    public Session getSession(String tenantDomain, String userstoreDomain) {
        LOGGER.info("############# getSessions sessions : " + sessions);
        if (counter.containsKey(getKey(tenantDomain, userstoreDomain))) {
            int lastcounter = counter.get(getKey(tenantDomain, userstoreDomain));
            int index = 0;
            if (lastcounter < (sessions.get(getKey(tenantDomain, userstoreDomain)).size() - 1)) {
                index = ++lastcounter;
            }
            counter.put(getKey(tenantDomain, userstoreDomain), index);
            return sessions.get(getKey(tenantDomain, userstoreDomain)).get(index);
        } else {
            counter.put(getKey(tenantDomain, userstoreDomain), 0);
            return sessions.get(getKey(tenantDomain, userstoreDomain)).get(0);
        }
    }

    public void removeSession(String tenantDomain, String userstoreDomain, Session session) {

        Iterator<Session> iterator = sessions.get(getKey(tenantDomain, userstoreDomain)).iterator();
        while (iterator.hasNext()) {
            Session tmpSession = iterator.next();

            if (tmpSession.getId().equals(session.getId())) {
                sessions.get(getKey(tenantDomain, userstoreDomain)).remove(tmpSession);
                break;
            }
        }
    }

    //TODO add this method to utility class
    private String convertToJson(UserOperation userOperation) {

        return String
                .format("{correlationId : '%s', requestType : '%s', requestData : %s}",
                        userOperation.getCorrelationId(),
                        userOperation.getRequestType(), userOperation.getRequestData());
    }

    public void removeSessions(String tenantDomain, String userstoreDomain) throws IOException {
        List<Session> sessionList = sessions.get(getKey(tenantDomain, userstoreDomain));
        for (Session session : sessionList) {
            UserOperation userOperation = new UserOperation();
            userOperation.setRequestType(UserStoreConstants.UM_OPERATION_TYPE_ERROR);
            userOperation.setRequestData("Clossing client connections.");
            session.getBasicRemote().sendText(convertToJson(userOperation));
        }
        sessions.remove(getKey(tenantDomain, userstoreDomain));
        counter.remove(getKey(tenantDomain, userstoreDomain));
    }
}
