/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.mock.web.MockHttpServletRequest;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.controller.ControllerManager;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author M.Diana - W.Ambu
 */
class TestControllerManager extends BaseTestCase {
    
    @Test
    void testService_1() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ControllerManager controller = (ControllerManager) this.getService(SystemConstants.CONTROLLER_MANAGER);
        MockHttpServletRequest request = (MockHttpServletRequest) reqCtx.getRequest();
        request.setServletPath("/it/homepage.page");
        int status = controller.service(reqCtx);
        assertEquals(ControllerManager.OUTPUT, status);

        request.setParameter("username", "admin");
        request.setParameter("password", "admin");
        status = controller.service(reqCtx);
        assertEquals(ControllerManager.OUTPUT, status);
    }
    
    @Test
    void testService_2() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ControllerManager controller = (ControllerManager) this.getService(SystemConstants.CONTROLLER_MANAGER);
        MockHttpServletRequest request = (MockHttpServletRequest) reqCtx.getRequest();
        request.setServletPath("/it/customers_page.page");
        int status = controller.service(reqCtx);
        assertEquals(ControllerManager.REDIRECT, status);

        request.setParameter("username", "admin");
        request.setParameter("password", "admin");
        request.setServletPath("/it/customers_page.page");
        status = controller.service(reqCtx);
        assertEquals(ControllerManager.OUTPUT, status);
    }
    
    @Test
    void testService_3() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ControllerManager controller = (ControllerManager) this.getService(SystemConstants.CONTROLLER_MANAGER);
        MockHttpServletRequest request = (MockHttpServletRequest) reqCtx.getRequest();
        request.setServletPath("/it/administrators_page.page");
        request.setRequestURI("/Entando/it/customers_page.page");
        int status = controller.service(reqCtx);
        assertEquals(ControllerManager.REDIRECT, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertTrue(redirectUrl.contains("/Entando/it/login.page?"));
        assertTrue(redirectUrl.contains("redirectflag=1"));
        assertTrue(redirectUrl.contains("returnUrl="));
        assertTrue(redirectUrl.contains("customers_page.page"));

        request.setParameter(RequestContext.PAR_REDIRECT_FLAG, "1");
        status = controller.service(reqCtx);
        assertEquals(ControllerManager.REDIRECT, status);
        redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertEquals("http://www.entando.com/Entando/it/errorpage.page", redirectUrl);
    }
    
    @BeforeEach
	private void init() throws Exception {
		try {
            RequestContext reqCtx = this.getRequestContext();
            ((MockHttpServletRequest) reqCtx.getRequest()).removeAllParameters();
            reqCtx.getRequest().getSession().removeAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);
		} catch (Throwable e) {
			throw new Exception(e);
		}
	}
    
}
