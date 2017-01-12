package org.apiguard.rest.utils;

import org.apiguard.entity.Api;
import org.apiguard.valueobject.ApiVo;

public class ObjectsConverter {

	public static ApiVo convertApiDomainToValue(Api apiDomain) {
		if (apiDomain == null) {
			return null;
		}

		return new ApiVo(apiDomain.getId(), apiDomain.getCreationDate(), apiDomain.getName(), apiDomain.getReqUri(),
				apiDomain.getFwdUri(), apiDomain.isAuthRequired(), apiDomain.isBasicAuth(), apiDomain.isHmacAuth(),
				apiDomain.isJwtAuth(), apiDomain.isKeyAuth(), apiDomain.isLdapAuth(), apiDomain.isOAuth2Auth());
	}

}
