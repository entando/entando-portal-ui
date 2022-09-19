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
package org.entando.entando.aps.system.services.controller.executor;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsWebApplicationUtils;

import freemarker.template.Template;

import java.io.IOException;
import java.io.StringReader;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class PageExecutorService implements ExecutorServiceInterface {

    private static final Logger _logger = LoggerFactory.getLogger(PageExecutorService.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        //nothing to do
    }

    @Override
    public void service(RequestContext reqCtx) {
        HttpServletRequest request = reqCtx.getRequest();
        HttpServletResponse response = reqCtx.getResponse();
        try {
            if (response.isCommitted()) {
                return;
            }
            IPageManager pageManager = (IPageManager) ApsWebApplicationUtils.getBean(
                    SystemConstants.PAGE_MANAGER, reqCtx.getRequest());
            IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            String notFoundCode = pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
            if (page.getCode().equalsIgnoreCase(notFoundCode)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            PageModel model = page.getModel();
            if (StringUtils.isBlank(model.getTemplate())) {
                String jspPath = model.getPageModelJspPath();
                RequestDispatcher dispatcher = request.getSession().getServletContext().getRequestDispatcher(jspPath);
                dispatcher.forward(request, response);
            } else {
                ExecutorBeanContainer ebc = (ExecutorBeanContainer) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_EXECUTOR_BEAN_CONTAINER);
                Template template = new Template(page.getCode(), new StringReader(model.getTemplate()), ebc.getConfiguration());
                try {
                    template.process(ebc.getTemplateModel(), response.getWriter());
                } catch (Throwable t) {
                    String msg = "Error detected while including a page template " + model.getCode();
                    _logger.error(msg, t);
                    throw new RuntimeException(msg, t);
                }
            }
        } catch (ServletException e) {
            String msg = "Error detected while including a page template";
            _logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (IOException e) {
            String msg = "IO error detected while including the page template";
            _logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

}
