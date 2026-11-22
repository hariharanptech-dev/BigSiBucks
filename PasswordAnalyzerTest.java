import java.util.Arrays;

/**
 * PasswordAnalyzerTest - simple tests that need NO external library (no JUnit).
 *
 * Each test gives the analyzer a password and checks the strength level.
 * Run it with:   java -cp out PasswordAnalyzerTest
 *
 * NOTE: The passwords below are made-up examples used only for testing.
 */
public class PasswordAnalyzerTest {

    private static final PasswordAnalyzer ANALYZER = new PasswordAnalyzer();
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {

        System.out.println("=== WEAK passwords ===");
        expect("",                     AnalysisResult.Strength.WEAK);      // empty
        expect("123456",               AnalysisResult.Strength.WEAK);      // common + sequence + short
        expect("password123",          AnalysisResult.Strength.WEAK);      // common password
        expect("admin",                AnalysisResult.Strength.WEAK);      // common + short
        expect("abcdef",               AnalysisResult.Strength.WEAK);      // sequence + short
        expect("aaaaaaaaaa",           AnalysisResult.Strength.WEAK);      // repeated characters
        expect("Password123!",         AnalysisResult.Strength.WEAK);      // common word + tweaks
        expect("P@ssw0rd",             AnalysisResult.Strength.WEAK);      // leet-speak of "password"
        expect("Admin@2024",           AnalysisResult.Strength.WEAK);      // common word + suffix
        expect("Ab1!xyZ",              AnalysisResult.Strength.WEAK);      // only 7 characters

        System.out.println("\n=== MODERATE passwords ===");
        expect("mydogsname",           AnalysisResult.Strength.MODERATE);  // 10 lowercase letters only
        expect("Hello2024",            AnalysisResult.Strength.MODERATE);  // no special character
        expect("Tr0ub4dor&3",          AnalysisResult.Strength.MODERATE);  // good mix, but only 11 chars
        expect("Xk9#mP2$vL7@1234",     AnalysisResult.Strength.MODERATE);  // strong, but has the sequence 1234
        expect("Zq8!xxxxKp3#",         AnalysisResult.Strength.MODERATE);  // strong, but repeats "xxxx"

        System.out.println("\n=== STRONG passwords ===");
        expect("Xk9#mP2$vL7@qR4!",     AnalysisResult.Strength.STRONG);
        expect("correct-Horse-battery-7-Staple!", AnalysisResult.Strength.STRONG);
        expect("Gr33n&Tiger*Jumps#Over9",         AnalysisResult.Strength.STRONG);

        System.out.println("\n=== Individual checks ===");
        expectRequirement("Hello2024",  "Contains a special character", false);
        expectRequirement("Hello2024",  "Contains an uppercase letter", true);
        expectRequirement("hello",      "Contains an uppercase letter", false);
        expectRequirement("abcd1234",   "predictable sequence", false);
        expectRequirement("zzzTop",     "repeated characters", false);
        expectRequirement("Qw9!pLm2",   "repeated characters", true);

        System.out.println("\n=== Score range ===");
        checkTrue("empty password scores 0", ANALYZER.analyze(new char[0]).getScore() == 0);
        checkTrue("null password scores 0", ANALYZER.analyze(null).getScore() == 0);
        checkTrue("perfect password scores 100", ANALYZER.analyze("Xk9#mP2$vL7@qR4!".toCharArray()).getScore() == 100);

        System.out.println("\n-----------------------------------");
        System.out.println("Passed: " + passedTests + "   Failed: " + failedTests);
        if (failedTests > 0) {
            System.exit(1); // non-zero exit code = "something failed"
        }
    }

    /** Checks that a password gets the expected strength level. */
    private static void expect(String password, AnalysisResult.Strength expected) {
        char[] chars = password.toCharArray();
        AnalysisResult result = ANALYZER.analyze(chars);
        Arrays.fill(chars, '\0'); // wipe, just like the real application does

        boolean ok = result.getStrength() == expected;
        // Test passwords are fake, so printing them here is fine. (The real app NEVER prints passwords.)
        report(ok, String.format("%-34s score=%3d  expected=%-8s got=%s",
                "\"" + password + "\"", result.getScore(),
                expected.getLabel(), result.getStrength().getLabel()));
    }

    /** Checks whether one named checklist item passed or failed. */
    private static void expectRequirement(String password, String descriptionPart, boolean expectedPassed) {
        AnalysisResult result = ANALYZER.analyze(password.toCharArray());
        for (AnalysisResult.Requirement r : result.getRequirements()) {
            if (r.getDescription().contains(descriptionPart)) {
                report(r.isPassed() == expectedPassed,
                        "\"" + password + "\" -> [" + descriptionPart + "] expected "
                                + (expectedPassed ? "PASS" : "FAIL"));
                return;
            }
        }
        report(false, "Requirement not found: " + descriptionPart);
    }

    private static void checkTrue(String name, boolean condition) {
        report(condition, name);
    }

    private static void report(boolean ok, String message) {
        if (ok) {
            passedTests++;
            System.out.println("  [PASS] " + message);
        } else {
            failedTests++;
            System.out.println("  [FAIL] " + message);
        }
    }
}
