package org.csstudio.alarm.beast.ui;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServletFilter implements javax.servlet.Filter {

    public void doFilter(ServletRequest req, ServletResponse res,
            FilterChain chain) throws ServletException, IOException {

        try {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;

            if (request.getPathInfo() == null || request.getPathInfo().equals("/")) {
                response.sendRedirect(response
                        .encodeRedirectURL(WebAlarmConstants.MAIN_SERVLET_NAME));
            } else {
                chain.doFilter(request, response);
            }
        } catch (ClassCastException e) {
            throw new ServletException("non-HTTP request or response");
        }

    }

    public void init(FilterConfig filterConfig) {
    }

    public void destroy() {
    }
}