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

package org.wso2.carbon.identity.agent.outbound.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.agent.outbound.exception.ClaimManagerException;
import org.wso2.carbon.identity.agent.outbound.model.Claim;

import java.util.Map;

/**
 *  Singleton to get the claim mappings from claim-org.wso2.carbon.identity.agent.outbound.config.xml.
 */
public class ClaimConfiguration {

    private static Logger log = LoggerFactory.getLogger(UserStoreConfiguration.class);
    private Map<String, Claim> claimMap;
    private static ClaimConfiguration instance = new ClaimConfiguration();

    private ClaimConfiguration() {
        try {
            init();
        } catch (ClaimManagerException e) {
            log.error("Error in configuring the ClaimManager: " + e.getMessage());
        }
    }

    /**
     *  Initializes the Claims with attribute IDs.
     */
    private void init() throws ClaimManagerException {
        ClaimConfigurationXMLProcessor claimConfigurationXMLProcessor
                = new ClaimConfigurationXMLProcessor();
        claimMap = claimConfigurationXMLProcessor.buildClaimConfigurationsFromFile();
    }

    /**
     * @return Instance of ClaimConfiguration with claims mapped to attributes.
     */
    public static ClaimConfiguration getConfiguration() {
        return instance;
    }

    /**
     * @return The map of claims from claim-org.wso2.carbon.identity.agent.outbound.config.xml.
     */
    public Map<String, Claim> getClaimMap() {
        return claimMap;
    }
}
