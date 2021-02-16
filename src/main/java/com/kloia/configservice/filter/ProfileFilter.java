package com.kloia.configservice.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Getter
@Setter
public class ProfileFilter implements Filter {

    public static final String CONFIG_PREFIX = "/configs";
    private static final String REQUEST_PARAM = "filtered";
    private static final String BRANCH_NAME = "/master/";
    private static final String FOLDER_SEPERATOR = "/";
    @Value("${environment}")
    private String environment;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String applicationName = findApplicationName(request.getRequestURI());
        if (request.getAttribute(REQUEST_PARAM) != null || StringUtils.isEmpty(applicationName)) {
            endFilter(servletRequest, servletResponse, filterChain);
            return;
        }
        beforeForwarding(request);
        if (applicationName.equals(CONFIG_PREFIX + "/resource/")) {
            prepareResourcePath(servletRequest, servletResponse, request, applicationName);
        } else {
            prepareConfigPath(servletRequest, servletResponse, applicationName);
        }
    }

    private void prepareConfigPath(ServletRequest servletRequest,
                                   ServletResponse servletResponse,
                                   String applicationName) throws ServletException, IOException {
        servletRequest.getRequestDispatcher(applicationName + environment).forward(servletRequest, servletResponse);
    }

    private void prepareResourcePath(ServletRequest servletRequest, ServletResponse servletResponse,
                                     HttpServletRequest request,
                                     String applicationName) throws ServletException, IOException {
        String resourceString = request.getRequestURI().substring(applicationName.length());
        servletRequest.getRequestDispatcher(getResourcePath(resourceString)).forward(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }

    private void beforeForwarding(HttpServletRequest request) {
        request.setAttribute(REQUEST_PARAM, true);
    }

    private String findApplicationName(String value) {
        Pattern pattern = Pattern.compile(CONFIG_PREFIX + "/(.*?)/");
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(0) : null;
    }

    private String getResourcePath(String value) {
        String[] split = value.split(FOLDER_SEPERATOR);
        return new StringBuilder()
                .append(CONFIG_PREFIX)
                .append(FOLDER_SEPERATOR)
                .append(split[0])
                .append(FOLDER_SEPERATOR)
                .append(environment)
                .append(BRANCH_NAME)
                .append(split[1])
                .toString();
    }

    private void endFilter(ServletRequest servletRequest,
                           ServletResponse servletResponse,
                           FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);
        servletResponse.flushBuffer();
    }
}
