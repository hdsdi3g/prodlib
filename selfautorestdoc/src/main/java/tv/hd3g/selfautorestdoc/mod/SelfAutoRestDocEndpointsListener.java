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

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import tv.hd3g.selfautorestdoc.SelfAutoRESTDoc;

@Component
public class SelfAutoRestDocEndpointsListener implements ApplicationListener<ApplicationEvent> {

	private final SelfAutoRESTDoc selfAutoRESTDoc;

	public SelfAutoRestDocEndpointsListener() {
		selfAutoRESTDoc = new SelfAutoRESTDoc();
	}

	@Override
	public void onApplicationEvent(final ApplicationEvent aEvent) {
		if (aEvent instanceof final ContextRefreshedEvent event) {
			event.getApplicationContext()
					.getBean(RequestMappingHandlerMapping.class)
					.getHandlerMethods().values().stream()
					.map(HandlerMethod::getBeanType)
					.forEach(selfAutoRESTDoc::registerClass);
		}
	}

	public SelfAutoRESTDoc getSelfAutoRESTDoc() {
		return selfAutoRESTDoc;
	}

}