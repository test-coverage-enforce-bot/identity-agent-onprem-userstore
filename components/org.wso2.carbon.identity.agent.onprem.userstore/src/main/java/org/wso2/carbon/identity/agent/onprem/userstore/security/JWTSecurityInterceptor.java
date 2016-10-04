/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.agent.onprem.userstore.security;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.msf4j.Interceptor;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.ServiceMethodInfo;
import org.wso2.msf4j.security.MSF4JSecurityException;
import org.wso2.msf4j.security.SecurityErrorCode;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Locale;
import java.util.Map;

/**
 * JWT security interceptor which validate the signature of each request.
 */
public class JWTSecurityInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(JWTSecurityInterceptor.class);
    private static final String AUTHORIZATION_HTTP_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "bearer";
    private static final String PUBLIC_KEY_LOCATION = "public.cert";
    private static final String CERTIFICATE_TYPE = "X509";
    private static final String STATUS_URI = "/status";

    private JSONObject jsonHeaderObject;
    private static final Base64 base64Url = new Base64(true);

    @Override
    public boolean preCall(Request request, Response responder, ServiceMethodInfo serviceMethodInfo)
            throws Exception {

        try {
            //No need to check security for status endpoint
            if (request.getUri().startsWith(STATUS_URI)) {
                return true;
            }

            Map<String, String> headers = request.getHeaders();

            if (headers != null && headers.containsKey(AUTHORIZATION_HTTP_HEADER)) {
                String authHeader = headers.get(AUTHORIZATION_HTTP_HEADER);
                String token = extractAccessToken(authHeader);
                return isValid(token);
            } else {
                throw new MSF4JSecurityException(SecurityErrorCode.AUTHENTICATION_FAILURE,
                        "Missing Authorization header is the request.");
            }
        } catch (MSF4JSecurityException e) {
            log.error(e.getMessage() + " Requested Path: " + request.getUri());
        }
        return false;
    }

    /**
     * @param authHeader Authorization Bearer header which contains the access token
     * @return access token
     */
    private String extractAccessToken(String authHeader) throws MSF4JSecurityException {
        authHeader = authHeader.trim();
        if (authHeader.toLowerCase(Locale.US).startsWith(BEARER_PREFIX)) {
            String[] authHeaderParts = authHeader.split(" ");
            if (authHeaderParts.length == 2) {
                return authHeaderParts[1];
            }
        }
        throw new MSF4JSecurityException(SecurityErrorCode.INVALID_AUTHORIZATION_HEADER,
                "Invalid Authorization header: " + authHeader);
    }

    private boolean isValid(String jwtToken) {

        String[] jwtTokenValues = jwtToken.split("\\.");
        String jwtAssertion = null;
        byte[] jwtSignature = null;

        if (jwtTokenValues.length > 0) {
            String value = new String(base64Url.decode(jwtTokenValues[0].getBytes()));
            JSONParser parser = new JSONParser();
            try {
                jsonHeaderObject = (JSONObject) parser.parse(value);
            } catch (ParseException e) {
                log.error("Error occurred while parsing JSON header ", e);
            }
        }

        if (jwtTokenValues.length > 1) {
            jwtAssertion = jwtTokenValues[0] + "." + jwtTokenValues[1];
        }

        if (jwtTokenValues.length > 2) {
            jwtSignature = base64Url.decode(jwtTokenValues[2].getBytes());
        }

        if (jwtAssertion != null && jwtSignature != null) {

            try {
                InputStream inStream = new FileInputStream(PUBLIC_KEY_LOCATION);
                Certificate certificate = CertificateFactory.getInstance(CERTIFICATE_TYPE)
                        .generateCertificate(inStream);
                Signature signature = Signature.getInstance(getSignatureAlgorithm(jsonHeaderObject));
                signature.initVerify(certificate);
                signature.update(jwtAssertion.getBytes());
                return signature.verify(jwtSignature);
            } catch (Exception e) {
                log.error("Error occurred while validating signature", e);
            }
        } else {
            log.warn("No signature exist in the request.");
            return false;
        }
        return false;
    }

    private String getSignatureAlgorithm(JSONObject jsonHeaderObject) {

        String signatureAlgorithm = (String) jsonHeaderObject.get("alg");

        switch (signatureAlgorithm) {
        case "RS256":
            signatureAlgorithm = "SHA256withRSA";
            break;
        case "RS515":
            signatureAlgorithm = "SHA512withRSA";
            break;
        case "RS384":
            signatureAlgorithm = "SHA384withRSA";
            break;
        case "RS512":
            signatureAlgorithm = "SHA512withRSA";
            break;
        default:
            signatureAlgorithm = "SHA256withRSA";
        }
        return signatureAlgorithm;
    }

    @Override
    public void postCall(Request request, int status, ServiceMethodInfo serviceMethodInfo) {

    }

}
