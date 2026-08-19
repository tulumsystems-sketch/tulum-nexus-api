package com.tulumcore.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Valida la firma HMAC que Mercado Pago envía en x-signature.
 * Sin secreto configurado, o si la firma no cierra, el webhook se ignora.
 */
@Component
public class MercadoPagoWebhookValidator {

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    public boolean secretoConfigurado() {
        return StringUtils.hasText(webhookSecret);
    }

    public boolean esValida(String xSignature, String xRequestId, String dataId) {
        if (!secretoConfigurado()
                || !StringUtils.hasText(xSignature)
                || !StringUtils.hasText(xRequestId)
                || !StringUtils.hasText(dataId)) {
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String clave = kv[0].trim();
            String valor = kv[1].trim();
            if ("ts".equals(clave)) {
                ts = valor;
            } else if ("v1".equals(clave)) {
                v1 = valor;
            }
        }
        if (!StringUtils.hasText(ts) || !StringUtils.hasText(v1)) {
            return false;
        }

        String manifiesto = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperado = HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    esperado.getBytes(StandardCharsets.US_ASCII),
                    v1.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
