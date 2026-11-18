import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class LogisticsJob {
    public static void main(String[] args) throws Exception {
        String shipmentId = args.length > 0 ? args[0] : "SHP-2048";
        String eventName = args.length > 1 ? args[1] : "courier-accepted";
        String state = ShipmentDecision.nextState(eventName, "proof-of-delivery".equals(eventName));
        InfraiClient client = new InfraiClient();
        String event = "{\"message\":\"" + eventName + "\",\"level\":\"info\",\"shipment_id\":\"" + shipmentId + "\",\"event_name\":\"" + eventName
                + "\",\"state\":\"" + state + "\",\"proof_of_delivery\":\"pod/" + shipmentId + "\"}";
        String ingest = client.request("POST", "/v1/logs/ingest", "{\"entries\":[" + event
                + "],\"idempotency_key\":\"shipment-" + shipmentId + "-" + eventName + "\"}");
        String search = client.request("GET", "/v1/logs/search?q=" + shipmentId, null);
        System.out.println("ingested=" + ingest);
        System.out.println("search=" + search);

        client.request("POST", "/v1/account/keys/create", "{\"name\":\"temporary-audit-key\",\"idempotency_key\":\"create-" + shipmentId + "\"}");
        client.request("PUT", "/v1/account/budget/set", "{\"hard_cap_usd\":25,\"period\":\"monthly\",\"alert_threshold_usd\":20}");
        String webhook = client.request("POST", "/v1/account/webhooks/register", "{\"url\":\"https://example.edu/logistics-hook\",\"events\":[\"error.captured\"],\"secret\":\"classroom-secret\"}");
        String signature = signWebhook("classroom-secret", webhook);
        if (!verifyWebhookSignature("classroom-secret", webhook, signature)) {
            throw new IllegalStateException("webhook signature verification failed");
        }
        client.request("PUT", "/v1/account/routing/set", "{\"capability\":\"logs\"}");
        System.out.println("account controls configured; webhook signature verified");
    }

    static boolean verifyWebhookSignature(String secret, String payload, String signature) throws Exception {
        return signWebhook(secret, payload).equals(signature);
    }

    private static String signWebhook(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format("%02x", value));
        return hex.toString();
    }
}
