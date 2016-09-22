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
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.onprem.userstore.constant.XMLConfigurationConstants;
import org.wso2.carbon.identity.agent.onprem.userstore.util.UserStoreUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class UserStoreConfigurationXMLProcessor {
    private static Logger log = LoggerFactory.getLogger(UserStoreConfigurationXMLProcessor.class);
    private static final String USERSTORE_CONFIG_FILE = "userstore-config.xml";
    private static Map<String,String> properties;
    private InputStream inStream = null;

    Map<String,String> buildUserStoreConfigurationFromFile(){
        OMElement rootElement;
        try {
            rootElement = getRootElement();
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
            map.put(propName.trim(), propValue.trim());
        }
        return map;
    }


    private OMElement getRootElement() throws XMLStreamException, IOException{
        OMXMLParserWrapper builder;

        File profileConfigXml = new File(UserStoreUtils.getConfigDirPath(),
                USERSTORE_CONFIG_FILE);
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
        builder = OMXMLBuilderFactory.createOMBuilder(inStream);

        return builder.getDocumentElement();
    }

}
