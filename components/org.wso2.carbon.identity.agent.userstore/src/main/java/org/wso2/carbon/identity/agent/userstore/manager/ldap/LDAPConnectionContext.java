/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.identity.agent.userstore.manager.ldap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.agent.userstore.constant.LDAPConstants;
import org.wso2.carbon.identity.agent.userstore.exception.UserStoreException;

import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**
 *  Connection for LDAP user stores.
 */
class LDAPConnectionContext {

    private static Log log = LogFactory.getLog(LDAPConnectionContext.class);
    @SuppressWarnings("rawtypes")
    private Hashtable<String, String> environment;
    private static final String CONNECTION_TIME_OUT = "LDAPConnectionTimeout";
    private static final String READ_TIME_OUT = "ReadTimeout";

    @SuppressWarnings({ "rawtypes", "unchecked" }) LDAPConnectionContext(Map<String, String> userStoreProperties)
            throws UserStoreException {

        String connectionURL = userStoreProperties.get(LDAPConstants.CONNECTION_URL);
        String connectionName = userStoreProperties.get(LDAPConstants.CONNECTION_NAME);
        String connectionPassword = userStoreProperties.get(LDAPConstants.CONNECTION_PASSWORD);

        if (log.isDebugEnabled()) {
            log.debug("Connection Name :: " + connectionName + ", Connection URL :: " + connectionURL);
        }

        environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");

        if (connectionName != null) {
            environment.put(Context.SECURITY_PRINCIPAL, connectionName);
        }

        if (connectionPassword != null) {
            environment.put(Context.SECURITY_CREDENTIALS, connectionPassword);
        }

        if (connectionURL != null) {
            environment.put(Context.PROVIDER_URL, connectionURL);
        }

        // Enable connection pooling if property is set in user-mgt.xml
        boolean isLDAPConnectionPoolingEnabled = false;
        String value = userStoreProperties.get(LDAPConstants.CONNECTION_POOLING_ENABLED);

        if (value != null && !value.trim().isEmpty()) {
            isLDAPConnectionPoolingEnabled = Boolean.parseBoolean(value);
        }

        environment.put("com.sun.jndi.ldap.connect.pool", isLDAPConnectionPoolingEnabled ? "true" : "false");

        // set referral status if provided in configuration.
        if (userStoreProperties.get(LDAPConstants.PROPERTY_REFERRAL) != null) {
            environment.put("java.naming.referral",
                    userStoreProperties.get(LDAPConstants.PROPERTY_REFERRAL));
        }
        //Set connect timeout if provided in configuration. Otherwise set default value
        String connectTimeout = userStoreProperties.get(CONNECTION_TIME_OUT);
        String readTimeout = userStoreProperties.get(READ_TIME_OUT);
        if (connectTimeout != null && !connectTimeout.trim().isEmpty()) {
            environment.put("com.sun.jndi.ldap.connect.timeout", connectTimeout);
        } else {
            environment.put("com.sun.jndi.ldap.connect.timeout", "5000");
        }

        if (StringUtils.isNotEmpty(readTimeout)) {
            environment.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        }
    }

    /**
     * @return Connection context of the LDAP userstore.
     * @throws UserStoreException If an error occurs while connecting to th userstore.
     */
    DirContext getContext() throws UserStoreException {
        DirContext context;
        try {
            context = new InitialDirContext(environment);

        } catch (NamingException e) {
            log.error("Error obtaining connection. " + e.getMessage(), e);
            log.error("Trying again to get connection.");

            try {
                context = new InitialDirContext(environment);
            } catch (Exception e1) {
                log.error("Error obtaining connection for the second time" + e.getMessage(), e);
                throw new UserStoreException("Error obtaining connection. " + e.getMessage(), e);
            }

        }
        return (context);
    }

    /**
     * @param userDN Distinguished name of the user to be authenticated
     * @param password Password of the user to be authenticated
     * @return The LDAP connection context with logged in as the given user.
     * @throws javax.naming.NamingException If the user cannot be authenticated or connection issue occurs.
     */
    LdapContext getContextWithCredentials(String userDN, String password)
            throws NamingException {
        LdapContext context;

        //create a temp env for this particular authentication session by copying the original env
        Hashtable<String, String> tempEnv = new Hashtable<>();
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            tempEnv.put(entry.getKey(), entry.getValue());
        }
        //replace connection name and password with the passed credentials to this method
        tempEnv.put(Context.SECURITY_PRINCIPAL, userDN);
        tempEnv.put(Context.SECURITY_CREDENTIALS, password);

        //replace environment properties with these credentials
        context = new InitialLdapContext(tempEnv, null);
        return (context);
    }

}
