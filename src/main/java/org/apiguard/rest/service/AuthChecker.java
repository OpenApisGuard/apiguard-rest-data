package org.apiguard.rest.service;

import org.apiguard.constants.AuthType;
import org.apiguard.service.ApiAuthService;
import org.apiguard.service.exceptions.ApiAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.util.Base64;

public abstract class AuthChecker {

    public final static String CLIENT_ALIAS = "keyId";
    public final static String ALGORITHM = "algorithm";
    public final static String HEADERS = "headers";
    public final static String SIGNATURE = "signature";

    @Autowired
    ApiAuthService apiService;

    @Autowired
    ApiAuthService apiAuthService;

    public boolean authenticate(String reqUri, HttpServletRequest req) throws ApiAuthException {
        String keyVal = req.getHeader(AuthType.KEY.getKey());
        if (keyVal != null) {
            return apiAuthService.keyAuthMatches(reqUri, keyVal);
        }

        //TODO: implement other auth
        //oauth, jwt, ldap and hmac use Authorization header
        keyVal = req.getHeader(HttpHeaders.AUTHORIZATION);

        try {
            if (keyVal != null) {
                String authid = keyVal.substring(0, keyVal.indexOf(" "));
                AuthType authType = AuthType.getAuthByKey(authid.toLowerCase());

                if (authType == null) {
                    throw new ApiAuthException("Invalid authorization type: " + authType);
                }

                switch (authType) {
                    case BASIC:
                        String base64 = keyVal.substring(keyVal.lastIndexOf(" ") + 1);
                        String credential = new String(DatatypeConverter.parseBase64Binary(base64)).replaceAll("\n", "");
                        int ind = credential.indexOf(":");
                        return apiAuthService.basicAuthMatches(reqUri, credential.substring(0, ind), credential.substring(ind+1));
                    case DIGITAL_SIGNATURE:
                        break;
                    case HMAC:
                        break;
                    case JWT:
                        break;
                    case LDAP:
                        break;
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
            return false;
        }

        return false;
    }

    public abstract boolean validateSignature(String reqUri, HttpServletRequest req) throws ApiAuthException;
}
