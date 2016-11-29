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

package org.wso2.carbon.identity.agent.onprem.userstore.model;

/**
 * Model representing a single claim.
 */
public class Claim {
    private String claimURI;
    private String attributeID;
    private String displayName;
    private String description;
    private boolean enabled;
    private boolean required;
    private int displayOrder;
    private boolean supportedByDefault;

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

    /**
     * @return Display Name of the claim.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName Display Name of the claim.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return Description of the Claim.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Description of the Claim.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Whether the claim is enabled or not.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled Whether the claim is enabled or not.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return Whether the claim is a required one.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @param required Whether the claim is a required one.
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * @return Number in the order of displaying.
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * @param displayOrder Number in the order of displaying.
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * @return Whether the claim is supported by default.
     */
    public boolean isSupportedByDefault() {
        return supportedByDefault;
    }

    /**
     * @param supportedByDefault Whether the claim is supported by default.
     */
    public void setSupportedByDefault(boolean supportedByDefault) {
        this.supportedByDefault = supportedByDefault;
    }
}
