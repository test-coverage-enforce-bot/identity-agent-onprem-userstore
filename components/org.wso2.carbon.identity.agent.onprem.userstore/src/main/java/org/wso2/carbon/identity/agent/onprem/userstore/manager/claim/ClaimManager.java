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

package org.wso2.carbon.identity.agent.onprem.userstore.manager.claim;


import org.wso2.carbon.identity.agent.onprem.userstore.model.Claim;
import java.util.ArrayList;
import java.util.Map;


/**
 * Userstore Claim Manager.
 */
public class ClaimManager {
    private Map<String, Claim> claimMap;

    public ClaimManager(Map<String, Claim> claimMap) {
        this.claimMap = claimMap;
    }

    /**
     * @param claimURI URI of the claim whose attribute name needed.
     * @return Name of the attribute mapped to the given claimURI
     */
    public String getClaimAttribute(String claimURI) {
        Claim claim = claimMap.get(claimURI);
        if (claim != null) {
            return claim.getAttributeID();
        }
        return null;
    }

    public String[] doListClaims() {
        ArrayList<String> claimList = new ArrayList<>();
        for (Map.Entry<String, Claim> mapEntry : claimMap.entrySet()) {
            if (mapEntry.getValue().isEnabled()) {
                claimList.add(mapEntry.getKey());
            }
        }
        return claimList.toArray(new String[0]);
    }
}
