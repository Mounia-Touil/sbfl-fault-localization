// Class: SWE6673
// Name: Mounia Touil
// Term: Spring 2026
//
// This is my SBFL project. The goal is to find which methods in JFreeChart
// are most likely to have a bug, by looking at which methods show up
// in failing tests more than passing tests.
//
// Command to compile and run:
//   javac SBFLProject.java && java SBFLProject NewCoverageData
//
// Input:  the NewCoverageData folder with all the .txt coverage files
// Output: a ranked list of methods with their suspiciousness scores

import java.io.*;
import java.util.*;
import java.io.OutputStreamWriter;

public class SBFLProject {

    // I picked these 10 test files to be the "failed" tests
    // because all the tests in the data are marked as passed,
    // so I had to manually choose some to fail
    private static final Set<Integer> FAILED_FILE_NUMBERS = new HashSet<>(
        Arrays.asList(1, 5, 20, 50, 100, 200, 500, 750, 1000, 1039)
    );

    // I only want to show the top 10 methods in each formula section
    private static final int TOP_N = 10;

    // this class just holds all the data for one method
    // like its name, how many tests covered it, and its scores
    static class MethodData {
        String name;
        int ef;  // number of failed tests that used this method
        int ep;  // number of passed tests that used this method

        double tarantula;
        double sbi;
        double jaccard;
        double ochiai;

        MethodData(String name) {
            this.name = name;
        }
    }

    // this is the comparator I use with Collections.sort()
    // it just sorts by score from highest to lowest
    static class ScoreComparator implements Comparator<MethodData> {
        private final String formula;

        ScoreComparator(String formula) {
            this.formula = formula;
        }

        @Override
        public int compare(MethodData a, MethodData b) {
            // flip a and b so we get highest first
            return Double.compare(getScore(b), getScore(a));
        }

        // just a helper so I can grab the right score by formula name
        private double getScore(MethodData m) {
            switch (formula) {
                case "tarantula": return m.tarantula;
                case "sbi":       return m.sbi;
                case "jaccard":   return m.jaccard;
                case "ochiai":    return m.ochiai;
                default:          return m.ochiai;
            }
        }
    }

    // this reads all the .txt files and fills up the ef and ep maps
    // ef = how many failed tests ran a method
    // ep = how many passed tests ran a method
    static void readCoverageFiles(
            File folder,
            Map<String, Integer> methodEf,
            Map<String, Integer> methodEp,
            int[] totals
    ) throws IOException {

        // grab all the txt files in the folder
        File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("no txt files found in: " + folder.getAbsolutePath());
            System.exit(1);
        }

        int totalFailed = 0;
        int totalPassed = 0;

        for (File f : files) {

            // get the number out of the file name like 42.txt -> 42
            int fileNumber;
            try {
                fileNumber = Integer.parseInt(f.getName().replace(".txt", ""));
            } catch (NumberFormatException e) {
                continue;
            }

            // check if this is one of my manually failed tests
            boolean isFailed = FAILED_FILE_NUMBERS.contains(fileNumber);

            // read the file line by line
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                }
            }

            if (lines.isEmpty()) continue;

            // first line is the test name, rest are the methods it ran
            Set<String> coveredMethods = new HashSet<>();
            for (int i = 1; i < lines.size(); i++) {
                String method = lines.get(i).trim();
                if (!method.isEmpty()) {
                    coveredMethods.add(method);
                }
            }

            // count up ef or ep for each method depending on pass/fail
            if (isFailed) {
                totalFailed++;
                for (String m : coveredMethods) {
                    methodEf.put(m, methodEf.getOrDefault(m, 0) + 1);
                }
            } else {
                totalPassed++;
                for (String m : coveredMethods) {
                    methodEp.put(m, methodEp.getOrDefault(m, 0) + 1);
                }
            }
        }

        totals[0] = totalFailed;
        totals[1] = totalPassed;
    }

    // the 4 formulas from the slides - each one calculates suspiciousness differently

    // tarantula formula
    static double tarantula(int ef, int ep, int totalFailed, int totalPassed) {
        if (totalFailed == 0 || totalPassed == 0) return 0.0;
        double failRatio   = (double) ef / totalFailed;
        double passRatio   = (double) ep / totalPassed;
        double denominator = failRatio + passRatio;
        if (denominator == 0.0) return 0.0;
        return failRatio / denominator;
    }

    // sbi formula
    static double sbi(int ef, int ep) {
        double denominator = ef + ep;
        if (denominator == 0.0) return 0.0;
        return ef / denominator;
    }

    // jaccard formula
    static double jaccard(int ef, int ep, int totalFailed) {
        double denominator = totalFailed + ep;
        if (denominator == 0.0) return 0.0;
        return (double) ef / denominator;
    }

    // ochiai formula - uses square root
    static double ochiai(int ef, int ep, int totalFailed) {
        double denominator = Math.sqrt((double) totalFailed * (ef + ep));
        if (denominator == 0.0) return 0.0;
        return ef / denominator;
    }

    // prints the top N ranked methods for whichever formula we pass in
    // uses Collections.sort with my ScoreComparator to sort first
    static void printRanking(List<MethodData> methods, String formula,
                             PrintWriter out, int topN) {

        // copy the list before sorting so i dont mess up the original
        List<MethodData> sorted = new ArrayList<>(methods);
        Collections.sort(sorted, new ScoreComparator(formula));

        String line = "-".repeat(120);
        out.println();
        out.println(line);
        out.printf("  TOP %d METHODS - %s FORMULA%n", topN, formula.toUpperCase());
        out.println(line);
        out.printf("  %-6s  %-10s  %-6s  %-6s  %s%n", "Rank", "Score", "ef", "ep", "Method");
        out.printf("  %-6s  %-10s  %-6s  %-6s  %s%n",
                   "------", "----------", "------", "------", "-".repeat(80));

        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            MethodData m = sorted.get(i);
            out.printf("  %-6d  %-10.6f  %-6d  %-6d  %s%n",
                       i + 1, getScore(m, formula), m.ef, m.ep, m.name);
        }
    }

    // helper method to return the score for a given formula
    static double getScore(MethodData m, String formula) {
        switch (formula) {
            case "tarantula": return m.tarantula;
            case "sbi":       return m.sbi;
            case "jaccard":   return m.jaccard;
            case "ochiai":    return m.ochiai;
            default:          return m.ochiai;
        }
    }

    // main method - runs everything in order
    public static void main(String[] args) throws IOException {

        // take the folder path from the command line
        String folderPath = (args.length > 0) ? args[0] : "NewCoverageData";
        File folder = new File(folderPath);

        if (!folder.isDirectory()) {
            System.out.println("cant find folder: " + folderPath);
            System.out.println("run like this: java SBFLProject NewCoverageData");
            System.exit(1);
        }

        System.out.println("Step 1: reading all the coverage files...");

        // two maps to track how many failed vs passed tests covered each method
        Map<String, Integer> methodEf = new HashMap<>();
        Map<String, Integer> methodEp = new HashMap<>();
        int[] totals = new int[2]; // index 0 = failed, index 1 = passed

        readCoverageFiles(folder, methodEf, methodEp, totals);

        int totalFailed = totals[0];
        int totalPassed = totals[1];

        System.out.println("  total tests: " + (totalFailed + totalPassed)
                           + "  failed: " + totalFailed
                           + "  passed: " + totalPassed);
        System.out.println("  tests i set as failed: " + FAILED_FILE_NUMBERS);

        System.out.println("Step 2: calculating the suspiciousness scores...");

        // collect all unique method names from both maps
        Set<String> allMethods = new HashSet<>();
        allMethods.addAll(methodEf.keySet());
        allMethods.addAll(methodEp.keySet());

        // go through each method and compute all 4 scores
        List<MethodData> methods = new ArrayList<>();
        for (String name : allMethods) {
            int ef = methodEf.getOrDefault(name, 0);
            int ep = methodEp.getOrDefault(name, 0);

            // if ef is 0 the method never showed up in a failing test so skip it
            if (ef == 0) continue;

            MethodData md  = new MethodData(name);
            md.ef          = ef;
            md.ep          = ep;
            md.tarantula   = tarantula(ef, ep, totalFailed, totalPassed);
            md.sbi         = sbi(ef, ep);
            md.jaccard     = jaccard(ef, ep, totalFailed);
            md.ochiai      = ochiai(ef, ep, totalFailed);
            methods.add(md);
        }

        System.out.println("  scored " + methods.size() + " methods total");

        System.out.println("Step 3: writing the report...");

        // save results to a file and also print to screen
        String outputFile = "sbfl_results.txt";
        PrintWriter fileOut    = new PrintWriter(new FileWriter(outputFile));
        PrintWriter consoleOut = new PrintWriter(new OutputStreamWriter(System.out), true);

        String header =
            "=".repeat(120) + "\n" +
            "  SBFL REPORT - Fault Localization for JFreeChart\n" +
            "  Class: SWE6673  |  Name: Mounia Touil  |  Term: Spring 2026\n" +
            "  Total tests : " + (totalFailed + totalPassed) + "\n" +
            "  Failed tests: " + totalFailed + "  (files: " + FAILED_FILE_NUMBERS + ")\n" +
            "  Passed tests: " + totalPassed + "\n" +
            "  Methods scored: " + methods.size() + "\n" +
            "=".repeat(120);

        consoleOut.println(header);
        fileOut.println(header);

        // print top 10 for each formula
        String[] formulae = {"ochiai", "tarantula", "sbi", "jaccard"};
        for (String formula : formulae) {
            printRanking(methods, formula, consoleOut, TOP_N);
            printRanking(methods, formula, fileOut, TOP_N);
        }

        // print the full list sorted by ochiai at the bottom
        List<MethodData> fullList = new ArrayList<>(methods);
        Collections.sort(fullList, new ScoreComparator("ochiai"));

        String fullHeader =
            "\n" + "=".repeat(120) + "\n" +
            "  FULL RANKED LIST - OCHIAI (all " + fullList.size() + " methods)\n" +
            "=".repeat(120);

        consoleOut.println(fullHeader);
        fileOut.println(fullHeader);

        String colHeader = String.format("  %-6s  %-10s  %-10s  %-10s  %-10s  %-6s  %-6s  %s",
            "Rank", "Ochiai", "Tarantula", "SBI", "Jaccard", "ef", "ep", "Method");
        consoleOut.println(colHeader);
        fileOut.println(colHeader);

        for (int i = 0; i < fullList.size(); i++) {
            MethodData m = fullList.get(i);
            String row = String.format(
                "  %-6d  %-10.6f  %-10.6f  %-10.6f  %-10.6f  %-6d  %-6d  %s",
                i + 1, m.ochiai, m.tarantula, m.sbi, m.jaccard, m.ef, m.ep, m.name);
            consoleOut.println(row);
            fileOut.println(row);
        }

        fileOut.flush();
        fileOut.close();

        System.out.println("\nDone! saved results to: " + outputFile);
    }
}
