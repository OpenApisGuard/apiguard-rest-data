package org.apiguard.rest.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apiguard.constants.AuthType;
import org.apiguard.service.ApiAuthService;
import org.apiguard.service.exceptions.ApiAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public abstract class AuthChecker {
    public final static String CLIENT_ALIAS = "keyId";
    public final static String ALGORITHM = "algorithm";
    public final static String HEADERS = "headers";
    public final static String SIGNATURE = "signature";

    private static final Logger log = LogManager.getLogger(AuthChecker.class);

    @Autowired
    ApiAuthService apiAuthService;

    public boolean authenticate(String reqUri, HttpServletRequest req) throws ApiAuthException {
        String keyVal = req.getHeader(AuthType.KEY.getKey());
        if (keyVal != null) {
            log.info("Validating auth using key auth: " + keyVal);
            return apiAuthService.keyAuthMatches(reqUri, keyVal);
        }

        //TODO: implement other auth
        //oauth, jwt, and hmac use Authorization header
        keyVal = req.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (keyVal != null) {
                String authid = keyVal.substring(0, keyVal.indexOf(" "));
                AuthType authType = AuthType.getAuthByKey(authid.toLowerCase());

                if (authType == null) {
                    log.warn("Invalid authorization type: " + authType + " for reqUri: " + reqUri);
                    throw new ApiAuthException("Invalid authorization type: " + authType + " for reqUri: " + reqUri);
                }

                log.debug("Validating authorization using auth type: " + authType + " for reqUri: " + reqUri);

                switch (authType) {
                    case BASIC:
                        String credential = getCredentials(keyVal);
                        int ind = credential.indexOf(":");
                        return apiAuthService.basicAuthMatches(reqUri, credential.substring(0, ind), credential.substring(ind+1));
                    case DIGITAL_SIGNATURE:
                        break;
                    case HMAC:
                        break;
                    case JWT:
                        return apiAuthService.jwtAuthMatches(getToken(keyVal));
                    case LDAP:
                        String ldapCredential = getCredentials(keyVal);
                        int cInd = ldapCredential.indexOf(":");
                        return apiAuthService.ldapAuthMatches(reqUri, ldapCredential.substring(0, cInd), ldapCredential.substring(cInd+1));
                    case OAUTH2:
                        break;
                    case SIGNATURE:
                        return validateSignature(reqUri, req);
                    default:
                        throw new ApiAuthException("Invalid authorization type: " + authType);
                }
            }
        }
        catch(Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        log.info("Authorization header is not found for reqUri: " + reqUri);

        return false;
    }

    private String getCredentials(String keyVal) {
        String base64 = keyVal.substring(keyVal.lastIndexOf(" ") + 1);
        return new String(DatatypeConverter.parseBase64Binary(base64)).replaceAll("\n", "");
    }

    private String getToken(String keyVal) {
        return keyVal.substring(keyVal.lastIndexOf(" ") + 1);
    }

    public abstract boolean validateSignature(String reqUri, HttpServletRequest req) throws ApiAuthException;
}
