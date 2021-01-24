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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates CJSON language expressions. Regarding the CJSON language see
 * the JavaDocs of {@link Transform}.
 */
/*
 * This is just quick-n-dirty copy-paste re-mix of the TDD interpreter from
 * my aged project, FMPP, but slightly modified, as outlined in the description
 * of the language.
 */
final class CJSONInterpreter {
    
    /**
     * Evaluates function calls to itself.
     */
    public static final EvaluationEnvironment SIMPLE_EVALUATION_ENVIRONMENT
            = new EvaluationEnvironment() {
        public Object evalFunctionCall(FunctionCall f, CJSONInterpreter ip) {
            return f;
        }

        public Object notify(
                EvaluationEvent event,
                CJSONInterpreter ip, String name, Object extra) {
            return null;
        }
    };

    /** For accelerating {@link #isUnquotedStringChar(char)}. */
    private static final boolean[] UQSTR_CHARS = {
        false, // NUL (0)
        false, // SOH (1)
        false, // STX (2)
        false, // ETX (3)
        false, // EOT (4)
        false, // ENQ (5)
        false, // ACK (6)
        false, // BEL (7)
        false, // BS (8)
        false, // HT (9)
        false, // LF (10)
        false, // VT (11)
        false, // FF (12)
        false, // CR (13)
        false, // SO (14)
        false, // SI (15)
        false, // DLE (16)
        false, // DC1 (17)
        false, // DC2 (18)
        false, // DC2 (19)
        false, // DC4 (20)
        false, // NAK (21)
        false, // SYN (22)
        false, // ETB (23)
        false, // CAN (24)
        false, // EM (25)
        false, // SUB (26)
        false, // ESC (27)
        false, // FS (28)
        false, // GS (29)
        false, // RS (30)
        false, // US (31)
        false, // SP (32)
        false, // ! (33)
        false, // " (34)
        false, // # (35)
        true, // $ (36)
        false, // % (37)
        false, // & (38)
        false, // ' (39)
        false, // ( (40)
        false, // ) (41)
        false, // * (42)
        false, // + (43)
        false, // , (44)
        true, // - (45)
        true, // . (46)
        false, // / (47)
        true, // 0 (48)
        true, // 1 (49)
        true, // 2 (50)
        true, // 3 (51)
        true, // 4 (52)
        true, // 5 (53)
        true, // 6 (54)
        true, // 7 (55)
        true, // 8 (56)
        true, // 9 (57)
        false, // : (58)
        false, // ; (59)
        false, // < (60)
        false, // = (61)
        false, // > (62)
        false, // ? (63)
        true, // @ (64)
        true, // A (65)
        true, // B (66)
        true, // C (67)
        true, // D (68)
        true, // E (69)
        true, // F (70)
        true, // G (71)
        true, // H (72)
        true, // I (73)
        true, // J (74)
        true, // K (75)
        true, // L (76)
        true, // M (77)
        true, // N (78)
        true, // O (79)
        true, // P (80)
        true, // Q (81)
        true, // R (82)
        true, // S (83)
        true, // T (84)
        true, // U (85)
        true, // V (86)
        true, // W (87)
        true, // X (88)
        true, // Y (89)
        true, // Z (90)
        false, // [ (91)
        false, // \ (92)
        false, // ] (93)
        false, // ^ (94)
        true, // _ (95)
        false, // ` (96)
        true, // a (97)
        true, // b (98)
        true, // c (99)
        true, // d (100)
        true, // e (101)
        true, // f (102)
        true, // g (103)
        true, // h (104)
        true, // i (105)
        true, // j (106)
        true, // k (107)
        true, // l (108)
        true, // m (109)
        true, // n (110)
        true, // o (111)
        true, // p (112)
        true, // q (113)
        true, // r (114)
        true, // s (115)
        true, // t (116)
        true, // u (117)
        true, // v (118)
        true, // w (119)
        true, // x (120)
        true, // y (121)
        true, // z (122)
        false, // { (123)
        false, // | (124)
        false, // } (125)
        false, // ~ (126)
        false  // DEL (127)
    };

    private int p;
    private int ln;
    private EvaluationEnvironment ee;
    private String tx;
    private String fileName;
    private boolean skipWSFoundNL;
    
    // Can't be instantiated
    private CJSONInterpreter() {
        // Nop
    }

    // -------------------------------------------------------------------------
    // Public static methods

    /**
     * Evaluates text as single CJSON expression.
     * 
     * @param text the text to interpret.
     * @param ee the {@link EvaluationEnvironment} used to resolve function
     *    calls. If it is <code>null</code> then
     *    {@link #SIMPLE_EVALUATION_ENVIRONMENT} will be used.
     * @param forceStringValues specifies if expressions as <tt>true</tt> and
     *    <tt>123</tt> should be interpreted as strings, or as boolean and
     *    number respectively.
     * @param fileName the path of the source file, or other description of the
     *    source. It is used for informative purposes only, as in error
     *    messages.
     * 
     * @return the result of the evaluation. Possibly an empty
     *    <code>Map</code>, but never <code>null</code>. 
     */
    public static Object eval(
            String text, EvaluationEnvironment ee, boolean forceStringValues,
            String fileName) throws EvaluationException {
        CJSONInterpreter ip = new CJSONInterpreter();
        ip.init(text, fileName, ee);
        ip.skipWS();
        if (ip.p == ip.ln) {
            throw ip.newSyntaxError("The text is empty.");
        }
        Object res = ip.fetchExpression(forceStringValues, false);
        ip.skipWS();
        if (ip.p < ip.ln) {
            throw ip.newSyntaxError("Extra character(s) after the expression.");
        }
        return res;
    }

    /**
     * Evaluates a {@link Fragment} as single CJSON expression. The expression
     * can be surrounded with superfluous white-space.
     * 
     * @see #eval(String, EvaluationEnvironment, boolean, String) 
     */
    public static Object eval(
            Fragment fragment,
            EvaluationEnvironment ee, boolean forceStringValues)
            throws EvaluationException {
        CJSONInterpreter ip = new CJSONInterpreter();
        ip.init(fragment, ee);
        ip.skipWS();
        if (ip.p == ip.ln) {
            throw ip.newSyntaxError("The text is empty.");
        }
        Object res = ip.fetchExpression(forceStringValues, false);
        ip.skipWS();
        if (ip.p < ip.ln) {
            throw ip.newSyntaxError("Extra character(s) after the expression.");
        }
        return res;
    }

    /**
     * Same as <code>eval(text, null, false, fileName)</code>.
     * @see #eval(String, EvaluationEnvironment, boolean, String)
     */
    public static Object eval(String text, String fileName)
            throws EvaluationException {
        return eval(text, null, false, fileName);
    }

    /**
     * Same as <code>eval(text, null, false, null)</code>.
     * @see #eval(String, EvaluationEnvironment, boolean, String)
     */
    public static Object eval(String text)
            throws EvaluationException {
        return eval(text, null, false, null);
    }
    
    /**
     * Evaluates text as a list of key:value pairs.
     * 
     * @param text the text to interpret.
     * @param ee the {@link EvaluationEnvironment} used to resolve function
     *    calls. If it is <code>null</code> then
     *    {@link #SIMPLE_EVALUATION_ENVIRONMENT} will be used.
     * @param forceStringValues specifies if expressions as <tt>true</tt> and
     *    <tt>123</tt> should be interpreted as strings, or as boolean and
     *    number respectively.
     * @param fileName the path of the source file, or other description of the
     *    source. It is used for informative purposes only, as in error
     *    messages.
     * 
     * @return the result of the evaluation. Possibly an empty
     *    <code>Map</code>, but never <code>null</code>. The entries in the
     *    map are guaranteed to be in the same order as they were defined in
     *    the CJSON expression.
     */
    public static Map<String, Object> evalAsMap(
            String text, EvaluationEnvironment ee, boolean forceStringValues,
            String fileName) throws EvaluationException {
        CJSONInterpreter ip = new CJSONInterpreter();
        ip.init(text, fileName, ee);
        Map<String, Object> res = new LinkedHashMap<String, Object>();
        boolean done = false;
        try {
            try {
                ip.ee.notify(
                        EvaluationEvent.ENTER_MAP,
                        ip, null, res);
                done = true;
            } catch (Throwable e) {
                throw ip.newWrappedError(e);
            }
            return ip.fetchMapInner(res, (char) 0x20, forceStringValues);
        } finally {
            if (done) {
                try {
                    ip.ee.notify(
                            EvaluationEvent.LEAVE_MAP,
                            ip, null, res);
                } catch (Throwable e) {
                    throw ip.newWrappedError(e);
                }
            }
        }
    }

    public static Map<String, Object> evalAsMap(File f)
            throws EvaluationException, IOException {
        return evalAsMap(f, null, false);
    }

    /**
     * Same as <code>evalAsMap(textFromUTF8File, null, false, null)</code>.
     * Loads the file with {@link #loadCJSONFile}.
     * @see #evalAsMap(String, EvaluationEnvironment, boolean, String)
     */
    public static Map<String, Object> evalAsMap(File f, EvaluationEnvironment ee, boolean forceStringValues)
            throws EvaluationException, IOException {
        String s;
        try (InputStream in = new FileInputStream(f)) {
            s = loadCJSONFile(in, f.getAbsolutePath());
        }
        return evalAsMap(s, ee, forceStringValues, f.getAbsolutePath());
    }
    
    /**
     * Same as <code>evalAsMap(text, null, false, null)</code>.
     * @see #evalAsMap(String, EvaluationEnvironment, boolean, String)
     */
    public static Map<String, Object> evalAsMap(String text)
            throws EvaluationException {
        return evalAsMap(text, null, false, null);
    }

    /**
     * Same as <code>evalAsMap(text, null, false, fileName)</code>.
     * @see #evalAsMap(String, EvaluationEnvironment, boolean, String)
     */
    public static Map<String, Object> evalAsMap(String text, String fileName)
            throws EvaluationException {
        return evalAsMap(text, null, false, fileName);
    }
    
    /**
     * Evaluates text as a list values.
     * 
     * @param text the text to interpret.
     * @param ee the {@link EvaluationEnvironment} used to resolve function
     *    calls. If it is <code>null</code> then
     *    {@link #SIMPLE_EVALUATION_ENVIRONMENT} will be used.
     * @param forceStringValues specifies if expressions as <tt>true</tt> and
     *    <tt>123</tt> should be interpreted as strings, or as boolean and
     *    number respectively.
     * @param fileName the path of the source file, or other description of the
     *    source. It is used for informative purposes only, as in error
     *    messages.
     * 
     * @return the result of the evaluation. Possibly an empty
     *    <code>List</code>, but never <code>null</code>.
     */
    public static List<Object> evalAsList(
            String text, EvaluationEnvironment ee, boolean forceStringValues,
            String fileName) throws EvaluationException {
        CJSONInterpreter ip = new CJSONInterpreter();
        ip.init(text, fileName, ee);
        List<Object> res = new ArrayList<Object>();
        boolean done = false;
        try {
            try {
                ip.ee.notify(
                        EvaluationEvent.ENTER_LIST,
                        ip, null, res);
                done = true;
            } catch (Throwable e) {
                throw ip.newWrappedError(e);
            }
            return ip.fetchListInner(res, (char) 0x20, forceStringValues);
        } finally {
            if (done) {
                try {
                    ip.ee.notify(
                            EvaluationEvent.LEAVE_LIST,
                            ip, null, res);
                } catch (Throwable e) {
                    throw ip.newWrappedError(e);
                }
            }
        }
    }

    /**
     * Same as <code>evalAsList(text, null, false, null)</code>.
     * @see #evalAsList(String, EvaluationEnvironment, boolean, String)
     */
    public static List<Object> evalAsList(String text)
            throws EvaluationException {
        return evalAsList(text, null, false, null);
    }

    /**
     * Same as <code>evalAsList(text, null, false, fileName)</code>.
     * @see #evalAsList(String, EvaluationEnvironment, boolean, String)
     */
    public static List<Object> evalAsList(String text, String fileName)
            throws EvaluationException {
        return evalAsList(text, null, false, fileName);
    }

    /**
     * Loads a CJSON file with utilizing <tt>#encoding:<i>enc</i></tt> header.
     * If the header is missing, UTF-8 will be used.
     * @param in the stream that reads the content of the file.
     * @param source the description of the location of the "file" (usually a
     *     path). Can be {@code null}.
     */
    public static String loadCJSONFile(InputStream in, String source)
            throws IOException {
        byte[] b = loadByteArray(in); 
        return loadCJSONFile(b, source);
    }

    /**
     * Loads a CJSON file with utilizing <tt>#encoding:<i>enc</i></tt> header.
     * If the header is missing, the encoding given as parameter is used.
     * 
     * @param b the content of the "file".
     * @param source the description of the location of the "file" (usually a
     *     path). Can be {@code null}.
     */
    public static String loadCJSONFile(byte[] b, String source)
            throws IOException {
        String charset = extractCharsetComment(b);
        try {
            return new String(b, charset == null ? "UTF-8" : charset);
        } catch (java.io.UnsupportedEncodingException e) {
            String msg = "Unsupported character encoding, "
                    + TextUtil.jQuote(charset) + " was specifed in ";
            if (source != null) {
                msg += "this CJSON file: " + source; 
            } else {
                msg += "the CJSON file."; 
            }
            throw new IOException(msg);
        }
    }

    /**
     * Converts an object to a CJSON-like representation (not necessary valid
     * CJSON).
     * @param value the object to convert
     * @return the CJSON "source code".
     */
    public static String dump(Object value) {
        StringBuilder buf = new StringBuilder();
        dumpValue(buf, value, "");
        return buf.toString();
    }

    /**
     * Returns the type-name of a value according to the CJSON language.
     */
    public static String cjsonTypeNameOfValue(Object value) {
        return cjsonTypeNameForClass(value != null ? value.getClass() : null);
    }

    public static String cjsonTypeNameForClass(Class<?> cl) {
        if (String.class.isAssignableFrom(cl)) {
            return "string";
        } else if (Integer.class.isAssignableFrom(cl)) {
            return "int";
        } else if (Long.class.isAssignableFrom(cl)) {
            return "long";
        } else if (Double.class.isAssignableFrom(cl)) {
            return "double";
        } else if (BigDecimal.class.isAssignableFrom(cl)) {
            return "big-decimal";
        } else if (Boolean.class.isAssignableFrom(cl)) {
            return "boolean";
        } else if (List.class.isAssignableFrom(cl)) {
            return "list";
        } else if (LinkedHashMap.class.isAssignableFrom(cl)) {
            return "map (order keeping)";
        } else if (Map.class.isAssignableFrom(cl)) {
            return "map";
        } else if (FunctionCall.class.isAssignableFrom(cl)) {
            return "function call";
        } else {
            return cl != null ? cl.getName() : "null";
        }
    }

    // -------------------------------------------------------------------------
    // Public non-static methods
    
    public int getPosition() {
        return p;
    }
    
    public String getText() {
        return tx;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public EvaluationEnvironment getEvaluationEnvironment() {
        return ee;
    }

    // -------------------------------------------------------------------------
    // Private

    /**
     * Fetches comma separated expressions. The expressions may surrounded with
     * superfluous WS.
     * @param list destination list
     * @param terminator The character that signals the end of the list.
     *     Use 0x20 for EOS. <code>p</code> will point to the terminator
     *     character when the method returns.
     */
    private List<Object> fetchListInner(
            List<Object> list, char terminator, boolean forceStringValues)
            throws EvaluationException {
        int listP = p - 1;
        skipWS();
        if (terminator == 0x20) {
            listP = p;
        }
        
        while (true) {
            char c;
            if (p < ln) {
                c = tx.charAt(p);
                if (c == terminator) {
                    return list;
                }
                if (c == ',') {
                    throw newSyntaxError(
                            "List item is missing before the comma.");
                }
            } else {
                if (terminator == 0x20) {
                    return list;
                } else {
                    throw newSyntaxError("Reached the end of the text, "
                            + "but the list was not closed with "
                            + TextUtil.jQuoteOrName(terminator) + ".",
                            listP);
                }
            }
            list.add(fetchExpression(forceStringValues, false));
            c = skipSeparator(
                    terminator, null, "This is a list, and not a map.");
            if (c == terminator) {
                return list;
            }
        }
    }
    
    /**
     * Fetches comma separated key:value pairs. The expressions can be
     * surrounded with superflous WS.
     * @param map destination map
     * @param terminator The character that signals the end of the key:value
     *     pair list.
     *     Use 0x20 for EOS. <code>p</code> will point to the terminator
     *     character when the method returns.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchMapInner(
            Map<String, Object> map, char terminator, boolean forceStringValues)
            throws EvaluationException {
        int p2;
        
        int mapP = p - 1;
        skipWS();
        if (terminator == 0x20) {
            mapP = p;
        }
        
        // Key lookup
        while (true) {
            char c;
            
            if (p < ln) {
                c = tx.charAt(p);
                if (c == terminator) {
                    return map;
                }
                if (c == ',') {
                    throw newSyntaxError(
                            "Key-value pair is missing before the comma.");
                }
            } else {
                if (terminator == 0x20) {
                    return map;
                } else {
                    throw newSyntaxError("Reached the end of the text, "
                            + "but the map was not closed with "
                            + TextUtil.jQuoteOrName(terminator) + ".",
                            mapP);
                }
            }
            
            int keyP = p;
            Object o1 = fetchExpression(false, true);
            FunctionCall keyFunc;
            if (o1 instanceof FunctionCall) {
                keyFunc = (FunctionCall) o1;
                try {
                    o1 = ee.evalFunctionCall(keyFunc, this);
                } catch (Throwable e) {
                    throw newError("Failed to evaluate function "
                            + TextUtil.jQuote(keyFunc.getName()) + ".",
                            keyP, e);
                }
            } else {
                keyFunc = null;
            }
            
            c = skipSeparator(terminator, null, null);
            if (c == ':') {
                if (!(o1 instanceof String)) {
                    if (keyFunc != o1) {
                        throw newError(
                                "The key must be a String, but it is a(n) "
                                + cjsonTypeNameOfValue(o1) + ".", keyP);
                    } else {
                        throw newError(
                                "You can't use the function here, "
                                + "because it can't be evaluated "
                                + "in this context.",
                                keyP);
                    }
                }
                
                if (p == ln) {
                    throw newSyntaxError(
                            "The key must be followed by a value because "
                            + "colon was used.", keyP);
                }
                
                Object o2;
                boolean done = false;
                try {
                    Object nr;
                    try {
                        nr = ee.notify(
                                EvaluationEvent.ENTER_MAP_KEY,
                                this, (String) o1, null);
                        done = true;
                    } catch (Throwable e) {
                        throw newWrappedError(e, keyP);
                    }
                    if (nr == null) {
                        o2 = fetchExpression(forceStringValues, false);
                        map.put((String) o1, o2);
                    } else {
                        p2 = p;
                        skipExpression();
                        if (nr == EvaluationEnvironment.RETURN_FRAGMENT) {
                            map.put((String) o1,
                                    new Fragment(tx, p2, p, fileName));
                        }
                    }
                } finally {
                    if (done) {
                        try {
                            ee.notify(
                                    EvaluationEvent.LEAVE_MAP_KEY,
                                    this, (String) o1, null);
                        } catch (Throwable e) {
                            throw newWrappedError(e);
                        }
                    }
                }
                
                c = skipSeparator(terminator, null,
                        "Colon is for separating the key from the value, "
                        + "and the value was alredy given previously.");
            } else if (c == ',' || c == terminator || c == 0x20) {
                if (keyFunc == null) {
                    if (o1 instanceof String) {
                        boolean done = false;
                        try {
                            Object nr;
                            try {
                                nr = ee.notify(
                                        EvaluationEvent.ENTER_MAP_KEY,
                                        this, (String) o1, null);
                                done = true;
                            } catch (Throwable e) {
                                throw newWrappedError(e, keyP);
                            }
                            if (nr == null
                                    || nr == EvaluationEnvironment
                                            .RETURN_FRAGMENT) {
                                map.put((String) o1, Boolean.TRUE);
                            }
                        } finally {
                            if (done) {
                                try {
                                    ee.notify(
                                            EvaluationEvent.LEAVE_MAP_KEY,
                                            this, (String) o1, null);
                                } catch (Throwable e) {
                                    throw newWrappedError(e);
                                }
                            }
                        }
                    } else if (o1 instanceof Map) {
                        map.putAll((Map<String, Object>) o1);
                    } else {
                        throw newError(
                                "This expression should be either a string "
                                + "or a map, but it is a(n) "
                                + cjsonTypeNameOfValue(o1) + ".", keyP);
                    }
                } else {
                    if (o1 instanceof Map) {
                        map.putAll((Map<String, Object>) o1);
                    } else {
                        if (keyFunc == o1) {
                            throw newError(
                                    "You can't use the function here, "
                                    + "because it can't be evaluated "
                                    + "in this context.",
                                    keyP);
                        } else {
                            throw newError(
                                    "Function doesn't evalute to a map, but "
                                    + "to " + cjsonTypeNameOfValue(o1)
                                    + ", so it can't be merged into the map.",
                                    keyP);
                        }
                    }
                }
            }
            if (c == terminator) {
                return map;
            }
        }
    }

    /**
     * Fetches arbitrary expression. No surrounding superflous WS is allowed! 
     */
    private Object fetchExpression(boolean forceStr, boolean mapKey)
            throws EvaluationException {
        char c;
       
        if (p >= ln) { //!!a
            throw new BugException("Calling fetchExpression when p >= ln.");
        }
       
        c = tx.charAt(p);
       
        // JSON Object:
        if (c == '{') {
            Object nr;
            p++;
            Object res;
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            boolean done = false;
            try {
                try {
                    nr = ee.notify(
                            EvaluationEvent.ENTER_MAP,
                            this, null, map);
                    done = true;
                } catch (Throwable e) {
                    throw newWrappedError(e);
                }
                if (nr == null) {
                    fetchMapInner(map, '}', forceStr);
                    res = map;
                } else {
                    p--;
                    int p2 = p;
                    skipExpression();
                    res = new Fragment(tx, p2, p, fileName);
                    p--;
                }
            } finally {
                if (done) {
                    try {
                        ee.notify(
                                EvaluationEvent.LEAVE_MAP,
                                this, null, map);
                    } catch (Throwable e) {
                        throw newWrappedError(e);
                    }
                }
            }
            p++;
            return res; //!
        }

        // JSON array:
        if (c == '[') {
            p++;
            List<Object> res = new ArrayList<Object>();
            boolean done = false;
            try {
                try {
                    ee.notify(
                            EvaluationEvent.ENTER_LIST,
                            this, null, res);
                    done = true;
                } catch (Throwable e) {
                    throw newWrappedError(e);
                }
                fetchListInner(res, ']', forceStr);
            } finally {
                if (done) {
                    try {
                        ee.notify(
                                EvaluationEvent.LEAVE_LIST,
                                this, null, res);
                    } catch (Throwable e) {
                        throw newWrappedError(e);
                    }
                }
            }
            p++;
            return res; //!
        }
       
        int b = p;
        
        // Quoted string:
        if (c == '"' || c == '\'') {
            char q = c;
            
            p++;
            while (p < ln) {
                c = tx.charAt(p);
                if (c == '\\') {
                    break;
                }
                p++;
                if (c == q) {
                    return tx.substring(b + 1, p - 1); //!
                }
            }
            if (p == ln) {
                throw newSyntaxError(
                        "The closing " + TextUtil.jQuoteOrName(q)
                        + " of the string is missing.",
                        b);
            }

            int bidx = b + 1;
            StringBuilder buf = new StringBuilder();
            while (true) {
                buf.append(tx.substring(bidx, p));
                if (p == ln - 1) {
                    throw newSyntaxError(
                            "The closing " + TextUtil.jQuoteOrName(q)
                            + " of the string is missing.",
                            b);
                }
                c = tx.charAt(p + 1);
                switch (c) {
                    case '"':
                        buf.append('"');
                        bidx = p + 2;
                        break;
                    case '\'':
                        buf.append('\'');
                        bidx = p + 2;
                        break;
                    case '\\':
                        buf.append('\\');
                        bidx = p + 2;
                        break;
                    case 'n':
                        buf.append('\n');
                        bidx = p + 2;
                        break;
                    case 'r':
                        buf.append('\r');
                        bidx = p + 2;
                        break;
                    case 't':
                        buf.append('\t');
                        bidx = p + 2;
                        break;
                    case 'f':
                        buf.append('\f');
                        bidx = p + 2;
                        break;
                    case 'b':
                        buf.append('\b');
                        bidx = p + 2;
                        break;
                    case 'g':
                        buf.append('>');
                        bidx = p + 2;
                        break;
                    case 'l':
                        buf.append('<');
                        bidx = p + 2;
                        break;
                    case 'a':
                        buf.append('&');
                        bidx = p + 2;
                        break;
                    case '{':
                        buf.append('{');
                        bidx = p + 2;
                        break;
                    case '/':  // JSON have this
                        buf.append('/');
                        bidx = p + 2;
                        break;
                    case 'x':
                    case 'u':
                        {
                            p += 2;
                            int x = p;
                            int y = 0;
                            int z = (ln - p) > 4 ? p + 4 : ln;  
                            while (p < z) {
                                char c2 = tx.charAt(p);
                                if (c2 >= '0' && c2 <= '9') {
                                    y <<= 4;
                                    y += c2 - '0';
                                } else if (c2 >= 'a' && c2 <= 'f') {
                                    y <<= 4;
                                    y += c2 - 'a' + 10;
                                } else if (c2 >= 'A' && c2 <= 'F') {
                                    y <<= 4;
                                    y += c2 - 'A' + 10;
                                } else {
                                    break;
                                }
                                p++;
                            }
                            if (x < p) {
                                buf.append((char) y);
                            } else {
                                throw newSyntaxError(
                                        "Invalid hexadecimal UNICODE escape in "
                                        + "the string literal.",
                                        x - 2);
                            }
                            bidx = p;
                            break;
                        }
                    default:
                        if (isWS(c)) {
                            boolean hasWS = false;
                            bidx = p + 1;
                            do {
                                if (c == 0xA || c == 0xD) {
                                    if (hasWS) {
                                        break;
                                    }
                                    hasWS = true;
                                    if (c == 0xD && bidx < ln - 1) {
                                        if (tx.charAt(bidx + 1) == 0xA) {
                                            bidx++;
                                        }
                                    }
                                }
                                bidx++;
                                if (bidx == ln) {
                                    break;
                                }
                                c = tx.charAt(bidx);
                            } while (isWS(c));
                            if (!hasWS) {
                                throw newSyntaxError(
                                        "Invalid usage of escape sequence "
                                        + "\\white-space. This escape sequence "
                                        + "can be used only before "
                                        + "line-break.");
                            }
                        } else {
                            throw newSyntaxError(
                                    "Invalid escape sequence \\" + c
                                    + " in the string literal.");
                        }
                }
                p = bidx;
                while (true) {
                    if (p == ln) {
                        throw newSyntaxError(
                                "The closing " + TextUtil.jQuoteOrName(q)
                                + " of the string is missing.",
                                b);
                    }
                    c = tx.charAt(p);
                    if (c == '\\') {
                        break;
                    }
                    if (c == q) {
                        buf.append(tx.substring(bidx, p));
                        p++;
                        return buf.toString(); //!
                    }
                    p++;
                }
            } // while true
        } // if quoted string
       
        // Raw string:
        char c2;
        if (p < ln - 1) {
            c2 = tx.charAt(p + 1); 
        } else {
            c2 = 0x20;
        }
        if (c == 'r' && (c2 == '"' || c2 == '\'')) {
            char q = c2;
            p += 2;
            while (p < ln) {
                c = tx.charAt(p);
                p++;
                if (c == q) {
                    return tx.substring(b + 2, p - 1); //!
                }
            }
            throw newSyntaxError(
                    "The closing " + TextUtil.jQuoteOrName(q)
                    + " of the string is missing.",
                    b);
        }
       
        // Unquoted string, boolean or number, or function call
        uqsLoop: while (true) {
            c = tx.charAt(p);
            if (!isUnquotedStringChar(c) && !(p == b && c == '+')) {
                break uqsLoop;
            }
            p++;
            if (p == ln) {
                break uqsLoop;
            }
        }
        if (b == p) {
            throw newSyntaxError("Unexpected character.", b);
        } else {
            String s = tx.substring(b, p);
            int funcP = b;
            int oldP = p;
            c = skipWS();
            if (c == '(') {
                p++;
                List<Object> params;
                boolean done = false;
                try {
                    try {
                        ee.notify(
                                EvaluationEvent.ENTER_FUNCTION_PARAMS,
                                this, s, null);
                    } catch (Throwable e) {
                        throw newWrappedError(e, funcP);
                    }
                    done = true;
                    params = fetchListInner(
                            new ArrayList<Object>(), ')', forceStr);
                } finally {
                    if (done) {
                        try {
                            ee.notify(
                                    EvaluationEvent.LEAVE_FUNCTION_PARAMS,
                                    this, s, null);
                        } catch (Throwable e) {
                            throw newWrappedError(e);
                        }
                    }
                }
                p++;
                FunctionCall func = new FunctionCall(s, params);
                if (!mapKey) {
                    try {
                        return ee.evalFunctionCall(func, this); //!
                    } catch (Throwable e) {
                        throw newError("Failed to evaluate function "
                                + TextUtil.jQuote(func.getName()) + ".",
                                b, e);
                    }
                } else {
                    return func;
                }
            } else {
                p = oldP;
                if (!forceStr && !mapKey) {
                    if (s.equals("true")) {
                        return Boolean.TRUE; //!
                    } else if (s.equals("false")) {
                        return Boolean.FALSE; //!
                    } else if (s.equals("null")) {
                        return null; //!
                    }
                    c = s.charAt(0);
                    if ((c >= '0' && c <= '9') || c == '+' || c == '-') {
                        String s2;
                        if (c == '+') {
                            s2 = s.substring(1); // Integer(s) doesn't know +.
                        } else {
                            s2 = s;
                        }
                        try {
                            return new Integer(s2); //!
                        } catch (NumberFormatException exc) {
                            // ignore
                        }
                        try {
                            return new BigDecimal(s2); //!
                        } catch (NumberFormatException exc) {
                            // ignore
                        }
                    }
                }
                return s; //!
            } // if not '('
        } // if b == p
    }

    /**
     * Skips a single expression. It's ignores syntax errors in the skipped
     * expression as far as it is clean where the end of the expression is. 
     */
    private void skipExpression() throws EvaluationException {
        char c;
       
        if (p >= ln) { //!!a
            throw new BugException("Calling fetchExpression when p >= ln.");
        }
       
        c = tx.charAt(p);
       
        // Map:
        if (c == '{') {
            p++;
            skipListing('}');
            p++;
            return;
        }
       
        // List:
        if (c == '[') {
            p++;
            skipListing(']');
            p++;
            return;
        }

        // Unresolved object in a dump:
        if (c == '<') {
            p++;
            skipListing('>');
            p++;
            return;
        }

        // Just for durability:
        if (c == '(') {
            p++;
            skipListing(')');
            p++;
            return;
        }
       
        int b = p;
        
        // Quoted string:
        if (c == '"' || c == '\'') {
            char q = c;
            
            p++;
            while (p < ln) {
                c = tx.charAt(p);
                if (c == '\\') {
                    if (p != ln - 1) {
                        p++;
                    }
                }
                p++;
                if (c == q) {
                    return; //!
                }
            }
            throw newSyntaxError(
                    "The closing " + TextUtil.jQuoteOrName(q)
                    + " of the string is missing.",
                    b);
        } // if quoted string
       
        // Raw string:
        char c2;
        if (p < ln - 1) {
            c2 = tx.charAt(p + 1); 
        } else {
            c2 = 0x20;
        }
        if (c == 'r' && (c2 == '"' || c2 == '\'')) {
            char q = c2;
            p += 2;
            while (p < ln) {
                c = tx.charAt(p);
                p++;
                if (c == q) {
                    return; //!
                }
            }
            throw newSyntaxError(
                    "The closing " + TextUtil.jQuoteOrName(q)
                    + " of the string is missing.",
                    b);
        }
       
        // Unquoted string, boolean or number, or function call
        uqsLoop: while (true) {
            c = tx.charAt(p);
            if (!isUnquotedStringChar(c)
                    && !(p == b && c == '+')) {
                break uqsLoop;
            }
            p++;
            if (p == ln) {
                break uqsLoop;
            }
        }
        if (b == p) {
            throw newSyntaxError("Unexpected character.", b);
        } else {
            int oldP = p;
            c = skipWS();
            if (c == '(') {
                p++;
                skipListing(')');
                p++;
            } else {
                p = oldP;
            } // if not '('
        } // if b == p
    }
    
    private void skipListing(char terminator) throws EvaluationException {
        int listP = p - 1;
        skipWS();
        if (terminator == 0x20) {
            listP = p;
        }
        
        while (true) {
            char c;
            if (p < ln) {
                c = tx.charAt(p);
                if (c == terminator) {
                    return;
                }
            } else {
                if (terminator == 0x20) {
                    return;
                } else {
                    throw newSyntaxError("Reached the end of the text, "
                            + "but the closing "
                            + TextUtil.jQuoteOrName(terminator)
                            + " is missing.",
                            listP);
                }
            }
            if (c == ',' || c == ':' || c == ';' || c == '=') {
                p++;
            } else {
                skipExpression();
            }
            c = skipWS();
            if (c == terminator) {
                return;
            }
        }
    }

    /**
     * Fetches separator between whatever items. 
     * 
     * @return the separator, which is either or <code>','</code>,
     * or <code>':'</code>, or <code>0x20</code> for EOS, or
     * <code>terminator</code> for the terminator character.
     * <code>','</code> means comma separation, or separation with implied comma
     * (i.e. sparation with NL).
     * 
     * <p><code>p</code> will point the first character of the item (or the
     * terminator character) after the skipped separator, unless an exception
     * aborts the execution of the method.
     * 
     * @param terminator the character that terminates the sequence of
     *     separated items. Use 0x20 for EOS.
     * @param commaBadReason if not <code>null</code>, comma will not be
     *     accepted as separator, and it is the reason why.
     * @param colonBadReason if not <code>null</code>, colon will not be
     *     accepted as separator, and it is the reason why.
     */
    private char skipSeparator(
            char terminator, String commaBadReason, String colonBadReason)
            throws EvaluationException {
        int intialP = p;
        char c = skipWS();
        boolean plusConverted = false;
        if (c == '+') {
            // deprecated the old map-union syntax
            throw newSyntaxError(
                    "The + operator is not supported. (Hint: if you want to "
                    + "break a string into multiple lines, use a quoted string "
                    + "literal, finish the line with \\, then just continue "
                    + "the literal in the next line with optional "
                    + "indentation.");
        }
        if (c == ',' || c == ':') {
            if (commaBadReason != null && c == ',') {
                if (!plusConverted)  {
                    throw newSyntaxError(
                            "Comma (,) shouldn't be used here. "
                            + commaBadReason);
                } else {
                    throw newSyntaxError(
                            "Plus sign (+), which is treated as comma (,) "
                            + "in this case, shouldn't be used here. "
                            + commaBadReason);
                }
            }
            if (colonBadReason != null && c == ':') {
                throw newSyntaxError(
                        "Colon (:) shouldn't be used here. " + colonBadReason);
            }
            p++;
            skipWS();
            return c;
        } else if (c == terminator) {
            return terminator;
        } else if (c == ';') {
            throw newSyntaxError(
                    "Semicolon (;) was unexpected here. If you want to "
                    + "separate items in a listing then use comma "
                    + "(,) instead.");
        } else if (c == '=') {
            throw newSyntaxError(
                    "Equals sign (=) was unexpected here. If you want to "
                    + "associate a key with a value then use "
                    + "colon (:) instead.");
        } else {
            if (c == 0x20) {
                // EOS
                return c;
            }
            if (skipWSFoundNL) {
                // implicit comma
                if (commaBadReason != null) {
                    throw newSyntaxError(
                            "Line-break shouldn't be used before this iteam as "
                            + "separator (which is the same as using comma). "
                            + commaBadReason);
                }
                return ',';
            } else {
                if (p == intialP) {
                    throw newSyntaxError("Character "
                            + TextUtil.jQuoteOrName(tx.charAt(p))
                            + " shouldn't occur here.");
                } else {
                    // WS* separator
                    throw newSyntaxError("No separator was used before "
                            + "the item. Items in listings should be "
                            + "separated with comma (,) or line-break. Keys "
                            + "and values in maps should be separated with "
                            + "colon (:).");
                }
            }
        }
    }

    /**
     * Increments <code>p</code> until it finds non-WS character or EOS, also
     * it transparently skips CJSON comments.
     * @return the non-WS char that terminates the WS, or 0x20 if EOS reached.
     */
    private char skipWS() throws EvaluationException {
        char c;
        skipWSFoundNL = false;
        while (p < ln) {
            c = tx.charAt(p);
            if (!isWS(c)) {
                if (c == '/' && p + 1 < ln && tx.charAt(p + 1) == '/') {
                    while (true) {
                        p++;
                        if (p == ln) {
                            return 0x20; //!
                        }
                        c = tx.charAt(p);
                        if (c == 0xA || c == 0xD) {
                            skipWSFoundNL = true;
                            break; //!
                        }
                    }
                } else if (c == '/' && p + 1 < ln && tx.charAt(p + 1) == '*') {
                    int commentP = p;
                    p++;
                    while (true) {
                        p++;
                        if (p + 1 >= ln) {
                            throw newSyntaxError(
                                    "Comment was not closed with \"*/\".",
                                    commentP); 
                        }
                        if (tx.charAt(p) == '*' && tx.charAt(p + 1) == '/') {
                            p++;
                            break; //!
                        }
                    }
                } else {
                    return c; //!
                }
            } else if (c == 0xD || c == 0xA) {
                skipWSFoundNL = true;
            }
            p++;
        }
        return 0x20;
    }
    
    /**
     * (Re)inits the evaluator object.
     */
    private void init(String text, String fileName, EvaluationEnvironment ee) {
        p = 0;
        skipWSFoundNL = false;
        tx = text;
        ln = text.length();
        this.fileName = fileName;
        this.ee = ee == null ? SIMPLE_EVALUATION_ENVIRONMENT : ee;
    }

    /**
     * (Re)inits the evaluator object.
     */
    private void init(Fragment fr, EvaluationEnvironment ee) {
        p = fr.getFragmentStart();
        skipWSFoundNL = false;
        tx = fr.getText();
        ln = fr.getFragmentEnd();
        this.fileName = fr.getFileName();
        this.ee = ee == null ? SIMPLE_EVALUATION_ENVIRONMENT : ee;
    }
    
    private static final String ENCODING_COMMENT_1 = "encoding";
    private static final String ENCODING_COMMENT_2 = "charset";

    /**
     * Same as <code>Character.isWhitespace</code>, but counts BOM as WS too.
     */
    private static boolean isWS(char c) {
        return Character.isWhitespace(c) || c == 0xFEFF;
    }
    
    private static boolean isUnquotedStringChar(char c) {
        return c < 128 ? UQSTR_CHARS[c] : Character.isLetterOrDigit(c);
    }
    
    /**
     * @return the name of the charset given in the comment, or {@code null} if
     *     there is no such comment.
     */
    private static String extractCharsetComment(byte[] b) {
        char c;
        String s;
        int p = 0;
        int ln = b.length;

        // Skip BOM, if present:
        if (p + 2 < ln
                && toChar(b[p]) == 0xEF
                && toChar(b[p + 1]) == 0xBB
                && toChar(b[p + 2]) == 0xBF) {
            p += 3;
        }
        
        // Skip WS
        while (p < ln && Character.isWhitespace(toChar(b[p]))) {
            p++;
        }

        // Do we start with "//"? 
        if (!(p + 1 < ln && toChar(b[p]) == '/' && toChar(b[p + 1]) == '/')) {
            return null; // No.
        }
        p += 2;
        
        p = extractCharsetComment_skipNonNLWS(b, p);
        int bp = p;
        while (p < ln) {
            c = toChar(b[p]);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                break;
            }
            p++;
        }
        if (p - bp != ENCODING_COMMENT_1.length()
                && p - bp != ENCODING_COMMENT_2.length()) {
            return null;
        }
        try {
            s = new String(b, bp, p - bp, "ISO-8859-1").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            throw new BugException("ISO-8859-1 decoding failed.", e);
        }
        if (!s.equals(ENCODING_COMMENT_1) && !s.equals(ENCODING_COMMENT_2)) {
            return null;
        }
        p = extractCharsetComment_skipNonNLWS(b, p);
        if (p == ln) {
            return null;
        }
        c = toChar(b[p]);
        if (c != ':') {
            return null;
        }
        p++;
        p = extractCharsetComment_skipNonNLWS(b, p);
        if (p == ln) {
            return null;
        }
        bp = p;
        while (p < ln) {
            c = toChar(b[p]);
            if (c == 0xA || c == 0xD) {
                break;
            }
            p++;
        }
        try {
            s = new String(b, bp, p - bp, "ISO-8859-1").trim();
            if (s.length() == 0) {
                return null;
            }
        } catch (UnsupportedEncodingException e) {
            throw new BugException("ISO-8859-1 decoding failed.", e);
        }
        return s;
    }
    
    private static int extractCharsetComment_skipNonNLWS(byte[] b, int p) {
        int ln = b.length;
        while (p < ln) {
            char c = toChar(b[p]);
            if (!Character.isWhitespace(c) || c == 0xD || c == 0xA) {
                break;
            }
            p++;
        }
        return p;
    }
    
    private static char toChar(byte b) {
        return (char) (0xFF & b);
    }

    private static void dumpMap(
            StringBuilder out, Map<String, Object> m, String indent) {
        Iterator<Map.Entry<String, Object>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> ent = it.next();
            out.append(
                    indent
                    + TextUtil.jQuote(ent.getKey()) + ": ");
            dumpValue(out, ent.getValue(), indent);
            out.append(LINE_BREAK);
        }
    }

    private static void dumpMapSL(StringBuilder out, Map<String, Object> m) {
        Iterator<Map.Entry<String, Object>> it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> ent = it.next();
            out.append(TextUtil.jQuote(ent.getKey()) + ":");
            dumpValueSL(out, ent.getValue());
            if (it.hasNext()) {
                out.append(", ");
            }
        }
    }

    private static void dumpList(
            StringBuilder out, List<?> ls, String indent) {
        Iterator<?> it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            out.append(indent);
            dumpValue(out, obj, indent);
            out.append(LINE_BREAK);
        }
    }

    private static void dumpListSL(StringBuilder out, List<?> ls) {
        Iterator<?> it = ls.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            dumpValueSL(out, obj);
            if (it.hasNext()) {
                out.append(", ");
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void dumpValue(StringBuilder out, Object o, String indent) {
        if (o instanceof Number || o instanceof Boolean) {
            out.append(o);
        } else if (o instanceof String) {
            out.append(TextUtil.jQuote((String) o));
        } else if (o instanceof Map) {
            out.append("{");
            out.append(LINE_BREAK);
            dumpMap(out, (Map<String, Object>) o, indent + "    ");
            out.append(indent + "}");
        } else if (o instanceof List) {
            out.append("[");
            out.append(LINE_BREAK);
            dumpList(out, (List<Object>) o, indent + "    ");
            out.append(indent + "]");
        } else if (o instanceof FunctionCall) {
            FunctionCall dir = (FunctionCall) o;
            out.append(dir.getName());
            out.append("(");
            dumpListSL(out, dir.getParams());
            out.append(")");
        } else {
            if (o == null) {
                out.append("<null>");
            } else {
                out.append("<");
                out.append(o.getClass().getName());
                out.append(" ");
                out.append(TextUtil.jQuote(o.toString()));
                out.append(">");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void dumpValueSL(StringBuilder out, Object o) {
        if (o instanceof Number || o instanceof Boolean) {
            out.append(o);
        } else if (o instanceof String) {
            out.append(TextUtil.jQuote((String) o));
        } else if (o instanceof Map) {
            out.append("{");
            dumpMapSL(out, (Map<String, Object>) o);
            out.append("}");
        } else if (o instanceof List) {
            out.append("[");
            dumpListSL(out, (List<Object>) o);
            out.append("]");
        } else if (o instanceof FunctionCall) {
            FunctionCall dir = (FunctionCall) o;
            out.append(dir.getName());
            out.append("(");
            dumpListSL(out, dir.getParams());
            out.append(")");
        } else {
            out.append("<");
            out.append(o.getClass().getName());
            out.append(" ");
            out.append(TextUtil.jQuote(o.toString()));
            out.append(">");
        }
    }

    private EvaluationException newSyntaxError(String message) {
        return newSyntaxError(message, p);
    }
    
    private EvaluationException newSyntaxError(String message, int position) {
        return new EvaluationException(
                "CJSON syntax error: " + message, tx, position, fileName);
    }

    private EvaluationException newError(String message, int position) {
        return new EvaluationException(
                "CJSON error: " + message, tx, position, fileName);
    }
        
    private EvaluationException newError(
            String message, int position, Throwable cause) {
        return new EvaluationException(
                "CJSON error: " + message, tx, position, fileName, cause);
    }
    
    private EvaluationException newWrappedError(Throwable e) {
        return newWrappedError(e, p);
    }
    
    private EvaluationException newWrappedError(Throwable e, int p) {
        if (e instanceof EvaluationException) {
            return (EvaluationException) e;
        }
        return new EvaluationException(
                "Error while evaluating CJSON: " + e.getMessage(),
                tx, p, fileName, e.getCause());
    }
    
    /**
     * Symbolizes a CJSON function call.
     * Function calls that are not evaluated during the evaluation of a CJSON
     * expressions will be present in the result as the instances of this class.
     */
    public static class FunctionCall {
        private final String name;
        private final List<Object> params;
            
        public FunctionCall(String name, List<Object> params) {
            this.name = name;
            this.params = params;
        }
    
        public String getName() {
            return name;
        }
    
        public List<Object> getParams() {
            return params;
        }
        
        @Override
        public String toString() {
            return CJSONInterpreter.dump(this);
        }
    }
    
    /**
     * Fragment extracted from a CJSON expression.
     */
    public static class Fragment {
        private final String text;
        private final int fragmentStart;
        private final int fragmentEnd;
        private final String fileName; 
        
        /**
         * Creates new CJSON fragment.
         * 
         * @param text the full CJSON text that contains the fragment.
         *     (In extreme case the fragment and the full text is the same.)
         * @param fragmentStart the start index of the fragment in the text.
         * @param fragmentEnd the start index of the fragment in the text
         * @param fileName the name of the file the text comes from (for
         * informational purposes only). It can be <code>null</code> if the
         * source file is unknown or there is no source file.
         */
        public Fragment(
                String text, int fragmentStart, int fragmentEnd,
                String fileName) {
            this.text = text;
            this.fragmentStart = fragmentStart;
            this.fragmentEnd = fragmentEnd;
            this.fileName = fileName;
        }
        
        /**
         * Returns the name of the file the text comes from (for informational
         * purposes only). It can be <code>null</code> if the source file is
         * unknown or there is no source file.
         */
        public String getFileName() {
            return fileName;
        }
    
        /**
         * Returns the full CJSON text that contains the fragmet. 
         */
        public String getText() {
            return text;
        }
    
        /**
         * Returns the start index of the fragment in the text.
         */
        public int getFragmentStart() {
            return fragmentStart;
        }
    
        /**
         * Returns the end index (exclusive) of the fragment in the text.
         */
        public int getFragmentEnd() {
            return fragmentEnd;
        }
    
        /**
         * Returns the fragment text.
         */
        @Override
        public String toString() {
            return text.substring(fragmentStart, fragmentEnd);
        }
    }
    
    public enum EvaluationEvent {
        /**
         * The code of event that indicates that we have started to evaluate the
         * value in a key:value pair.
         */
        ENTER_MAP_KEY,
        
        /**
         * The code of event that indicates that we have finished to evaluate
         * the value in a key:value pair.
         */
        LEAVE_MAP_KEY,
    
        /**
         * The code of event that indicates that we have started to evaluate the
         * parameter list in a function call.
         */
        ENTER_FUNCTION_PARAMS,
        
        /**
         * The code of event that indicates that we have finished to evaluate
         * the parameter list in a function call.
         */
        LEAVE_FUNCTION_PARAMS,
    
        /**
         * The code of event that indicates that we have started to evaluate the
         * items in a list. This does not include function call parameter lists.
         */
        ENTER_LIST,
        
        /**
         * The code of event that indicates that we have finished to evaluate
         * the items in a list.
         */
        LEAVE_LIST,
    
    
        /**
         * The code of event that indicates that we have started to evaluate the
         * items in a map.
         */
        ENTER_MAP,
        
        /**
         * The code of event that indicates that we have finished to evaluate
         * the items in a list.
         */
        LEAVE_MAP
    }

    /**
     * Callbacks that let you control the behavior of CJSON expression
     * evaluation.
     */
    public interface EvaluationEnvironment {
        
        Object RETURN_SKIP = new Object();
    
        Object RETURN_FRAGMENT = new Object();
        
        /**
         * Evaluates the function call. This method may simply returns its
         * parameter, which means that the function was not resolved, and thus
         * the function call will be available for further interpretation in the
         * result of the CJSON expression evaluation.
         * 
         * @param fc the function call to evaluate.
         *   
         * @return the return value of the function call. During the evaluation
         *     of a CJSON expression, function calls will be replaced with
         *     their return values. 
         *     If the return value is a {@link FunctionCall} object, it will not
         *     be evaluated again. This way, the final result of a CJSON
         *     expression evaluation can contain {@link FunctionCall} objects.
         * @throws Exception
         */
        Object evalFunctionCall(FunctionCall fc, CJSONInterpreter ip)
                throws Exception;
        
        /**
         * Notifies about an event during expression evaluation.
         * 
         * @param event An <code>EVENT_...</code> constant. Further events may
         *     will be added later, so the implementation must silently ignore
         *     events that it does not know. It is guaranteed that for each
         *     <code>EVENT_ENTER_...</code> event there will be an
         *     <code>EVENT_LEAVE_...</code> event later, except if
         *     <code>notifyContextChange</code> has thrown exception during
         *     handling <code>EVENT_ENTER_...</code>, in which case it is
         *     guaranteed that there will be no corresponding
         *     <code>EVENT_LEAVE_...</code> event.
         * @param ip the {@link CJSONInterpreter} instance that evaluates the
         *      text. The value returned by
         *      {@link CJSONInterpreter#getPosition()} will be the position in
         *      the text where the this even has been created:
         *      <ul>
         *        <li>{@link EvaluationEvent#ENTER_MAP_KEY}: points the first
         *            character of the <i>value</i> of the key:<i>value</i>
         *            pair.
         *        <li>{@link EvaluationEvent#ENTER_LIST},
         *            {@link EvaluationEvent#ENTER_MAP}, and
         *            {@link EvaluationEvent#ENTER_FUNCTION_PARAMS}: points the
         *            first character after the <tt>[</tt> and <tt>(</tt>
         *            respectively.  
         *        <li>{@link EvaluationEvent#LEAVE_LIST},
         *            {@link EvaluationEvent#LEAVE_MAP}, and
         *            {@link EvaluationEvent#LEAVE_FUNCTION_PARAMS}: points the
         *            terminating character, that is, the <tt>]</tt> or
         *            <tt>)</tt> or the character after the end of the string.
         *      </ul>  
         * @param name For {@link EvaluationEvent#ENTER_MAP_KEY} and
         *     {@link EvaluationEvent#ENTER_FUNCTION_PARAMS}, the name of the
         *     map key or function. It is <code>null</code> otherwise.
         * @param extra Even specific extra information.
         *     <ul>
         *       <li>For {@link EvaluationEvent#ENTER_MAP},
         *           {@link EvaluationEvent#LEAVE_MAP}, 
         *           {@link EvaluationEvent#ENTER_LIST},
         *           {@link EvaluationEvent#LEAVE_LIST} it is the
         *           <code>Map</code> or <code>List</code> that is being
         *           built by the map or list. It's OK to modify this
         *           <code>Map</code> or <code>List</code>.
         *       <li>For other events it's
         *           value is currently <code>null</code>.  
         *     </ul>
         * @return return The allowed return values and their meaning depends on
         *     the event. But return value <code>null</code> always means
         *     "do nothing special". The currently defined non-<code>null</code>
         *     return values for the events:
         *     <ul>
         *       <li>{@link EvaluationEvent#ENTER_MAP_KEY}:
         *          <ul>
         *            <li>{@link #RETURN_SKIP}: Skip the key:value
         *                pair. That is, the key:value pair will not be added to
         *                the map. The value expression will not be evaluated.
         *            <li>{@link #RETURN_FRAGMENT}: The value of the key:value
         *                pair will be the {@link Fragment} that stores the
         *                value expression. The value expression will not be
         *                evaluated.
         *                However, if the value is implicit boolean 
         *                <code>true</code>, (i.e. you omit the value) then
         *                {@link #RETURN_FRAGMENT} has no effect. 
         *          </ul>
         *       <li>
         *       <li>{@link EvaluationEvent#ENTER_MAP} if the map uses
         *           <tt>{</tt> and <tt>}</tt>):
         *          <ul>
         *            <li>{@link #RETURN_FRAGMENT}: The value of the map will be
         *                the {@link Fragment} that stores the map expression.
         *                The map expression will not be evaluated.
         *          </ul>
         *       </li>
         *     </ul>
         */
        Object notify(
                EvaluationEvent event, CJSONInterpreter ip,
                String name, Object extra)
                throws Exception;
    }

    public static class EvaluationException extends Exception {
        public EvaluationException(String message) {
            super(message);
        }
    
        public EvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
            
        public EvaluationException(String message, int position) {
            super(message + LINE_BREAK
                    + "Error location: character " + (position + 1));
        }
    
        public EvaluationException(
                String message, int position, Throwable cause) {
            super(message + LINE_BREAK
                    + "Error location: character " + (position + 1),
                    cause);
        }
            
        public EvaluationException(
                String message, String text, int position, String fileName) {
            super(createSourceCodeErrorMessage(
                    message, text, position, fileName, 56));
        }

        public EvaluationException(
                String message, String text, int position, String fileName,
                Throwable cause) {
            super(createSourceCodeErrorMessage(
                    message, text, position, fileName, 56),
                    cause);
        }
    }

    private static final String LINE_BREAK = "\n";

    private static String createSourceCodeErrorMessage(
            String message, String srcCode, int position, String fileName,
            int maxQuotLength) {
        int ln = srcCode.length();
        if (position < 0) {
            position = 0;
        }
        if (position >= ln) {
            if (position == ln) {
                return message + LINE_BREAK
                        + "Error location: The very end of "
                        + (fileName == null ? "the text" : fileName)
                        + ".";
            } else {
                return message + LINE_BREAK
                        + "Error location: ??? (after the end of "
                        + (fileName == null ? "the text" : fileName)
                        + ")";
            }
        }
            
        int i;
        char c;
        int rowBegin = 0;
        int rowEnd;
        int row = 1;
        char lastChar = 0;
        for (i = 0; i <= position; i++) {
            c = srcCode.charAt(i);
            if (lastChar == 0xA) {
                rowBegin = i;
                row++;
            } else if (lastChar == 0xD && c != 0xA) {
                rowBegin = i;
                row++;
            }
            lastChar = c;
        }
        for (i = position; i < ln; i++) {
            c = srcCode.charAt(i);
            if (c == 0xA || c == 0xD) {
                if (c == 0xA && i > 0 && srcCode.charAt(i - 1) == 0xD) {
                    i--;
                }
                break;
            }
        }
        rowEnd = i - 1;
        if (position > rowEnd + 1) {
            position = rowEnd + 1;
        }
        int col = position - rowBegin + 1;
        if (rowBegin > rowEnd) {
            return message + LINE_BREAK
                    + "Error location: line "
                    + row + ", column " + col
                    + (fileName == null ? ":" : " in " + fileName + ":")
                    + LINE_BREAK
                    + "(Can't show the line because it is empty.)";
        }
        String s1 = srcCode.substring(rowBegin, position);
        String s2 = srcCode.substring(position, rowEnd + 1);
        s1 = expandTabs(s1, 8);
        int ln1 = s1.length();
        s2 = expandTabs(s2, 8, ln1);
        int ln2 = s2.length();
        if (ln1 + ln2 > maxQuotLength) {
            int newLn2 = ln2 - ((ln1 + ln2) - maxQuotLength);
            if (newLn2 < 6) {
                newLn2 = 6;
            }
            if (newLn2 < ln2) {
                s2 = s2.substring(0, newLn2 - 3) + "...";
                ln2 = newLn2;
            }
            if (ln1 + ln2 > maxQuotLength) {
                s1 = "..." + s1.substring((ln1 + ln2) - maxQuotLength + 3);
            }
        }
        StringBuilder res = new StringBuilder(message.length() + 80);
        res.append(message);
        res.append(LINE_BREAK);
        res.append("Error location: line ");
        res.append(row);
        res.append(", column ");
        res.append(col);
        if (fileName != null) {
            res.append(" in ");
            res.append(fileName);
        }
        res.append(":");
        res.append(LINE_BREAK);
        res.append(s1);
        res.append(s2);
        res.append(LINE_BREAK);
        int x = s1.length();
        while (x != 0) {
            res.append(' ');
            x--;
        }
        res.append('^');
            
        return res.toString();
    }

    /**
     * Same as <code>expandTabs(text, tabWidth, 0)</code>.
     * @see #expandTabs(String, int, int) 
     */
    private static String expandTabs(String text, int tabWidth) {
        return expandTabs(text, tabWidth, 0);
    }

    /**
     * Replaces all occurances of character tab with spaces.
     * @param tabWidth the distance of tab stops.
     * @param startCol the index of the column in which the first character of
     *     the string is from the left edge of the page. The index of the first
     *     column is 0.
     * @return String The string after the replacements.
     */
    private static String expandTabs(String text, int tabWidth, int startCol) {
        int e = text.indexOf('\t');
        if (e == -1) {
            return text;
        }
        int b = 0;
        int tln = text.length();
        StringBuilder buf = new StringBuilder(tln + 16);
        do {
            buf.append(text.substring(b, e));
            int col = buf.length() + startCol;
            for (int i = tabWidth * (1 + col / tabWidth) - col; i > 0; i--) {
                buf.append(' ');
            }
            b = e + 1;
            e = text.indexOf('\t', b);
        } while (e != -1);
        buf.append(text.substring(b));
        return buf.toString();
    }

    private static byte[] loadByteArray(InputStream in)
            throws IOException {
        return loadByteArray(in, 512, false, 2);
    }

    private static byte[] loadByteArray(
            InputStream in, int initialSize, boolean sizeExpected,
            double multipier)
            throws IOException {
        int size = 0;
        int bcap = initialSize;
        byte[] b = new byte[bcap];
        try {
            int rdn;
            readLoop: while ((rdn = in.read(b, size, bcap - size)) != -1) {
                size += rdn;
                if (bcap == size) {
                    int nextByte = -1;
                    if (sizeExpected) {
                        // If the initialSize was the expected size of the
                        // "file", then resizing the buffer is certainly
                        // needless, as the next in.read(...) call would just
                        // return with -1. So let's see if it would...
                        nextByte = in.read();
                        if (nextByte == -1) {
                            break readLoop;
                        }
                    }
                    
                    bcap = (int) (bcap * multipier) + 64;
                    byte[] newB = new byte[bcap];
                    System.arraycopy(b, 0, newB, 0, size);
                    b = newB;
                    
                    // We have guessed badly, so...
                    if (nextByte != -1) {
                        b[size] = (byte) nextByte;
                        size++;
                    }
                }
            }
        } finally {
            in.close();
        }
        if (b.length != size) {
            byte[] newB = new byte[size];
            System.arraycopy(b, 0, newB, 0, size);
            return newB;
        } else {
            return b;
        }
    }

}
