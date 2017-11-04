package org.apiguard.rest.utils;

import org.apiguard.cassandra.entity.ApiEntity;
import org.apiguard.cassandra.entity.ClientEntity;
import org.apiguard.commons.utils.DateTimeFormater;
import org.apiguard.entity.*;
import org.apiguard.valueobject.*;

import java.util.ArrayList;
import java.util.List;

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

public class ObjectsConverter {

	public static ApiVo convertApiDomainToValue(Api domain) {
		if (domain == null) {
			return null;
		}

		return new ApiVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getName(), domain.getReqUri(),
				domain.getDownstreamUri(), domain.isAuthRequired(), domain.isBasicAuth(), domain.isKeyAuth(),
				domain.isHmacAuth(), domain.isOAuth2Auth(), domain.isJwtAuth(), domain.isLdapAuth());
	}

	public static List<ApiVo> convertApiListDomainToValue(List<ApiEntity> domains) {
		if (domains == null) {
			return null;
		}

		List<ApiVo> res = new ArrayList<ApiVo>(domains.size());
		for(ApiEntity c : domains) {
			res.add(convertApiDomainToValue(c));
		}
		return res;
	}

	public static ClientVo convertClientDomainToValue(Client domain) {
		if (domain == null) {
			return null;
		}

		return new ClientVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientId(), domain.getEmail(),
				domain.getFirstName(), domain.getLastName());
	}

	public static List<ClientVo> convertClientListDomainToValue(List<ClientEntity> domains) {
		if (domains == null) {
			return null;
		}

		List<ClientVo> res = new ArrayList<ClientVo>(domains.size());
		for(ClientEntity c : domains) {
			res.add(convertClientDomainToValue(c));
		}
		return res;
	}

	public static KeyAuthVo convertKeyAuthDomainToValue(KeyAuth domain) {
		if (domain == null) {
			return null;
		}

		return new KeyAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientId(), domain.getDecryptedKey(),
				domain.getReqUri());
	}

	public static BasicAuthVo convertBasicAuthDomainToValue(BasicAuth domain) {
		if (domain == null) {
			return null;
		}

		return new BasicAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientId(), "******",
				domain.getReqUri());
	}

	public static SignatureAuthVo convertSignatureAuthDomainToValue(SignatureAuth domain) {
		if (domain == null) {
			return null;
		}

		return new SignatureAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientAlias(), domain.getClientId(), domain.getDecryptedSecret(),
				domain.getReqUri());
	}

	public static LdapAuthVo convertLdapAuthDomainToValue(LdapAuth domain) {
		if (domain == null) {
			return null;
		}

		return new LdapAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()), DateTimeFormater.toString(domain.getLastUpdateDate()),
				domain.getClientId(), domain.getReqUri(), domain.getLdapUrl(), domain.getAdminDn(), domain.getAdminPassword(), domain.getUserBase(),
				domain.getUserAttr(), domain.getCacheExpireInSecond());
	}

	public static JwtAuthVo convertJwtAuthDomainToValue(JwtAuth domain) {
		if (domain == null) {
			return null;
		}

		return new JwtAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()), DateTimeFormater.toString(domain.getLastUpdateDate()),
				domain.getClientId(), domain.getReqUri(), domain.getIssuer(), domain.getSecret(), domain.isNotBefore(), domain.isExpires());
	}
}
