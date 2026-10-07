package lk.booknplay.controller.webhook;

import lk.booknplay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook/payhere")
@RequiredArgsConstructor
public class PayHereWebhookController {

    private final PaymentService paymentService;

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> notify(@RequestParam MultiValueMap<String, String> form) {
        Map<String, String> fields = new HashMap<>();
        form.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                fields.put(key, values.get(0));
            }
        });
        paymentService.processPayHereNotify(fields);
        return ResponseEntity.ok("OK");
    }
}
