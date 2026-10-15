package io.github.gfander.dualforge;

/** Run without Minecraft; incorrect rejection and acceptance must fail the assertion. */
public final class FnFAdmissionExpectationContract {
    public static void main(String[] args) {
        FnFAdmissionExpectation.requireSupplierRejection(() -> {
            throw new IllegalStateException(FnFAdmissionExpectation.REASON);
        });
        rejected(() -> {});
        rejected(() -> { throw new IllegalStateException("some other failure"); });
        rejected(() -> { throw new IllegalStateException(); });
        rejected(() -> { throw new IllegalStateException(FnFAdmissionExpectation.REASON + " extra"); });
        rejected(() -> { throw new IllegalStateException("wrapper", new IllegalStateException(FnFAdmissionExpectation.REASON)); });
        try {
            FnFAdmissionExpectation.requireSupplierRejection(() -> { throw new IllegalArgumentException("wrong type"); });
            throw new AssertionError("Wrong exception type accepted");
        } catch (IllegalArgumentException expected) { }
        System.out.println("FNF EXPECTATION CONTRACT PASS positive=1 negatives=6");
    }

    private static void rejected(Runnable admission) {
        boolean failed = false;
        try { FnFAdmissionExpectation.requireSupplierRejection(admission); }
        catch (AssertionError expected) { failed = true; }
        if (!failed) throw new AssertionError("Invalid admission outcome accepted");
    }
}
