import unittest
from fnf_receipt import REASON, PASS_LINE, validate_fnf_admission


class FnFReceiptTests(unittest.TestCase):
    def test_exact_rejection(self):
        self.assertEqual(validate_fnf_admission(self.receipt()), REASON)

    def receipt(self):
        return "FNF_PLAN_ADMISSION=REJECTED reason=" + REASON + "\n" + PASS_LINE + "\n"

    def test_invalid_receipts_fail(self):
        valid = self.receipt()
        for text in ["", PASS_LINE, valid.replace(REASON, "other IllegalStateException"),
                     valid.replace("REJECTED", "ACCEPTED"), valid.replace(REASON, REASON + " extra"),
                     valid + "FNF_PLAN_ADMISSION=REJECTED reason=other\n",
                     valid.replace("NOT RUN", "PASS"), valid + PASS_LINE + "\n",
                     valid.replace(PASS_LINE, "FNF READ_ONLY ADMISSION PASS")]:
            with self.subTest(text=text), self.assertRaises(AssertionError):
                validate_fnf_admission(text)


if __name__ == "__main__":
    unittest.main()
