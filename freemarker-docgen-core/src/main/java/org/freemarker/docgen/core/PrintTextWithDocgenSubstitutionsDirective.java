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

import static org.freemarker.docgen.core.PrintTextWithDocgenSubstitutionsDirective.InsertDirectiveType.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.text.StringEscapeUtils;

import com.google.common.collect.ImmutableList;

import freemarker.core.Environment;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.NonStringException;
import freemarker.core.TemplateHTMLOutputModel;
import freemarker.core.TemplateValueFormatException;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

public class PrintTextWithDocgenSubstitutionsDirective implements TemplateDirectiveModel {

    private static final String PARAM_TEXT = "text";
    private static final String DOCGEN_TAG_START = "[docgen";
    private static final String DOCGEN_TAG_END = "]";
    private static final String DOCGEN_END_TAG_START = "[/docgen";

    enum InsertDirectiveType {
        INSERT_FILE("insertFile"),
        INSERT_OUTPUT("insertOutput");

        private final String directiveName;

        InsertDirectiveType(String directiveName) {
            this.directiveName = directiveName;
        }
    }

    private final Transform transform;

    public PrintTextWithDocgenSubstitutionsDirective(Transform transform) {
        this.transform = transform;
    }

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        String text = null;
        for (Map.Entry<String, TemplateModel> entry : ((Map<String, TemplateModel>) params).entrySet()) {
            String paramName = entry.getKey();
            TemplateModel paramValue = entry.getValue();
            if (paramValue != null) {
                if (PARAM_TEXT.equals(paramName)) {
                    if (!(paramValue instanceof TemplateScalarModel)) {
                        throw new NonStringException("The \"" + PARAM_TEXT + "\" argument must be a string!", env);
                    }
                    text = ((TemplateScalarModel) paramValue).getAsString();
                } else {
                    throw new TemplateException("Unsupported parameter: " + StringUtil.jQuote(paramName), env);
                }
            }
        }
        if (text == null) {
            throw new TemplateException("Missing required \"" + PARAM_TEXT + "\" argument", env);
        }

        if (loopVars.length != 0) {
            throw new TemplateException("Directive doesn't support loop variables", env);
        }

        if (body != null) {
            throw new TemplateException("Directive doesn't support nested content", env);
        }

        new DocgenSubstitutionInterpreter(text, env).execute();
    }

    private static final String DOCGEN_WD_TAG = "[docgen:wd]";
    private static final Pattern DOCGEN_WD_TAG_AND_SLASH_PATTERN = Pattern.compile(Pattern.quote(DOCGEN_WD_TAG) +  "/?");

    private class DocgenSubstitutionInterpreter {
        private final String text;
        private final Environment env;
        private final Writer out;
        private int cursor;
        private int lastDocgenTagStart;

        public DocgenSubstitutionInterpreter(String text, Environment env) {
            this.text = text;
            this.env = env;
            this.out = env.getOut();
        }

        private void execute() throws TemplateException, IOException {
            int lastUnprintedIdx = 0;
            parseText: while (true) {
                cursor = findNextDocgenTagStart(lastUnprintedIdx);
                if (cursor == -1) {
                    break parseText;
                } else {
                    lastDocgenTagStart = cursor;
                }

                HTMLOutputFormat.INSTANCE.output(text.substring(lastUnprintedIdx, cursor), out);
                lastUnprintedIdx = cursor;

                cursor += DOCGEN_TAG_START.length();
                skipRequiredToken(".");
                String subvarName = fetchRequiredVariableName();

                if (Transform.VAR_CUSTOM_VARIABLES.equals(subvarName)) {
                    skipRequiredToken(".");
                    String customVarName = fetchRequiredVariableName();
                    skipRequiredToken(DOCGEN_TAG_END);
                    lastUnprintedIdx = cursor;

                    insertCustomVariable(customVarName);
                } else if (INSERT_FILE.directiveName.equals(subvarName)) {
                    InsertDirectiveArgs args = fetchInsertDirectiveArgs(subvarName, INSERT_FILE);
                    lastUnprintedIdx = cursor;
                    insertFile(args);
                } else if (INSERT_OUTPUT.directiveName.equals(subvarName)) {
                    InsertDirectiveArgs args = fetchInsertDirectiveArgs(subvarName, INSERT_OUTPUT);
                    lastUnprintedIdx = cursor;
                    insertOutput(args);
                } else {
                    throw new TemplateException(
                            "Unsupported docgen subvariable " + StringUtil.jQuote(subvarName) + ".", env);
                }

            }
            HTMLOutputFormat.INSTANCE.output(text.substring(lastUnprintedIdx, text.length()), out);
        }

        private void insertCustomVariable(String customVarName) throws TemplateException, IOException {
            TemplateHashModel customVariables =
                    Objects.requireNonNull(
                            (TemplateHashModel) env.getVariable(Transform.VAR_CUSTOM_VARIABLES));
            TemplateModel customVarValue = customVariables.get(customVarName);
            if (customVarValue == null) {
                throw newErrorInDocgenTag(
                        "Docgen custom variable " + StringUtil.jQuote(customVarName)
                                + " wasn't defined or is null.");
            }

            printValue(customVarName, customVarValue);
        }

        /** Horrible hack to mimic ${var}; the public FreeMarker API should have something like this! */
        private void printValue(String varName, TemplateModel varValue) throws TemplateException,
                IOException {
            Object formattedValue;
            if (varValue instanceof TemplateNumberModel) {
                try {
                    formattedValue = env.getTemplateNumberFormat().format((TemplateNumberModel) varValue);
                } catch (TemplateValueFormatException e) {
                    throw newFormattingFailedException(varName, e);
                }
            } else if (varValue instanceof TemplateDateModel) {
                TemplateDateModel tdm = (TemplateDateModel) varValue;
                try {
                    formattedValue = env.getTemplateDateFormat(tdm.getDateType(), tdm.getAsDate().getClass())
                            .format(tdm);
                } catch (TemplateValueFormatException e) {
                    throw newFormattingFailedException(varName, e);
                }
            } else if (varValue instanceof TemplateScalarModel) {
                formattedValue = ((TemplateScalarModel) varValue).getAsString();
            } else if (varValue instanceof TemplateBooleanModel) {
                String[] booleanStrValues = env.getBooleanFormat().split(",");
                formattedValue = ((TemplateBooleanModel) varValue).getAsBoolean()
                        ? booleanStrValues[0] : booleanStrValues[1];
            } else {
                throw new TemplateException(
                        "Docgen custom variable " + StringUtil.jQuote(varName)
                                + " has an unsupported type: "
                                + ClassUtil.getFTLTypeDescription(varValue),
                        env);
            }
            if (formattedValue instanceof String) {
                HTMLOutputFormat.INSTANCE.output((String) formattedValue, out);
            } else {
                HTMLOutputFormat.INSTANCE.output((TemplateHTMLOutputModel) formattedValue, out);
            }
        }

        private void insertFile(InsertDirectiveArgs args) throws TemplateException, IOException {
            int slashIndex = args.path.indexOf("/");
            String symbolicNameStep = slashIndex != -1 ? args.path.substring(0, slashIndex) : args.path;
            if (!symbolicNameStep.startsWith("@") || symbolicNameStep.length() < 2) {
                throw newErrorInDocgenTag("Path argument must start with @<symbolicName>/, "
                        + " where <symbolicName> is in " + transform.getInsertableFiles().keySet() + ".");
            }
            String symbolicName = symbolicNameStep.substring(1);
            Path symbolicNamePath = transform.getInsertableFiles().get(symbolicName);
            if (symbolicNamePath == null) {
                throw newErrorInDocgenTag("Symbolic insertable file name "
                        + StringUtil.jQuote(symbolicName) + " is not amongst the defined names: "
                        + transform.getInsertableFiles().keySet());
            }
            symbolicNamePath = symbolicNamePath.toAbsolutePath().normalize();
            Path resolvedFilePath = slashIndex != -1
                    ? symbolicNamePath.resolve(args.path.substring(slashIndex + 1))
                    : symbolicNamePath;
            resolvedFilePath = resolvedFilePath.normalize();
            if (!resolvedFilePath.startsWith(symbolicNamePath)) {
                throw newErrorInDocgenTag("Resolved path ("
                        + resolvedFilePath + ") is not inside the base path ("
                        + symbolicNamePath + ").");
            }
            if (!Files.isRegularFile(resolvedFilePath)) {
                throw newErrorInDocgenTag("Not an existing file: " + resolvedFilePath);
            }

            Charset charset;
            if (args.charset != null) {
                try {
                    charset = Charset.forName(args.charset);
                } catch (UnsupportedCharsetException e) {
                    throw newErrorInDocgenTag("Unsupported charset: " + args.charset);
                }
            } else {
                charset = StandardCharsets.UTF_8;
            }

            try (InputStream in = Files.newInputStream(resolvedFilePath)) {
                String fileContent = IOUtils.toString(in, charset);
                String fileExt = FilenameUtils.getExtension(resolvedFilePath.getFileName().toString());
                if (fileExt != null && fileExt.toLowerCase().startsWith("ftl")) {
                    fileContent = removeFTLCopyrightComment(fileContent);
                }

                cutAndInsertContent(args, fileContent);
            }
        }

        private void insertOutput(InsertDirectiveArgs args) throws TemplateException, IOException {
            if (args.printCommand) {
                out.write("> ");
                out.write(DOCGEN_WD_TAG_AND_SLASH_PATTERN.matcher(StringUtil.chomp(args.body)).replaceAll(""));
                out.write("\n");
            }

            List<String> splitCmdLine = BashCommandLineArgsParser.parse(args.body);
            if (splitCmdLine.isEmpty()) {
                throw newErrorInDocgenTag("Command to execute was empty");
            }
            String cmdKey = splitCmdLine.get(0);
            Map<String, Transform.InsertableOutputCommandProperties> cmdPropsMap =
                    transform.getInsertableOutputCommands();
            Transform.InsertableOutputCommandProperties cmdProps = cmdPropsMap.get(cmdKey);
            if (cmdProps == null) {
                throw newErrorInDocgenTag(
                        "The " + Transform.SETTING_INSERTABLE_OUTPUT_COMMANDS
                                + " configuration setting doesn't have entry with key " + StringUtil.jQuote(cmdKey)
                                + ". "
                                + (cmdPropsMap.isEmpty()
                                        ? "That setting is empty."
                                        : "It has these keys: " + String.join(", ", cmdPropsMap.keySet())));
            }

            Method mainMethod = getMainMethod(cmdKey, cmdProps);

            StringWriter stdOutCapturer;
            PrintStream prevOut = System.out;
            Map<String, String> prevSystemProperties = new HashMap<>();
            try {
                stdOutCapturer = new StringWriter();
                PrintStream stdOutCapturerPrintStream = new PrintStream(
                        new WriterOutputStream(stdOutCapturer, Charset.defaultCharset()));
                System.setOut(stdOutCapturerPrintStream);

                cmdProps.getSystemProperties().forEach((k, v) -> {
                    String prevValue = setOrClearSystemProperty(k, v);
                    prevSystemProperties.put(k, prevValue);
                });

                List<String> rawCmdArgs = splitCmdLine.subList(1, splitCmdLine.size());
                List<String> cmdArgs = ImmutableList.<String>builder()
                        .addAll(cmdProps.getPrependedArguments())
                        .addAll(rawCmdArgs)
                        .addAll(cmdProps.getAppendedArguments())
                        .build().stream()
                        .map(cmdArg -> {
                            Path wdSubst = cmdProps.getWdSubstitution();
                            if (wdSubst == null) {
                                return cmdArg;
                            }
                            return cmdArg.replace(DOCGEN_WD_TAG, wdSubst.toAbsolutePath().toString());
                        })
                        .collect(Collectors.toList());

                Object returnValue;
                try {
                    returnValue = mainMethod.invoke(null, (Object) cmdArgs.toArray(new String[0]));
                } catch (Exception e) {
                    throw newErrorInDocgenTag("Error when executing command with "
                                    + cmdProps.getMainClassName() + "." + cmdProps.getMainMethodName()
                                    + ", and arguments " + cmdArgs + ".",
                            e);
                }
                if (returnValue instanceof Integer && ((Integer) returnValue) != 0) {
                    throw newErrorInDocgenTag(
                            "Command execution has returned with non-0 exit code " + returnValue
                                    + ", from " + cmdProps.getMainClassName() + "." + cmdProps.getMainMethodName()
                                    + ", called with arguments " + cmdArgs + ".");
                }

                stdOutCapturerPrintStream.flush();
            } finally {
                prevSystemProperties.forEach(PrintTextWithDocgenSubstitutionsDirective::setOrClearSystemProperty);
                System.setOut(prevOut);
            }
            cutAndInsertContent(args, stdOutCapturer.toString());
        }

        private void cutAndInsertContent(InsertDirectiveArgs args, String content)
                throws TemplateException, IOException {
            if (args.from != null) {
                Matcher matcher = args.from.matcher(content);
                if (matcher.find()) {
                    String remaining = content.substring(matcher.start());
                    content = "[\u2026]"
                            + (remaining.startsWith("\n") || remaining.startsWith("\r") ? "" : "\n")
                            + remaining;
                } else if (!args.fromOptional) {
                    throw newErrorInDocgenTag(
                            "\"from\" regular expression has no match in the file content: " + args.from);
                }
            }

            if (args.to != null) {
                Matcher matcher = args.to.matcher(content);
                if (matcher.find()) {
                    String remaining = content.substring(0, matcher.start());
                    content = remaining
                            + (remaining.endsWith("\n") || remaining.endsWith("\r") ? "" : "\n")
                            + "[\u2026]";
                } else if (!args.toOptional) {
                    throw newErrorInDocgenTag(
                            "\"to\" regular expression has no match in the file content: " + args.to);
                }
            }

            HTMLOutputFormat.INSTANCE.output(content, out);
        }

        private Method getMainMethod(String cmdKey, Transform.InsertableOutputCommandProperties cmdProps) throws
                TemplateException {
            String mainClassName = cmdProps.getMainClassName();
            Class<?> mainClass;
            try {
                mainClass = Transform.class.getClassLoader().loadClass(mainClassName);
            } catch (Exception e) {
                throw newErrorInDocgenTag(
                        "The main class referred by "
                                + Transform.SETTING_INSERTABLE_OUTPUT_COMMANDS + "[" + StringUtil.jQuote(cmdKey) + "], "
                                + StringUtil.jQuote(mainClassName) + ", couldn't be loaded",
                        e);
            }

            String mainMethodName = cmdProps.getMainMethodName();
            Method mainMethod;
            try {
                mainMethod = mainClass.getMethod(mainMethodName, String[].class);
            } catch (Exception e) {
                throw newErrorInDocgenTag(
                        "Couldn't get " + mainMethodName + "(String[]) method from class "
                                + mainClassName + ".",
                        e);
            }
            if ((mainMethod.getModifiers() & Modifier.STATIC) == 0) {
                throw newErrorInDocgenTag(
                        mainMethodName + "(String[]) method from class "
                                + mainClassName + " must be static.");
            }
            if ((mainMethod.getModifiers() & Modifier.PUBLIC) == 0) {
                throw newErrorInDocgenTag(
                        mainMethodName + "(String[]) method from class "
                                + mainClassName + " must be public.");
            }
            Class<?> returnType = mainMethod.getReturnType();
            if (returnType != void.class && returnType != int.class) {
                throw newErrorInDocgenTag(
                        mainMethodName + "(String[]) method from class "
                                + mainClassName + " must return void or int, but return type was " + returnType);
            }
            return mainMethod;
        }

        private int findNextDocgenTagStart(int fromIndex) {
            int startIdx = text.indexOf(DOCGEN_TAG_START, fromIndex);
            if (startIdx == -1) {
                return -1;
            }
            int afterTagStartIdx = startIdx + DOCGEN_TAG_START.length();
            if (afterTagStartIdx < text.length()
                    && !Character.isJavaIdentifierPart(text.charAt(afterTagStartIdx))) {
                return startIdx;
            }
            return -1;
        }

        private int findNextDocgenEndTag(int fromIndex) {
            int startIdx = text.indexOf(DOCGEN_END_TAG_START, fromIndex);
            if (startIdx == -1) {
                return -1;
            }
            int afterTagStartIdx = startIdx + DOCGEN_END_TAG_START.length();
            if (afterTagStartIdx < text.length()
                    && !Character.isJavaIdentifierPart(text.charAt(afterTagStartIdx))) {
                return startIdx;
            }
            return -1;
        }

        private void skipRequiredWS() throws DocgenTagException {
            if (!skipWS()) {
                throw newUnexpectedTokenException("whitespace", env);
            }
        }

        private boolean skipWS() {
            boolean found = false;
            while (cursor < text.length()) {
                if (Character.isWhitespace(text.charAt(cursor))) {
                    cursor++;
                    found = true;
                } else {
                    break;
                }
            }
            return found;
        }

        private void skipRequiredToken(String token) throws TemplateException {
            if (!skipOptionalToken(token)) {
                throw newUnexpectedTokenException(StringUtil.jQuote(token), env);
            }
        }

        private boolean skipOptionalToken(String token) throws TemplateException {
            skipWS();
            for (int i = 0; i < token.length(); i++) {
                char expectedChar = token.charAt(i);
                int lookAheadCursor = cursor + i;
                if (charAt(lookAheadCursor) != expectedChar) {
                    return false;
                }
            }
            cursor += token.length();
            skipWS();
            return true;
        }

        private String fetchRequiredVariableName() throws TemplateException {
            String varName = fetchOptionalVariableName();
            if (varName == null) {
                throw newUnexpectedTokenException("variable name", env);
            }
            return varName;
        }

        private String fetchOptionalVariableName() {
            if (!Character.isJavaIdentifierStart(charAt(cursor))) {
                return null;
            }
            int varNameStart = cursor;
            cursor++;
            while (Character.isJavaIdentifierPart(charAt(cursor))) {
                cursor++;
            }
            return text.substring(varNameStart, cursor);
        }

        private String fetchRequiredString() throws TemplateException {
            String result = fetchOptionalString();
            if (result == null) {
                throw newUnexpectedTokenException("string literal", env);
            }
            return result;
        }

        private String fetchOptionalString() throws TemplateException {
            char quoteChar = charAt(cursor);
            boolean rawString = quoteChar == 'r';
            if (rawString) {
                if (cursor + 1 < text.length()) {
                    quoteChar = charAt(cursor + 1);
                }
            }
            if (quoteChar != '"' && quoteChar != '\'') {
                return null;
            }
            cursor += rawString ? 2 : 1;
            int stringStartIdx = cursor;
            while (cursor < text.length() && charAt(cursor) != quoteChar) {
                if (!rawString && charAt(cursor) == '\\') {
                    throw new DocgenTagException(
                            "Backslash is currently not supported in string literal in Docgen tags, "
                                    + "except in raw strings (like r\"regular\\s+expression\").", env);
                }
                cursor++;
            }
            if (charAt(cursor) != quoteChar) {
                throw new DocgenTagException("Unclosed string literal in a Docgen tag.", env);
            }
            String result = text.substring(stringStartIdx, cursor);
            cursor++;
            return result;
        }

        private boolean fetchRequiredBoolean() throws TemplateException {
            Boolean result = fetchOptionalBoolean();
            if (result == null) {
                throw newUnexpectedTokenException("boolean", env);
            }
            return result;
        }

        private Boolean fetchOptionalBoolean() throws DocgenTagException {
            String name = fetchOptionalVariableName();
            if (name == null) {
                return null;
            }
            if (name.equals("true")) {
                return true;
            } else if (name.equals("false")) {
                return false;
            } else {
                throw new DocgenTagException("true or false", env);
            }
        }

        private char charAt(int index) {
            return index < text.length() ? text.charAt(index) : 0;
        }

        private DocgenTagException newUnexpectedTokenException(String expectedTokenDesc, Environment env) {
            return new DocgenTagException(
                    "Expected " + expectedTokenDesc + " after this: " + text.substring(lastDocgenTagStart, cursor),
                    env);
        }

        private TemplateException newErrorInDocgenTag(String errorDetail) {
            return newErrorInDocgenTag(errorDetail, null);
        }

        private TemplateException newErrorInDocgenTag(String errorDetail, Throwable cause) {
            return new DocgenTagException(
                    "\nError in docgen tag: " + text.substring(lastDocgenTagStart, cursor) + "\n" + errorDetail
                            + (cause != null ? "\nSee cause exception for more!" : ""),
                    cause,
                    env);
        }

        private TemplateException newFormattingFailedException(String customVarName, TemplateValueFormatException e) {
            return new TemplateException(
                    "Formatting failed for Docgen custom variable " + StringUtil.jQuote(customVarName),
                    e, env);
        }

        private InsertDirectiveArgs fetchInsertDirectiveArgs(
                String subvarName, InsertDirectiveType insertDirectiveType) throws
                TemplateException {
            InsertDirectiveArgs args = new InsertDirectiveArgs();
            args.toOptional = true;

            if (insertDirectiveType == INSERT_FILE) {
                skipWS();
                args.path = fetchRequiredString();
            }

            Set<String> paramNamesSeen = new HashSet<>();
            String paramName;
            while (skipWS() && (paramName = fetchOptionalVariableName()) != null) {
                skipRequiredToken("=");
                if (!paramNamesSeen.add(paramName)) {
                    throw new DocgenTagException(
                            "Duplicate docgen." + subvarName +  " parameter " + StringUtil.jQuote(paramName) + ".",
                            env);
                }
                if (insertDirectiveType == INSERT_FILE && paramName.equals("charset")) {
                    args.charset = StringEscapeUtils.unescapeXml(fetchRequiredString());
                } else if (paramName.equals("from")) {
                    args.from = parseRegularExpressionParam(paramName, StringEscapeUtils.unescapeXml(fetchRequiredString()));
                } else if (paramName.equals("to")) {
                    args.to = parseRegularExpressionParam(paramName, StringEscapeUtils.unescapeXml(fetchRequiredString()));
                } else if (paramName.equals("fromOptional")) {
                    args.fromOptional = fetchRequiredBoolean();
                } else if (paramName.equals("toOptional")) {
                    args.toOptional = fetchRequiredBoolean();
                } else if (insertDirectiveType == INSERT_OUTPUT
                        && paramName.equals("printCommand")) {
                    args.printCommand = fetchRequiredBoolean();
                } else {
                    throw new DocgenTagException(
                            "Unsupported docgen." + subvarName +  " parameter " + StringUtil.jQuote(paramName) + ".",
                            env);
                }
            }

            skipRequiredToken(DOCGEN_TAG_END);
            int indexAfterStartTag = cursor;

            if (insertDirectiveType == INSERT_OUTPUT) {
                int endTagIndex = findNextDocgenEndTag(cursor);
                if (endTagIndex == -1) {
                    throw new DocgenTagException(
                            "Missing docgen end-tag after " + DOCGEN_TAG_START + "." + subvarName + " ...]", env);
                }
                lastDocgenTagStart = endTagIndex;

                args.body = StringEscapeUtils.unescapeXml(text.substring(indexAfterStartTag, endTagIndex));

                cursor = endTagIndex + DOCGEN_END_TAG_START.length();
                skipRequiredToken(".");
                String endSubvarName = fetchRequiredVariableName();
                if (!endSubvarName.equals(subvarName)) {
                    throw new DocgenTagException(
                            "End-tag " + DOCGEN_END_TAG_START + "." + endSubvarName + "] doesn't match "
                                    + DOCGEN_TAG_START + "." + subvarName + " ...] tag.", env);
                }
                skipRequiredToken("]");
            }

            args.indexAfterDirective = cursor;

            return args;
        }

        private Pattern parseRegularExpressionParam(String paramName, String paramValue) throws TemplateException {
            Objects.requireNonNull(paramName);
            Objects.requireNonNull(paramValue);
            Pattern parsedParamValue;
            try {
                parsedParamValue = Pattern.compile(paramValue, Pattern.MULTILINE);
            } catch (PatternSyntaxException e) {
                throw newErrorInDocgenTag("Invalid regular expression for parameter \"" +
                        paramName + "\": " + paramValue);
            }
            return parsedParamValue;
        }

    }

    private static String setOrClearSystemProperty(String k, String v) {
        return v != null ? System.setProperty(k, v) : System.clearProperty(k);
    }

    public static String removeFTLCopyrightComment(String ftl) {
        int copyrightPartIdx = ftl.indexOf("Licensed to the Apache Software Foundation");
        if (copyrightPartIdx == -1) {
            return ftl;
        }

        final int commentFirstIdx;
        final boolean squareBracketTagSyntax;
        {
            String ftlBeforeCopyright = ftl.substring(0, copyrightPartIdx);
            int abCommentStart = ftlBeforeCopyright.lastIndexOf("<#--");
            int sbCommentStart = ftlBeforeCopyright.lastIndexOf("[#--");
            squareBracketTagSyntax = sbCommentStart > abCommentStart;
            commentFirstIdx = squareBracketTagSyntax ? sbCommentStart : abCommentStart;
            if (commentFirstIdx == -1) {
                throw new AssertionError("Can't find copyright comment start");
            }
        }

        final int commentLastIdx;
        {
            int commentEndStart = ftl.indexOf(squareBracketTagSyntax ? "--]" : "-->", copyrightPartIdx);
            if (commentEndStart == -1) {
                throw new AssertionError("Can't find copyright comment end");
            }
            commentLastIdx = commentEndStart + 2;
        }

        final int afterCommentNLChars;
        if (commentLastIdx + 1 < ftl.length()) {
            char afterCommentChar = ftl.charAt(commentLastIdx + 1);
            if (afterCommentChar == '\n' || afterCommentChar == '\r') {
                if (afterCommentChar == '\r' && commentLastIdx + 2 < ftl.length()
                        && ftl.charAt(commentLastIdx + 2) == '\n') {
                    afterCommentNLChars = 2;
                } else {
                    afterCommentNLChars = 1;
                }
            } else {
                afterCommentNLChars = 0;
            }
        } else {
            afterCommentNLChars = 0;
        }

        return ftl.substring(0, commentFirstIdx) + ftl.substring(commentLastIdx + afterCommentNLChars + 1);
    }

    static class InsertDirectiveArgs {
        private String path;
        private String charset;
        private Pattern from;
        private boolean fromOptional;
        private Pattern to;
        private boolean toOptional;
        private String body;
        private int indexAfterDirective;
        private boolean printCommand;
    }

}
