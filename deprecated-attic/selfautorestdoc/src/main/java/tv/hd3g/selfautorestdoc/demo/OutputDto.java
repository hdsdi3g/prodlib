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
import java.util.Objects;

import org.springframework.lang.Nullable;

public class OutputDto extends BaseRepresentationModel {

    private String textValue;
    private Map<String, String> subMap;
    private List<String> subList;
    private SubOutputDto subInputDto;

    public static class SubOutputDto {
        private String subTextValue;

        public String getSubTextValue() {
            return subTextValue;
        }

        public void setSubTextValue(final String subTextValue) {
            this.subTextValue = subTextValue;
        }
    }

    public String getTextValue() {
        return textValue;
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

    public SubOutputDto getSubInputDto() {
        return subInputDto;
    }

    public void setSubInputDto(final SubOutputDto subInputDto) {
        this.subInputDto = subInputDto;
    }

    public void setTextValue(final String textValue) {
        this.textValue = textValue;
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = super.hashCode();
        result = prime * result + Objects.hash(subInputDto, subList, subMap, textValue);
        return result;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final var other = (OutputDto) obj;
        return Objects.equals(subInputDto, other.subInputDto) && Objects.equals(subList, other.subList) && Objects
                .equals(subMap, other.subMap) && Objects.equals(textValue, other.textValue);
    }

}
