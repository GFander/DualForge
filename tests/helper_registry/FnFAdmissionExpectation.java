package io.github.gfander.dualforge;

/** Test-only assertion; never included in the compatibility JAR. */
final class FnFAdmissionExpectation {
    static final String REASON = "Registry helper rejected: SUPPLIER_BUSINESS_UNSUPPORTED";

    static void requireSupplierRejection(Runnable admission) {
        try {
            admission.run();
        } catch (IllegalStateException rejected) {
            if (!REASON.equals(rejected.getMessage())) {
                throw new AssertionError("Unexpected FnF rejection reason: " + rejected.getMessage(), rejected);
            }
            return;
        }
        throw new AssertionError("Unverified FnF closure admitted");
    }
}
