/*
 * This file is part of supervisablekit.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2022
 *
 */
package tv.hd3g.jobkit.engine;

public enum SupervisableResultState {
	/**
	 * Works is done
	 */
	WORKS_DONE,
	/**
	 * Works is not done, but this is not an app problem
	 */
	WORKS_CANCELED,
	/**
	 * No works to not done, this is not a problem
	 */
	NOTHING_TO_DO;

}
