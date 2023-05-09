/*
 * This file is part of AuthKit.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.authkit.mod.dto.ressource;

import java.util.Date;
import java.util.function.Function;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import tv.hd3g.authkit.mod.dto.ExternalAuthUserDto;
import tv.hd3g.authkit.mod.entity.Userprivacy;

/**
 * In and Out DTO
 */
@EqualsAndHashCode(callSuper = false)
@Getter
public class UserPrivacyDto extends RepresentationModel<UserPrivacyDto> {

	/**
	 * Ignored during creation/update
	 */
	@Nullable
	private Date created;
	/**
	 * Ignored during creation/update
	 */
	@Nullable
	private String userUUID;

	@Nullable
	@Setter
	private String name;

	@Nullable
	@Setter
	private String address;

	@Nullable
	@Size(max = 16)
	@Setter
	private String postalcode;

	@Nullable
	@Size(max = 3)
	@Setter
	private String country;

	@Nullable
	@Size(max = 3)
	@Setter
	private String lang;

	@Nullable
	@Setter
	private String email;

	@Nullable
	@Size(max = 128)
	@Setter
	private String company;

	@Nullable
	@Setter
	private String phone;

	public UserPrivacyDto() {
	}

	public UserPrivacyDto(final Userprivacy user, final Function<byte[], String> unCipher) {
		created = user.getCreated();
		userUUID = user.getUserUUID();
		name = unCipher.apply(user.getName());
		address = unCipher.apply(user.getAddress());
		postalcode = user.getPostalcode();
		country = user.getCountry();
		lang = user.getLang();
		email = unCipher.apply(user.getEmail());
		company = user.getCompany();
		phone = unCipher.apply(user.getPhone());
	}

	public UserPrivacyDto(final ExternalAuthUserDto importFromLDAP) {
		if (importFromLDAP.getUserLongName() != null) {
			name = importFromLDAP.getUserLongName();
		}
		if (importFromLDAP.getUserEmail() != null) {
			email = importFromLDAP.getUserEmail();
		}
		/**
		 * ... more from LDAP
		 */
	}

	public void mergue(final Userprivacy user, final Function<String, byte[]> cipher) {
		if (name != null) {
			user.setName(cipher.apply(name));
		}
		if (address != null) {
			user.setAddress(cipher.apply(address));
		}
		if (postalcode != null) {
			user.setPostalcode(postalcode);
		}
		if (country != null) {
			user.setCountry(country);
		}
		if (lang != null) {
			user.setLang(lang);
		}
		if (email != null) {
			user.setEmail(cipher.apply(email));
		}
		if (company != null) {
			user.setCompany(company);
		}
		if (phone != null) {
			user.setPhone(cipher.apply(phone));
		}
	}

}
