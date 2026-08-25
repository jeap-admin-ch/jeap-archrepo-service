package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Renders the errors of the docs API as RFC 9457 problem documents.
 * <p>
 * Scoped to the docs API package, and deliberately not switched on globally with
 * {@code spring.mvc.problemdetails.enabled}: that would change the error format of every existing endpoint of the
 * service, which this API is added alongside rather than on top of.
 */
@RestControllerAdvice(basePackageClasses = DocsApiPaths.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
class DocsApiExceptionHandler {

    private static final String PROBLEM_TYPE_PREFIX = "https://jeap.admin.ch/problems/archrepo/";

    @ExceptionHandler(DocsApiException.class)
    ProblemDetail handleDocsApiException(DocsApiException exception, HttpServletRequest request) {
        if (exception.getStatus().is5xxServerError()) {
            log.error("Docs API request to '{}' failed: {}", request.getRequestURI(), exception.getMessage(), exception);
        } else {
            log.debug("Docs API request to '{}' rejected: {}", request.getRequestURI(), exception.getMessage());
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problemDetail.setType(URI.create(PROBLEM_TYPE_PREFIX + exception.getProblemType()));
        problemDetail.setTitle(exception.getTitle());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
