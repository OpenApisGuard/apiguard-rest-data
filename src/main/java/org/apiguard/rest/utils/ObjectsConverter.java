package org.apiguard.rest.utils;

import org.apiguard.commons.utils.DateTimeFormater;
import org.apiguard.entity.Api;
import org.apiguard.entity.BasicAuth;
import org.apiguard.entity.Client;
import org.apiguard.entity.KeyAuth;
import org.apiguard.valueobject.ApiVo;
import org.apiguard.valueobject.BasicAuthVo;
import org.apiguard.valueobject.ClientVo;
import org.apiguard.valueobject.KeyAuthVo;

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

	public static ClientVo convertClientDomainToValue(Client domain) {
		if (domain == null) {
			return null;
		}

		return new ClientVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientId());
	}

	public static KeyAuthVo convertKeyAuthDomainToValue(KeyAuth domain) {
		if (domain == null) {
			return null;
		}

		return new KeyAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientId(), domain.getKey(),
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

}
