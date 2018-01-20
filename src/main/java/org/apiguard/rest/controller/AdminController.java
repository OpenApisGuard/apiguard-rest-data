package org.apiguard.rest.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.constants.AuthType;
import org.apiguard.entity.Api;
import org.apiguard.rest.utils.ObjectsConverter;
import org.apiguard.service.ApiService;
import org.apiguard.service.exceptions.ApiException;
import org.apiguard.valueobject.ApiVo;
import org.apiguard.valueobject.BaseRestResource;
import org.apiguard.valueobject.EexceptionVo;
import org.apiguard.valueobject.MessageStringVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
@RequestMapping(value = "/apiguard/apis")
public class AdminController extends BaseController {
    public static final String ADMIN_URL = "/apiguard/apis";

    private static final Logger log = LogManager.getLogger(AdminController.class);

    @Autowired
    private ApiService<ApiEntity> apiService;

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

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity getApis(HttpServletRequest req, HttpServletResponse res, @RequestParam(value = "p", required = false, defaultValue = "0") int page,
                                     @RequestParam(value = "c", required = false, defaultValue = "25") int count)
            throws Exception {
        try {
            List<ApiEntity> apis = apiService.getAllApis();

            List<ApiVo> apiVos = ObjectsConverter.convertApiListDomainToValue(apis);
            return new ResponseEntity<List>(apiVos, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/health")
    @ResponseBody
    public ResponseEntity<BaseRestResource> healthCheck(HttpServletResponse res) throws IOException {
        try {
            return new ResponseEntity<BaseRestResource>(HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
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
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            log.info(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
        }
    }
}