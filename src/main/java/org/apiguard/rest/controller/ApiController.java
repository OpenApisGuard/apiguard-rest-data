package org.apiguard.rest.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.constants.AuthType;
import org.apiguard.entity.Api;
import org.apiguard.rest.utils.ObjectsConverter;
import org.apiguard.service.ApiService;
import org.apiguard.service.exceptions.ApiException;
import org.apiguard.valueobject.ApiVo;
import org.apiguard.valueobject.BaseRestResource;
import org.springframework.beans.factory.annotation.Autowired;
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
	ApiService<ApiEntity> cassandraService;

	// @RequestMapping(method = RequestMethod.GET, produces =
	// MediaType.APPLICATION_JSON_VALUE)
	// @ResponseBody
	// public List<ApiEntity> getApis(HttpServletResponse response) {
	// List<ApiEntity> apis = cassandraService.getAllApis();
	// return apis;
	// }

	@RequestMapping(method = RequestMethod.GET)
	public String forwardApi(HttpServletRequest req, HttpServletResponse res) throws Exception {
		String reqUri = (String) req.getHeader("request_uri");
		isValid(reqUri, "Request uri is not provided.", res);

		ApiEntity api = cassandraService.getApiByReqUri(reqUri);
		if (api == null || api.getFwdUri().isEmpty()) {
			res.sendError(HttpStatus.BAD_REQUEST.value(), "{\"Error\", \"" + reqUri + " is not configured.\"}");
			return null;
		}
		
		// check whether api needs auth
		if (api.isAuthRequired()) {
			res.sendError(HttpStatus.FORBIDDEN.value(), "{\"Error\", \"Your authentication credentials are invalid.\"}");
			return null;
		}

		return "redirect:" + api.getFwdUri();
	}

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addApi(@RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {
		try {
			String name = (String) jsonPayload.get("name");
			String reqUri = (String) jsonPayload.get("request_uri");
			String fwdUri = (String) jsonPayload.get("forward_uri");

			// TODO: make it return null
			boolean valid = isValid(name, "Api name is not provided.", res);
			isValid(reqUri, "Request uri is not provided.", res);
			isValid(fwdUri, "Forward uri is not provided.", res);

			Api addApi = cassandraService.addApi(name, reqUri, fwdUri);
			ApiVo apiVo = ObjectsConverter.convertApiDomainToValue(addApi);

			return new ResponseEntity<BaseRestResource>(apiVo, HttpStatus.CREATED);
		} 
		catch (ApiException e) {
			res.sendError(HttpStatus.CONFLICT.value(), "{\"Error\", \"" + e.getMessage() + "\"}");
		}
		catch (Exception e) {
			res.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "{\"Error\", \"" + e.getMessage() + "\"}");
		}

		return null;
	}

	@RequestMapping(value = "/{api}/auth/{method}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addAuth(@PathVariable("api") String apiName,
			@PathVariable("method") String method, HttpServletResponse res)
			throws IOException {
		try {
			AuthType authType = AuthType.getAuthByName(method);
			if (authType == null) {
				res.sendError(HttpStatus.BAD_REQUEST.value(), "{\"Error\", \"Auth type is not found.\"}");
				return null;
			}

			Api api = cassandraService.getApiByName(apiName);
			if (api == null) {
				res.sendError(HttpStatus.BAD_REQUEST.value(), "{\"Error\", \"" + apiName + " is not configured.\"}");
				return null;
			}

			Api addApi = cassandraService.updateApiAuth(api.getReqUri(), authType, true);
			ApiVo apiVo = ObjectsConverter.convertApiDomainToValue(addApi);

			return new ResponseEntity<BaseRestResource>(apiVo, HttpStatus.OK);
		} catch (ApiException e) {
			res.sendError(HttpStatus.BAD_REQUEST.value(), "{\"Error\", \"" + e.getMessage() + "\"}");
		} catch (Exception e) {
			res.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "{\"Error\", \"" + e.getMessage() + "\"}");
		}

		return null;
	}
}