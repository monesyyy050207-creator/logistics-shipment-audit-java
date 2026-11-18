public final class ShipmentDecision {
    private ShipmentDecision() {}

    public static String nextState(String eventName, boolean proofOfDeliveryPresent) {
        if ("proof-of-delivery-exception".equals(eventName) || !proofOfDeliveryPresent) {
            return "EXCEPTION_REVIEW";
        }
        return "DELIVERY_PROGRESS";
    }
}
