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

package org.wso2.carbon.identity.agent.outbound.exception;

/**
 * The org.wso2.carbon.identity.agent.outbound.exception to throw when there is a problem with the claim management.
 */
public class ClaimManagerException extends Exception {

    /*
     * Default serial
     */
    private static final long serialVersionUID = -6057036683816666265L;

    public ClaimManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClaimManagerException(String message) {
        super(message);
    }
}
