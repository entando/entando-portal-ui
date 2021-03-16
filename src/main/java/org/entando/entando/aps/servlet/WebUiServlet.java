/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.jettison.json.JSONObject;
import org.entando.entando.ent.exception.EntException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class WebUiServlet extends AbstractFrontEndServlet {
    
    private static final Logger _logger = LoggerFactory.getLogger(WebUiServlet.class);
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            RequestContext reqCtx = this.initRequestContext(request, response);
            _logger.debug("Output");
            this.initFreemarker(request, response, reqCtx);
            this.executePage(request, reqCtx);
        } catch (IOException io) {
            _logger.error("IO error", io);
            throw io;
        } catch (Throwable t) {
            _logger.error("Error building response", t);
            throw new ServletException("Error building response", t);
        }
    }
    
    protected RequestContext initRequestContext(HttpServletRequest request, HttpServletResponse response) throws IOException, EntException {
        RequestContext reqCtx = new RequestContext();
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        reqCtx.setRequest(request);
        reqCtx.setResponse(response);
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        try {
            JSONObject obj = new JSONObject(body);
            String pageCode = obj.getString("pageCode");
            String langCode = obj.getString("langCode");
            String username = obj.getString("username");
            IPageManager pageManager = (IPageManager) ApsWebApplicationUtils.getBean(SystemConstants.PAGE_MANAGER, request);
            ILangManager langManager = (ILangManager) ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, request);
            IAuthorizationManager authManager = (IAuthorizationManager) ApsWebApplicationUtils.getBean(SystemConstants.AUTHORIZATION_SERVICE, request);
            IAuthenticationProviderManager authenticationProviderManager = (IAuthenticationProviderManager) ApsWebApplicationUtils.getBean(SystemConstants.AUTHENTICATION_PROVIDER_MANAGER, request);
            IPage currentPage = pageManager.getOnlinePage(pageCode);
            
            Lang currentLang = langManager.getLang(langCode);
            UserDetails currentUser = authenticationProviderManager.getUser(username);
            //boolean authorized = authManager.isAuth(currentUser, currentPage);
            
            request.getSession().setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, currentUser);
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, currentLang);
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, currentPage);
        } catch (Exception e) {
            _logger.error("Error reading body request", e);
            throw new EntException("Error reading body request", e);
        }
        /*
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
        */
        return reqCtx;
    }
    
}
