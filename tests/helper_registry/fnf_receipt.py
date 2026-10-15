"""Strict read-only FnF receipt, independent of process exit status."""
REASON = "Registry helper rejected: SUPPLIER_BUSINESS_UNSUPPORTED"
PASS_LINE = "FNF READ_ONLY ADMISSION PASS; FNF_RUNTIME=NOT RUN; no helper output published"


def validate_fnf_admission(text, version="legacy-4b"):
    reason = {"legacy-4b":REASON,"supplier-v1":"Registry helper rejected: ITEM_FIELD_DESCRIPTOR","typed-v1":"Registry helper rejected: GLOBAL_EVENT_BUS_UNSUPPORTED"}.get(version)
    if version not in ("legacy-4b", "supplier-v1", "typed-v1"):
        raise AssertionError("Unknown FnF contract version")
    lines = text.splitlines()
    admissions = [line for line in lines if line.startswith("FNF_PLAN_ADMISSION=")]
    if admissions != ["FNF_PLAN_ADMISSION=REJECTED reason=" + reason]:
        raise AssertionError("Missing, conflicting or incorrect FnF rejection reason")
    runtime = [line for line in lines if "FNF_RUNTIME=" in line]
    if runtime != [PASS_LINE]:
        raise AssertionError("Missing or conflicting read-only FnF runtime status")
    if [line for line in lines if line.startswith("FNF_CONTRACT_VERSION=")] != ([] if version=="legacy-4b" else ["FNF_CONTRACT_VERSION="+version]):
        raise AssertionError("Missing or conflicting version receipt")
    return reason
