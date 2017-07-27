package org.apiguard.rest.controller;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.constants.AuthType;
import org.apiguard.entity.Api;
import org.apiguard.http.ApiGuardApacheHttpClient;
import org.apiguard.http.HttpClientException;
import org.apiguard.rest.service.AuthChecker;
import org.apiguard.rest.utils.ObjectsConverter;
import org.apiguard.service.ApiService;
import org.apiguard.service.exceptions.ApiAuthException;
import org.apiguard.service.exceptions.ApiException;
import org.apiguard.valueobject.ApiVo;
import org.apiguard.valueobject.BaseRestResource;
import org.apiguard.valueobject.EexceptionVo;
import org.apiguard.valueobject.MessageStringVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

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
@RequestMapping(value = "/apis")
public class ApiController extends BaseController {
    @Autowired
    private ApiService<ApiEntity> apiService;

    @Autowired
    private AuthChecker authChecker;

    @Autowired
    private ApiGuardApacheHttpClient httpClient;

//    @PostConstruct
//    public void createWebClient() {
//        List<ApiVo> apiVos = new ArrayList<ApiVo>();
//        List<ApiEntity> apis = apiService.getAllApis();
//        for (ApiEntity a : apis) {
//            apiVos.add(ObjectsConverter.convertApiDomainToValue(a));
//        }
//        httpClient = new ApiGuardApacheHttpClient();
//    }

//	 @RequestMapping(method = RequestMethod.GET, produces =
//	 MediaType.APPLICATION_JSON_VALUE)
//	 @ResponseBody
//	 public List<ApiEntity> getApis(HttpServletResponse response) {
//	 List<ApiEntity> apis = cassandraService.getAllApis();
//	 return apis;
//	 }

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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }

        return forward(req, res, api);
    }

    private ResponseEntity forward(HttpServletRequest req, HttpServletResponse res, Api api) throws HttpClientException, IOException {
        String method = req.getMethod();

        //TODO: add patch and option
        //TODO: add option to pass headers or custom headers

        //TODO: add ftp support
        HttpResponse resp = null;
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

        HttpHeaders responseHeaders = new HttpHeaders();
        HttpEntity entity = resp.getEntity();
        String mimeType = "";
        String respStr = "";
        if (entity != null) {
            ContentType contentType = ContentType.getOrDefault(entity);
            mimeType = contentType.getMimeType();
            responseHeaders.setContentType(MediaType.valueOf(mimeType));
            respStr = EntityUtils.toString(entity);
        }

        if (mimeType.contains("pdf")) {
            //TODO: support pdf later
            return new ResponseEntity<String>(respStr, responseHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>(respStr, responseHeaders, HttpStatus.OK);
        }
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<BaseRestResource> addApi(@RequestBody Map<String, Object> jsonPayload,
                                                   HttpServletResponse res) throws IOException {
        try {
            String name = (String) jsonPayload.get("name");
            String reqUri = (String) jsonPayload.get("request_uri");
            String downstreamUri = (String) jsonPayload.get("downstream_uri");

            if (!isValid(name)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Api name is not provided."));
            }
            if (!isValid(reqUri)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Request uri is not provided."));
            }
            if (!isValid(downstreamUri)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Downstream uri is not provided."));
            }

            Api addApi = apiService.addApi(name, reqUri, downstreamUri);
            ApiVo apiVo = ObjectsConverter.convertApiDomainToValue(addApi);
            return new ResponseEntity<BaseRestResource>(apiVo, HttpStatus.CREATED);
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }
    }

    /**
     * Update downstream uri of existing api
     *
     * @param jsonPayload
     * @param res
     * @return
     * @throws IOException
     */
    @RequestMapping(method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<BaseRestResource> updateApi(@RequestBody Map<String, Object> jsonPayload,
                                                      HttpServletResponse res) throws IOException {
        try {
            String name = (String) jsonPayload.get("name");
            String reqUri = (String) jsonPayload.get("request_uri");
            String downstreamUri = (String) jsonPayload.get("downstream_uri");

            if (!isValid(name)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Api name is not provided."));
            }
            if (!isValid(reqUri)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Request uri is not provided."));
            }
            if (!isValid(downstreamUri)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Downstream uri is not provided."));
            }

            Api addApi = apiService.updateApi(name, reqUri, downstreamUri);
            ApiVo apiVo = ObjectsConverter.convertApiDomainToValue(addApi);
            return new ResponseEntity<BaseRestResource>(apiVo, HttpStatus.OK);
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }
    }

    @RequestMapping(value = "/{api}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<BaseRestResource> deleteApi(@PathVariable("api") String name,
                                                      HttpServletResponse res) throws IOException {
        try {
            if (!isValid(name)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Api name is not provided."));
            }

            HttpStatus status = HttpStatus.OK;
            String msg = "API name: " + name + " deleted successfully";
            apiService.deleteApi(name);

            return ResponseEntity.status(status).body((BaseRestResource) new MessageStringVo(msg));
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }
    }

    @RequestMapping(value = "/{api}/auths/{method}", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<BaseRestResource> addAuth(@PathVariable("api") String apiName,
                                                    @PathVariable("method") String method, HttpServletResponse res) throws IOException {
        try {
            AuthType authType = AuthType.getAuthByName(method);
            if (authType == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Auth type is not found."));
            }

            Api api = apiService.getApiByName(apiName);
            if (api == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(apiName + " is not configured."));
            }

            Api addApi = apiService.updateApiAuth(api.getReqUri(), authType, true);
            ApiVo apiVo = ObjectsConverter.convertApiDomainToValue(addApi);

            return new ResponseEntity<BaseRestResource>(apiVo, HttpStatus.OK);
        } catch (ApiException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }
    }
}