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

package org.wso2.carbon.identity.agent.userstore.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.userstore.constant.CommonConstants;
import org.wso2.carbon.identity.agent.userstore.constant.XMLConfigurationConstants;
import org.wso2.carbon.identity.agent.userstore.exception.ClaimManagerException;
import org.wso2.carbon.identity.agent.userstore.exception.XMLException;
import org.wso2.carbon.identity.agent.userstore.model.Claim;
import org.wso2.carbon.identity.agent.userstore.util.XMLUtils;

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

/**
 *  Process XML files and retrieve claims.
 */
public class ClaimConfigurationXMLProcessor {

    private static Logger log = LoggerFactory.getLogger(UserStoreConfigurationXMLProcessor.class);
    private static final String CLAIM_CONFIG_FILE = "claim-org.wso2.carbon.identity.agent.outbound.config.xml";
    private static final String CONF_DIR = "conf";
    private InputStream inStream = null;

    /**
     * @return The Map of claims and attributes
     */
    Map<String, Claim> buildClaimConfigurationsFromFile() throws ClaimManagerException {
        OMElement rootElement;
        Map<String, Claim> properties;
        try {
            rootElement = getRootElement();
            properties = buildClaimConfiguration(rootElement);

            if (inStream != null) {
                inStream.close();
            }
        } catch (IOException e) {
            String message = "Error while closing the input stream";
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
            throw new ClaimManagerException(message, e);
        } catch (XMLStreamException e) {
            String message = "Error while validating the XML file";
            if (log.isDebugEnabled()) {
                log.debug(message, e);
            }
            throw new ClaimManagerException(message, e);
        }
        return properties;
    }

    /**
     * @param rootElement The root OMElement of the XML file
     * @return The map of claims
     */
    private Map<String, Claim> buildClaimConfiguration(OMElement rootElement) throws ClaimManagerException {
        Claim claim = null;
        String claimURI;
        Map<String, Claim> map = new HashMap<>();
        Iterator<?> ite = rootElement.getChildrenWithName(new QName(
                XMLConfigurationConstants.LOCAL_NAME_CLAIM));
        while (ite.hasNext()) {
            claim = new Claim();
            OMElement claimElement = (OMElement) ite.next();
            OMElement claimURIElement =
                    claimElement.getFirstChildWithName(
                            new QName(XMLConfigurationConstants.LOCAL_NAME_CLAIM_URI));
            if (claimURIElement != null) {
                claimURI = claimURIElement.getText();
                claim.setClaimURI(claimURI);
            } else {
                throw new ClaimManagerException("Required Attribute Claim URI not set in a claim");
            }
            OMElement attributeIDElement =
                    claimElement.getFirstChildWithName(new QName(XMLConfigurationConstants.LOCAL_NAME_ATTRIBUTE_ID));
            if (attributeIDElement != null) {
                String attributeID = attributeIDElement.getText();
                claim.setAttributeID(attributeID);
            }
            map.put(claimURI, claim);
        }
        return map;
    }

    /**
     * @return The <ClaimConfig> element of the claim-org.wso2.carbon.identity.agent.outbound.config.xml file.
     * @throws javax.xml.stream.XMLStreamException If an error occurs in building the XML configurations.
     * @throws java.io.IOException If the file does not exist, is a directory rather than a regular file,
     * or for some other reason cannot be opened for reading.
     * @throws ClaimManagerException If the inputStream is null or cannot validate the XML file.
     */
    private OMElement getRootElement() throws XMLStreamException, IOException, ClaimManagerException {
        OMXMLParserWrapper builder;

        File profileConfigXml = new File(System.getProperty(CommonConstants.CARBON_HOME),
                CONF_DIR + File.separator + CLAIM_CONFIG_FILE);
        if (profileConfigXml.exists()) {

            inStream = new FileInputStream(profileConfigXml);
        }

        if (inStream == null) {
            String message = "Claim configuration not found.";
            if (log.isDebugEnabled()) {
                log.debug(message);
            }
            throw new FileNotFoundException(message);
        }
        try {
            inStream = XMLUtils.replaceSystemVariablesInXml(inStream);
        } catch (XMLException e) {
            throw new ClaimManagerException(e.getMessage(), e);
        }

        builder = new StAXOMBuilder(inStream);
        OMElement rootElement = builder.getDocumentElement();
        return rootElement;
    }
}
