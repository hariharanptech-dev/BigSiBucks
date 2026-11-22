import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AnalysisResult - a simple "data holder" (model) class.
 *
 * The PasswordAnalyzer fills one of these objects and hands it to the GUI.
 * The GUI only DISPLAYS the data; it never does any security logic itself.
 *
 * SECURITY NOTE: This class never stores the password text. It only stores
 * yes/no results, a score and advice messages.
 */
public class AnalysisResult {

    /** The three possible strength levels. */
    public enum Strength {
        WEAK("Weak"),
        MODERATE("Moderate"),
        STRONG("Strong");

        private final String label;

        Strength(String label) {
            this.label = label;
        }

        /** Human-friendly text, e.g. "Weak". */
        public String getLabel() {
            return label;
        }
    }

    /** One line of the checklist, e.g. "Contains a number" -> passed or failed. */
    public static class Requirement {
        private final String description;
        private final boolean passed;
        private final int points; // how many score points this requirement is worth

        public Requirement(String description, boolean passed, int points) {
            this.description = description;
            this.passed = passed;
            this.points = points;
        }

        public String getDescription() { return description; }
        public boolean isPassed()      { return passed; }
        public int getPoints()         { return points; }
    }

    private final List<Requirement> requirements;
    private final int score;            // 0 - 100
    private final Strength strength;
    private final List<String> suggestions;

    public AnalysisResult(List<Requirement> requirements, int score,
                          Strength strength, List<String> suggestions) {
        // Copy the lists so nobody can change them later (immutability = safer code).
        this.requirements = Collections.unmodifiableList(new ArrayList<>(requirements));
        this.score = score;
        this.strength = strength;
        this.suggestions = Collections.unmodifiableList(new ArrayList<>(suggestions));
    }

    public List<Requirement> getRequirements() { return requirements; }
    public int getScore()                      { return score; }
    public Strength getStrength()              { return strength; }
    public List<String> getSuggestions()       { return suggestions; }
}
