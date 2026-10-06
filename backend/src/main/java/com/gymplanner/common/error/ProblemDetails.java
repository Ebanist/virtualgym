package com.gymplanner.common.error;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

    public static final String CODE = "code";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:gymplanner:error:" + code));
        problem.setProperty(CODE, code);
        return problem;
    }
}
