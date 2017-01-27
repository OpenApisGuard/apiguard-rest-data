package org.apiguard.rest.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.cassandra.entity.ClientEntity;
import org.apiguard.service.ApiService;
import org.apiguard.service.ClientService;
import org.apiguard.valueobject.BaseRestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<BaseRestResource> getClient(HttpServletRequest req, HttpServletResponse res) throws Exception {
		return null;
	}

	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<BaseRestResource> addApi(@RequestBody Map<String, Object> jsonPayload,
			HttpServletResponse res) throws IOException {

		return null;
	}
}