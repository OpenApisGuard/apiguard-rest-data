package org.apiguard.rest.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apiguard.cassandra.entity.*;
import org.apiguard.rest.utils.ObjectsConverter;
import org.apiguard.service.ApiAuthService;
import org.apiguard.service.ApiService;
import org.apiguard.service.ClientService;
import org.apiguard.service.exceptions.ApiAuthException;
import org.apiguard.service.exceptions.ClientException;
import org.apiguard.valueobject.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
@RequestMapping(value = "/apiguard/clients")
public class ClientController extends BaseController {
	public static final String ADMIN_URL = "/apiguard/clients";

	private static final String PARAM_REQUEST_URI = "request_uri";
	private static final String PARAM_ID = "id";
	private static final String PARAM_EMAIL = "email";
	private static final String PARAM_FIRST_NAME = "first_name";
	private static final String PARAM_LAST_NAME = "last_name";
	private static final String PARAM_KEY = "key";
	private static final String PARAM_PASSWORD = "password";
	private static final String PARAM_CLIENT_ALIAS = "client_alias";
	private static final String PARAM_SECRET = "secret";
	private static final String PARAM_PROXY_NAME = "proxy_name";
	private static final String PARAM_LDAP_URL = "ldap_url";
	private static final String PARAM_ADMIN_DN = "admin_dn";
	private static final String PARAM_ADMIN_PASSWORD = "admin_password";
	private static final String PARAM_USER_BASE = "user_base";
	private static final String PARAM_USER_ATTR = "user_attribute";
	private static final String PARAM_CACHE_EXPIRE_SEC = "cache_expire_seconds";
	private static final String PARAM_VALID_NOT_BEFORE = "not_before";
	private static final String PARAM_EXPIRES = "expires";

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	private static final Logger log = LogManager.getLogger(ClientController.class);

	@Autowired
	ApiService<ApiEntity> apiService;

	@Autowired
	@Qualifier("cassandraClientService")
	ClientService<ClientEntity> clientService;

	@Autowired
	ApiAuthService apiAuthService;

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity getClients(HttpServletRequest req, HttpServletResponse res, @RequestParam(value = "p", required = false, defaultValue = "0") int page,
													   @RequestParam(value = "c", required = false, defaultValue = "25") int count)
			throws Exception {
		try {
			List<ClientEntity> clients = clientService.getClients(page, count);

			List<ClientVo> clientVos = ObjectsConverter.convertClientListDomainToValue(clients);
			return new ResponseEntity<List>(clientVos, HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> getClientById(@PathVariable("clientId") String clientId, HttpServletRequest req, HttpServletResponse res)
			throws Exception {
		try {
			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("clientId is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("clientId is not found."));
			}
			ClientVo clientVo = ObjectsConverter.convertClientDomainToValue(client);
			return new ResponseEntity<BaseRestResource>(clientVo, HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addClient(@RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {
		
		try {
			String id = (String) jsonPayload.get(PARAM_ID);
			String firstName = (String) jsonPayload.get(PARAM_FIRST_NAME);
			String lastName = (String) jsonPayload.get(PARAM_LAST_NAME);
			String email = (String) jsonPayload.get(PARAM_EMAIL);

			if (! StringUtils.isEmpty(email) && clientService.existsEmail(email)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Email: " + email + " is already in use by another account."));
			}

			if (!isValid(id)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}
			
			ClientEntity client = clientService.addClient(id, email, firstName, lastName);
			ClientVo clientVo = ObjectsConverter.convertClientDomainToValue(client);
			return new ResponseEntity<BaseRestResource>(clientVo, HttpStatus.CREATED);
		} catch (ClientException e) {
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}/key-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addKeyAuth(@PathVariable("clientId") String clientId, @RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {

		try {
			String key = (String) jsonPayload.get(PARAM_KEY);
			String reqUri = (String) jsonPayload.get(PARAM_REQUEST_URI);

			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}

			// auto generated key
			if (!isValid(key)) {
				key = UUID.randomUUID().toString();
			}

			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Request URI is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(clientId + " is not configured."));
			}
			
			KeyAuthEntity keyAuth = apiAuthService.addKeyAuth(reqUri, clientId, key);

			KeyAuthVo keyAuthVo = ObjectsConverter.convertKeyAuthDomainToValue(keyAuth);
			return new ResponseEntity<BaseRestResource>(keyAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException e) {
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}/basic-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addBasicAuth(@PathVariable("clientId") String clientId, @RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {
		
		try {
			String pwd = (String) jsonPayload.get(PARAM_PASSWORD);
			String reqUri = (String) jsonPayload.get(PARAM_REQUEST_URI);
			
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
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(clientId + " is not configured."));
			}
			
			BasicAuthEntity basicAuth = apiAuthService.addBasicAuth(reqUri, clientId, pwd);
			
			BasicAuthVo basicAuthVo = ObjectsConverter.convertBasicAuthDomainToValue(basicAuth);
			return new ResponseEntity<BaseRestResource>(basicAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException e) {
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{client}/signature-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addSignatureAuth(@PathVariable("client") String clientId, @RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {

		try {
			String clientAlias = (String) jsonPayload.get(PARAM_CLIENT_ALIAS);
			String secret = (String) jsonPayload.get(PARAM_SECRET);
			String reqUri = (String) jsonPayload.get(PARAM_REQUEST_URI);
			String proxyName = (String) jsonPayload.get(PARAM_PROXY_NAME);

			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("client id is not provided."));
			}
			
			if (!isValid(clientAlias)) {
				clientAlias = (isValid(proxyName)? proxyName : clientId) + "_" + SDF.format(new Timestamp(System.currentTimeMillis()));
			}

			if (!isValid(secret)) {
				secret = UUID.randomUUID().toString();
			}

			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Request URI is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(clientId + " is not configured."));
			}

            SignatureAuthEntity signatureAuth = apiAuthService.addHttpSignatureAuth(reqUri, clientId, clientAlias, secret);

            SignatureAuthVo signatureAuthVo = ObjectsConverter.convertSignatureAuthDomainToValue(signatureAuth);
            return new ResponseEntity<BaseRestResource>(signatureAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException e) {
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}/ldap-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addLdapAuth(@PathVariable("clientId") String clientId, @RequestBody Map<String, Object> jsonPayload,
														 HttpServletResponse res) throws IOException {
		try {
			String reqUri = (String) jsonPayload.get(PARAM_REQUEST_URI);
			String ldapUrl = (String) jsonPayload.get(PARAM_LDAP_URL);
			String adminDn = (String) jsonPayload.get(PARAM_ADMIN_DN);
			String adminPwd = (String) jsonPayload.get(PARAM_ADMIN_PASSWORD);
			String userBase = (String) jsonPayload.get(PARAM_USER_BASE);
			String userAttr = (String) jsonPayload.get(PARAM_USER_ATTR);
			Integer cacheExpireInSecond = (Integer) jsonPayload.get(PARAM_CACHE_EXPIRE_SEC);

			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}

			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Request URI is not provided."));
			}

			if (!isValid(ldapUrl)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("LDAP url is not provided."));
			}

			if (!isValid(adminDn)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Admin DN is not provided."));
			}

			if (!isValid(adminPwd)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Admin password is not provided."));
			}

			if (!isValid(userBase)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("User base is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(clientId + " is not configured."));
			}

			LdapAuthEntity ldapAuthEntity = apiAuthService.addLdapAuth(reqUri, clientId, ldapUrl, adminDn, adminPwd, userBase, userAttr, cacheExpireInSecond);

			LdapAuthVo ldapAuthVo = ObjectsConverter.convertLdapAuthDomainToValue(ldapAuthEntity);
			return new ResponseEntity<BaseRestResource>(ldapAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException e) {
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

	@RequestMapping(value = "/{clientId}/jwt-auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addJwtAuth(@PathVariable("clientId") String clientId, @RequestBody Map<String, Object> jsonPayload,
														HttpServletResponse res) throws IOException {
		try {
			String reqUri = (String) jsonPayload.get(PARAM_REQUEST_URI);
			Boolean notBefore = new Boolean((String) jsonPayload.get(PARAM_VALID_NOT_BEFORE));
			Boolean expires = new Boolean((String) jsonPayload.get(PARAM_EXPIRES));

			if (!isValid(clientId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("id is not provided."));
			}

			if (!isValid(reqUri)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body((BaseRestResource) new EexceptionVo("Request URI is not provided."));
			}

			ClientEntity client = clientService.getClient(clientId);
			if (client == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(clientId + " is not configured."));
			}

			JwtAuthEntity jwtAuthEntity = apiAuthService.addJwtAuth(reqUri, clientId, notBefore, expires);

			JwtAuthVo jwtAuthVo = ObjectsConverter.convertJwtAuthDomainToValue(jwtAuthEntity);
			return new ResponseEntity<BaseRestResource>(jwtAuthVo, HttpStatus.CREATED);
		}
		catch (ApiAuthException e) {
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body((BaseRestResource) new EexceptionVo(e.getMessage()));
		}
	}

}