package org.apiguard.rest.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public class BaseController {

	public boolean isValid(String val, String errorMessage, HttpServletResponse response) throws IOException {
		boolean isEmpty = StringUtils.isEmpty(val);
		if (isEmpty) {
			response.sendError(HttpStatus.BAD_REQUEST.value(), "{\"Error\", \"" + errorMessage + "\"}");
			return false;
		}
		return true;
	}

}
