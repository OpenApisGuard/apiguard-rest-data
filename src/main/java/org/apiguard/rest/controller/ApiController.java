package org.apiguard.rest.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.entity.Api;
import org.apiguard.http.ApiGuardApacheHttpClient;
import org.apiguard.http.HttpClientException;
import org.apiguard.rest.service.AuthChecker;
import org.apiguard.service.ApiService;
import org.apiguard.service.exceptions.ApiAuthException;
import org.apiguard.valueobject.BaseRestResource;
import org.apiguard.valueobject.EexceptionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

@Controller
@RequestMapping(value = "/")
public class ApiController extends BaseController {
    private static final Logger log = LogManager.getLogger(ApiController.class);

    @Autowired
    private ApiService<ApiEntity> apiService;

    @Autowired
    private AuthChecker authChecker;

    @Autowired
    private ApiGuardApacheHttpClient httpClient;

    @RequestMapping(value = "/**")
    @ResponseBody
    public ResponseEntity forwardApi(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String reqUri = req.getServletPath().replaceFirst("/apis", "");
        if (!isValid(reqUri)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request uri is not provided.");
        }

        Api api = apiService.getApiByReqUri(reqUri);
        if (api == null || api.getDownstreamUri().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request: " + reqUri);
        }

        // check whether api needs auth
        try {
            if (api.isAuthRequired() && !authChecker.authenticate(api.getReqUri(), req)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your authentication credentials are invalid.");
            }
        } catch (ApiAuthException e) {
            log.info("Faied to auth because of bad credentials");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }

        return forward(req, res, api);
    }

    private ResponseEntity forward(HttpServletRequest req, HttpServletResponse res, Api api) throws HttpClientException, IOException {
        String method = req.getMethod();

        //TODO: add patch and option
        //TODO: add option to pass headers or custom headers

        //TODO: add ftp support
        ResponseEntity resp = null;
        if (method.equalsIgnoreCase(ApiGuardApacheHttpClient.METHOD_DELETE)) {
            resp = httpClient.delete(api.getDownstreamUri(), req.getQueryString(), getHeaders(req), getContent(req));
        }
        else if (method.equalsIgnoreCase(ApiGuardApacheHttpClient.METHOD_POST)) {
            resp = httpClient.post(api.getDownstreamUri(), req.getQueryString(), getHeaders(req), getContent(req));
        }
        else if (method.equalsIgnoreCase(ApiGuardApacheHttpClient.METHOD_PUT)) {
            resp = httpClient.put(api.getDownstreamUri(), req.getQueryString(), getHeaders(req), getContent(req));
        }
        else { // get
            resp = httpClient.get(api.getDownstreamUri(), req.getQueryString(), getHeaders(req));
        }

        return resp;
    }
}