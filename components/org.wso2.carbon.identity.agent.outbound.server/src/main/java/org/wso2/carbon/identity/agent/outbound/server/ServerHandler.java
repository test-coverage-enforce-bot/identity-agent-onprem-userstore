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

    public void addSession(String tenant, Session session) {
        if (sessions.containsKey(tenant)) {
            List<Session> tenantSessions = sessions.get(tenant);
            tenantSessions.add(session);
            sessions.put(tenant, tenantSessions);
        } else {
            List<Session> tenantSessions = new ArrayList<>();
            tenantSessions.add(session);
            sessions.put(tenant, tenantSessions);
        }
        LOGGER.info("############# addSession sessions : " + sessions);
    }

    /**
     * Get client session as round robin to send message
     * @param tenant
     * @return
     */
    public Session getSession(String tenant) {
        LOGGER.info("############# getSessions sessions : " + sessions);
        if (counter.containsKey(tenant)) {
            int lastcounter = counter.get(tenant);
            int index = 0;
            if (lastcounter < (sessions.get(tenant).size() - 1)) {
                index = ++lastcounter;
            }
            counter.put(tenant, index);
            return sessions.get(tenant).get(index);
        } else {
            counter.put(tenant, 0);
            return sessions.get(tenant).get(0);
        }
    }

    public void removeSession(String tenant, Session session) {

        Iterator<Session> iterator = sessions.get(tenant).iterator();
        while (iterator.hasNext()) {
            Session tmpSession = iterator.next();

            if (tmpSession.getId().equals(session.getId())) {
                sessions.get(tenant).remove(tmpSession);
                break;
            }
        }
    }
}
