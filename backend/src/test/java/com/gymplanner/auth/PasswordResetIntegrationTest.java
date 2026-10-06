package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymplanner.support.AbstractIntegrationTest;
import com.gymplanner.support.TestUsers;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    @Test
    void resetLinkIsLoggedAndCanBeUsedOnce(CapturedOutput output) throws Exception {
        TestUsers.Registered user = TestUsers.register(mvc, objectMapper);
        mvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email()))))
                .andExpect(status().isNoContent());

        Matcher matcher = Pattern.compile(Pattern.quote(user.email()) + ": \\S+reset-password\\?token=(\\S+)")
                .matcher(output.getOut());
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        String confirm = json(Map.of("token", token, "newPassword", "Reset789abc"));
        mvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON).content(confirm))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON).content(confirm))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", "Reset789abc"))))
                .andExpect(status().isOk());
    }
}
