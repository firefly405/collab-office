package com.collaboffice.doc.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FormulaEngine {

    private static final Pattern CELL_REF = Pattern.compile("[A-Z]+\\d+");
    private static final Pattern RANGE_REF = Pattern.compile("([A-Z]+\\d+):([A-Z]+\\d+)");

    public String calculate(String formula, Map<String, String> cellValues) {
        if (formula == null || !formula.startsWith("=")) return formula;
        String expr = formula.substring(1).trim();
        try {
            return evaluate(expr, cellValues);
        } catch (Exception e) {
            return "#ERROR!";
        }
    }

    private String evaluate(String expr, Map<String, String> values) {
        expr = expr.trim();

        if (expr.toUpperCase().startsWith("SUM(")) {
            return evaluateAggregate(expr, values, "SUM");
        }
        if (expr.toUpperCase().startsWith("AVERAGE(")) {
            return evaluateAggregate(expr, values, "AVERAGE");
        }
        if (expr.toUpperCase().startsWith("MAX(")) {
            return evaluateAggregate(expr, values, "MAX");
        }
        if (expr.toUpperCase().startsWith("MIN(")) {
            return evaluateAggregate(expr, values, "MIN");
        }
        if (expr.toUpperCase().startsWith("COUNT(")) {
            return evaluateAggregate(expr, values, "COUNT");
        }
        if (expr.toUpperCase().startsWith("IF(")) {
            return evaluateIf(expr, values);
        }
        if (expr.toUpperCase().startsWith("CONCAT(")) {
            return evaluateConcat(expr, values);
        }
        if (expr.toUpperCase().startsWith("TRIM(")) {
            return evaluateTrim(expr, values);
        }
        if (expr.toUpperCase().startsWith("UPPER(")) {
            return evaluateCase(expr, values, true);
        }
        if (expr.toUpperCase().startsWith("LOWER(")) {
            return evaluateCase(expr, values, false);
        }
        if (expr.toUpperCase().startsWith("LEFT(")) {
            return evaluateSubstring(expr, values, 0);
        }
        if (expr.toUpperCase().startsWith("RIGHT(")) {
            return evaluateSubstringRight(expr, values);
        }

        return evaluateArithmetic(expr, values);
    }

    private String evaluateAggregate(String expr, Map<String, String> values, String func) {
        String inner = extractInner(expr);
        List<Double> numbers = resolveNumbers(inner, values);

        if (numbers.isEmpty()) return "0";

        return switch (func) {
            case "SUM" -> formatNum(numbers.stream().mapToDouble(Double::doubleValue).sum());
            case "AVERAGE" -> formatNum(numbers.stream().mapToDouble(Double::doubleValue).average().orElse(0));
            case "MAX" -> formatNum(numbers.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            case "MIN" -> formatNum(numbers.stream().mapToDouble(Double::doubleValue).min().orElse(0));
            case "COUNT" -> String.valueOf(numbers.size());
            default -> "0";
        };
    }

    private String evaluateIf(String expr, Map<String, String> values) {
        String inner = extractInner(expr);
        String[] parts = splitTopLevel(inner, ',');
        if (parts.length < 2) return "#ERROR!";
        String condition = parts[0].trim();
        String trueVal = parts.length > 1 ? parts[1].trim() : "0";
        String falseVal = parts.length > 2 ? parts[2].trim() : "0";

        boolean result = evaluateCondition(condition, values);
        return result ? resolveValue(trueVal, values) : resolveValue(falseVal, values);
    }

    private boolean evaluateCondition(String condition, Map<String, String> values) {
        if (condition.contains(">=")) {
            String[] p = condition.split(">=");
            return parseNum(resolveValue(p[0].trim(), values)) >= parseNum(resolveValue(p[1].trim(), values));
        }
        if (condition.contains("<=")) {
            String[] p = condition.split("<=");
            return parseNum(resolveValue(p[0].trim(), values)) <= parseNum(resolveValue(p[1].trim(), values));
        }
        if (condition.contains("<>")) {
            String[] p = condition.split("<>");
            return !resolveValue(p[0].trim(), values).equals(resolveValue(p[1].trim(), values));
        }
        if (condition.contains(">")) {
            String[] p = condition.split(">");
            return parseNum(resolveValue(p[0].trim(), values)) > parseNum(resolveValue(p[1].trim(), values));
        }
        if (condition.contains("<")) {
            String[] p = condition.split("<");
            return parseNum(resolveValue(p[0].trim(), values)) < parseNum(resolveValue(p[1].trim(), values));
        }
        if (condition.contains("=")) {
            String[] p = condition.split("=");
            return resolveValue(p[0].trim(), values).equals(resolveValue(p[1].trim(), values));
        }
        return parseNum(resolveValue(condition, values)) != 0;
    }

    private String evaluateConcat(String expr, Map<String, String> values) {
        String inner = extractInner(expr);
        String[] parts = splitTopLevel(inner, ',');
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(resolveValue(p.trim(), values));
        }
        return sb.toString();
    }

    private String evaluateTrim(String expr, Map<String, String> values) {
        String inner = extractInner(expr);
        return resolveValue(inner, values).trim();
    }

    private String evaluateCase(String expr, Map<String, String> values, boolean upper) {
        String inner = extractInner(expr);
        String val = resolveValue(inner, values);
        return upper ? val.toUpperCase() : val.toLowerCase();
    }

    private String evaluateSubstring(String expr, Map<String, String> values, int side) {
        String inner = extractInner(expr);
        String[] parts = splitTopLevel(inner, ',');
        String str = resolveValue(parts[0].trim(), values);
        int len = parts.length > 1 ? (int) parseNum(parts[1].trim()) : 1;
        if (side == 0) {
            return str.length() > len ? str.substring(0, len) : str;
        }
        return str.length() > len ? str.substring(str.length() - len) : str;
    }

    private String evaluateSubstringRight(String expr, Map<String, String> values) {
        return evaluateSubstring(expr, values, 1);
    }

    private String evaluateArithmetic(String expr, Map<String, String> values) {
        Matcher m = CELL_REF.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String cellRef = m.group();
            String val = values.getOrDefault(cellRef, values.getOrDefault(cellRef.toUpperCase(), "0"));
            m.appendReplacement(sb, val != null ? val : "0");
        }
        m.appendTail(sb);
        String resolved = sb.toString();

        try {
            return formatNum(simpleEval(resolved));
        } catch (Exception e) {
            try {
                return String.valueOf(Double.parseDouble(resolved));
            } catch (Exception e2) {
                return resolved;
            }
        }
    }

    private double simpleEval(String expr) {
        expr = expr.replaceAll("\\s+", "");
        return parseSimpleExpression(expr);
    }

    private double parseSimpleExpression(String expr) {
        if (expr.isEmpty()) return 0;
        int idx = findLowestOp(expr);
        if (idx < 0) return Double.parseDouble(expr);
        char op = expr.charAt(idx);
        double left = parseSimpleExpression(expr.substring(0, idx));
        double right = parseSimpleExpression(expr.substring(idx + 1));
        return switch (op) {
            case '+' -> left + right;
            case '-' -> left - right;
            case '*' -> left * right;
            case '/' -> right != 0 ? left / right : Double.NaN;
            default -> 0;
        };
    }

    private int findLowestOp(String expr) {
        int level = 0;
        int lowestIdx = -1;
        int lowestPrio = Integer.MAX_VALUE;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') level++;
            else if (c == '(') level--;
            else if (level == 0) {
                int prio = getOpPriority(c);
                if (prio > 0 && prio < lowestPrio) {
                    lowestPrio = prio;
                    lowestIdx = i;
                }
            }
        }
        return lowestIdx;
    }

    private int getOpPriority(char c) {
        return switch (c) {
            case '+', '-' -> 1;
            case '*', '/' -> 2;
            default -> 0;
        };
    }

    private String extractInner(String expr) {
        int start = expr.indexOf('(');
        int end = expr.lastIndexOf(')');
        if (start < 0 || end < start) return expr;
        return expr.substring(start + 1, end);
    }

    private String[] splitTopLevel(String str, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (c == delimiter && depth == 0) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private List<Double> resolveNumbers(String expr, Map<String, String> values) {
        List<Double> numbers = new ArrayList<>();
        Matcher rm = RANGE_REF.matcher(expr);
        if (rm.find()) {
            String start = rm.group(1);
            String end = rm.group(2);
            int[] s = parseCellKey(start);
            int[] e = parseCellKey(end);
            for (int r = s[0]; r <= e[0]; r++) {
                for (int c = s[1]; c <= e[1]; c++) {
                    String key = indexToCellRef(r, c);
                    numbers.add(parseNum(values.getOrDefault(key, "0")));
                }
            }
        } else {
            String[] parts = splitTopLevel(expr, ',');
            for (String p : parts) {
                String v = resolveValue(p.trim(), values);
                numbers.add(parseNum(v));
            }
        }
        return numbers;
    }

    private String resolveValue(String expr, Map<String, String> values) {
        Matcher cm = CELL_REF.matcher(expr);
        if (cm.matches()) {
            return values.getOrDefault(expr.toUpperCase(), expr);
        }
        String[] parts = splitTopLevel(expr, ',');
        if (parts.length > 1) {
            String[] all = splitTopLevel(expr, ',');
            return resolveValue(all[0].trim(), values);
        }
        return expr.replace("\"", "");
    }

    private int[] parseCellKey(String key) {
        int split = 0;
        while (split < key.length() && Character.isLetter(key.charAt(split))) split++;
        int col = 0;
        for (int i = 0; i < split; i++) {
            col = col * 26 + (Character.toUpperCase(key.charAt(i)) - 'A' + 1);
        }
        return new int[]{Integer.parseInt(key.substring(split)) - 1, col - 1};
    }

    private String indexToCellRef(int row, int col) {
        StringBuilder sb = new StringBuilder();
        col++;
        while (col > 0) {
            col--;
            sb.insert(0, (char) ('A' + col % 26));
            col /= 26;
        }
        return sb.append(row + 1).toString();
    }

    private double parseNum(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private String formatNum(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }
}
