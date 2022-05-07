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
package tv.hd3g.selfautorestdoc.mod;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Usage:
 * $ java -jar myspringapp.jar export-rest-doc-api
 */
@Component
public class ExportRESTDocAPICmdLine implements ApplicationRunner {

	@Autowired
	private SelfAutoRestDocEndpointsListener selfAutoRestDocEndpointsListener;
	@Value("${selfautorestdoc.outputfile:API.md}")
	private File outputFile;
	@Value("${selfautorestdoc.baseProjectURL:/blob/master}")
	private String baseProjectURL;

	@Override
	public void run(final ApplicationArguments args) throws Exception {
		if (args.getNonOptionArgs().contains("export-rest-doc-api") == false) {
			return;
		}
		selfAutoRestDocEndpointsListener.getSelfAutoRESTDoc().writeToMD(outputFile, baseProjectURL);
		if (args.getNonOptionArgs().contains("dont-quit-after-done") == false) {
			System.exit(0);
		}
	}

}
