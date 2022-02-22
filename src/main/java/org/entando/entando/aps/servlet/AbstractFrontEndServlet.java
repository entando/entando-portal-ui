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
package org.entando.entando.aps.servlet;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.entando.entando.aps.system.services.controller.executor.ExecutorBeanContainer;
import org.entando.entando.aps.system.services.controller.executor.ExecutorServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.util.ApsWebApplicationUtils;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class AbstractFrontEndServlet extends freemarker.ext.servlet.FreemarkerServlet {

	protected void initFreemarker(HttpServletRequest request,
			HttpServletResponse response, RequestContext reqCtx)
			throws TemplateModelException {
		Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		DefaultObjectWrapper wrapper = new DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		config.setObjectWrapper(wrapper);
		config.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
		TemplateModel templateModel = this.createModel(wrapper, this.getServletContext(), request, response);
		ExecutorBeanContainer ebc = new ExecutorBeanContainer(config, templateModel);
		reqCtx.addExtraParam(SystemConstants.EXTRAPAR_EXECUTOR_BEAN_CONTAINER, ebc);
	}

	protected void executePage(HttpServletRequest request, RequestContext reqCtx) {
		List<ExecutorServiceInterface> executors = (List<ExecutorServiceInterface>) ApsWebApplicationUtils.getBean("ExecutorServices", request);
		for (int i = 0; i < executors.size(); i++) {
			ExecutorServiceInterface executor = executors.get(i);
			executor.service(reqCtx);
		}
	}

	@Override
	protected TemplateModel createModel(ObjectWrapper wrapper, ServletContext servletContext,
			HttpServletRequest request, HttpServletResponse response) throws TemplateModelException {
		TemplateModel template = super.createModel(wrapper, servletContext, request, response);
		if (template instanceof AllHttpScopesHashModel) {
			AllHttpScopesHashModel hashModel = ((AllHttpScopesHashModel) template);
			ServletContextHashModel servletContextModel = (ServletContextHashModel) hashModel.get(KEY_APPLICATION);
			if (null == servletContextModel.getServlet()) {
				ServletContextHashModel newServletContextModel = new ServletContextHashModel(this, wrapper);
				servletContextModel = new ServletContextHashModel(this, wrapper);
				servletContext.setAttribute(ATTR_APPLICATION_MODEL, servletContextModel);
				TaglibFactory taglibs = new TaglibFactory(servletContext);
				servletContext.setAttribute(ATTR_JSP_TAGLIBS_MODEL, taglibs);
				hashModel.putUnlistedModel(KEY_APPLICATION, newServletContextModel);
				hashModel.putUnlistedModel(KEY_APPLICATION_PRIVATE, newServletContextModel);
			}
		}
		return template;
	}

	private static final String ATTR_APPLICATION_MODEL = ".freemarker.Application";
	private static final String ATTR_JSP_TAGLIBS_MODEL = ".freemarker.JspTaglibs";

}
