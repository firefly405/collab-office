package com.collaboffice.common.handler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MissingResourceTest {
    @RestController static class ResourceFixture {
        @GetMapping("/missing-resource") void missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET,"api/health");
        }
    }
    @Test void absentResourceReturns404InsteadOfSystemError() throws Exception {
        MockMvcBuilders.standaloneSetup(new ResourceFixture()).setControllerAdvice(new GlobalExceptionHandler()).build()
            .perform(get("/missing-resource")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
    }
}
