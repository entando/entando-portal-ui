/*
 * Copyright 2022-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.system.services.controller.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.entando.entando.aps.system.services.controller.AbstractTestExecutorService;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
class TestPageExecutorService extends AbstractTestExecutorService {

	@Test
    void testExecution() throws Exception {
		super.setUserOnSession("admin");
        ExecutorServiceInterface wes = (ExecutorServiceInterface) super.getApplicationContext().getBean(PageExecutorService.class);
		IPageManager pageManager = super.getApplicationContext().getBean(SystemConstants.PAGE_MANAGER, IPageManager.class);
        IPage currentPage = pageManager.getOnlinePage("homepage");
		super.getRequestContext().addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, currentPage);
		wes.service(super.getRequestContext());
        assertEquals(HttpStatus.OK.value(), super.getRequestContext().getResponse().getStatus());
	}

	@Test
    void testNotFoundExecution() throws Exception {
		super.setUserOnSession("admin");
        ExecutorServiceInterface wes = (ExecutorServiceInterface) super.getApplicationContext().getBean(PageExecutorService.class);
		IPageManager pageManager = super.getApplicationContext().getBean(SystemConstants.PAGE_MANAGER, IPageManager.class);
        String notFoundPageCode = pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        IPage notFoundPage = pageManager.getOnlinePage(notFoundPageCode);
		super.getRequestContext().addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, notFoundPage);
		wes.service(super.getRequestContext());
        assertEquals(HttpStatus.NOT_FOUND.value(), super.getRequestContext().getResponse().getStatus());
	}
    
}
