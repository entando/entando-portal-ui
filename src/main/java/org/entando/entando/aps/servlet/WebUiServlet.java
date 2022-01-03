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
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * @author E.Santoboni
 */
public class WebUiServlet extends AbstractFrontEndServlet {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(StartupListener.class);
    
    public static final String PAGE_CODE_KEY = "pageCode";
    public static final String LANG_CODE_KEY = "langCode";
    public static final String USERNAME_KEY = "username";
    public static final String BASE_URL_KEY = "applicationBaseURL";
    public static final String CSP_TOKEN_KEY = "cspToken";
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String webUiEnabled = System.getenv(SystemConstants.WEB_UI_ENABLED);
        if (StringUtils.isEmpty(webUiEnabled) || !Boolean.TRUE.toString().equalsIgnoreCase(webUiEnabled)) {
            logger.warn("Web-UI servlet is not enabled");
            return;
        }
        request.setCharacterEncoding("UTF-8");
        try {
            RequestContext reqCtx = this.initRequestContext(request, response);
            logger.debug("Output");
            this.initFreemarker(request, response, reqCtx);
            this.executePage(request, reqCtx);
        } catch (IOException io) {
            logger.error("IO error", io);
            throw io;
        } catch (Exception t) {
            logger.error("Error building response", t);
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
            String pageCode = this.getParamValue(obj, PAGE_CODE_KEY);
            String langCode = this.getParamValue(obj, LANG_CODE_KEY);
            String username = this.getParamValue(obj, USERNAME_KEY);
            ILangManager langManager = (ILangManager) ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, request);
            Lang currentLang = (!StringUtils.isBlank(langCode)) ? langManager.getLang(langCode) : null;
            if (null == currentLang) {
                if (!StringUtils.isBlank(langCode)) {
                    logger.warn("Invalid language requested: '{}'", langCode);
                }
                currentLang = langManager.getDefaultLang();
            }
            UserDetails currentUser = null;
            IUserManager userManager = (IUserManager) ApsWebApplicationUtils.getBean(SystemConstants.USER_MANAGER, request);
            if (StringUtils.isBlank(username) || SystemConstants.GUEST_USER_NAME.equalsIgnoreCase(username)) {
                currentUser = userManager.getGuestUser();
            } else {
                IAuthenticationProviderManager authenticationProviderManager = (IAuthenticationProviderManager) ApsWebApplicationUtils.getBean(SystemConstants.AUTHENTICATION_PROVIDER_MANAGER, request);
                currentUser = authenticationProviderManager.getUser(username);
                if (null == currentUser) {
                    logger.warn("Invalid user requested: '{}'", username);
                    currentUser = userManager.getGuestUser();
                }
            }
            ConfigInterface configManager = (ConfigInterface) ApsWebApplicationUtils.getBean(SystemConstants.BASE_CONFIG_MANAGER, request);
            IPageManager pageManager = (IPageManager) ApsWebApplicationUtils.getBean(SystemConstants.PAGE_MANAGER, request);
            IAuthorizationManager authManager = (IAuthorizationManager) ApsWebApplicationUtils.getBean(SystemConstants.AUTHORIZATION_SERVICE, request);
            IPage currentPage = (!StringUtils.isBlank(pageCode)) ? pageManager.getOnlinePage(pageCode) : null;
            if (StringUtils.isBlank(pageCode)) {
                String homepagePageCode = configManager.getParam(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE);
                currentPage = pageManager.getOnlinePage(homepagePageCode);
            } else if (null == currentPage) {
                if (!StringUtils.isBlank(pageCode)) {
                    logger.warn("Invalid page requested: '{}'", pageCode);
                }
                String notFoundPageCode = configManager.getParam(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
                currentPage = pageManager.getOnlinePage(notFoundPageCode);
            } else if (!authManager.isAuth(currentUser, currentPage)) {
                String loginPageCode = configManager.getParam(IPageManager.CONFIG_PARAM_LOGIN_PAGE_CODE);
                currentPage = pageManager.getOnlinePage(loginPageCode);
            }
            String applicationBaseURL = this.getParamValue(obj, BASE_URL_KEY);
            if (!StringUtils.isBlank(applicationBaseURL)) {
                reqCtx.addExtraParam(SystemConstants.EXTRAPAR_WEB_UI_APPL_BASE_URL, applicationBaseURL);
            }
            String cspToken = this.getParamValue(obj, CSP_TOKEN_KEY);
            if (!StringUtils.isBlank(cspToken)) {
                reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CSP_NONCE_TOKEN, cspToken);
            }
            request.getSession().setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, currentUser);
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, currentLang);
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, currentPage);
        } catch (Exception e) {
            logger.error("Error reading body request", e);
            throw new EntException("Error reading body request", e);
        }
        return reqCtx;
    }
    
    private String getParamValue(JSONObject obj, String paramName) throws JSONException {
        if (obj.has(paramName)) {
            return obj.getString(paramName);
        }
        return null;
    }
    
}
