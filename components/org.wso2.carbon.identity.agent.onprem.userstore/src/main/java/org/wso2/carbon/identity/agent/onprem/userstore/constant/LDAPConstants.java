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
package org.wso2.carbon.identity.agent.onprem.userstore.constant;

public class LDAPConstants {
    public static final String CONNECTION_URL = "ConnectionURL";
    public static final String CONNECTION_NAME = "ConnectionName";
    public static final String CONNECTION_PASSWORD = "ConnectionPassword";
    public static final String USER_SEARCH_BASE = "UserSearchBase";
    public static final String USER_NAME_LIST_FILTER = "UserNameListFilter";
    public static final String USER_NAME_ATTRIBUTE = "UserNameAttribute";
    public static final String DISPLAY_NAME_ATTRIBUTE = "DisplayNameAttribute";
    public static final String USER_DN_PATTERN = "UserDNPattern";
    public static final String PROPERTY_REFERRAL = "Referral";
    //filter attribute in user-mgt.xml that filters users by user name
    public static final String USER_NAME_SEARCH_FILTER = "UserNameSearchFilter";
    //KDC specific constant
    public static final String SERVER_PRINCIPAL_ATTRIBUTE_VALUE = "Service";
    //DNS related constant
    public static final String ACTIVE_DIRECTORY_DOMAIN_CONTROLLER_SERVICE = "_ldap._tcp.";
    public static final String SRV_ATTRIBUTE_NAME = "SRV";
    public static final String A_RECORD_ATTRIBUTE_NAME = "A";
    public static final String DNS_URL = "URLOfDNS";
    public static final String DNS_DOMAIN_NAME = "DNSDomainName";
    public static final String CONNECTION_POOLING_ENABLED = "ConnectionPoolingEnabled";

}
