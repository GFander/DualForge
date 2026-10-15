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
        FnFAdmissionExpectation.requireSupplierV1Rejection(() -> {throw new IllegalStateException(FnFAdmissionExpectation.SUPPLIER_V1_REASON);});
        rejectedV1(() -> {});
        rejectedV1(() -> {throw new IllegalStateException(FnFAdmissionExpectation.REASON);});
        rejectedV1(() -> {throw new IllegalStateException(FnFAdmissionExpectation.SUPPLIER_V1_REASON+" extra");});
        System.out.println("FNF EXPECTATION CONTRACT PASS positive=2 negatives=9");
    }

    private static void rejectedV1(Runnable admission) {
        boolean failed=false;try{FnFAdmissionExpectation.requireSupplierV1Rejection(admission);}catch(AssertionError expected){failed=true;}
        if(!failed)throw new AssertionError("Invalid supplier-v1 outcome accepted");
    }

    private static void rejected(Runnable admission) {
        boolean failed = false;
        try { FnFAdmissionExpectation.requireSupplierRejection(admission); }
        catch (AssertionError expected) { failed = true; }
        if (!failed) throw new AssertionError("Invalid admission outcome accepted");
    }
}
