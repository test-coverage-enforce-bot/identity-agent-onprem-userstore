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
package org.wso2.carbon.identity.agent.onprem.userstore.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.constant.CommonConstants;
import org.wso2.carbon.identity.agent.onprem.userstore.constant.XMLConfigurationConstants;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.UserStoreException;
import org.wso2.carbon.identity.agent.onprem.userstore.exception.XMLException;
import org.wso2.carbon.identity.agent.onprem.userstore.util.XMLUtils;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;


/**.
 *  Preocess XML files and retrieve properties
 */
class UserStoreConfigurationXMLProcessor {
    private static Logger log = LoggerFactory.getLogger(UserStoreConfigurationXMLProcessor.class);
    private static final String USERSTORE_CONFIG_FILE = "userstore-config.xml";
    private static final String CONF_DIR = "conf";
    private static Map<String, String> properties;
    private InputStream inStream = null;
    private SecretResolver secretResolver;

    Map<String, String> buildUserStoreConfigurationFromFile() {
        OMElement rootElement;
        try {
            rootElement = getConfigElement();
            properties = buildUserStoreConfiguration(rootElement);

            if (inStream != null) {
                inStream.close();
            }
        } catch (Exception e) {
            String message = "Error while reading userstore configuration from file";
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
        }
        return properties;
    }

    private Map<String, String> buildUserStoreConfiguration(OMElement rootElement) {
        Map<String, String> map = new HashMap<>();
        Iterator<?> ite = rootElement.getChildrenWithName(new QName(
                XMLConfigurationConstants.LOCAL_NAME_PROPERTY));
        while (ite.hasNext()) {
            OMElement propElem = (OMElement) ite.next();
            String propName = propElem.getAttributeValue(new QName(
                    XMLConfigurationConstants.ATTR_NAME_PROP_NAME));
            String propValue = propElem.getText();

            if (secretResolver != null && secretResolver.isInitialized()) {
                if (secretResolver.isTokenProtected("UserManager.Property." + propName)) {
                    propValue = secretResolver.resolve("UserManager.Property." + propName);
                }
            }
            map.put(propName.trim(), propValue.trim());
        }
        return map;
    }


    private OMElement getConfigElement() throws XMLStreamException, IOException, UserStoreException {
        OMXMLParserWrapper builder;

        File profileConfigXml = new File(System.getProperty(CommonConstants.CARBON_HOME),
                CONF_DIR + File.separator + USERSTORE_CONFIG_FILE);
        if (profileConfigXml.exists()) {
            inStream = new FileInputStream(profileConfigXml);
        }

        if (inStream == null) {
            String message = "Profile configuration not found.";
            if (log.isDebugEnabled()) {
                log.debug(message);
            }
            throw new FileNotFoundException(message);
        }
        try {
            inStream = XMLUtils.replaceSystemVariablesInXml(inStream);
        } catch (XMLException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        builder = new StAXOMBuilder(inStream);
        OMElement rootElement = builder.getDocumentElement();

        setSecretResolver(rootElement);


        return rootElement.getFirstChildWithName(new QName(
                XMLConfigurationConstants.LOCAL_NAME_CONFIGURATION));
    }

    private void setSecretResolver(OMElement rootElement) {
        secretResolver = SecretResolverFactory.create(rootElement, true);
    }
}
