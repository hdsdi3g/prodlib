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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import tv.hd3g.commons.authkit.CheckBefore;

/**
 * A class comment!
 */
@RestController
@RequestMapping(value = "/serverPath", produces = APPLICATION_JSON_VALUE)
public class DemoRestController {

    @PostMapping(name = "Post Demo", value = "postActionControllerPath", consumes = "text/xml")
    public ResponseEntity<OutputDto> postActionController(@RequestBody @Validated final InputDto chPasswordDto,
            final HttpServletRequest request) {
        return null;
    }

    /**
     * A comment for get demo
     */
    @GetMapping(name = "Get Demo", value = "getActionController/{textValueVarName}/path")
    public ResponseEntity<OutputDto> getActionController(
            @PathVariable("textValueVarName") @NotEmpty final String textValue,
            @RequestParam(defaultValue = "0") final int numValue,
            final HttpServletRequest request) {
        return null;
    }

    @PutMapping(name = "Put Demo", value = "putActionControllerPath")
    public ResponseEntity<BaseRepresentationModel> putActionController() {
        return null;
    }

    @DeleteMapping(name = "Delete Demo", value = "deleteActionControllerPath")
    @CheckBefore("rightForDelete")
    @CheckBefore("alternateRightForDelete")
    public ResponseEntity<BaseRepresentationModel> deleteActionController() {
        return null;
    }

    @PatchMapping(name = "Patch Demo", value = "patchActionControllerPath")
    @CheckBefore({ "rightForPatch", "anotherRightForPatch" })
    public ResponseEntity<BaseRepresentationModel> patchActionController() {
        return null;
    }

}
