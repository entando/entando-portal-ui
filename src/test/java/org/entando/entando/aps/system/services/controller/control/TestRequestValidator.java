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
package org.entando.entando.aps.system.services.controller.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.mock.web.MockHttpServletRequest;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.IParameterizableManager;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.system.services.controller.control.ControlServiceInterface;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import java.util.HashMap;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author M.Casari
 */
class TestRequestValidator extends BaseTestCase {

    @Test
    void testService() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/it/homepage.wp");
        int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.CONTINUE, status);
        Lang lang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        assertNotNull(page);
        assertNotNull(lang);
        assertEquals("it", lang.getCode());
        assertEquals("homepage", page.getCode());
    }

    @Test
    void testServiceFailure_1() throws EntException {
        String notFoundPageCode = this.pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        Map<String, String> paramsToUpgrade = new HashMap<>();
        try {
            RequestContext reqCtx = this.getRequestContext();
            ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/it/notexists.wp");//Page does not exist
            int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
            assertEquals(ControllerManager.CONTINUE, status);
            IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            assertEquals(notFoundPageCode, page.getCode());
            
            paramsToUpgrade.put(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, "invalid");
            ((IParameterizableManager)this.pageManager).updateParams(paramsToUpgrade);
            
            ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/it/notexists.wp");//Page does not exist
            status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
            assertEquals(ControllerManager.REDIRECT, status);
            String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
            assertEquals("http://www.entando.com/Entando/it/errorpage.page?redirectflag=1", redirectUrl);
        } catch (Exception e) {
            throw e;
        } finally {
            paramsToUpgrade.put(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, notFoundPageCode);
            ((IParameterizableManager)this.pageManager).updateParams(paramsToUpgrade);
        }
    }

    @Test
    void testServiceFailure_2() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/wrongpath.wp");//wrong path
        int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.REDIRECT, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertEquals("http://www.entando.com/Entando/it/errorpage.page?redirectflag=1", redirectUrl);
    }

    @Test
    void testServiceFailure_3() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/cc/homepage.wp");//lang does not exist
        int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.REDIRECT, status);
        String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
        assertEquals("http://www.entando.com/Entando/it/errorpage.page?redirectflag=1", redirectUrl);
    }

    @BeforeEach
    private void init() throws Exception {
        try {
            this.requestValidator = (ControlServiceInterface) this.getApplicationContext().getBean("RequestValidatorControlService");
            this.pageManager = this.getApplicationContext().getBean(SystemConstants.PAGE_MANAGER, IPageManager.class);
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }
    
    private ControlServiceInterface requestValidator;
    private IPageManager pageManager;

}
