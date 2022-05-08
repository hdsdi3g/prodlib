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
package tv.hd3g.authkit.mod.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import tv.hd3g.authkit.mod.entity.Userprivacy;

@Repository
public interface UserPrivacyRepository extends JpaRepository<Userprivacy, Long> {

	@Query("SELECT up FROM Userprivacy up WHERE up.userUUID IN (?1)")
	List<Userprivacy> getByUserUUID(final Collection<String> uuid);

	@Query("SELECT up FROM Userprivacy up WHERE up.userUUID = ?1")
	Userprivacy getByUserUUID(final String uuid);

}
