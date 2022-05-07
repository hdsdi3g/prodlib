/*
 * This file is part of SelfAutoRESTDoc.
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
package tv.hd3g.selfautorestdoc.demo;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class InputDto {

	@NotEmpty
	private String text;
	@NotNull
	private Map<String, String> subMap;
	@NotNull
	private List<String> subList;
	private SubInputDto subInputDto;

	public static class SubInputDto {
		private final String subTextValue;

		public SubInputDto(final String subTextValue) {
			this.subTextValue = subTextValue;
		}

		public String getSubTextValue() {
			return subTextValue;
		}
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public Map<String, String> getSubMap() {
		return subMap;
	}

	public void setSubMap(final Map<String, String> subMap) {
		this.subMap = subMap;
	}

	public List<String> getSubList() {
		return subList;
	}

	public void setSubList(final List<String> subList) {
		this.subList = subList;
	}

	public SubInputDto getSubInputDto() {
		return subInputDto;
	}

	public void setSubInputDto(final SubInputDto subInputDto) {
		this.subInputDto = subInputDto;
	}

}
