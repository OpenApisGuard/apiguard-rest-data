package org.apiguard.rest.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apiguard.service.exceptions.ApiAuthException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

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

@Component
public class DefaultAuthChecker extends AuthChecker {

    private static final Logger log = LogManager.getLogger(DefaultAuthChecker.class);

    /**
     * Authorization:signature keyId="clientId:clientAlias",algorithm="hmac-sha256",headers="request date digest",signature="abc.."
     *
     * (request-target): get /bams/items/011060454/inventory
     * date: Thu, 20 Apr 2017 14:01:19 PDT
     * digest: SHA-256=47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=
     *
     * @param reqUri
     * @param req
     * @return
     * @throws ApiAuthException
     */
    public boolean validateSignature(String reqUri, HttpServletRequest req) throws ApiAuthException {
        try {
            String authVal = req.getHeader(HttpHeaders.AUTHORIZATION);
            authVal = authVal.substring(authVal.indexOf(" ")+1);

            String clientId = "";
            String clientAlias = "";
            String algorithm = "";
            String signature = "";

            List<String> headersToSign = new ArrayList<String>();

            String[] params = authVal.split(",");
            for (String p : params) {
                int ind = p.indexOf("=");
                String k = p.substring(0, ind);
                String v = p.substring(ind+2, p.length()-1);

                if (k.equals(CLIENT_ALIAS)) {
                    int endIndex = v.indexOf(":");
                    clientId = v.substring(0, endIndex);
                    clientAlias = v.substring(endIndex+1);
                }
                else if (k.equals(ALGORITHM)) {
                    algorithm = v;
                }
                else if (k.equals(SIGNATURE)) {
                    signature = v;
                }
                else if (k.equals(HEADERS)) {
                    String[] headers = v.split(" ");
                    for (String h : headers) {
                        headersToSign.add(h);
                    }
                }
            }

            if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientAlias) || StringUtils.isEmpty(algorithm) || headersToSign.isEmpty()) {
                throw new ApiAuthException("Required auth info is missing.  Please check http signature documentation for more info.");
            }

            // prepare for string to sign
            StringBuilder sb = new StringBuilder();
            sb.append("request: ");
            sb.append(req.getMethod().toLowerCase());
            sb.append(" ");
            sb.append(req.getRequestURI());

            if (! StringUtils.isEmpty(req.getQueryString())) {
                sb.append("?");
                sb.append(req.getQueryString());
            }

            for (String s : headersToSign) {
                String hv = req.getHeader(s);
                if (! StringUtils.isEmpty(hv)) {
                    sb.append("\n");
                    sb.append(s);
                    sb.append(": ");
                    sb.append(hv);
                }
            }

            String stringToSign = sb.toString();

            return apiAuthService.signatureAuthMatches(reqUri, clientId, clientAlias, algorithm, stringToSign, signature);
        }
        catch(Exception e) {
            log.error(e.getMessage(), e);
            throw new ApiAuthException(e.getMessage(), e);
        }
    }
}
