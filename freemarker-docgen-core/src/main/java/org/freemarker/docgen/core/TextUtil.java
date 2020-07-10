/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.freemarker.docgen.core;

final class TextUtil {

    // Can't be instantiated
    private TextUtil() {
        // nop
    }

    /**
     *  Quotes string as Java language string literal.
     */
    public static String jQuote(String s) {
        if (s == null) {
            return "null";
        }
        String s2;
        int ln = s.length();
        int next = 0;
        int i = 0;
        StringBuilder b = new StringBuilder(ln + 3);
        b.append("\"");
        while (i < ln) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"' || c < 0x20) {
                b.append(s.substring(next, i));
                switch (c) {
                case '\\':
                    b.append("\\\\"); break;
                case '"':
                    b.append("\\\""); break;
                case '\n':
                    b.append("\\n"); break;
                case '\r':
                    b.append("\\r"); break;
                case '\t':
                    b.append("\\t"); break;
                case '\b':
                    b.append("\\b"); break;
                case '\f':
                    b.append("\\f"); break;
                default:
                    b.append("\\u0000");
                    int x = b.length();
                    s2 = Integer.toHexString(c);                    
                    b.replace(x - s2.length(), x, s2);    
                }
                next = i + 1;
            }
            i++;
        }
        if (next < ln) {
            b.append(s.substring(next));
        }
        b.append("\"");
        return b.toString();
    }

    /**
     *  Quotes character as Java language character, except quote characters,
     *  which are referred with name.
     */
    public static String jQuoteOrName(char c) {
        if (c == '\\' || c == '\''  || c == '"' || c < 0x20) {
            switch (c) {
            case '\\':
                return "'\\\\'";
            case '\'':
                return "\"apostrophe-quote\"";
            case '"':
                return "\"quotation mark\"";
            case '\n':
                return "'\\n'";
            case '\r':
                return "'\\r'";
            case '\t':
                return "'\\t'";
            case '\b':
                return "'\\b'";
            case '\f':
                return "'\\f'";
            default:
                String s = Integer.toHexString(c);
                int ln = s.length();
                if (ln == 1) {
                    return "'\\u000" + s + "'";
                } else if (ln == 2) {
                    return "'\\u00" + s + "'";
                } else if (ln == 3) {
                    return "'\\u0" + s + "'";
                } else {
                    return "'\\u" + s + "'";
                }
            }
        } else {
            return "'" + c + "'";
        }
    }
    
    private static final char[] UPPER_ROMAN_DIGITS = new char[] {
        'I', 'V',
        'X', 'L',
        'C', 'D',
        'M'
    }; 

    private static final char[] LOWER_ROMAN_DIGITS = new char[] {
        'i', 'v',
        'x', 'l',
        'c', 'd',
        'm'
    }; 

    /**
     * Converts a number to upper-case Roman number, like XVI; up to 3999.
     * @throws IllegalArgumentException if the number is not in the [1..3999]
     *    range.
     */
    public static String toUpperRomanNumber(int n) {
        return toRomanNumber(n, UPPER_ROMAN_DIGITS);
    }
    
    /**
     * Converts a number to lower-case Roman number, like xvi; up to 3999.
     * @throws IllegalArgumentException if the number is not in the [1..3999]
     *    range.
     */
    public static String toLowerRomanNumber(int n) {
        return toRomanNumber(n, LOWER_ROMAN_DIGITS);
    }
    
    private static String toRomanNumber(int n, char[] romanDigits) {
        // We fetch the decimal digits from right to left.
        // The res buffer will contain the Roman number *backwards*, and thus it
        // also will contain the Roman "digits" backwards, like 7 will be "IIV".
        // At the very end the buffer is reversed.
        
        if (n > 3999) {
            throw new IllegalArgumentException("toRomanNumber only supports "
                    + "numbers  in the [1..3999] range, but the number was "
                    + n + ".");
        }
        
        StringBuilder res = new StringBuilder();
        int base = 0;
        while (n != 0) {
            int digit = n % 10;
            n /= 10;
            if (digit != 0) {
                switch (digit) {
                case 3:
                    res.append(romanDigits[base]);
                    // falls through
                case 2:
                    res.append(romanDigits[base]);
                    // falls through
                case 1:
                    res.append(romanDigits[base]);
                    break;
                    
                case 4:
                    res.append(romanDigits[base + 1])
                            .append(romanDigits[base]);
                    break;
                    
                case 8:
                    res.append(romanDigits[base]);
                    // falls through
                case 7:
                    res.append(romanDigits[base]);
                    // falls through
                case 6:
                    res.append(romanDigits[base]);
                    // falls through
                case 5:
                    res.append(romanDigits[base + 1]);
                    break;
                    
                case 9: 
                    res.append(romanDigits[base + 2]);
                    res.append(romanDigits[base]);
                    break;
                    
                default:
                    throw new BugException("Unexpected branch");
                }
            }
            base += 2;
        }
        return res.reverse().toString();
    }
    
    /**
     * Converts a number to upper-case Latin (alpha) number, like
     * A, B, C, and so on, then Z, AA, AB, etc.
     */
    public static String toUpperLatinNumber(int n) {
        return toLatinNumber(n, 'A');
    }
    
    /**
     * Converts a number to lower-case Latin (alpha) number, like
     * a, b, c, and so on, then z, aa, ab, etc.
     */
    public static String toLowerLatinNumber(int n) {
        return toLatinNumber(n, 'a');
    }
    
    private static String toLatinNumber(final int n, char oneDigit) {
        if (n < 1) {
            throw new IllegalArgumentException("Can't convert 0 or negative "
                    + "numbers to latin-number.");
        }
        
        // First find out how many "digits" will we need. We start from A, then
        // try AA, then AAA, etc. (Note that the smallest digit is "A", which is
        // 1, not 0. Hence this isn't like a usual 26-based number-system):
        int reached = 1;
        int weight = 1;
        while (true) {
            int nextWeight = weight * 26;
            int nextReached = reached + nextWeight;
            if (nextReached <= n) {
                // So we will have one more digit
                weight = nextWeight;
                reached = nextReached;
            } else {
                // No more digits
                break;
            }
        }
        
        // Increase the digits of the place values until we get as close
        // to n as possible (but don't step over it).
        StringBuilder sb = new StringBuilder();
        while (weight != 0) {
            // digitIncrease: how many we increase the digit which is already 1
            final int digitIncrease = (n - reached) / weight;
            sb.append((char) (oneDigit + digitIncrease));
            reached += digitIncrease * weight;
            
            weight /= 26;
        }
        
        return sb.toString();
    }

    public static String detectEOL(String s, String defaultEOL) {
        int unixEOLs = 0;
        int windowsEOLs = 0;
        int macEOLs = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                unixEOLs++;
            }
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                    windowsEOLs++;
                } else {
                    macEOLs++;
                }
            }
        }
        
        if (unixEOLs > windowsEOLs && unixEOLs > macEOLs) {
            return "\n";
        } else if (windowsEOLs > unixEOLs && windowsEOLs > macEOLs) {
            return "\r\n";
        } else if (macEOLs > unixEOLs && macEOLs > windowsEOLs) {
            return "\r";
        } else {
            return defaultEOL;
        }
    }
    
    public static String normalizeEOL(final String s, String eol) {
        int ln = s.length();
        int i = 0;
        while (i < ln) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n') {
                break;
            }
            i++;
        }
        if (i == ln) {
            return s;
        }

        StringBuilder sb = new StringBuilder(ln + ln / 10);
        int nextToAppend = 0;
        while (i < ln) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n') {
                sb.append(s, nextToAppend, i);
                sb.append(eol);
                if (c == '\r' && i + 1 < ln && s.charAt(i + 1) == '\n') {
                    i++;
                }
                nextToAppend = i + 1;
            }
            i++;
        }
        sb.append(s, nextToAppend, ln);
        return sb.toString();
    }
    
    public static void main(String[] args) {
        System.out.println(normalizeEOL("", "|"));
        System.out.println(normalizeEOL("abc", "|"));
        System.out.println(normalizeEOL("a\nb\nc\r\nd", "|"));
        System.out.println(normalizeEOL("\n\n\r\r\r\n", "|"));
    }
    
}
