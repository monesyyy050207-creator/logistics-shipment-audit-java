public final class ShipmentDecisionTest {
    public static void main(String[] args) {
        check("DELIVERY_PROGRESS", ShipmentDecision.nextState("courier-accepted", true));
        check("EXCEPTION_REVIEW", ShipmentDecision.nextState("proof-of-delivery-exception", false));
        System.out.println("ShipmentDecisionTest passed");
    }

    private static void check(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
}
