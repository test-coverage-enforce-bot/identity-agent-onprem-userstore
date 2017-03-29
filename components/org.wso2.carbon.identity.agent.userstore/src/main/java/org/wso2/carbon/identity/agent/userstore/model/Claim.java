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

package org.wso2.carbon.identity.agent.userstore.model;

/**
 * Model representing a single claim.
 */
public class Claim {
    private String claimURI;
    private String attributeID;

    /**
     * @return claimURI of the claim.
     */
    public String getClaimURI() {
        return claimURI;
    }

    /**
     * @param claimURI ClaimURI of the representing claim.
     */
    public void setClaimURI(String claimURI) {
        this.claimURI = claimURI;
    }

    /**
     * @return ID of the attribute mapped to this claim.
     */
    public String getAttributeID() {
        return attributeID;
    }

    /**
     * @param attributeID ID of the attribute mapped to this claim.
     */
    public void setAttributeID(String attributeID) {
        this.attributeID = attributeID;
    }
}
