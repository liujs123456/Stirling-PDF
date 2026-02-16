package stirling.software.SPDF.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler newHandler() {
        StaticMessageSource messageSource = new StaticMessageSource();
        return new GlobalExceptionHandler(
                messageSource, mock(org.springframework.core.env.Environment.class));
    }

    private static HttpServletRequest mockRequest(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    @Test
    @DisplayName("handleMissingParameter: returns 400 and includes parameterName/parameterType")
    void handleMissingParameter_returnsBadRequestWithDetails() {
        GlobalExceptionHandler handler = newHandler();
        HttpServletRequest req = mockRequest("/api/test");

        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("q", "String");

        ResponseEntity<ProblemDetail> resp = handler.handleMissingParameter(ex, req);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

        ProblemDetail pd = resp.getBody();
        assertNotNull(pd);

        // common property set by createBaseProblemDetail(...)
        assertEquals("/api/test", pd.getProperties().get("path"));

        // handler-specific properties
        assertEquals("q", pd.getProperties().get("parameterName"));
        assertEquals("String", pd.getProperties().get("parameterType"));

        // keep it simple: type exists
        assertNotNull(pd.getType());

        // title is set both as ProblemDetail.title and property "title"
        assertEquals(pd.getTitle(), pd.getProperties().get("title"));
    }

    @Test
    @DisplayName("handleMethodNotSupported: returns 405 and includes supportedMethods")
    void handleMethodNotSupported_returnsMethodNotAllowed() {
        GlobalExceptionHandler handler = newHandler();
        HttpServletRequest req = mockRequest("/api/method");

        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("TRACE", List.of("GET", "POST"));

        ResponseEntity<ProblemDetail> resp = handler.handleMethodNotSupported(ex, req);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, resp.getHeaders().getContentType());

        ProblemDetail pd = resp.getBody();
        assertNotNull(pd);

        assertEquals("/api/method", pd.getProperties().get("path"));
        assertNotNull(pd.getType());

        assertEquals("TRACE", pd.getProperties().get("method"));
        assertNotNull(pd.getProperties().get("supportedMethods"));
    }
}
