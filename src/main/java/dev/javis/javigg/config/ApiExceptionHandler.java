package dev.javis.javigg.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import dev.javis.javigg.livegame.NotInGameException;
import dev.javis.javigg.riot.RiotApiException;
import jakarta.validation.ConstraintViolationException;

/**
 * Maps failures to RFC 7807 problem responses with client-safe messages; internal details stay in
 * the logs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RiotApiException.class)
    public ResponseEntity<ProblemDetail> handleRiot(RiotApiException e) {
        if (e.isNotFound()) {
            return problem(HttpStatus.NOT_FOUND, "Summoner not found");
        }
        if (e.isRateLimited()) {
            ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);
            if (e.retryAfter() != null) {
                response.header(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, e.retryAfter().toSeconds())));
            }
            return response.body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                    "Too many requests right now, try again shortly"));
        }
        if (e.status() == 400) {
            return problem(HttpStatus.BAD_REQUEST, "Invalid request");
        }
        log.warn("Riot API failure: {}", e.getMessage(), e);
        return problem(HttpStatus.BAD_GATEWAY, "Riot API is unavailable, try again later");
    }

    @ExceptionHandler(NotInGameException.class)
    public ResponseEntity<ProblemDetail> handleNotInGame(NotInGameException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameters");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
