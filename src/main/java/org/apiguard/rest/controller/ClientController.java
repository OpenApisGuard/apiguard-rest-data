package org.apiguard.rest.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.cassandra.entity.BasicAuthEntity;
import org.apiguard.cassandra.entity.ClientEntity;
import org.apiguard.cassandra.entity.KeyAuthEntity;
import org.apiguard.rest.utils.ObjectsConverter;
import org.apiguard.service.ApiAuthService;
import org.apiguard.service.ApiService;
import org.apiguard.service.ClientService;
import org.apiguard.service.exceptions.ApiAuthException;
import org.apiguard.service.exceptions.ClientException;
import org.apiguard.valueobject.BaseRestResource;
import org.apiguard.valueobject.BasicAuthVo;
import org.apiguard.valueobject.ClientVo;
import org.apiguard.valueobject.EexceptionVo;
import org.apiguard.valueobject.KeyAuthVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@RequestMapping(value = "/clients")
public class ClientController extends BaseController {

	@Autowired
	ApiService<ApiEntity> apiService;

	@Autowired
	@Qualifier("cassandraClientService")
	ClientService<ClientEntity> clientService;

	@Autowired
	ApiAuthService apiAuthService;

	@RequestMapping(value = "/{clientId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> getClient(@PathVariable("clientId") String clientId, HttpServletRequest req, HttpServletResponse res)
			throws Exception {
		try {
			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("clientId is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			ClientVo clientVo = null;
			if (client != null) {
				clientVo = ObjectsConverter.convertClientDomainToValue(client);
			}
			return new ResponseEntity<BaseRestResource>(clientVo, HttpStatus.OK);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addClient(@RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {
		
		try {
			String id = (String) jsonPayload.get("id");
			
			if (!isValid(id)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}
			
			ClientEntity client = clientService.addClient(id);
			ClientVo clientVo = ObjectsConverter.convertClientDomainToValue(client);
			return new ResponseEntity<BaseRestResource>(clientVo, HttpStatus.CREATED);
		} catch (ClientException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}/key-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addKeyAuth(@PathVariable("clientId") String clientId, @RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {

		try {
			String key = (String) jsonPayload.get("key");
			String reqUri = (String) jsonPayload.get("request_uri");

			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}

			if (!isValid(key)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("key is not provided."));
			}

			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Request URI is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(client + " is not configured."));
			}
			
			KeyAuthEntity keyAuth = apiAuthService.addKeyAuth(reqUri, clientId, key);
			
			KeyAuthVo keyAuthVo = ObjectsConverter.convertKeyAuthDomainToValue(keyAuth);
			return new ResponseEntity<BaseRestResource>(keyAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException ae) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(ae.getMessage()));
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}/basic-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addBasicAuth(@PathVariable("clientId") String clientId, @RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {
		
		try {
			String pwd = (String) jsonPayload.get("password");
			String reqUri = (String) jsonPayload.get("request_uri");
			
			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}
			
			if (!isValid(pwd)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("password is not provided."));
			}
			
			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Request URI is not provided."));
			}
			
			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(client + " is not configured."));
			}
			
			BasicAuthEntity basicAuth = apiAuthService.addBasicAuth(reqUri, clientId, pwd);
			
			BasicAuthVo basicAuthVo = ObjectsConverter.convertBasicAuthDomainToValue(basicAuth);
			return new ResponseEntity<BaseRestResource>(basicAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException ae) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(ae.getMessage()));
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}
}