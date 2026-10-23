package com.syncflow.project.infrastructure.webhook;

import com.syncflow.common.domain.DomainException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookDispatcherServiceTest {

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> WebhookDispatcherService.validateTarget("file:///etc/passwd", true))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> WebhookDispatcherService.validateTarget("javascript:alert(1)", true))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsCredentialsInUrl() {
        assertThatThrownBy(() -> WebhookDispatcherService.validateTarget("https://user:pw@example.com/hook", true))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void blocksInternalTargetsUnlessAllowed() {
        for (String url : new String[]{"http://127.0.0.1/hook", "http://localhost:8080/x", "http://10.1.2.3/",
                "http://192.168.0.10/", "http://169.254.169.254/latest/meta-data", "http://[::1]/"}) {
            assertThatThrownBy(() -> WebhookDispatcherService.validateTarget(url, false))
                    .as(url).isInstanceOf(DomainException.class);
        }
        assertThat(WebhookDispatcherService.validateTarget("http://127.0.0.1/hook", true).getHost()).isEqualTo("127.0.0.1");
    }

    @Test
    void signatureIsHmacSha256OfTimestampAndBody() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("s3cret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal("1700000000.{\"a\":1}".getBytes(StandardCharsets.UTF_8)));
        assertThat(WebhookDispatcherService.sign("s3cret", "1700000000.{\"a\":1}")).isEqualTo(expected);
    }
}
