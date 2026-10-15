package io.github.gfander.dualforge;

/** Test-only assertion; never included in the compatibility JAR. */
final class FnFAdmissionExpectation {
    static final String REASON = "Registry helper rejected: SUPPLIER_BUSINESS_UNSUPPORTED";

    static final String SUPPLIER_V1_REASON = "Registry helper rejected: ITEM_FIELD_DESCRIPTOR";

    static final String TYPED_V1_REASON = "Registry helper rejected: GLOBAL_EVENT_BUS_UNSUPPORTED";
    static void requireTypedV1Rejection(Runnable admission) { requireRejection(admission,TYPED_V1_REASON); }

    static void requireSupplierRejection(Runnable admission) { requireRejection(admission,REASON); }
    static void requireSupplierV1Rejection(Runnable admission) { requireRejection(admission,SUPPLIER_V1_REASON); }
    private static void requireRejection(Runnable admission,String expected) {
        try {
            admission.run();
        } catch (IllegalStateException rejected) {
            if (!expected.equals(rejected.getMessage())) {
                throw new AssertionError("Unexpected FnF rejection reason: " + rejected.getMessage(), rejected);
            }
            return;
        }
        throw new AssertionError("Unverified FnF closure admitted");
    }
}
