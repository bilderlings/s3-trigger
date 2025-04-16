package io.jenkins.plugins.s3trigger;

import com.jayway.jsonpath.JsonPath;
import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.springframework.http.HttpStatus;

@Extension
public class S3Webhook extends CrumbExclusion implements UnprotectedRootAction {
    private static final Logger LOGGER = Logger.getLogger(S3Webhook.class.getName());

    public static final String WEBHOOK_URL = "s3";

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return WEBHOOK_URL;
    }

    @RequirePOST
    @WebMethod(name = "trigger")
    public HttpResponse doBuild(StaplerRequest req, StaplerResponse res) {
        var authorizationHeader = req.getHeader("authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            LOGGER.warning("Access Denied: bearer token not provided");
            return HttpResponses.forbidden();
        }

        var config = S3TriggerConfiguration.get();
        var token = authorizationHeader.substring("Bearer ".length()).trim();
        if (!token.equals(config.getToken())) {
            LOGGER.warning("Access Denied: invalid bearer token");
            return HttpResponses.forbidden();
        }

        Set<String> bucketNames;
        try {
            List<String> buckets = JsonPath.read(req.getInputStream(), "$.Records[*].s3.bucket.name");
            bucketNames = new HashSet<>(buckets);
        } catch (IOException e) {
            LOGGER.warning("Unable to read webhook body" + e.getMessage());
            return HttpResponses.error(HttpStatus.BAD_REQUEST.value(), "Unable to handle request");
        }

        for (String bucket : bucketNames) {
            config.addToQueue(bucket);
        }

        return HttpResponses.ok();
    }

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/" + WEBHOOK_URL)) {
            chain.doFilter(req, res);
            return true;
        }
        return false;
    }
}
