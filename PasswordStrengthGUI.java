import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/**
 * PasswordStrengthGUI - the window (View) of the application.
 *
 * This class ONLY builds the screen and shows results. All password logic
 * lives in PasswordAnalyzer. This is called "separation of concerns".
 *
 * SECURITY:
 *  - Uses JPasswordField (hides the characters).
 *  - Copies of the password (char[]) are wiped with Arrays.fill() right after use.
 *  - Nothing is written to a file, the console, or the network.
 */
public class PasswordStrengthGUI extends JFrame {

    // ------------------------------------------------------------------
    // THEME: colours and fonts (change them here to re-style the whole app)
    // ------------------------------------------------------------------
    private static final Color NAVY_BACKGROUND = new Color(0x0A192F);
    private static final Color CARD_BACKGROUND = new Color(0x112240);
    private static final Color CARD_BORDER     = new Color(0x1F3A5F);
    private static final Color FIELD_BACKGROUND = new Color(0x0B1730);
    private static final Color ACCENT_GREEN    = new Color(0x00E676);
    private static final Color TEXT_PRIMARY    = new Color(0xE6F1FF);
    private static final Color TEXT_MUTED      = new Color(0x8892B0);
    private static final Color WEAK_RED        = new Color(0xFF5252);
    private static final Color MODERATE_AMBER  = new Color(0xFFB300);

    private static final String FONT_FAMILY = Font.SANS_SERIF; // works on every operating system

    // ------------------------------------------------------------------
    // Components we need to update later
    // ------------------------------------------------------------------
    private final PasswordAnalyzer analyzer = new PasswordAnalyzer(); // the logic class

    private final JPasswordField passwordField = new JPasswordField();
    private final JCheckBox showPasswordCheckBox = new JCheckBox("Show password");
    private final JLabel strengthLabel = new JLabel("Not analyzed yet");
    private final JLabel scoreLabel = new JLabel("Score: -- / 100");
    private final StrengthMeter strengthMeter = new StrengthMeter();
    private final JPanel checklistPanel = new JPanel();
    private final JTextArea suggestionsArea = new JTextArea();

    /** The normal "dots" character, remembered so we can restore it after "Show password". */
    private final char defaultEchoChar = passwordField.getEchoChar();

    // ------------------------------------------------------------------
    // Constructor: builds the whole window
    // ------------------------------------------------------------------
    public PasswordStrengthGUI() {
        super("Password Strength Analyzer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Outer panel with dark navy background and padding around the edges
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(NAVY_BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(buildInputCard(), BorderLayout.NORTH);
        body.add(buildResultsArea(), BorderLayout.CENTER);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);

        setContentPane(root);
        setSize(980, 780);
        setMinimumSize(new Dimension(880, 720));
        setLocationRelativeTo(null); // centre on screen

        showInitialState();
    }

    // ------------------------------------------------------------------
    // BUILDING THE SCREEN
    // ------------------------------------------------------------------

    /** Title + subtitle at the top. */
    private JPanel buildHeader() {
        JLabel title = new JLabel("PASSWORD STRENGTH ANALYZER");
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        title.setForeground(ACCENT_GREEN);

        JLabel subtitle = new JLabel("Check how well your password resists guessing attacks  \u2022  "
                + "100% local analysis: nothing is saved or sent anywhere");
        subtitle.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    /** Card 1: password field, show/hide toggle, Analyze and Reset buttons. */
    private RoundedPanel buildInputCard() {
        RoundedPanel card = createCard("ENTER PASSWORD");

        // --- password field styling ---
        passwordField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        passwordField.setBackground(FIELD_BACKGROUND);
        passwordField.setForeground(TEXT_PRIMARY);
        passwordField.setCaretColor(ACCENT_GREEN);
        passwordField.setPreferredSize(new Dimension(100, 46));
        setFieldBorder(CARD_BORDER);
        // Green border while the field has focus
        passwordField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { setFieldBorder(ACCENT_GREEN); }
            @Override public void focusLost(FocusEvent e)   { setFieldBorder(CARD_BORDER); }
        });
        // Pressing ENTER inside the field also analyzes
        passwordField.addActionListener(e -> analyzePassword());

        // --- show / hide toggle ---
        showPasswordCheckBox.setOpaque(false);
        showPasswordCheckBox.setForeground(TEXT_PRIMARY);
        showPasswordCheckBox.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        showPasswordCheckBox.setFocusPainted(false);
        showPasswordCheckBox.addActionListener(e -> togglePasswordVisibility());

        // --- buttons ---
        AccentButton analyzeButton = new AccentButton("Analyze", true);
        AccentButton resetButton = new AccentButton("Reset", false);
        analyzeButton.addActionListener(e -> analyzePassword());
        resetButton.addActionListener(e -> resetForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(resetButton);
        buttons.add(analyzeButton);

        JPanel controlsRow = new JPanel(new BorderLayout());
        controlsRow.setOpaque(false);
        controlsRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        controlsRow.add(showPasswordCheckBox, BorderLayout.WEST);
        controlsRow.add(buttons, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(passwordField, BorderLayout.NORTH);
        content.add(controlsRow, BorderLayout.CENTER);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** Two columns: [meter + suggestions] on the left, [checklist] on the right. */
    private JPanel buildResultsArea() {
        // ----- Left column -----
        RoundedPanel strengthCard = createCard("PASSWORD STRENGTH");
        strengthLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        strengthLabel.setForeground(TEXT_MUTED);
        scoreLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        scoreLabel.setForeground(TEXT_MUTED);

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        labelRow.add(strengthLabel, BorderLayout.WEST);
        labelRow.add(scoreLabel, BorderLayout.EAST);

        JPanel strengthContent = new JPanel(new BorderLayout(0, 14));
        strengthContent.setOpaque(false);
        strengthContent.add(labelRow, BorderLayout.NORTH);
        strengthContent.add(strengthMeter, BorderLayout.CENTER);
        strengthCard.add(strengthContent, BorderLayout.CENTER);

        RoundedPanel suggestionsCard = createCard("SUGGESTIONS");
        suggestionsArea.setEditable(false);
        suggestionsArea.setLineWrap(true);
        suggestionsArea.setWrapStyleWord(true);
        suggestionsArea.setOpaque(false);
        suggestionsArea.setForeground(TEXT_PRIMARY);
        suggestionsArea.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(suggestionsArea);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        suggestionsCard.add(scrollPane, BorderLayout.CENTER);

        JPanel leftColumn = new JPanel(new BorderLayout(0, 18));
        leftColumn.setOpaque(false);
        leftColumn.add(strengthCard, BorderLayout.NORTH);
        leftColumn.add(suggestionsCard, BorderLayout.CENTER);

        // ----- Right column -----
        RoundedPanel checklistCard = createCard("SECURITY CHECKLIST");
        checklistPanel.setOpaque(false);
        checklistPanel.setLayout(new BoxLayout(checklistPanel, BoxLayout.Y_AXIS));
        JPanel checklistWrapper = new JPanel(new BorderLayout());
        checklistWrapper.setOpaque(false);
        checklistWrapper.add(checklistPanel, BorderLayout.NORTH); // NORTH keeps rows from stretching
        checklistCard.add(checklistWrapper, BorderLayout.CENTER);

        JPanel columns = new JPanel(new GridLayout(1, 2, 18, 0));
        columns.setOpaque(false);
        columns.add(leftColumn);
        columns.add(checklistCard);
        return columns;
    }

    /** Creates an empty rounded "card" with a small green title. Content goes in BorderLayout.CENTER. */
    private RoundedPanel createCard(String title) {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        titleLabel.setForeground(ACCENT_GREEN);
        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }

    private void setFieldBorder(Color lineColor) {
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lineColor, 2, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    // ------------------------------------------------------------------
    // BUTTON ACTIONS
    // ------------------------------------------------------------------

    /** Show or hide the typed characters. */
    private void togglePasswordVisibility() {
        if (showPasswordCheckBox.isSelected()) {
            passwordField.setEchoChar((char) 0);       // (char) 0 means "show the real characters"
        } else {
            passwordField.setEchoChar(defaultEchoChar); // back to dots
        }
    }

    /** Called when the user clicks Analyze (or presses Enter). */
    private void analyzePassword() {
        char[] password = passwordField.getPassword();
        try {
            if (password.length == 0) {
                showEmptyPasswordMessage();
                return;
            }
            AnalysisResult result = analyzer.analyze(password); // all the work happens locally
            displayResult(result);
        } finally {
            // Always wipe our copy of the password, even if something went wrong.
            Arrays.fill(password, '\0');
        }
    }

    /** Called when the user clicks Reset: clears the password and every result. */
    private void resetForm() {
        passwordField.setText("");                  // clear the field
        showPasswordCheckBox.setSelected(false);    // hide the password again
        passwordField.setEchoChar(defaultEchoChar);
        showInitialState();
        passwordField.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // UPDATING THE SCREEN
    // ------------------------------------------------------------------

    /** Screen before anything has been analyzed (also used by Reset). */
    private void showInitialState() {
        strengthLabel.setText("Not analyzed yet");
        strengthLabel.setForeground(TEXT_MUTED);
        scoreLabel.setText("Score: -- / 100");
        strengthMeter.setLevel(0, TEXT_MUTED);
        suggestionsArea.setText("Type a password and click \"Analyze\" to see personalised tips.");
        showChecklistPlaceholder("Your results will appear here after you click \"Analyze\".");
    }

    private void showEmptyPasswordMessage() {
        strengthLabel.setText("No password entered");
        strengthLabel.setForeground(TEXT_MUTED);
        scoreLabel.setText("Score: -- / 100");
        strengthMeter.setLevel(0, TEXT_MUTED);
        suggestionsArea.setText("Please type a password first.");
        showChecklistPlaceholder("Enter a password to see the checklist.");
    }

    /** Shows the results of an analysis in every card. */
    private void displayResult(AnalysisResult result) {
        Color levelColor = colorFor(result.getStrength());

        strengthLabel.setText(result.getStrength().getLabel());
        strengthLabel.setForeground(levelColor);
        scoreLabel.setText("Score: " + result.getScore() + " / 100");
        strengthMeter.setLevel(result.getScore(), levelColor);

        // --- checklist: green tick or red cross per requirement ---
        checklistPanel.removeAll();
        for (AnalysisResult.Requirement requirement : result.getRequirements()) {
            checklistPanel.add(createChecklistRow(requirement));
        }
        checklistPanel.revalidate();
        checklistPanel.repaint();

        // --- suggestions ---
        StringBuilder text = new StringBuilder();
        for (String suggestion : result.getSuggestions()) {
            text.append("\u2022 ").append(suggestion).append("\n\n");
        }
        suggestionsArea.setText(text.toString().trim());
        suggestionsArea.setCaretPosition(0); // scroll back to the top
    }

    /** One row of the checklist: [icon] description. */
    private JPanel createChecklistRow(AnalysisResult.Requirement requirement) {
        JLabel icon = new JLabel(new StatusIcon(requirement.isPassed()));

        JLabel text = new JLabel(requirement.getDescription());
        text.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        text.setForeground(requirement.isPassed() ? TEXT_PRIMARY : new Color(0xFFB4B4));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        row.add(icon, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private void showChecklistPlaceholder(String message) {
        JLabel placeholder = new JLabel("<html><body style='width:230px'>" + message + "</body></html>");
        placeholder.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        placeholder.setForeground(TEXT_MUTED);
        checklistPanel.removeAll();
        checklistPanel.add(placeholder);
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private Color colorFor(AnalysisResult.Strength strength) {
        switch (strength) {
            case WEAK:     return WEAK_RED;
            case MODERATE: return MODERATE_AMBER;
            default:       return ACCENT_GREEN;
        }
    }

    // ==================================================================
    // SMALL CUSTOM COMPONENTS (nested classes)
    // Swing has no "rounded card" or "meter", so we paint our own.
    // ==================================================================

    /** A panel with rounded corners, a card colour and a thin border. */
    private static class RoundedPanel extends JPanel {
        RoundedPanel() {
            setOpaque(false); // we paint the background ourselves
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A rounded button: green filled (primary) or outlined (secondary). */
    private static class AccentButton extends JButton {
        private final boolean primary;

        AccentButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
            setForeground(primary ? NAVY_BACKGROUND : TEXT_PRIMARY);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 28, 10, 28));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = getModel().isRollover() || getModel().isPressed();
            if (primary) {
                g2.setColor(hover ? ACCENT_GREEN.brighter() : ACCENT_GREEN);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            } else {
                if (hover) {
                    g2.setColor(new Color(0x1B2F52));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                }
                g2.setColor(hover ? ACCENT_GREEN : CARD_BORDER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g); // draws the button text on top
        }
    }

    /** The coloured horizontal strength bar (0 - 100). */
    private static class StrengthMeter extends JComponent {
        private int score = 0;
        private Color fillColor = TEXT_MUTED;

        StrengthMeter() {
            setPreferredSize(new Dimension(100, 18));
        }

        void setLevel(int score, Color fillColor) {
            this.score = score;
            this.fillColor = fillColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Dark track
            g2.setColor(FIELD_BACKGROUND);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

            // Coloured fill, clipped to the rounded track shape
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, h, h));
            g2.setColor(fillColor);
            g2.fillRect(0, 0, (int) (w * score / 100.0), h);

            // Thin dividers marking the Weak / Moderate / Strong zones (at 50 and 90)
            g2.setColor(CARD_BACKGROUND);
            g2.fillRect((int) (w * 0.50), 0, 2, h);
            g2.fillRect((int) (w * 0.90), 0, 2, h);
            g2.dispose();
        }
    }

    /** A round green tick or red cross icon, painted with code (works on every OS and font). */
    private static class StatusIcon implements Icon {
        private static final int SIZE = 22;
        private final boolean passed;

        StatusIcon(boolean passed) {
            this.passed = passed;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(passed ? ACCENT_GREEN : WEAK_RED);
            g2.fillOval(x, y, SIZE, SIZE);

            g2.setColor(NAVY_BACKGROUND);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (passed) {
                // tick: down-right, then up-right
                g2.drawLine(x + 6, y + 11, x + 10, y + 15);
                g2.drawLine(x + 10, y + 15, x + 16, y + 7);
            } else {
                // cross: two diagonal lines
                g2.drawLine(x + 7, y + 7, x + 15, y + 15);
                g2.drawLine(x + 15, y + 7, x + 7, y + 15);
            }
            g2.dispose();
        }

        @Override public int getIconWidth()  { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }
}
