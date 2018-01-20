package org.apiguard.rest.controller;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

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

public class BaseController {

	public boolean isValid(String val) throws IOException {
		return !StringUtils.isEmpty(val);
	}

	public HashMap<String, String> getHeaders(HttpServletRequest req) {
		HashMap<String, String> res = null;
		Enumeration<String> headerNames = req.getHeaderNames();
		if (headerNames.hasMoreElements()) {
			res = new HashMap<String, String>();
			while (headerNames.hasMoreElements()) {
				String h = headerNames.nextElement();
                if (!h.equalsIgnoreCase("host")) {
                    res.put(h, req.getHeader(h));
                }
			}
		}
		return res;
	}

	public String getContent(HttpServletRequest req) throws IOException {
		StringBuffer sb = new StringBuffer();
		String line = null;

		BufferedReader reader = req.getReader();
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		return sb.toString();
	}
}
