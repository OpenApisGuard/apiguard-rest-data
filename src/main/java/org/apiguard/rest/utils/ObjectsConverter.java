package org.apiguard.rest.utils;

import org.apiguard.commons.utils.DateTimeFormater;
import org.apiguard.entity.*;
import org.apiguard.valueobject.*;

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

	public static SignatureAuthVo convertSignatureAuthDomainToValue(SignatureAuth domain) {
		if (domain == null) {
			return null;
		}

		return new SignatureAuthVo(domain.getId(), DateTimeFormater.toString(domain.getCreationDate()),
				DateTimeFormater.toString(domain.getLastUpdateDate()), domain.getClientAlias(), domain.getClientId(), domain.getSecret(),
				domain.getReqUri());
	}

}
