# Spectrum-Based Fault Localization for JFreeChart

A Java tool that reads test-coverage data from 2,262 tests and ranks 888 methods by how "suspicious" they are, using four fault-localization formulas.

---

## Overview

When a test fails, a developer must find *where* the bug is. In a large program this takes a long time.

**Spectrum-Based Fault Localization (SBFL)** is a method that helps. The idea is simple:

> A method that runs in **failing** tests a lot, but rarely in **passing** tests, is more likely to contain the bug.

For each method, we count two numbers:

- **ef** = number of failed tests that ran the method
- **ep** = number of passed tests that ran the method

Then a formula turns these counts into a **suspiciousness score**. The developer checks the top-ranked methods first.

The program used is **JFreeChart**, an open-source Java library for drawing charts.

## What I did (solo project)

- Wrote `SBFLProject.java`, which reads every coverage file in `NewCoverageData/`.
- Counted `ef` and `ep` for every method.
- Implemented **four formulas**: Tarantula, SBI, Jaccard, and Ochiai.
- Sorted the methods with `Collections.sort()` and a custom `Comparator`, and printed the **top 10** for each formula.
- Saved the full report to `sbfl_results.txt`.
- Also ran a separate **mutation testing** experiment on JFreeChart (see below).

### An honest note about the data

In the course data, **every test was marked as passed** – there were no failing tests.
SBFL needs failing tests, so I chose 10 tests and marked them as failed: files 1, 5, 20, 50, 100, 200, 500, 750, 1000, and 1039.
This gives 10 failed and 2,252 passed tests. Because the failures are not from a real bug, the results are a **simulation** that shows how SBFL works, not real bug detection.

## The four formulas

`F` = total failed tests, `P` = total passed tests.

| Formula | Equation | Simple meaning |
|---|---|---|
| **Tarantula** | (ef/F) ÷ ((ef/F) + (ep/P)) | Compares the *percent* of failed tests vs. percent of passed tests that use the method. |
| **SBI** | ef ÷ (ef + ep) | Of all tests that ran the method, what share failed? |
| **Jaccard** | ef ÷ (F + ep) | Overlap between "tests that ran the method" and "tests that failed". |
| **Ochiai** | ef ÷ √(F × (ef + ep)) | Like Jaccard, but uses a square root. Often the most accurate in research. |

All scores go from 0 (not suspicious) to 1 (very suspicious).

Code for Ochiai and Tarantula:

```java
static double ochiai(int ef, int ep, int totalFailed) {
    double denominator = Math.sqrt((double) totalFailed * (ef + ep));
    if (denominator == 0.0) return 0.0;
    return ef / denominator;
}

static double tarantula(int ef, int ep, int totalFailed, int totalPassed) {
    if (totalFailed == 0 || totalPassed == 0) return 0.0;
    double failRatio = (double) ef / totalFailed;
    double passRatio = (double) ep / totalPassed;
    double denominator = failRatio + passRatio;
    if (denominator == 0.0) return 0.0;
    return failRatio / denominator;
}
```

Every formula checks for **division by zero** first, so the program never crashes on empty counts.

## Results

| Item | Value |
|---|---|
| Coverage files (tests) | 2,262 |
| Failed / passed tests | 10 / 2,252 |
| Methods scored (ef > 0) | 888 |

The top-ranked methods had **ef = 1 and ep = 0**: they ran in one failing test and never in a passing test.
Examples: `GanttRenderer.drawTask`, `GanttRenderer.drawItem`, `CategoryLineAnnotation.setCategory2`.

| Formula | Top score |
|---|---|
| Tarantula | 1.000 |
| SBI | 1.000 |
| Ochiai | 0.316 |
| Jaccard | 0.100 |

The numbers are different, but **all four formulas agreed on the same top methods**. That is a good sign that the ranking is stable.

## Mutation testing (extra experiment)

**Mutation testing** makes many small changes ("mutants") to the code, like changing `<` to `<=`, and checks if the tests notice. A test suite that "kills" more mutants is stronger.

I ran **PIT** (a mutation testing tool for Java) on JFreeChart with Maven and Java 11:

| Metric | Value |
|---|---|
| Mutants generated | 110,818 |
| Mutants killed | 36,667 |
| **Mutation score** | **≈ 33%** |
| Line coverage | 55% |
| Run time | about 2 hours |

**Lesson:** 55% of lines were *executed* by tests, but only 33% of mutants were *caught*. Running code is not the same as checking its output. Coverage alone does not prove a test suite is good.

## Tech used

Java (BufferedReader, HashMap, Collections.sort, Comparator) · JFreeChart coverage data · PIT mutation testing · Maven

## Repository contents

```
SBFLProject.java       the SBFL tool
NewCoverageData/       2,262 coverage files (one per test)
sbfl_results.txt       output: top 10 per formula + full ranked list
docs/                  proposal, SBFL report, mutation testing report
```

## How to run

Requires Java 11 or newer.

```bash
javac SBFLProject.java
java SBFLProject NewCoverageData
```

The program prints the ranking and writes `sbfl_results.txt`.

Each coverage file has the test name on the first line, and then one covered method per line.

## What I learned

- How SBFL turns simple test counts into a ranked list of suspicious code.
- How different formulas weigh the same data in different ways, and why Ochiai is popular.
- That good data matters: without real failing tests, results are only a simulation. I documented this limit instead of hiding it.
- That code coverage and test quality are different things (mutation score vs. line coverage).
- Clean Java design: a small data class, a reusable comparator, and safe math.

**Course:** SWE 6673 Software Testing and Verification · Kennesaw State University · Spring 2026
