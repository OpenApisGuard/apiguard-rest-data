package org.apiguard.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.springframework.util.StringUtils;

public class BaseController {

	public boolean isValid(String val) throws IOException {
		return !StringUtils.isEmpty(val);
	}
	
	public String getResponse(HttpServletResponse res, Response resp) throws IOException {
		res.setContentType(resp.getMediaType().toString());
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = InputStream.class.cast(resp.getEntity()).read(buffer)) != -1) {
			    result.write(buffer, 0, length);
			}
			
			return result.toString("UTF-8");
		}
		finally {
			result.close();
		}
	}

}
