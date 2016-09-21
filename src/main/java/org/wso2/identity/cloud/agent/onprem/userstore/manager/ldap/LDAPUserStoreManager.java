/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.identity.cloud.agent.onprem.userstore.manager.ldap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.identity.cloud.agent.onprem.userstore.constant.CommonConstants;
import org.wso2.identity.cloud.agent.onprem.userstore.constant.LDAPConstants;
import org.wso2.identity.cloud.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.identity.cloud.agent.onprem.userstore.manager.common.UserStoreManager;
import org.wso2.identity.cloud.agent.onprem.userstore.util.JNDIUtil;
import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LDAPUserStoreManager implements UserStoreManager {

    private Map<String, String> userStoreProperties = null;
    private static Log log = LogFactory.getLog(LDAPUserStoreManager.class);
    private static final int MAX_USER_CACHE = 200;
    private static final String MULTI_ATTRIBUTE_SEPARATOR = "MultiAttributeSeparator";
    private static final String PROPERTY_REFERRAL_IGNORE ="ignore";
    private static Map<String, Object> userCache = new ConcurrentHashMap<>(MAX_USER_CACHE);
    private LDAPConnectionContext connectionSource;

    public LDAPUserStoreManager(Map<String,String> userStoreProperties)
            throws UserStoreException {
        if (log.isDebugEnabled()) {
            log.debug("Started " + System.currentTimeMillis());
        }

        this.userStoreProperties = userStoreProperties;
        // check if required configurations are in the user-mgt.xml
        checkRequiredUserStoreConfigurations();
        connectionSource = null;
        this.connectionSource = new LDAPConnectionContext(this.userStoreProperties);
    }

    private void checkRequiredUserStoreConfigurations() throws UserStoreException {

        log.debug("Checking LDAP configurations ");
        String connectionURL = userStoreProperties.get(LDAPConstants.CONNECTION_URL);

        if (connectionURL == null || connectionURL.trim().length() == 0) {
            throw new UserStoreException(
                    "Required ConnectionURL property is not set at the LDAP configurations");
        }
        String connectionName = userStoreProperties.get(LDAPConstants.CONNECTION_NAME);
        if (connectionName == null || connectionName.trim().length() == 0) {
            throw new UserStoreException(
                    "Required ConnectionNme property is not set at the LDAP configurations");
        }
        String connectionPassword =
                userStoreProperties.get(LDAPConstants.CONNECTION_PASSWORD);
        if (connectionPassword == null || connectionPassword.trim().length() == 0) {
            throw new UserStoreException(
                    "Required ConnectionPassword property is not set at the LDAP configurations");
        }
        String userSearchBase = userStoreProperties.get(LDAPConstants.USER_SEARCH_BASE);
        if (userSearchBase == null || userSearchBase.trim().length() == 0) {
            throw new UserStoreException(
                    "Required UserSearchBase property is not set at the LDAP configurations");
        }
        String usernameListFilter =
                userStoreProperties.get(LDAPConstants.USER_NAME_LIST_FILTER);
        if (usernameListFilter == null || usernameListFilter.trim().length() == 0) {
            throw new UserStoreException(
                    "Required UserNameListFilter property is not set at the LDAP configurations");
        }

        String usernameSearchFilter =
                userStoreProperties.get(LDAPConstants.USER_NAME_SEARCH_FILTER);
        if (usernameSearchFilter == null || usernameSearchFilter.trim().length() == 0) {
            throw new UserStoreException(
                    "Required UserNameSearchFilter property is not set at the LDAP configurations");
        }

        String usernameAttribute =
                userStoreProperties.get(LDAPConstants.USER_NAME_ATTRIBUTE);
        if (usernameAttribute == null || usernameAttribute.trim().length() == 0) {
            throw new UserStoreException(
                    "Required UserNameAttribute property is not set at the LDAP configurations");
        }

    }

    /**
     *
     */
    public boolean doAuthenticate(String userName, Object credential) throws UserStoreException {

        boolean debug = log.isDebugEnabled();


        String failedUserDN = null;

        if (userName == null || credential == null) {
            return false;
        }

        userName = userName.trim();

        String password = (String) credential;
        password = password.trim();

        if (userName.equals("") || password.equals("")) {
            return false;
        }

        if (debug) {
            log.debug("Authenticating user " + userName);
        }

        boolean bValue = false;
        // check cached user DN first.
        String name;
        LdapName ldn = (LdapName)userCache.get(userName);
        if (ldn != null) {
            name = ldn.toString();
            try {
                if (debug) {
                    log.debug("Cache hit. Using DN " + name);
                }
                bValue = this.bindAsUser(name, (String) credential);
            } catch (NamingException e) {
                // do nothing if bind fails since we check for other DN
                // patterns as well.
                if (log.isDebugEnabled()) {
                    log.debug("Checking authentication with UserDN " + name + "failed " +
                            e.getMessage(), e);
                }
            }

            if (bValue) {
                return true;
            }
            // we need not check binding for this name again, so store this and check
            failedUserDN = name;

        }
        // read DN patterns from user-mgt.xml
        String patterns = userStoreProperties.get(LDAPConstants.USER_DN_PATTERN);

        if (patterns != null && !patterns.isEmpty()) {

            if (debug) {
                log.debug("Using UserDNPatterns " + patterns);
            }

            // if the property is present, split it using # to see if there are
            // multiple patterns specified.
            String[] userDNPatternList = patterns.split("#");
            if (userDNPatternList.length > 0) {
                for (String userDNPattern : userDNPatternList) {
                    name = MessageFormat.format(userDNPattern, escapeSpecialCharactersForDN(userName));
                    // check if the same name is found and checked from cache
                    if (failedUserDN != null && failedUserDN.equalsIgnoreCase(name)) {
                        continue;
                    }

                    if (debug) {
                        log.debug("Authenticating with " + name);
                    }
                    try {
                        bValue = this.bindAsUser(name, (String) credential);
                        if (bValue) {
                            LdapName ldapName = new LdapName(name);
                            userCache.put(userName, ldapName);
                            break;
                        }
                    } catch (NamingException e) {
                        // do nothing if bind fails since we check for other DN
                        // patterns as well.
                        if (log.isDebugEnabled()) {
                            log.debug("Checking authentication with UserDN " + userDNPattern +
                                    "failed " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
        else
        {
            name = getNameInSpaceForUserName(userName);
            try {
                if (name != null) {
                    if (debug) {
                        log.debug("Authenticating with " + name);
                    }
                    bValue = this.bindAsUser(name, (String) credential);
                    if (bValue) {
                        LdapName ldapName = new LdapName(name);
                        userCache.put(userName, ldapName);
                    }
                }
            } catch (NamingException e) {
                String errorMessage = "Cannot bind user : " + userName;
                if (log.isDebugEnabled()) {
                    log.debug(errorMessage, e);
                }
                throw new UserStoreException(errorMessage, e);
            }
        }

        return bValue;
    }


    /**
     *
     */
    public Map<String, String> getUserPropertyValues(String userName, String[] propertyNames) throws UserStoreException {

        String userAttributeSeparator = ",";
        String userDN = null;
        LdapName ldn = (LdapName)userCache.get(userName);

        if (ldn == null) {
            // read list of patterns from user-mgt.xml
            String patterns = userStoreProperties.get(LDAPConstants.USER_DN_PATTERN);

            if (patterns != null && !patterns.isEmpty()) {

                if (log.isDebugEnabled()) {
                    log.debug("Using User DN Patterns " + patterns);
                }

                if (patterns.contains("#")) {
                    userDN = getNameInSpaceForUserName(userName);
                } else {
                    userDN = MessageFormat.format(patterns, escapeSpecialCharactersForDN(userName));
                }
            }
        } else {
            userDN = ldn.toString();
        }

        Map<String, String> values = new HashMap<>();
        // if user name contains domain name, remove domain name
        String[] userNames = userName.split(CommonConstants.DOMAIN_SEPARATOR);
        if (userNames.length > 1) {
            userName = userNames[1];
        }

        DirContext dirContext = this.connectionSource.getContext();
        String userSearchFilter = userStoreProperties.get(LDAPConstants.USER_NAME_SEARCH_FILTER);
        String searchFilter = userSearchFilter.replace("?", escapeSpecialCharactersForFilter(userName));

        NamingEnumeration<?> answer = null;
        NamingEnumeration<?> attrs = null;
        try {
            if (userDN != null) {
                SearchControls searchCtls = new SearchControls();
                searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                if (propertyNames != null && propertyNames.length > 0) {
                    searchCtls.setReturningAttributes(propertyNames);
                }
                try {
                    answer = dirContext.search(escapeDNForSearch(userDN), searchFilter, searchCtls);
                } catch (PartialResultException e) {
                    // can be due to referrals in AD. so just ignore error
                    String errorMessage = "Error occurred while searching directory context for user : " + userDN + " searchFilter : " + searchFilter;
                    if (isIgnorePartialResultException()) {
                        if (log.isDebugEnabled()) {
                            log.debug(errorMessage, e);
                        }
                    } else {
                        throw new UserStoreException(errorMessage, e);
                    }
                } catch (NamingException e) {
                    String errorMessage = "Error occurred while searching directory context for user : " + userDN + " searchFilter : " + searchFilter;
                    if (log.isDebugEnabled()) {
                        log.debug(errorMessage, e);
                    }
                    throw new UserStoreException(errorMessage, e);
                }
            } else {
                answer = this.searchForUser(searchFilter, propertyNames, dirContext);
            }
            assert answer != null;
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();
                Attributes attributes = sr.getAttributes();
                if (attributes != null) {
                    assert propertyNames != null;
                    for (String name : propertyNames) {
                        if (name != null) {
                            Attribute attribute = attributes.get(name);
                            if (attribute != null) {
                                StringBuilder attrBuffer = new StringBuilder();
                                for (attrs = attribute.getAll(); attrs.hasMore(); ) {
                                    Object attObject = attrs.next();
                                    String attr = null;
                                    if (attObject instanceof String) {
                                        attr = (String) attObject;
                                    } else if (attObject instanceof byte[]) {
                                        //if the attribute type is binary base64 encoded string will be returned
                                        attr = new String(Base64.encodeBase64((byte[]) attObject));
                                    }

                                    if (attr != null && attr.trim().length() > 0) {
                                        String attrSeparator = userStoreProperties.get(MULTI_ATTRIBUTE_SEPARATOR);
                                        if (attrSeparator != null && !attrSeparator.trim().isEmpty()) {
                                            userAttributeSeparator = attrSeparator;
                                        }
                                        attrBuffer.append(attr).append(userAttributeSeparator);
                                    }
                                    String value = attrBuffer.toString();

                                /*
                                 * Length needs to be more than userAttributeSeparator.length() for a valid
                                 * attribute, since we
                                 * attach userAttributeSeparator
                                 */
                                    if (value.trim().length() > userAttributeSeparator.length()) {
                                        value = value.substring(0, value.length() - userAttributeSeparator.length());
                                        values.put(name, value);
                                    }

                                }
                            }
                        }
                    }
                }
            }

        } catch (NamingException e) {
            String errorMessage = "Error occurred while getting user property values for user : " + userName;
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new UserStoreException(errorMessage, e);
        } finally {
            // close the naming enumeration and free up resource
            JNDIUtil.closeNamingEnumeration(attrs);
            JNDIUtil.closeNamingEnumeration(answer);
            // close directory context
            JNDIUtil.closeContext(dirContext);
        }
        return values;
    }


    /**
     *
     */
    public String[] doListUsers(String filter, int maxItemLimit) throws UserStoreException {
        boolean debug = log.isDebugEnabled();
        String[] userNames = new String[0];

        if (maxItemLimit == 0) {
            return userNames;
        }

        int givenMax;
        int searchTime;

        try {
            givenMax =
                    Integer.parseInt(userStoreProperties.get(CommonConstants.PROPERTY_MAX_USER_LIST));
        } catch (Exception e) {
            givenMax = CommonConstants.MAX_USER_ROLE_LIST;
        }

        try {
            searchTime =
                    Integer.parseInt(userStoreProperties.get(CommonConstants.PROPERTY_MAX_SEARCH_TIME));
        } catch (Exception e) {
            searchTime = CommonConstants.MAX_SEARCH_TIME;
        }

        if (maxItemLimit < 0 || maxItemLimit > givenMax) {
            maxItemLimit = givenMax;
        }

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setCountLimit(maxItemLimit);
        searchCtls.setTimeLimit(searchTime);

        if (filter.contains("?") || filter.contains("**")) {
            throw new UserStoreException(
                    "Invalid character sequence entered for user search. Please enter valid sequence.");
        }

        StringBuilder searchFilter =
                new StringBuilder(
                        userStoreProperties.get(LDAPConstants.USER_NAME_LIST_FILTER));
        String searchBases = userStoreProperties.get(LDAPConstants.USER_SEARCH_BASE);

        String userNameProperty =
                userStoreProperties.get(LDAPConstants.USER_NAME_ATTRIBUTE);

        String serviceNameAttribute = "sn";

        StringBuilder finalFilter = new StringBuilder();

        // read the display name attribute - if provided
        String displayNameAttribute =
                userStoreProperties.get(LDAPConstants.DISPLAY_NAME_ATTRIBUTE);

        String[] returnedAtts;

        if (StringUtils.isNotEmpty(displayNameAttribute)) {
            returnedAtts =
                    new String[]{userNameProperty, serviceNameAttribute,
                            displayNameAttribute};
            finalFilter.append("(&").append(searchFilter).append("(").append(displayNameAttribute)
                    .append("=").append(escapeSpecialCharactersForFilterWithStarAsRegex(filter)).append("))");
        } else {
            returnedAtts = new String[]{userNameProperty, serviceNameAttribute};
            finalFilter.append("(&").append(searchFilter).append("(").append(userNameProperty).append("=")
                    .append(escapeSpecialCharactersForFilterWithStarAsRegex(filter)).append("))");
        }

        if (debug) {
            log.debug("Listing users. SearchBase: " + searchBases + " Constructed-Filter: " + finalFilter.toString());
            log.debug("Search controls. Max Limit: " + maxItemLimit + " Max Time: " + searchTime);
        }

        searchCtls.setReturningAttributes(returnedAtts);
        DirContext dirContext = null;
        NamingEnumeration<SearchResult> answer = null;
        List<String> list = new ArrayList<>();

        try {
            dirContext = connectionSource.getContext();
            // handle multiple search bases
            String[] searchBaseArray = searchBases.split("#");

            for (String searchBase : searchBaseArray) {

                answer = dirContext.search(escapeDNForSearch(searchBase), finalFilter.toString(), searchCtls);
                while (answer.hasMoreElements()) {
                    SearchResult sr = answer.next();
                    if (sr.getAttributes() != null) {
                        log.debug("Result found ..");
                        Attribute attr = sr.getAttributes().get(userNameProperty);

						/*
						 * If this is a service principle, just ignore and
						 * iterate rest of the array. The entity is a service if
						 * value of surname is Service
						 */
                        Attribute attrSurname = sr.getAttributes().get(serviceNameAttribute);

                        if (attrSurname != null) {
                            if (debug) {
                                log.debug(serviceNameAttribute + " : " + attrSurname);
                            }
                            String serviceName = (String) attrSurname.get();
                            if (serviceName != null
                                    && serviceName
                                    .equals(LDAPConstants.SERVER_PRINCIPAL_ATTRIBUTE_VALUE)) {
                                continue;
                            }
                        }

                        if (attr != null) {
                            String name = (String) attr.get();
                            list.add(name);
                        }
                    }
                }
            }
            userNames = list.toArray(new String[list.size()]);
            Arrays.sort(userNames);

            if (debug) {
                for (String username : userNames) {
                    log.debug("result: " + username);
                }
            }
        } catch (PartialResultException e) {
            // can be due to referrals in AD. so just ignore error
            String errorMessage =
                    "Error occurred while getting user list for filter : " + filter + "max limit : " + maxItemLimit;
            if (isIgnorePartialResultException()) {
                if (log.isDebugEnabled()) {
                    log.debug(errorMessage, e);
                }
            } else {
                throw new UserStoreException(errorMessage, e);
            }
        } catch (NamingException e) {
            String errorMessage =
                    "Error occurred while getting user list for filter : " + filter + "max limit : " + maxItemLimit;
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new UserStoreException(errorMessage, e);
        } finally {
            JNDIUtil.closeNamingEnumeration(answer);
            JNDIUtil.closeContext(dirContext);
        }
        return userNames;
    }

    private boolean bindAsUser(String dn, String credentials) throws NamingException,
            UserStoreException {
        boolean isAuthed = false;
        boolean debug = log.isDebugEnabled();
        LdapContext cxt = null;
        try {
            // cxt = new InitialLdapContext(env, null);
            cxt = this.connectionSource.getContextWithCredentials(dn, credentials);
            isAuthed = true;
        } catch (AuthenticationException e) {
			 // we avoid throwing an exception here since we throw that exception
            // in a one level above this.
            if (debug) {
                log.debug("Authentication failed " + e);
            }

        } finally {
            JNDIUtil.closeContext(cxt);
        }

        if (debug) {
            log.debug("User: " + dn + " is authnticated: " + isAuthed);
        }
        return isAuthed;
    }

    private NamingEnumeration<SearchResult> searchForUser(String searchFilter,
                                                          String[] returnedAtts,
                                                          DirContext dirContext)
            throws UserStoreException {
        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String searchBases = userStoreProperties.get(LDAPConstants.USER_SEARCH_BASE);
        if (returnedAtts != null && returnedAtts.length > 0) {
            searchCtls.setReturningAttributes(returnedAtts);
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Searching for user with SearchFilter: " + searchFilter + " in SearchBase: " + dirContext.getNameInNamespace());
            } catch (NamingException e) {
                log.debug("Error while getting DN of search base", e);
            }
            if (returnedAtts == null) {
                log.debug("No attributes requested");
            } else {
                for (String attribute : returnedAtts) {
                    log.debug("Requesting attribute :" + attribute);
                }
            }
        }

        String[] searchBaseAraay = searchBases.split("#");
        NamingEnumeration<SearchResult> answer = null;

        try {
            for (String searchBase : searchBaseAraay) {
                answer = dirContext.search(escapeDNForSearch(searchBase), searchFilter, searchCtls);
                if (answer.hasMore()) {
                    return answer;
                }
            }
        } catch (PartialResultException e) {
            // can be due to referrals in AD. so just ignore error
            String errorMessage ="Error occurred while search user for filter : " + searchFilter;
            if (isIgnorePartialResultException()) {
                if (log.isDebugEnabled()) {
                    log.debug(errorMessage, e);
                }
            } else {
                throw new UserStoreException(errorMessage, e);
            }
        } catch (NamingException e) {
            String errorMessage ="Error occurred while search user for filter : " + searchFilter;
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new UserStoreException(errorMessage, e);
        }
        return answer;
    }

    private String getNameInSpaceForUserName(String userName) throws UserStoreException {
        // check the cache first
        LdapName ldn = (LdapName)userCache.get(userName);
        if (ldn != null) {
            return ldn.toString();
        }

        String searchBase;
        String userSearchFilter = userStoreProperties.get(LDAPConstants.USER_NAME_SEARCH_FILTER);
        userSearchFilter = userSearchFilter.replace("?", escapeSpecialCharactersForFilter(userName));
        String userDNPattern = userStoreProperties.get(LDAPConstants.USER_DN_PATTERN);
        if (userDNPattern != null && userDNPattern.trim().length() > 0) {
            String[] patterns = userDNPattern.split("#");
            for (String pattern : patterns) {
                searchBase = MessageFormat.format(pattern, escapeSpecialCharactersForDN(userName));
                String userDN = getNameInSpaceForUserName(userName, searchBase, userSearchFilter);
                // check in another DN pattern
                if (userDN != null) {
                    return userDN;
                }
            }
        }

        searchBase = userStoreProperties.get(LDAPConstants.USER_SEARCH_BASE);

        return getNameInSpaceForUserName(userName, searchBase, userSearchFilter);

    }

    private String getNameInSpaceForUserName(String userName, String searchBase, String searchFilter) throws UserStoreException {
        boolean debug = log.isDebugEnabled();

        if (userCache.get(userName) != null) {
            return userCache.get(userName).toString();
        }

        String userDN = null;

        DirContext dirContext = this.connectionSource.getContext();
        NamingEnumeration<SearchResult> answer = null;
        try {
            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            if (log.isDebugEnabled()) {
                try {
                    log.debug("Searching for user with SearchFilter: " + searchFilter + " in SearchBase: " + dirContext.getNameInNamespace());
                } catch (NamingException e) {
                    log.debug("Error while getting DN of search base", e);
                }
            }
            SearchResult userObj;
            String[] searchBases = searchBase.split("#");
            for (String base : searchBases) {
                answer = dirContext.search(escapeDNForSearch(base), searchFilter, searchCtls);
                if (answer.hasMore()) {
                    userObj = answer.next();
                    if (userObj != null) {
                        //no need to decode since , if decoded the whole string, can't be encoded again
                        //eg CN=Hello\,Ok=test\,test, OU=Industry
                        userDN = userObj.getNameInNamespace();
                        break;
                    }
                }
            }
            if (userDN != null) {
                LdapName ldn = new LdapName(userDN);
                userCache.put(userName, ldn);
            }
            if (debug) {
                log.debug("Name in space for " + userName + " is " + userDN);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        } finally {
            JNDIUtil.closeNamingEnumeration(answer);
            JNDIUtil.closeContext(dirContext);
        }
        return userDN;
    }


    private String escapeSpecialCharactersForFilter(String dnPartial) {
        boolean replaceEscapeCharacters = true;
        dnPartial = dnPartial.replace("\\*", "*");

        String replaceEscapeCharactersAtUserLoginString = userStoreProperties
                .get(CommonConstants.PROPERTY_REPLACE_ESCAPE_CHARACTERS_AT_USER_LOGIN);

        if (replaceEscapeCharactersAtUserLoginString != null) {
            replaceEscapeCharacters = Boolean
                    .parseBoolean(replaceEscapeCharactersAtUserLoginString);
            if (log.isDebugEnabled()) {
                log.debug("Replace escape characters configured to: "
                        + replaceEscapeCharactersAtUserLoginString);
            }
        }

        if (replaceEscapeCharacters) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dnPartial.length(); i++) {
                char currentChar = dnPartial.charAt(i);
                switch (currentChar) {
                    case '\\':
                        sb.append("\\5c");
                        break;
                    case '*':
                        sb.append("\\2a");
                        break;
                    case '(':
                        sb.append("\\28");
                        break;
                    case ')':
                        sb.append("\\29");
                        break;
                    case '\u0000':
                        sb.append("\\00");
                        break;
                    default:
                        sb.append(currentChar);
                }
            }
            return sb.toString();
        } else {
            return dnPartial;
        }
    }

    private String escapeSpecialCharactersForDN(String text) {
        boolean replaceEscapeCharacters = true;
        text = text.replace("\\*", "*");

        String replaceEscapeCharactersAtUserLoginString = userStoreProperties
                .get(CommonConstants.PROPERTY_REPLACE_ESCAPE_CHARACTERS_AT_USER_LOGIN);

        if (replaceEscapeCharactersAtUserLoginString != null) {
            replaceEscapeCharacters = Boolean
                    .parseBoolean(replaceEscapeCharactersAtUserLoginString);
            if (log.isDebugEnabled()) {
                log.debug("Replace escape characters configured to: "
                        + replaceEscapeCharactersAtUserLoginString);
            }
        }

        if (replaceEscapeCharacters) {
            StringBuilder sb = new StringBuilder();
            if ((text.length() > 0) && ((text.charAt(0) == ' ') || (text.charAt(0) == '#'))) {
                sb.append('\\'); // add the leading backslash if needed
            }
            for (int i = 0; i < text.length(); i++) {
                char currentChar = text.charAt(i);
                switch (currentChar) {
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case ',':
                        sb.append("\\,");
                        break;
                    case '+':
                        sb.append("\\+");
                        break;
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '<':
                        sb.append("\\<");
                        break;
                    case '>':
                        sb.append("\\>");
                        break;
                    case ';':
                        sb.append("\\;");
                        break;
                    case '*':
                        sb.append("\\2a");
                        break;
                    default:
                        sb.append(currentChar);
                }
            }
            if ((text.length() > 1) && (text.charAt(text.length() - 1) == ' ')) {
                sb.insert(sb.length() - 1, '\\'); // add the trailing backslash if needed
            }
            if (log.isDebugEnabled()) {
                log.debug("value after escaping special characters in " + text + " : " + sb.toString());
            }
            return sb.toString();
        } else {
            return text;
        }

    }

    private String escapeDNForSearch(String dn){
        boolean replaceEscapeCharacters = true;

        String replaceEscapeCharactersAtUserLoginString = userStoreProperties
                .get(CommonConstants.PROPERTY_REPLACE_ESCAPE_CHARACTERS_AT_USER_LOGIN);

        if (replaceEscapeCharactersAtUserLoginString != null) {
            replaceEscapeCharacters = Boolean
                    .parseBoolean(replaceEscapeCharactersAtUserLoginString);
            if (log.isDebugEnabled()) {
                log.debug("Replace escape characters configured to: "
                        + replaceEscapeCharactersAtUserLoginString);
            }
        }
        if (replaceEscapeCharacters) {
            return dn.replace("\\\\", "\\\\\\").replace("\\\"", "\\\\\"");
        } else {
            return dn;
        }
    }

    private String escapeSpecialCharactersForFilterWithStarAsRegex(String dnPartial){
        boolean replaceEscapeCharacters = true;

        String replaceEscapeCharactersAtUserLoginString = userStoreProperties
                .get(CommonConstants.PROPERTY_REPLACE_ESCAPE_CHARACTERS_AT_USER_LOGIN);

        if (replaceEscapeCharactersAtUserLoginString != null) {
            replaceEscapeCharacters = Boolean
                    .parseBoolean(replaceEscapeCharactersAtUserLoginString);
            if (log.isDebugEnabled()) {
                log.debug("Replace escape characters configured to: "
                        + replaceEscapeCharactersAtUserLoginString);
            }
        }

        if (replaceEscapeCharacters) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dnPartial.length(); i++) {
                char currentChar = dnPartial.charAt(i);
                switch (currentChar) {
                    case '\\':
                        if(dnPartial.charAt(i+1) == '*'){
                            sb.append("\\2a");
                            i++;
                            break;
                        }
                        sb.append("\\5c");
                        break;
                    case '(':
                        sb.append("\\28");
                        break;
                    case ')':
                        sb.append("\\29");
                        break;
                    case '\u0000':
                        sb.append("\\00");
                        break;
                    default:
                        sb.append(currentChar);
                }
            }
            return sb.toString();
        } else {
            return dnPartial;
        }
    }

    private boolean isIgnorePartialResultException() {

        return PROPERTY_REFERRAL_IGNORE.equals(userStoreProperties.get(LDAPConstants.PROPERTY_REFERRAL));
    }

}
