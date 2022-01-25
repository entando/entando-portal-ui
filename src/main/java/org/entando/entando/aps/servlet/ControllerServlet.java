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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.util.ApsWebApplicationUtils;

import java.security.SecureRandom;
import org.apache.commons.lang3.StringUtils;

/**
 * Servlet di controllo, punto di ingresso per le richieste di pagine del portale.
 * Predispone il contesto di richiesta, invoca il controller e ne gestisce lo stato di uscita.
 * @author M.Diana - W.Ambu
 */
public class ControllerServlet extends AbstractFrontEndServlet {

	private static final Logger _logger = LoggerFactory.getLogger(ControllerServlet.class);

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		RequestContext reqCtx = this.initRequestContext(request, response);
		int status = this.controlRequest(request, reqCtx);
		if (status == ControllerManager.REDIRECT) {
			_logger.debug("Redirection");
			this.redirect(reqCtx, response);
		} else if (status == ControllerManager.OUTPUT) {
			_logger.debug("Output");
			try {
				this.initFreemarker(request, response, reqCtx);
				this.executePage(request, reqCtx);
			} catch (Throwable t) {
				_logger.error("Error building response", t);
				throw new ServletException("Error building response", t);
			}
		} else if (status == ControllerManager.ERROR) {
			_logger.debug("Error");
			this.outputError(reqCtx, response);
		} else {
			_logger.error("Error: final status = {} - request: {}",
					ControllerManager.getStatusDescription(status),
					request.getServletPath());
			throw new ServletException("Service not available");
		}
		return;
	}

    protected RequestContext initRequestContext(HttpServletRequest request,
            HttpServletResponse response) {
        RequestContext reqCtx = new RequestContext();
        _logger.debug("Request:" + request.getServletPath());
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        reqCtx.setRequest(request);
        reqCtx.setResponse(response);
        ConfigInterface configManager = (ConfigInterface) ApsWebApplicationUtils.getBean(SystemConstants.BASE_CONFIG_MANAGER, request);
        String cspEnabled = configManager.getParam(SystemConstants.PAR_CSP_ENABLED);
        if (Boolean.TRUE.equals(Boolean.valueOf(cspEnabled))) {
            String currentToken = this.createSecureRandomString();
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CSP_NONCE_TOKEN, currentToken);
            String cspParams = "script-src 'nonce-" + currentToken + "'";
            String extraConfig = configManager.getParam(SystemConstants.PAR_CSP_HEADER_EXTRA_CONFIG);
            if (!StringUtils.isBlank(extraConfig)) {
                cspParams += " " + extraConfig;
            }
            response.setHeader("content-security-policy", cspParams);
        }
        return reqCtx;
    }

    public String createSecureRandomString() {
        int leftLimit = 48;
        int rightLimit = 122;
        int targetStringLength = 64;
        SecureRandom rand = new SecureRandom();
        return rand.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

	protected int controlRequest(HttpServletRequest request,
			RequestContext reqCtx) {
		ControllerManager controller = (ControllerManager) ApsWebApplicationUtils.getBean(SystemConstants.CONTROLLER_MANAGER, request);
		return controller.service(reqCtx);
	}

	protected void redirect(RequestContext reqCtx, HttpServletResponse response)
			throws ServletException {
		try {
			String url = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
			response.sendRedirect(url);
		} catch (Exception e) {
			throw new ServletException("Service not available", e);
		}
	}

	protected void outputError(RequestContext reqCtx, HttpServletResponse response) throws ServletException {
		try {
			if (!response.isCommitted()) {
				Integer httpErrorCode = (Integer) reqCtx.getExtraParam("errorCode");
				if (httpErrorCode == null) {
					httpErrorCode = new Integer(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				}
				response.sendError(httpErrorCode.intValue());
			}
		} catch (IOException e) {
			_logger.error("outputError", e);
			throw new ServletException("Service not available");
		}
	}

}
