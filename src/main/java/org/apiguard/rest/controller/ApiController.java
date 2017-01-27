package org.apiguard.rest.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.commons.http.ApiGuardHttpClient;
import org.apiguard.commons.http.HttpClientException;
import org.apiguard.constants.AuthType;
import org.apiguard.entity.Api;
import org.apiguard.rest.utils.ObjectsConverter;
import org.apiguard.service.ApiService;
import org.apiguard.service.exceptions.ApiException;
import org.apiguard.valueobject.ApiVo;
import org.apiguard.valueobject.BaseRestResource;
import org.apiguard.valueobject.EexceptionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/apis")
public class ApiController extends BaseController {
	@Autowired
	private ApiService<ApiEntity> apiService;
	
	private ApiGuardHttpClient httpClient;
	
	@PostConstruct
	public void createWebClient() {
		List<ApiVo> apiVos = new ArrayList<ApiVo>();
		List<ApiEntity> apis = apiService.getAllApis();
		for(ApiEntity a: apis) {
			apiVos.add(ObjectsConverter.convertApiDomainToValue(a));
		}
		httpClient = new ApiGuardHttpClient(apiVos);
	}

	// @RequestMapping(method = RequestMethod.GET, produces =
	// MediaType.APPLICATION_JSON_VALUE)
	// @ResponseBody
	// public List<ApiEntity> getApis(HttpServletResponse response) {
	// List<ApiEntity> apis = cassandraService.getAllApis();
	// return apis;
	// }

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity forwardApi(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String reqUri = (String) req.getHeader("request_uri");
		if (!isValid(reqUri)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request uri is not provided.");
		}

		Api api = apiService.getApiByReqUri(reqUri);
		if (api == null || api.getFwdUri().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reqUri + " is not configured yet.");
		}

		// check whether api needs auth
		if (api.isAuthRequired()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your authentication credentials are invalid.");
		}

		return forward(res, api);
	}

	private ResponseEntity forward(HttpServletResponse res, Api api) throws HttpClientException, IOException {
		Response resp = httpClient.callService(api.getFwdUri());
		
		HttpHeaders responseHeaders = new HttpHeaders();
		String mediaTypeStr = resp.getMediaType().toString();
		responseHeaders.setContentType(MediaType.valueOf(mediaTypeStr));
	    
		if(mediaTypeStr.contains("pdf")) {
			//TODO: support pdf later
			return new ResponseEntity<String>(getResponse(res, resp), responseHeaders, HttpStatus.OK);
		}
		else {
			return new ResponseEntity<String>(getResponse(res, resp), responseHeaders, HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/{api}", method = RequestMethod.GET)
	public ResponseEntity forwardApi(@PathVariable("api") String apiName, HttpServletRequest req,
			HttpServletResponse res) throws Exception {
		if (!isValid(apiName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Request uri is not provided."));
		}

		Api api = apiService.getApiByName(apiName);
		if (api == null || api.getFwdUri().isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Api name: " + apiName + " is not configured."));
		}

		// check whether api needs auth
		if (api.isAuthRequired()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body((BaseRestResource) new EexceptionVo("Your authentication credentials are invalid."));
		}

		return forward(res, api);
	}

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addApi(@RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {
		try {
			String name = (String) jsonPayload.get("name");
			String reqUri = (String) jsonPayload.get("request_uri");
			String fwdUri = (String) jsonPayload.get("forward_uri");

			if (!isValid(name)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Api name is not provided."));
			}
			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Request uri is not provided."));
			}
			if (!isValid(fwdUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo("Forward uri is not provided."));
			}

			Api addApi = apiService.addApi(name, reqUri, fwdUri);
			httpClient.addWebClient(fwdUri);
			
			ApiVo apiVo = ObjectsConverter.convertApiDomainToValue(addApi);
			return new ResponseEntity<BaseRestResource>(apiVo, HttpStatus.CREATED);
		} catch (ApiException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{api}/auth/{method}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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