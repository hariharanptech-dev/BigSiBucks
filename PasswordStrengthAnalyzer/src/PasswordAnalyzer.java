import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PasswordAnalyzer - the "brain" of the project.
 *
 * It contains ALL the password-checking logic and has NO GUI code, so it can
 * be tested on its own (see test/PasswordAnalyzerTest.java).
 *
 * HOW SCORING WORKS
 * Every requirement is worth some points. The points add up to 100:
 *
 *   At least 8 characters ............ 15
 *   12 or more characters ............ 15
 *   Uppercase letter ................. 10
 *   Lowercase letter ................. 10
 *   Number ........................... 10
 *   Special character ................ 10
 *   Not a common password ............ 10
 *   No sequences (1234, abcd) ........ 10
 *   No repeated characters (aaa) ..... 10
 *
 * HOW THE LEVEL IS CHOSEN
 *   WEAK     : score below 50, OR shorter than 8 characters, OR a common password
 *   STRONG   : score of 90+ AND no common/sequence/repeat problems
 *   MODERATE : everything in between
 *
 * The score is capped to match the level (Weak <= 49, Moderate <= 89), so the
 * strength meter never contradicts the label. Example: "Password123!" collects
 * many points, but it is a well-known password, so it is capped at 49 (Weak).
 *
 * SECURITY: The analysis is done locally. Nothing is saved, printed or sent.
 */
public class PasswordAnalyzer {

    public static final int MIN_LENGTH = 8;
    public static final int RECOMMENDED_LENGTH = 12;

    /** A run of this many consecutive characters (like 1234 or abcd) counts as a sequence. */
    private static final int SEQUENCE_LENGTH = 4;

    // ---------------------------------------------------------------
    // REGULAR EXPRESSIONS (Regex)
    // Pattern.compile() is done ONCE here because compiling is slow.
    // ---------------------------------------------------------------

    /** [A-Z] = any one capital letter. */
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");

    /** [a-z] = any one small letter. */
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");

    /** [0-9] = any one digit. */
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    /** [^A-Za-z0-9] = any character that is NOT a letter or digit (symbols, spaces...). */
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    /**
     * (.)\1{2,} = "any character, followed by the SAME character 2 or more times".
     * So it matches aaa, 111, !!!! ... (3 or more identical characters in a row).
     */
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{2,}", Pattern.CASE_INSENSITIVE);

    /** [^a-z]+$ = "one or more non-letters at the very END" (used to strip 123 or ! from the end). */
    private static final Pattern TRAILING_NON_LETTERS = Pattern.compile("[^a-z]+$");

    // ---------------------------------------------------------------
    // DATA LISTS
    // ---------------------------------------------------------------

    /** Very common passwords found in real data breaches (all lowercase). */
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "123456", "1234567", "12345678", "123456789", "1234567890", "12345",
            "111111", "000000", "123123", "654321", "666666", "121212",
            "password", "password1", "password123", "passw0rd", "p@ssw0rd", "p@ssword",
            "admin", "admin123", "administrator", "root", "toor", "guest", "user",
            "welcome", "welcome1", "welcome123", "login", "access", "secret",
            "qwerty", "qwerty123", "qwertyuiop", "abc123", "abcdef", "abcd1234",
            "iloveyou", "letmein", "monkey", "dragon", "football", "baseball",
            "master", "princess", "sunshine", "shadow", "superman", "batman",
            "trustno1", "changeme", "default", "test", "test123", "hello123",
            "freedom", "whatever", "starwars", "mustang", "ninja", "michael"
    ));

    /** Common keyboard-walk patterns (lowercase). */
    private static final List<String> KEYBOARD_PATTERNS = Arrays.asList(
            "qwert", "asdfg", "zxcvb", "qazwsx", "1q2w3e", "!@#$%"
    );

    // ---------------------------------------------------------------
    // MAIN METHOD
    // ---------------------------------------------------------------

    /**
     * Analyzes a password and returns the full result.
     *
     * @param password the password as a char[] (that is what JPasswordField gives us).
     *                 Why char[] and not String? A char[] can be wiped from memory
     *                 afterwards, a String cannot. The caller should wipe it.
     * @return an AnalysisResult (it contains NO copy of the password)
     */
    public AnalysisResult analyze(char[] password) {
        if (password == null) {
            password = new char[0];
        }

        int length = password.length;
        boolean hasText = length > 0;

        // CharBuffer.wrap lets regex read our char[] directly, without making a String copy.
        CharSequence text = CharBuffer.wrap(password);

        // For the word-list and pattern checks we need a lowercase String.
        // (Limitation: a Java String cannot be wiped; it lives until garbage collected.)
        String lower = new String(password).toLowerCase(Locale.ROOT);

        // ----- Run every check -----
        boolean longEnough     = length >= MIN_LENGTH;
        boolean recommendedLen = length >= RECOMMENDED_LENGTH;
        boolean hasUpper       = UPPERCASE.matcher(text).find();
        boolean hasLower       = LOWERCASE.matcher(text).find();
        boolean hasDigit       = DIGIT.matcher(text).find();
        boolean hasSpecial     = SPECIAL.matcher(text).find();

        // For an EMPTY password these "absence" checks must not earn free points.
        boolean notCommon      = hasText && !isCommonPassword(lower);
        boolean noSequence     = hasText && !hasSequentialPattern(lower);
        boolean noRepeats      = hasText && !REPEATED.matcher(text).find();

        // ----- Build the checklist + suggestions -----
        List<AnalysisResult.Requirement> requirements = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        addRequirement(requirements, suggestions, "At least 8 characters long", longEnough, 15,
                "Use at least 8 characters. Every extra character makes guessing much harder.");
        addRequirement(requirements, suggestions, "12 or more characters (recommended)", recommendedLen, 15,
                "Aim for 12+ characters. A passphrase of several random words is easy to remember and hard to crack.");
        addRequirement(requirements, suggestions, "Contains an uppercase letter (A-Z)", hasUpper, 10,
                "Add at least one uppercase letter (A-Z).");
        addRequirement(requirements, suggestions, "Contains a lowercase letter (a-z)", hasLower, 10,
                "Add at least one lowercase letter (a-z).");
        addRequirement(requirements, suggestions, "Contains a number (0-9)", hasDigit, 10,
                "Add at least one number (0-9).");
        addRequirement(requirements, suggestions, "Contains a special character (!@#$%...)", hasSpecial, 10,
                "Add at least one special character such as ! @ # $ % & *.");
        addRequirement(requirements, suggestions, "Is not a commonly used password", notCommon, 10,
                "Avoid common passwords and easy tweaks of them (like \"Password123!\"). Attackers try these first.");
        addRequirement(requirements, suggestions, "No predictable sequence (1234, abcd, qwerty)", noSequence, 10,
                "Avoid sequences such as 1234, abcd or qwerty. They are some of the first guesses an attacker makes.");
        addRequirement(requirements, suggestions, "No repeated characters (aaa, 111)", noRepeats, 10,
                "Avoid repeating the same character 3 or more times in a row.");

        // ----- Add up the score -----
        int score = 0;
        for (AnalysisResult.Requirement r : requirements) {
            if (r.isPassed()) {
                score += r.getPoints();
            }
        }

        // ----- Decide the strength level -----
        boolean predictable = hasText && (!notCommon || !noSequence || !noRepeats);
        AnalysisResult.Strength strength;
        if (score < 50 || !longEnough || !notCommon) {
            strength = AnalysisResult.Strength.WEAK;
            score = Math.min(score, 49);   // keep the meter consistent with the label
        } else if (score >= 90 && !predictable) {
            strength = AnalysisResult.Strength.STRONG;
        } else {
            strength = AnalysisResult.Strength.MODERATE;
            score = Math.min(score, 89);   // predictable passwords can never reach the Strong zone
        }

        // Nothing to fix? Give general good-practice tips instead.
        if (suggestions.isEmpty()) {
            suggestions.add("Excellent! This password meets every requirement.");
            suggestions.add("Use a different password for every account, ideally stored in a password manager.");
            suggestions.add("Turn on two-factor authentication (2FA) wherever it is available.");
        }

        return new AnalysisResult(requirements, score, strength, suggestions);
    }

    // ---------------------------------------------------------------
    // HELPER METHODS
    // ---------------------------------------------------------------

    /** Adds one checklist line, and a suggestion if the requirement failed. */
    private void addRequirement(List<AnalysisResult.Requirement> requirements, List<String> suggestions,
                                String description, boolean passed, int points, String suggestionIfFailed) {
        requirements.add(new AnalysisResult.Requirement(description, passed, points));
        if (!passed) {
            suggestions.add(suggestionIfFailed);
        }
    }

    /**
     * Is this a well-known password? We check three things:
     *   1. exact match                      ->  "password123"
     *   2. a word + digits/symbols at end   ->  "Admin@2024"   (base word is "admin")
     *   3. "leet speak" versions            ->  "P@ssw0rd"     (becomes "password")
     */
    private boolean isCommonPassword(String lower) {
        return matchesCommonList(lower) || matchesCommonList(undoLeetSpeak(lower));
    }

    private boolean matchesCommonList(String candidate) {
        if (COMMON_PASSWORDS.contains(candidate)) {
            return true;
        }
        // Remove digits/symbols from the END, then try again: "welcome2024!" -> "welcome"
        String baseWord = TRAILING_NON_LETTERS.matcher(candidate).replaceFirst("");
        return !baseWord.isEmpty() && COMMON_PASSWORDS.contains(baseWord);
    }

    /** Converts common look-alike substitutions back to letters: @ -> a, 0 -> o, 3 -> e, $ and 5 -> s. */
    private String undoLeetSpeak(String text) {
        return text.replace('@', 'a')
                   .replace('0', 'o')
                   .replace('3', 'e')
                   .replace('$', 's')
                   .replace('5', 's');
    }

    /**
     * Finds predictable patterns:
     *  - runs of 4+ consecutive digits or letters, going up or down (1234, abcd, 4321, dcba)
     *  - keyboard walks (qwert, asdfg ...)
     */
    private boolean hasSequentialPattern(String lower) {
        int runLength = 1;   // how long the current run is
        int direction = 0;   // +1 = going up (a,b,c), -1 = going down (c,b,a)

        for (int i = 1; i < lower.length(); i++) {
            char previous = lower.charAt(i - 1);
            char current = lower.charAt(i);
            int difference = current - previous;

            boolean bothDigits  = Character.isDigit(previous) && Character.isDigit(current);
            boolean bothLetters = Character.isLetter(previous) && Character.isLetter(current);

            if ((difference == 1 || difference == -1) && (bothDigits || bothLetters)) {
                if (difference == direction) {
                    runLength++;                 // the run continues
                } else {
                    runLength = 2;               // a new run starts (2 characters so far)
                    direction = difference;
                }
                if (runLength >= SEQUENCE_LENGTH) {
                    return true;
                }
            } else {
                runLength = 1;                   // the run is broken, start over
                direction = 0;
            }
        }

        for (String pattern : KEYBOARD_PATTERNS) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
