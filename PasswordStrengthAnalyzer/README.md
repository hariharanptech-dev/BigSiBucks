# Password Strength Analyzer

A beginner-friendly **cybersecurity desktop application** written in **Java (Swing)**.
It analyzes a password, classifies it as **Weak**, **Moderate** or **Strong**, shows which security requirements it meets (green tick) or misses (red cross), and gives tips to improve it.

Everything runs **locally on your computer**. Your password is never saved, logged or sent anywhere.

---

## Features

| # | Feature |
|---|---------|
| 1 | Password input with `JPasswordField` (characters are hidden) |
| 2 | Show / Hide password toggle |
| 3 | Length check: minimum 8 characters, 12+ recommended |
| 4 | Uppercase, lowercase, number and special-character checks (Regex) |
| 5 | Common-password detection (`password123`, `admin`, `123456`, `P@ssw0rd`, `Admin@2024` ...) |
| 6 | Predictable-pattern detection (`1234`, `abcd`, `4321`, `qwerty`, `aaa`) |
| 7 | Checklist with **green tick** (passed) / **red cross** (failed) |
| 8 | Classification: **Weak / Moderate / Strong** with a 0-100 score |
| 9 | Colour strength meter (red / amber / green) |
| 10 | Personalised improvement suggestions |
| 11 | **Analyze** and **Reset** buttons (Enter key also analyzes) |
| 12 | Dark-navy, green-accent, card-based UI |

## Technologies

* Java 11 or newer (developed and tested on Java 21)
* Java Swing (GUI)
* Regular Expressions (`java.util.regex`)
* Object-Oriented Programming
* **No external libraries**

---

## Project Structure

```
PasswordStrengthAnalyzer/
├── src/
│   ├── Main.java                  # Entry point - starts the app
│   ├── PasswordStrengthGUI.java   # GUI only (window, cards, meter, checklist)
│   ├── PasswordAnalyzer.java      # Logic only (all checks, scoring, suggestions)
│   └── AnalysisResult.java        # Data holder passed from logic to GUI
├── test/
│   └── PasswordAnalyzerTest.java  # Test cases (no JUnit needed)
├── run.sh                         # Compile + run (Linux / macOS)
├── run.bat                        # Compile + run (Windows)
└── README.md
```

---

## Setup

1. Install a **JDK** (not just a JRE) - version 11 or newer, for example from <https://adoptium.net>.
2. Check it works. Both commands must print a version:

```bash
java -version
javac -version
```

## Compile and Run

**Easiest way**

```bash
# Linux / macOS
chmod +x run.sh
./run.sh

# Windows: double-click run.bat, or in Command Prompt:
run.bat
```

**Manual way** (from inside the `PasswordStrengthAnalyzer` folder)

```bash
# 1. Compile every source file into a folder called "out"
javac -d out src/*.java

# 2. Run the program
java -cp out Main
```

On Windows PowerShell/CMD use `src\*.java` in step 1 if the forward slash does not work.

## Run the Tests

```bash
javac -d out src/*.java test/*.java
java -cp out PasswordAnalyzerTest
```

Expected last line: `Passed: 27   Failed: 0`

---

## How to Use

1. Type a password in the field.
2. Tick **Show password** if you want to see what you typed.
3. Click **Analyze** (or press Enter).
4. Read the strength label, the meter, the checklist and the suggestions.
5. Click **Reset** to wipe the field and all results.

---

## Test Cases

These are made-up example passwords. The score is capped so it always matches the label (Weak <= 49, Moderate <= 89).

### Weak

| Password | Score | Why |
|----------|:----:|-----|
| *(empty)* | 0 | Nothing entered |
| `123456` | 20 | Too short, common, sequence |
| `password123` | 49 | Very common password |
| `admin` | 30 | Too short, common |
| `abcdef` | 20 | Too short, sequence |
| `aaaaaaaaaa` | 45 | Repeated characters, only one character type |
| `Password123!` | 49 | Looks complex but is a famous password with a tweak |
| `P@ssw0rd` | 49 | "Leet speak" for `password` |
| `Admin@2024` | 49 | Common word `admin` + suffix |
| `Ab1!xyZ` | 49 | Only 7 characters (minimum is 8) |

### Moderate

| Password | Score | Why not Strong |
|----------|:----:|----------------|
| `mydogsname` | 55 | Only lowercase letters, under 12 characters |
| `Hello2024` | 75 | No special character, under 12 characters |
| `Tr0ub4dor&3` | 85 | Good mix, but only 11 characters |
| `Xk9#mP2$vL7@1234` | 89 | Strong otherwise, but contains the sequence `1234` |
| `Zq8!xxxxKp3#` | 89 | Strong otherwise, but repeats `xxxx` |

### Strong

| Password | Score |
|----------|:----:|
| `Xk9#mP2$vL7@qR4!` | 100 |
| `correct-Horse-battery-7-Staple!` | 100 |
| `Gr33n&Tiger*Jumps#Over9` | 100 |

---

## Code Explanation

### Architecture (separation of concerns)

```
   User
    |
    v
PasswordStrengthGUI  --- char[] --->  PasswordAnalyzer
 (shows things)       <-- AnalysisResult --  (thinks about things)
```

* **`PasswordAnalyzer`** has *no* GUI code. It receives a `char[]` and returns an `AnalysisResult`. Because it is independent, it can be tested without opening a window.
* **`AnalysisResult`** is a plain data object: a list of requirements (each passed/failed), a score, a strength level and a list of suggestions. It never contains the password.
* **`PasswordStrengthGUI`** builds the window and displays an `AnalysisResult`. It does *no* security checking itself.
* **`Main`** simply starts the GUI on Swing's Event Dispatch Thread.

### The Regular Expressions

| Regex | Meaning | Used for |
|-------|---------|----------|
| `[A-Z]` | one capital letter | uppercase check |
| `[a-z]` | one small letter | lowercase check |
| `[0-9]` | one digit | number check |
| `[^A-Za-z0-9]` | any character that is *not* a letter/digit | special-character check |
| `(.)\1{2,}` | any character followed by *the same character* 2+ more times | repeated characters (`aaa`, `111`) |
| `[^a-z]+$` | one or more non-letters at the end | strips `2024!` from `welcome2024!` before the common-password lookup |

### Scoring (total 100 points)

| Requirement | Points |
|-------------|:-----:|
| At least 8 characters | 15 |
| 12 or more characters | 15 |
| Uppercase letter | 10 |
| Lowercase letter | 10 |
| Number | 10 |
| Special character | 10 |
| Not a common password | 10 |
| No sequence (`1234`, `abcd`, `qwerty`) | 10 |
| No repeated characters (`aaa`) | 10 |

### Choosing the level

* **Weak** - score below 50, **or** shorter than 8 characters, **or** a common password (this rule overrides the score).
* **Strong** - score of 90 or more **and** no common / sequence / repeat problems.
* **Moderate** - everything else.

### Detecting common passwords

1. Convert the password to lowercase.
2. Exact match against a built-in list (`password123`, `qwerty`, `admin` ...).
3. Strip digits/symbols from the **end** and check again (`Admin@2024` -> `admin`).
4. Undo "leet speak" (`@`->a, `0`->o, `3`->e, `$`/`5`->s) and check again (`P@ssw0rd` -> `password`).

### Detecting predictable patterns

A loop walks through the password comparing each character with the one before it. If the difference is `+1` or `-1` (for example `a`->`b` or `4`->`3`) the "run" grows; if the run reaches 4 characters (`1234`, `abcd`, `dcba`) it is flagged. Keyboard walks such as `qwert`, `asdfg`, `zxcvb` are found with a simple `contains` check.

### GUI design

* Colours are constants at the top of `PasswordStrengthGUI` - change them to re-theme the app.
* Swing has no rounded cards, meter or icons, so small nested classes paint them with `Graphics2D`: `RoundedPanel`, `AccentButton`, `StrengthMeter`, `StatusIcon`. The tick/cross is drawn with lines, so it looks identical on every operating system.

---

## Security Design

| Requirement | How it is met |
|-------------|---------------|
| Do not save passwords | No file, database or preference API is used anywhere |
| Do not transmit passwords | No networking code is imported or used |
| Do not log passwords | No `System.out` calls in the application code (the test file prints only its own made-up samples) |
| Analyze locally | All logic runs inside the JVM on your machine |
| Clear on Reset | `resetForm()` empties the field, hides the password and clears all results |
| Use `JPasswordField` | Characters are masked by default; the password is read as `char[]` |
| Wipe memory | The `char[]` from the field is overwritten with `Arrays.fill(password, '\0')` in a `finally` block |

**Honest limitation:** for the lowercase word-list check the analyzer makes one temporary lowercase `String`. Java `String`s cannot be wiped and remain in memory until garbage-collected. This is acceptable for a learning project; a production tool would avoid even this copy.

## Limitations and Ideas to Extend

* The common-password list is small (about 60 entries). Real tools use lists with millions of entries, or the HaveIBeenPwned *k-anonymity* API (this would need networking, which this project intentionally avoids).
* Strength is rule-based, not a true entropy or `zxcvbn`-style estimate.
* Ideas: live analysis while typing, a random strong-password generator, loading a larger wordlist from a file, estimated "time to crack", dark/light theme switch.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `javac: command not found` | You installed a JRE only. Install a **JDK**. |
| `Could not find or load main class Main` | Run from the project folder and use `java -cp out Main`. |
| `HeadlessException` on Linux | You are on a server without a display. Run on a desktop, or install a non-headless JRE (e.g. `openjdk-21-jre`). |

## Disclaimer

This is an educational tool. A "Strong" rating means the password passes these rules; it is not a guarantee. Never reuse passwords across sites, and use a password manager with two-factor authentication.
