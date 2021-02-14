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

import static org.freemarker.docgen.core.DocBook5Constants.*;
import static org.freemarker.docgen.core.SettingUtils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.dom.NodeModel;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.DateParseException;
import freemarker.template.utility.StringUtil;

public final class Transform {

    // -------------------------------------------------------------------------
    // Constants:

    static final String FILE_BOOK = "book.xml";
    static final String FILE_ARTICLE = "article.xml";
    static final String FILE_SETTINGS = "docgen.cjson";
    /** Used for the Table of Contents file when a different node was marked to be the index.html. */
    static final String FILE_TOC_HTML = "toc.html";
    static final String FILE_DETAILED_TOC_HTML = "detailed-toc.html";
    static final String FILE_INDEX_HTML = "index.html";
    static final String FILE_SEARCH_RESULTS_HTML = "search-results.html";
    static final String FILE_TOC_JSON_TEMPLATE = "toc-json.ftl";
    static final String FILE_TOC_JSON_OUTPUT = "toc.js";
    static final String FILE_ECLIPSE_TOC_TEMPLATE = "eclipse-toc.ftlx";
    static final String FILE_ECLIPSE_TOC_OUTPUT = "eclipse-toc.xml";
    static final String DIR_TEMPLATES = "docgen-templates";

    static final String FILE_SITEMAP_XML_TEMPLATE = "sitemap.ftlx";
    static final String FILE_SITEMAP_XML_OUTPUT = "sitemap.xml";

    static final String SETTING_IGNORED_FILES = "ignoredFiles";
    static final String SETTING_VALIDATION = "validation";
    static final String SETTING_OFFLINE = "offline";
    static final String SETTING_SIMPLE_NAVIGATION_MODE = "simpleNavigationMode";
    static final String SETTING_DEPLOY_URL = "deployUrl";
    static final String SETTING_ONLINE_TRACKER_HTML = "onlineTrackerHTML";
    static final String SETTING_COOKIE_CONSENT_SCRIPT_URL = "cookieConsentScriptURL";
    static final String SETTING_REMOVE_NODES_WHEN_ONLINE = "removeNodesWhenOnline";
    static final String SETTING_INTERNAL_BOOKMARKS = "internalBookmarks";
    static final String SETTING_EXTERNAL_BOOKMARKS = "externalBookmarks";
    static final String SETTING_COPYRIGHT_HOLDER = "copyrightHolder";
    static final String SETTING_COPYRIGHT_HOLDER_SITE = "copyrightHolderSite";
    static final String SETTING_COPYRIGHT_START_YEAR = "copyrightStartYear";
    static final String SETTING_COPYRIGHT_SUFFIX = "copyrightSuffix";
    static final String SETTING_COPYRIGHT_COMMENT_FILE = "copyrightCommentFile";
    static final String SETTING_SEO_META = "seoMeta";
    static final String SETTING_LOGO = "logo";
    static final String SETTING_LOGO_KEY_SRC = "src";
    static final String SETTING_LOGO_KEY_ALT = "alt";
    static final String SETTING_LOGO_KEY_HREF = "href";
    static final Set<String> SETTING_LOGO_MAP_KEYS;
    static {
        SETTING_LOGO_MAP_KEYS = new LinkedHashSet<>();
        SETTING_LOGO_MAP_KEYS.add(SETTING_LOGO_KEY_SRC);
        SETTING_LOGO_MAP_KEYS.add(SETTING_LOGO_KEY_ALT);
        SETTING_LOGO_MAP_KEYS.add(SETTING_LOGO_KEY_HREF);
    }
    static final String SETTING_SIDE_TOC_LOGOS = "sideTOCLogos";
    static final String SETTING_TABS = "tabs";
    static final String SETTING_SECONDARY_TABS = "secondaryTabs";
    static final String SETTING_SOCIAL_LINKS = "socialLinks";
    static final String SETTING_FOOTER_SITEMAP = "footerSiteMap";
    static final String SETTING_OLINKS = "olinks";
    static final String SETTING_ECLIPSE = "eclipse";
    static final String SETTING_SHOW_EDITORAL_NOTES = "showEditoralNotes";
    static final String SETTING_GENERATE_ECLIPSE_TOC = "generateEclipseTOC";
    static final String SETTING_SHOW_XXE_LOGO = "showXXELogo";
    static final String SETTING_SEARCH_KEY = "searchKey";
    static final String SETTING_DISABLE_JAVASCRIPT = "disableJavaScript";
    static final String SETTING_TIME_ZONE = "timeZone";
    static final String SETTING_LOCALE = "locale";
    static final String SETTING_CONTENT_DIRECTORY = "contentDirectory";
    static final String SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK
            = "lowestPageTOCElementRank";
    static final String SETTING_LOWEST_FILE_ELEMENT_RANK
            = "lowestFileElementRank";
    static final String SETTING_MAX_TOF_DISPLAY_DEPTH = "maxTOFDisplayDepth";
    static final String SETTING_MAX_MAIN_TOF_DISPLAY_DEPTH
            = "maxMainTOFDisplayDepth";
    static final String SETTING_NUMBERED_SECTIONS = "numberedSections";
    static final String SETTING_CUSTOM_VARIABLES = "customVariables";
    static final String SETTING_INSERTABLE_FILES = "insertableFiles";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS = "insertableOutputCommands";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_CLASS_KEY = "mainClass";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_METHOD_KEY = "mainMethod";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS_SYSTEM_PROPERTIES_KEY = "systemProperties";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS_PREPENDED_ARGUMENTS_KEY = "prependedArguments";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS_APPENDED_ARGUMENTS_KEY = "appendedArguments";
    static final String SETTING_INSERTABLE_OUTPUT_COMMANDS_DOCGEN_WD_REPLACED_WITH_KEY = "docgenWdReplacedWith";
    static final Set<String> SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS;
    static final Set<String> SETTING_INSERTABLE_OUTPUT_COMMANDS_REQUIRED_KEYS;
    static {
        SETTING_INSERTABLE_OUTPUT_COMMANDS_REQUIRED_KEYS = new LinkedHashSet<>();
        SETTING_INSERTABLE_OUTPUT_COMMANDS_REQUIRED_KEYS.add(SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_CLASS_KEY);
        SETTING_INSERTABLE_OUTPUT_COMMANDS_REQUIRED_KEYS.add(SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_METHOD_KEY);
        SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS = new LinkedHashSet<>();
        SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS.add(SETTING_INSERTABLE_OUTPUT_COMMANDS_SYSTEM_PROPERTIES_KEY);
        SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS.add(SETTING_INSERTABLE_OUTPUT_COMMANDS_PREPENDED_ARGUMENTS_KEY);
        SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS.add(SETTING_INSERTABLE_OUTPUT_COMMANDS_APPENDED_ARGUMENTS_KEY);
        SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS.add(
                SETTING_INSERTABLE_OUTPUT_COMMANDS_DOCGEN_WD_REPLACED_WITH_KEY);
    }

    static final String SETTING_VALIDATION_PROGRAMLISTINGS_REQ_ROLE
            = "programlistingsRequireRole";
    static final String SETTING_VALIDATION_PROGRAMLISTINGS_REQ_LANG
            = "programlistingsRequireLanguage";
    static final String SETTING_VALIDATION_OUTPUT_FILES_CAN_USE_AUTOID
            = "outputFilesCanUseAutoID";
    static final String SETTING_VALIDATION_MAXIMUM_PROGRAMLISTING_WIDTH
            = "maximumProgramlistingWidth";
    static final String SETTING_ECLIPSE_LINK_TO = "link_to";

    static final String SETTING_SEO_META_KEY_TITLE = "title";
    static final String SETTING_SEO_META_KEY_FULL_TITLE = "fullTitle";
    static final String SETTING_SEO_META_KEY_DESCRIPTION = "description";
    static final Set<String> SETTING_SEO_META_KEYS;
    static {
        SETTING_SEO_META_KEYS = new LinkedHashSet<>();
        SETTING_SEO_META_KEYS.add(SETTING_SEO_META_KEY_TITLE);
        SETTING_SEO_META_KEYS.add(SETTING_SEO_META_KEY_FULL_TITLE);
        SETTING_SEO_META_KEYS.add(SETTING_SEO_META_KEY_DESCRIPTION);
    }

    static final String COMMON_LINK_KEY_CLASS = "class";
    static final String COMMON_LINK_KEY_HREF = "href";
    static final Set<String> COMMON_LINK_KEYS;
    static {
        COMMON_LINK_KEYS = new LinkedHashSet<>();
        COMMON_LINK_KEYS.add(COMMON_LINK_KEY_CLASS);
        COMMON_LINK_KEYS.add(COMMON_LINK_KEY_HREF);
    }

    private static final String VAR_OFFLINE
            = SETTING_OFFLINE;
    private static final String VAR_SIMPLE_NAVIGATION_MODE
            = SETTING_SIMPLE_NAVIGATION_MODE;
    private static final String VAR_DEPLOY_URL
            = SETTING_DEPLOY_URL;
    private static final String VAR_ONLINE_TRACKER_HTML
            = SETTING_ONLINE_TRACKER_HTML;
    private static final String VAR_COOKIE_CONSENT_SCRIPT_URL = SETTING_COOKIE_CONSENT_SCRIPT_URL;
    private static final String VAR_COPYRIGHT_COMMENT = "copyrightComment";
    private static final String VAR_COPYRIGHT_JAVA_COMMENT = "copyrightJavaComment";
    private static final String VAR_SHOW_EDITORAL_NOTES
            = "showEditoralNotes";
    private static final String VAR_TRANSFORM_START_TIME
            = "transformStartTime";
    private static final String VAR_SHOW_XXE_LOGO
            = SETTING_SHOW_XXE_LOGO;
    private static final String VAR_SEARCH_KEY
            = SETTING_SEARCH_KEY;
    private static final String VAR_DISABLE_JAVASCRIPT
            = SETTING_DISABLE_JAVASCRIPT;
    private static final String VAR_ECLIPSE_LINK_TO = SETTING_ECLIPSE_LINK_TO;
    private static final String VAR_INTERNAL_BOOKMARDS
            = SETTING_INTERNAL_BOOKMARKS;
    private static final String VAR_EXTERNAL_BOOKMARDS
            = SETTING_EXTERNAL_BOOKMARKS;
    private static final String VAR_LOGO = SETTING_LOGO;
    private static final String VAR_SIDE_TOC_LOGOS = SETTING_SIDE_TOC_LOGOS;
    private static final String VAR_COPYRIGHT_HOLDER = SETTING_COPYRIGHT_HOLDER;
    private static final String VAR_COPYRIGHT_HOLDER_SITE = SETTING_COPYRIGHT_HOLDER_SITE;
    private static final String VAR_COPYRIGHT_START_YEAR = SETTING_COPYRIGHT_START_YEAR;
    private static final String VAR_COPYRIGHT_SUFFIX = SETTING_COPYRIGHT_SUFFIX;
    private static final String VAR_SEO_META_TITLE_OVERRIDE = "seoMetaTitleOverride";
    private static final String VAR_SEO_META_FULL_TITLE_OVERRIDE = "seoMetaFullTitleOverride";
    private static final String VAR_SEO_META_DESCRIPTION = "seoMetaDescription";
    private static final String VAR_TABS = SETTING_TABS;
    private static final String VAR_SECONDARY_TABS = SETTING_SECONDARY_TABS;
    private static final String VAR_SOCIAL_LINKS = SETTING_SOCIAL_LINKS;
    private static final String VAR_FOOTER_SITEMAP = SETTING_FOOTER_SITEMAP;
    private static final String VAR_OLINKS
            = SETTING_OLINKS;
    private static final String VAR_TOC_DISPLAY_DEPTH
            = SETTING_MAX_TOF_DISPLAY_DEPTH;
    private static final String VAR_NUMBERED_SECTIONS
            = SETTING_NUMBERED_SECTIONS;
    static final String VAR_CUSTOM_VARIABLES = SETTING_CUSTOM_VARIABLES;
    private static final String VAR_INDEX_ENTRIES
            = "indexEntries";
    private static final String VAR_PAGE_TYPE = "pageType";
    private static final String VAR_ALTERNATIVE_TOC_LINK
            = "alternativeTOCLink";
    private static final String VAR_ALTERNATIVE_TOC_LABEL
            = "alternativeTOCLabel";
    private static final String VAR_PARENT_FILE_ELEMENT = "parentFileElement";
    private static final String VAR_NEXT_FILE_ELEMENT = "nextFileElement";
    private static final String VAR_PREVIOUS_FILE_ELEMENT
            = "previousFileElement";
    private static final String VAR_ROOT_ELEMENT = "rootElement";
    private static final String VAR_SHOW_NAVIGATION_BAR = "showNavigationBar";
    private static final String VAR_SHOW_BREADCRUMB = "showBreadCrumb";

    private static final String VAR_JSON_TOC_ROOT = "tocRoot";

    private static final String PAGE_TYPE_DETAILED_TOC = "docgen:detailed_toc";
    private static final String PAGE_TYPE_SEARCH_RESULTS = "docgen:search_results";

    private static final String OLINK_SCHEMA_START = "olink:";
    private static final String ID_SCHEMA_START = "id:";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    static final String SYSPROP_GENERATION_TIME = "docgen.generationTime";

    // Docgen-specific XML attributes (added during DOM-tree postediting):

    /**
     * Marks an element for which a separate file is created; attached to
     * document structure elements, value is always {@code "true"}.
     */
    private static final String A_DOCGEN_FILE_ELEMENT = "docgen_file_element";

    /**
     * Marks an element for which a page ToC ("Page Contents") line is shown;
     * attached to document structure elements, it's value is always
     * {@code "true"}.
     */
    private static final String A_DOCGEN_PAGE_TOC_ELEMENT
            = "docgen_page_toc_element";

    /**
     * Marks and element that is shown in the <em>detailed</em> main ToC;
     * attached to document structure elements, it's value is always
     * {@code "true"}.
     */
    private static final String A_DOCGEN_DETAILED_TOC_ELEMENT
            = "docgen_detailed_toc_element";

    /**
     * The top-level document-structure element is marked with this;
     * it's value is always {@code "true"}.
     */
    private static final String A_DOCGEN_ROOT_ELEMENT = "docgen_root_element";

    /**
     * The numbering or letter or whatever that is shown before the tile, such
     * as "2.4" or "IV"; attached to document structure elements that use a
     * title prefix.
     */
    private static final String A_DOCGEN_TITLE_PREFIX = "docgen_title_prefix";

    /**
     * The integer ordinal of the document structure element within its own ToC
     * level, counting all kind of preceding document structure siblings;
     * attached to the document structure element.
     *
     * @see #A_DOCGEN_NUMBERING
     */
    private static final String A_DOCGEN_UNITED_NUMBERING
        = "docgen_united_numbering";

    /**
     * Describes how "big" a title should be; attached to the document structure
     * element (no to the title element). For the possible values see the
     * {@code AV_DOCGEN_TITLE_RANK_...} constants. For even more information see
     * {@link #preprocessDOM_addRanks(Document)}.
     */
    private static final String A_DOCGEN_RANK = "docgen_rank";

    /** An element for which it's not possible to create a link. */
    private static final String A_DOCGEN_NOT_ADDRESSABLE = "docgen_not_addressable";

    private static final String AV_INDEX_ROLE = "index.html";

    /**
     * This is how automatically added id attribute values start.
     */
    static final String AUTO_ID_PREFIX = "autoid_";

    static final String DOCGEN_ID_PREFIX = "docgen_";

    /** Elements for which an id attribute automatically added if missing */
    private static final Set<String> GUARANTEED_ID_ELEMENTS;
    static {
        Set<String> idAttElems = new HashSet<>();

        for (String elemName : DOCUMENT_STRUCTURE_ELEMENTS) {
            idAttElems.add(elemName);
        }

        idAttElems.add(E_GLOSSARY);
        idAttElems.add(E_GLOSSENTRY);

        GUARANTEED_ID_ELEMENTS = Collections.unmodifiableSet(idAttElems);
    }

    /**
     * Elements whose children will go into a single output file regardless
     * of the element ranks, and whose children never use title prefixes
     * (labels).
     */
    private static final Set<String> PREFACE_LIKE_ELEMENTS;
    static {
        Set<String> sinlgeFileElems = new HashSet<>();

        sinlgeFileElems.add(E_PREFACE);

        PREFACE_LIKE_ELEMENTS = Collections.unmodifiableSet(sinlgeFileElems);
    }

    private static final String XMLNS_DOCGEN = "http://freemarker.org/docgen";
    private static final String E_SEARCHRESULTS = "searchresults";
    private static final String SEARCH_RESULTS_PAGE_TITLE = "Search results";
    private static final String SEARCH_RESULTS_ELEMENT_ID = "searchresults";

    // -------------------------------------------------------------------------
    // Settings:

    private File cfgFile;

    private File destDir;

    private File srcDir;

    private File contentDir;

    private File customVariableFileDir;

    private List<Pattern> ignoredFilePathPatterns = new ArrayList<>();

    private Boolean offline;

    private String deployUrl;

    private String onlineTrackerHTML;
    private String cookieConstentScriptURL;

    private Set<String> removeNodesWhenOnline;

    /** Element types for which a new output file is created  */
    private DocumentStructureRank lowestFileElemenRank = DocumentStructureRank.SECTION1;

    private DocumentStructureRank lowestPageTOCElemenRank = DocumentStructureRank.SECTION3;

    private int maxTOFDisplayDepth = Integer.MAX_VALUE;

    private int maxMainTOFDisplayDepth;  // 0 indicates "not set";

    private boolean numberedSections;

    private boolean generateEclipseTOC;

    private boolean simpleNavigationMode;

    private boolean showEditoralNotes;

    private boolean showXXELogo;

    private String searchKey;

    private boolean disableJavaScript;

    private boolean validate = true;

    private Locale locale = Locale.US;

    private TimeZone timeZone = TimeZone.getTimeZone("GMT");

    private boolean printProgress;

    private final LinkedHashMap<String, String> internalBookmarks = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> externalBookmarks = new LinkedHashMap<>();
    private Map<String, Map<String, String>> footerSiteMap = new LinkedHashMap<>();;

    private final Map<String, Object> customVariablesFromSettingsFile = new HashMap<>();
    private final Map<String, Object> customVariableOverrides = new HashMap<>();

    private final Map<String, String> insertableFilesFromSettingsFile = new HashMap<>();
    private final Map<String, String> insertableFilesOverrides = new HashMap<>();

    private final Map<String, InsertableOutputCommandProperties> insertableOutputCommands = new HashMap<>();

    private final LinkedHashMap<String, String> tabs = new LinkedHashMap<>();

    private final Map<String, Map<String, String>> secondaryTabs = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> socialLinks = new LinkedHashMap<>();

    private Logo logo;

    private final List<Logo> sideTOCLogos = new ArrayList<>();

    private String copyrightHolder;
    private String copyrightHolderSite;
    private String copyrightSuffix;
    private Integer copyrightStartYear;
    private String copyrightComment;
    private String copyrightJavaComment;

    private final Map<String, Map<String, String>> seoMeta = new LinkedHashMap();

    private DocgenValidationOptions validationOps = new DocgenValidationOptions();

    String eclipseLinkTo;

    // -------------------------------------------------------------------------
    // Global transformation state:

    private boolean executed;

    private Map<String, String> olinks = new HashMap<>();
    private Map<String, List<NodeModel>> primaryIndexTermLookup;
    private Map<String, SortedMap<String, List<NodeModel>>> secondaryIndexTermLookup;
    private Map<String, Element> elementsById;
    private List<TOCNode> tocNodes;
    private List<String> indexEntries;
    private Map<String, Path> insertableFiles;
    private Configuration fmConfig;

    // -------------------------------------------------------------------------
    // Output-file-specific state:

    private TOCNode currentFileTOCNode;

    // -------------------------------------------------------------------------
    // Misc. fields:

    private DocgenLogger logger = new DocgenLogger() {

        @Override
        public void info(String message) {
            if (printProgress) {
                System.out.println(message);
            }
        }

        @Override
        public void warning(String message) {
            if (printProgress) {
                System.out.println("Warning:" + message);
            }
        }
    };

    // -------------------------------------------------------------------------
    // Methods:

    /**
     * Loads the source XML and generates the output in the destination
     * directory. Don't forget to set JavaBean properties first.
     *
     * @throws DocgenException If a docgen-specific error occurs
     * @throws IOException If a file or other resource is missing or otherwise
     *      can't be read/written.
     * @throws SAXException If the XML is not well-formed and valid, or the
     *      SAX XML parsing has other problems.
     */
    public void execute()
            throws DocgenException, IOException, SAXException {
        if (executed) {
            throw new DocgenException(
                    "This transformation was alrady executed; "
                    + "use a new " + Transform.class.getName() + ".");
        }
        executed  = true;

        // Check Java Bean properties:

        if (srcDir == null) {
            throw new DocgenException(
                    "The source directory (the DocBook XML) wasn't specified.");
        }
        if (!srcDir.isDirectory()) {
            throw new IOException(
                    "Source directory doesn't exist: "
                    + srcDir.getAbsolutePath());
        }

        if (destDir == null) {
            throw new DocgenException(
                    "The destination directory wasn't specified.");
        }
        // Note: This directory will be created automatically if missing.

        // Load configuration file:

        File templatesDir = null;

        cfgFile = new File(srcDir, FILE_SETTINGS);
        if (cfgFile.exists()) {
            Map<String, Object> cfg;
            try {
                cfg = CJSONInterpreter.evalAsMap(cfgFile, new DocgenCJSONEvaluationEnvironment(), false);
            } catch (CJSONInterpreter.EvaluationException e) {
                throw new DocgenException(e.getMessage(), e.getCause());
            }

            for (Entry<String, Object> cfgEnt : cfg.entrySet()) {
                final String topSettingName = cfgEnt.getKey();
                final SettingName settingName = SettingName.topLevel(cfgFile, topSettingName);
                final Object settingValue = cfgEnt.getValue();

                if (topSettingName.equals(SETTING_IGNORED_FILES)) {
                    castSettingToList(settingName, settingValue, String.class).forEach(
                            pattern -> ignoredFilePathPatterns.add(FileUtil.globToRegexp(pattern)));
                } else if (topSettingName.equals(SETTING_OLINKS)) {
                    olinks.putAll(
                            castSettingToMap(settingName, settingValue, String.class, String.class));
                } else if (topSettingName.equals(SETTING_INTERNAL_BOOKMARKS)) {
                    internalBookmarks.putAll(
                            castSettingToMap(settingName, settingValue, String.class, String.class));
                    // Book-mark targets will be checked later, when the XML
                    // document is already loaded.
                } else if (topSettingName.equals(SETTING_EXTERNAL_BOOKMARKS)) {
                    externalBookmarks.putAll(
                            castSettingToMap(settingName, settingValue, String.class, String.class));
                } else if (topSettingName.equals(SETTING_LOGO)) {
                    logo = castMapToLogo(settingName, settingValue);
                } else if (topSettingName.equals(SETTING_SIDE_TOC_LOGOS)) {
                    List<Map<String, Object>> listOfMaps = castSetting(
                            settingName, settingValue,
                            List.class,
                            new ListItemType(Map.class),
                            new MapEntryType<>(String.class, Object.class));
                    for (int i = 0; i < listOfMaps.size(); i++) {
                        sideTOCLogos.add(castMapToLogo(settingName.subKey(i), listOfMaps.get(i)));
                    }
                } else if (topSettingName.equals(SETTING_COPYRIGHT_HOLDER)) {
                    copyrightHolder = castSetting(settingName, settingValue, String.class);
                } else if (topSettingName.equals(SETTING_COPYRIGHT_HOLDER_SITE)) {
                    copyrightHolderSite = castSetting(settingName, settingValue, String.class);
                } else if (topSettingName.equals(SETTING_COPYRIGHT_START_YEAR)) {
                    copyrightStartYear = castSetting(settingName, settingValue, Integer.class);
                } else if (topSettingName.equals(SETTING_COPYRIGHT_SUFFIX)) {
                    copyrightSuffix = castSetting(settingName, settingValue, String.class);
                } else if (topSettingName.equals(SETTING_COPYRIGHT_COMMENT_FILE)) {
                    copyrightComment =
                            StringUtil.chomp(getFileContentForSetting(settingName, settingValue));
                    String eol = TextUtil.detectEOL(copyrightComment, "\n");
                    StringBuilder sb = new StringBuilder("/*").append(eol);
                    new BufferedReader(new StringReader(copyrightComment)).lines()
                            .forEach(s -> sb.append(" * ").append(s).append(eol));
                    sb.append(" */");
                    copyrightJavaComment = sb.toString();
                } else if (topSettingName.equals(SETTING_SEO_META)) {
                    this.seoMeta.putAll(
                            castSetting(
                                    settingName, settingValue,
                                    Map.class,
                                    new MapEntryType<>(String.class, Map.class),
                                    new MapEntryType<>(
                                            String.class, Collections.emptySet(), SETTING_SEO_META_KEYS,
                                            String.class)));
                } else if (topSettingName.equals(SETTING_CUSTOM_VARIABLES)) {
                    customVariablesFromSettingsFile.putAll(
                            // Allow null values in the Map, as the caller can override them.
                            castSettingToMap(settingName, settingValue, String.class, Object.class, true));
                } else if (topSettingName.equals(SETTING_INSERTABLE_FILES)) {
                    insertableFilesFromSettingsFile.putAll(
                            // Allow null values in the Map, as the caller can override them.
                            castSettingToMap(settingName, settingValue, String.class, String.class, true));
                } else if (topSettingName.equals(SETTING_INSERTABLE_OUTPUT_COMMANDS)) {
                    Map<String, Map<String, Object>> m = castSetting(
                            settingName, settingValue,
                            Map.class,
                            new MapEntryType(String.class, Map.class),
                            new MapEntryType(
                                    String.class, SETTING_INSERTABLE_OUTPUT_COMMANDS_REQUIRED_KEYS, SETTING_INSERTABLE_OUTPUT_COMMANDS_OPTIONAL_KEYS,
                                    Object.class, false));
                    for (Entry<String, Map<String, Object>> ent : m.entrySet()) {
                        String commandKey = ent.getKey();
                        Map<String, Object> outputCmdProps = ent.getValue();
                        InsertableOutputCommandProperties commandProps = new InsertableOutputCommandProperties(
                                castSetting(
                                        settingName.subKey(commandKey,
                                                SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_CLASS_KEY),
                                        outputCmdProps.get(SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_CLASS_KEY),
                                        String.class
                                ),
                                castSetting(
                                        settingName.subKey(commandKey,
                                                SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_METHOD_KEY),
                                        outputCmdProps.get(SETTING_INSERTABLE_OUTPUT_COMMANDS_MAIN_METHOD_KEY),
                                        String.class
                                ),
                                castSetting(
                                        settingName.subKey(commandKey, SETTING_INSERTABLE_OUTPUT_COMMANDS_SYSTEM_PROPERTIES_KEY),
                                        outputCmdProps.get(SETTING_INSERTABLE_OUTPUT_COMMANDS_SYSTEM_PROPERTIES_KEY),
                                        new DefaultValue<>(Collections.emptyMap()),
                                        Map.class, new MapEntryType(String.class, String.class)
                                ),
                                castSetting(
                                        settingName.subKey(commandKey, SETTING_INSERTABLE_OUTPUT_COMMANDS_PREPENDED_ARGUMENTS_KEY),
                                        outputCmdProps.get(SETTING_INSERTABLE_OUTPUT_COMMANDS_PREPENDED_ARGUMENTS_KEY),
                                        new DefaultValue<>(Collections.emptyList()),
                                        List.class
                                ),
                                castSetting(
                                        settingName.subKey(commandKey, SETTING_INSERTABLE_OUTPUT_COMMANDS_APPENDED_ARGUMENTS_KEY),
                                        outputCmdProps.get(SETTING_INSERTABLE_OUTPUT_COMMANDS_APPENDED_ARGUMENTS_KEY),
                                        new DefaultValue<>(Collections.emptyList()),
                                        List.class
                                ),
                                Optional.ofNullable(
                                        SettingUtils.<String>castSetting( // Explicit generic type to dodge JDK 8 252 bug
                                                settingName.subKey(commandKey,
                                                        SETTING_INSERTABLE_OUTPUT_COMMANDS_DOCGEN_WD_REPLACED_WITH_KEY),
                                                outputCmdProps.get(
                                                        SETTING_INSERTABLE_OUTPUT_COMMANDS_DOCGEN_WD_REPLACED_WITH_KEY),
                                                DefaultValue.NULL,
                                                String.class
                                        )
                                ).map(it -> Paths.get(it).toAbsolutePath().normalize()).orElse(null)
                        );
                        insertableOutputCommands.put(commandKey, commandProps);
                    }
                } else if (topSettingName.equals(SETTING_TABS)) {
                    tabs.putAll(
                            castSettingToMap(settingName, settingValue, String.class, String.class));
                } else if (topSettingName.equals(SETTING_SECONDARY_TABS)) {
                    secondaryTabs.putAll(
                            castSetting(
                                    settingName, settingValue,
                                    Map.class,
                                    new MapEntryType(String.class, Map.class),
                                    new MapEntryType(String.class, COMMON_LINK_KEYS, String.class)));
                } else if (topSettingName.equals(SETTING_SOCIAL_LINKS)) {
                    socialLinks.putAll(
                            castSetting(
                                    settingName, settingValue,
                                    Map.class,
                                    new MapEntryType(String.class, Map.class),
                                    new MapEntryType(String.class, COMMON_LINK_KEYS, String.class)));
                } else if (topSettingName.equals(SETTING_FOOTER_SITEMAP)) {
                    footerSiteMap.putAll(
                            castSetting(
                                    settingName, settingValue,
                                    Map.class,
                                    new MapEntryType(String.class, Map.class),
                                    new MapEntryType(String.class, String.class)));
                }else if (topSettingName.equals(SETTING_VALIDATION)) {
                    castSettingToMap(settingName, settingValue, String.class, Object.class)
                            .forEach((name, value) -> {
                                if (name.equals(
                                        SETTING_VALIDATION_PROGRAMLISTINGS_REQ_ROLE)) {
                                    validationOps.setProgramlistingRequiresRole(
                                            castSetting(settingName.subKey(name), value, Boolean.class));
                                } else if (name.equals(
                                        SETTING_VALIDATION_PROGRAMLISTINGS_REQ_LANG)) {
                                    validationOps.setProgramlistingRequiresLanguage(
                                            castSetting(settingName.subKey(name), value, Boolean.class));
                                } else if (name.equals(
                                        SETTING_VALIDATION_OUTPUT_FILES_CAN_USE_AUTOID)
                                ) {
                                    validationOps.setOutputFilesCanUseAutoID(
                                            castSetting(settingName.subKey(name), value, Boolean.class));
                                } else if (name.equals(
                                        SETTING_VALIDATION_MAXIMUM_PROGRAMLISTING_WIDTH)
                                ) {
                                    validationOps.setMaximumProgramlistingWidth(
                                            castSetting(settingName.subKey(name), value, Integer.class));
                                } else {
                                    throw newCfgFileException(settingName.subKey(name), "Unknown validation option: " + name);
                                }
                            });
                } else if (topSettingName.equals(SETTING_OFFLINE)) {
                    if (offline == null) {  // Ignore if the caller has already set this
                        offline = castSetting(settingName, settingValue, Boolean.class);
                    }
                } else if (topSettingName.equals(SETTING_SIMPLE_NAVIGATION_MODE)) {
                    simpleNavigationMode = castSetting(settingName, settingValue, Boolean.class);
                } else if (topSettingName.equals(SETTING_DEPLOY_URL)) {
                    deployUrl = castSetting(settingName, settingValue, String.class);
                } else if (topSettingName.equals(SETTING_ONLINE_TRACKER_HTML)) {
                    onlineTrackerHTML = getFileContentForSetting(settingName, settingValue);
                    if (onlineTrackerHTML.startsWith("<!--")) {
                        int commentEnd = onlineTrackerHTML.indexOf("-->");
                        if (commentEnd != -1) {
                            commentEnd += 3;
                            String comment = onlineTrackerHTML.substring(0, commentEnd);
                            if (comment.contains("copyright") || comment.contains("Copyright")) {
                                onlineTrackerHTML = onlineTrackerHTML.substring(commentEnd);
                            }
                        }
                    }
                    String eol = TextUtil.detectEOL(onlineTrackerHTML, "\n");
                    onlineTrackerHTML = onlineTrackerHTML.trim();
                    onlineTrackerHTML += eol;
                } else if (topSettingName.equals(SETTING_COOKIE_CONSENT_SCRIPT_URL)) {
                    cookieConstentScriptURL = castSetting(settingName, settingValue, String.class);
                } else if (topSettingName.equals(SETTING_REMOVE_NODES_WHEN_ONLINE)) {
                    removeNodesWhenOnline = Collections.unmodifiableSet(new HashSet<>(
                            castSettingToList(settingName, settingValue, String.class)));
                } else if (topSettingName.equals(SETTING_ECLIPSE)) {
                    castSettingToMap(settingName, settingValue, String.class, Object.class)
                            .forEach((name, value) -> {
                                if (name.equals(SETTING_ECLIPSE_LINK_TO)) {
                                    eclipseLinkTo = castSetting(
                                            settingName.subKey(name), value, String.class);
                                } else {
                                    throw newCfgFileException(settingName, "Unknown Eclipse option: " + name);
                                }
                            });
                } else if (topSettingName.equals(SETTING_LOCALE)) {
                    String s = castSetting(settingName, settingValue, String.class);
                    locale = StringUtil.deduceLocale(s);
                } else if (topSettingName.equals(SETTING_TIME_ZONE)) {
                    String s = castSetting(settingName, settingValue, String.class);
                    timeZone = TimeZone.getTimeZone(s);
                } else if (topSettingName.equals(SETTING_GENERATE_ECLIPSE_TOC)) {
                    generateEclipseTOC = castSetting(settingName, settingValue, Boolean.class);
                } else if (topSettingName.equals(SETTING_SHOW_EDITORAL_NOTES)) {
                    showEditoralNotes = castSetting(settingName, settingValue, Boolean.class);
                } else if (topSettingName.equals(SETTING_SHOW_XXE_LOGO)) {
                    showXXELogo = castSetting(settingName, settingValue, Boolean.class);
                } else if (topSettingName.equals(SETTING_SEARCH_KEY)) {
                    searchKey = castSetting(settingName, settingValue, String.class);
                }else if (topSettingName.equals(SETTING_DISABLE_JAVASCRIPT)) {
                    disableJavaScript = castSetting(settingName, settingValue, Boolean.class);
                } else if (topSettingName.equals(SETTING_CONTENT_DIRECTORY)) {
                    String s = castSetting(settingName, settingValue, String.class);
                    contentDir = new File(srcDir, s);
                    if (!contentDir.isDirectory()) {
                        throw newCfgFileException(
                                settingName,
                                "It's not an existing directory: " + contentDir.getAbsolutePath());
                    }
                } else if (topSettingName.equals(SETTING_LOWEST_FILE_ELEMENT_RANK)
                        || topSettingName.equals(SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK)) {
                    DocumentStructureRank rank;
                    String strRank = castSetting(settingName, settingValue, String.class);
                    try {
                        rank = DocumentStructureRank.valueOf(
                                strRank.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        String msg;
                        if (strRank.equalsIgnoreCase("article")) {
                            msg = "\"article\" is not a rank, since articles "
                                + "can have various ranks depending on their "
                                + "context. (Hint: if the article is the "
                                + "top-level element then it has \"chapter\" "
                                + "rank.)";
                        } else {
                            msg = "Unknown rank: " + strRank;
                        }
                        throw newCfgFileException(settingName, msg);
                    }

                    if (topSettingName.equals(SETTING_LOWEST_FILE_ELEMENT_RANK)) {
                        lowestFileElemenRank = rank;
                    } else if (topSettingName.equals(SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK)) {
                        lowestPageTOCElemenRank = rank;
                    } else {
                        throw new BugException("Unexpected setting name.");
                    }
                } else if (topSettingName.equals(SETTING_MAX_TOF_DISPLAY_DEPTH)) {
                    maxTOFDisplayDepth = castSetting(settingName, settingValue, Integer.class);
                    if (maxTOFDisplayDepth < 1) {
                        throw newCfgFileException(settingName, "Value must be at least 1.");
                    }
                } else if (topSettingName.equals(SETTING_MAX_MAIN_TOF_DISPLAY_DEPTH)) {
                    maxMainTOFDisplayDepth = castSetting(settingName, settingValue, Integer.class);
                    if (maxTOFDisplayDepth < 1) {
                        throw newCfgFileException(settingName, "Value must be at least 1.");
                    }
                } else if (topSettingName.equals(SETTING_NUMBERED_SECTIONS)) {
                    numberedSections = castSetting(settingName, settingValue, Boolean.class);
                } else {
                    throw newCfgFileException(settingName, "Unknown setting name.");
                }
            } // for each cfg settings

            if (deployUrl == null) {
                throw new DocgenException(
                        "The \"" + SETTING_DEPLOY_URL + "\" setting wasn't specified");
            }
            if (offline == null) {
                throw new DocgenException(
                        "The \"" + SETTING_OFFLINE
                        + "\" setting wasn't specified; it must be set to true or false");
            }
            if (logo == null) {
                throw new DocgenException(
                        "The \"" + SETTING_LOGO
                        + "\" setting wasn't specified; it must be set currently, as the layout reserves space for it.");
            }
            if (copyrightHolder == null) {
                throw new DocgenException(
                        "The \"" + SETTING_COPYRIGHT_HOLDER + "\" setting wasn't specified.");
            }
            if (copyrightHolderSite == null) {
                throw new DocgenException(
                        "The \"" + SETTING_COPYRIGHT_HOLDER_SITE + "\" setting wasn't specified.");
            }
            if (copyrightStartYear == null) {
                throw new DocgenException(
                        "The \"" + SETTING_COPYRIGHT_START_YEAR + "\" setting wasn't specified.");
            }
        }

        // Ensure proper rank relations:
        if (lowestPageTOCElemenRank.compareTo(lowestFileElemenRank) > 0) {
            lowestPageTOCElemenRank = lowestFileElemenRank;
        }

        // Ensure {@link #maxMainTOFDisplayDepth} is set:
        if (maxMainTOFDisplayDepth == 0) {
            maxMainTOFDisplayDepth = maxTOFDisplayDepth;
        }

        templatesDir = new File(srcDir, DIR_TEMPLATES);
        if (!templatesDir.exists()) {
            templatesDir = null;
        }

        if (contentDir == null) {
            contentDir = srcDir;
        }

        // Initialize state fields

        primaryIndexTermLookup = new HashMap<>();
        secondaryIndexTermLookup = new HashMap<>();
        elementsById = new HashMap<>();
        tocNodes = new ArrayList<>();
        indexEntries = new ArrayList<>();

        // Setup FreeMarker:

        try {
            Logger.selectLoggerLibrary(Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {
            throw new BugException(e);
        }

        logger.info("Using FreeMarker " + Configuration.getVersion());
        fmConfig = new Configuration(Configuration.VERSION_2_3_25);

        TemplateLoader templateLoader = new ClassTemplateLoader(
                Transform.class, "templates");
        if (templatesDir != null) {
            templateLoader = new MultiTemplateLoader(
                    new TemplateLoader[] { new FileTemplateLoader(templatesDir), templateLoader });
        }
        fmConfig.setTemplateLoader(templateLoader);

        fmConfig.setLocale(locale);
        fmConfig.setTimeZone(timeZone);

        fmConfig.setDefaultEncoding(UTF_8.name());
        fmConfig.setOutputEncoding(UTF_8.name());

        // Do the actual job:

        // - Load and validate the book XML
        final File docFile;
        {
            final File docFile1 = new File(contentDir, FILE_BOOK);
            if (docFile1.isFile()) {
                docFile = docFile1;
            } else {
                final File docFile2 = new File(contentDir, FILE_ARTICLE);
                if (docFile2.isFile()) {
                    docFile = docFile2;
                } else {
                    throw new DocgenException("The book file is missing: "
                            + docFile1.getAbsolutePath() + " or " + docFile2.getAbsolutePath());
                }
            }
        }
        Document doc = XMLUtil.loadDocBook5XML(
                docFile, validate, validationOps, logger);
        ignoredFilePathPatterns.add(FileUtil.globToRegexp(docFile.getName()));

        // - Post-edit and examine the DOM:
        preprocessDOM(doc);

        // Resolve Docgen URL schemes in setting values:
        // Olinks must come first:
        if (olinks != null) {
            for (Entry<String, String> olinkEnt : olinks.entrySet()) {
                olinkEnt.setValue(resolveDocgenURL(SETTING_OLINKS, olinkEnt.getValue()));
            }
        }
        if (tabs != null) {
            for (Entry<String, String> tabEnt : tabs.entrySet()) {
                tabEnt.setValue(resolveDocgenURL(SETTING_TABS, tabEnt.getValue()));
            }
        }
        for (Map<String, String> secondaryTab : secondaryTabs.values()) {
            secondaryTab.put("href", resolveDocgenURL(SETTING_SECONDARY_TABS, secondaryTab.get("href")));
        }
        if (externalBookmarks != null) {
            for (Entry<String, String> bookmarkEnt : externalBookmarks.entrySet()) {
                bookmarkEnt.setValue(resolveDocgenURL(SETTING_EXTERNAL_BOOKMARKS, bookmarkEnt.getValue()));
            }
        }
        for (Map<String, String> tab : socialLinks.values()) {
            tab.put("href", resolveDocgenURL(SETTING_SOCIAL_LINKS, tab.get("href")));
        }
        for (Map<String, String> links : footerSiteMap.values()) {
            for (Map.Entry<String, String> link : links.entrySet()) {
                link.setValue(resolveDocgenURL(SETTING_FOOTER_SITEMAP, link.getValue()));
            }
        }
        if (logo != null) {
            resolveLogoHref(logo);
        }
        for (Logo logo : sideTOCLogos) {
            resolveLogoHref(logo);
        }

        // - Create destination directory:
        if (!destDir.isDirectory() && !destDir.mkdirs()) {
            throw new IOException("Failed to create destination directory: "
                    + destDir.getAbsolutePath());
        }
        logger.info("Output directory: " + destDir.getAbsolutePath());

        // - Check internal book-marks:
        for (Entry<String, String> ent : internalBookmarks.entrySet()) {
            String id = ent.getValue();
            if (!elementsById.containsKey(id)) {
                throw newCfgFileException(
                        SettingName.topLevel(cfgFile, SETTING_INTERNAL_BOOKMARKS),
                        "No element with id \"" + id + "\" exists in the book.");
            }
        }

        insertableFiles = computeInsertableFiles();

        // - Setup common data-model variables:
        try {
            // Settings:
            fmConfig.setSharedVariable(
                    VAR_OFFLINE, offline);
            fmConfig.setSharedVariable(
                    VAR_SIMPLE_NAVIGATION_MODE, simpleNavigationMode);
            fmConfig.setSharedVariable(
                    VAR_DEPLOY_URL, deployUrl);
            fmConfig.setSharedVariable(
                    VAR_ONLINE_TRACKER_HTML, onlineTrackerHTML);
            fmConfig.setSharedVariable(
                    VAR_COOKIE_CONSENT_SCRIPT_URL, cookieConstentScriptURL);
            fmConfig.setSharedVariable(
                    VAR_SHOW_EDITORAL_NOTES, showEditoralNotes);
            fmConfig.setSharedVariable(
                    VAR_SHOW_XXE_LOGO, showXXELogo);
            fmConfig.setSharedVariable(
                    VAR_SEARCH_KEY, searchKey);
            fmConfig.setSharedVariable(
                    VAR_DISABLE_JAVASCRIPT, disableJavaScript);
            fmConfig.setSharedVariable(
                    VAR_OLINKS, olinks);
            fmConfig.setSharedVariable(
                    VAR_NUMBERED_SECTIONS, numberedSections);
            fmConfig.setSharedVariable(
                    VAR_LOGO, logo);
            fmConfig.setSharedVariable(
                    VAR_SIDE_TOC_LOGOS, sideTOCLogos);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_HOLDER, copyrightHolder);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_HOLDER_SITE, copyrightHolderSite);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_SUFFIX, copyrightSuffix);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_START_YEAR, copyrightStartYear);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_COMMENT, copyrightComment);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_JAVA_COMMENT, copyrightJavaComment);
            fmConfig.setSharedVariable(
                    VAR_TABS, tabs);
            fmConfig.setSharedVariable(
                    VAR_SECONDARY_TABS, secondaryTabs);
            fmConfig.setSharedVariable(
                    VAR_SOCIAL_LINKS, socialLinks);
            fmConfig.setSharedVariable(
                    VAR_FOOTER_SITEMAP, footerSiteMap);
            fmConfig.setSharedVariable(
                    VAR_EXTERNAL_BOOKMARDS, externalBookmarks);
            fmConfig.setSharedVariable(
                    VAR_INTERNAL_BOOKMARDS, internalBookmarks);
            fmConfig.setSharedVariable(
                    VAR_ROOT_ELEMENT, doc.getDocumentElement());
            fmConfig.setSharedVariable(
                    VAR_CUSTOM_VARIABLES, computeCustomVariables());

            fmConfig.setSharedVariable(
                    "printTextWithDocgenSubstitutions",
                    new PrintTextWithDocgenSubstitutionsDirective(this));
            fmConfig.setSharedVariable(
                    "chopLinebreak",
                    ChopLinebreakDirective.INSTANCE);

            // Calculated data:
            {
                Date generationTime;
                String generationTimeStr = System.getProperty(SYSPROP_GENERATION_TIME);
                if (generationTimeStr == null) {
                    generationTime = new Date();
                } else {
                    try {
                        generationTime = DateUtil.parseISO8601DateTime(generationTimeStr, DateUtil.UTC,
                                new DateUtil.TrivialCalendarFieldsToDateConverter());
                    } catch (DateParseException e) {
                        throw new DocgenException(
                                "Malformed \"" + SYSPROP_GENERATION_TIME
                                + "\" system property value: " + generationTimeStr, e);
                    }
                }
                fmConfig.setSharedVariable(VAR_TRANSFORM_START_TIME, generationTime);
            }
            fmConfig.setSharedVariable(
                    VAR_INDEX_ENTRIES, indexEntries);
            int tofCntLv1 = countTOFEntries(tocNodes.get(0), 1);
            int tofCntLv2 = countTOFEntries(tocNodes.get(0), 2);
            fmConfig.setSharedVariable(
                    VAR_SHOW_NAVIGATION_BAR,
                    tofCntLv1 != 0
                            || internalBookmarks.size() != 0
                            || externalBookmarks.size() != 0);
            fmConfig.setSharedVariable(
                    VAR_SHOW_BREADCRUMB, tofCntLv1 != tofCntLv2);

            // Helper methods and directives:
            fmConfig.setSharedVariable(
                    "NodeFromID", nodeFromID);
            fmConfig.setSharedVariable(
                    "CreateLinkFromID", createLinkFromID);
            fmConfig.setSharedVariable(
                    "primaryIndexTermLookup", primaryIndexTermLookup);
            fmConfig.setSharedVariable(
                    "secondaryIndexTermLookup", secondaryIndexTermLookup);
            fmConfig.setSharedVariable(
                    "CreateLinkFromNode", createLinkFromNode);
        } catch (TemplateModelException e) {
            throw new BugException(e);
        }

        // - Generate ToC JSON-s:
        {
            logger.info("Generating ToC JSON...");
            Template template = fmConfig.getTemplate(FILE_TOC_JSON_TEMPLATE);
            try (Writer wr = FileUtil.newFileWriter(new File(destDir, FILE_TOC_JSON_OUTPUT))) {
                try {
                    SimpleHash dataModel = new SimpleHash(fmConfig.getObjectWrapper());
                    dataModel.put(VAR_JSON_TOC_ROOT, tocNodes.get(0));
                    template.process(dataModel, wr, null, NodeModel.wrap(doc));
                } catch (TemplateException e) {
                    throw new BugException("Failed to generate ToC JSON "
                            + "(see cause exception).", e);
                }
            }
        }

        // - Generate Sitemap XML:
        {
            logger.info("Generating Sitemap XML...");
            Template template = fmConfig.getTemplate(FILE_SITEMAP_XML_TEMPLATE);
            try (Writer wr = FileUtil.newFileWriter(new File(destDir, FILE_SITEMAP_XML_OUTPUT))) {
                try {
                    SimpleHash dataModel = new SimpleHash(fmConfig.getObjectWrapper());
                    dataModel.put(VAR_JSON_TOC_ROOT, tocNodes.get(0));
                    template.process(dataModel, wr, null, NodeModel.wrap(doc));
                } catch (TemplateException e) {
                    throw new BugException("Failed to generate Sitemap XML"
                            + "(see cause exception).", e);
                }
            }
        }


        // - Generate the HTML-s:
        logger.info("Generating HTML files...");
        int htmlFileCounter = 0;
        for (TOCNode tocNode : tocNodes) {
            if (tocNode.getOutputFileName() != null) {
                try {
                    currentFileTOCNode = tocNode;
                    try {
                        // All output-file-specific processing comes here.
                        htmlFileCounter += generateHTMLFile();
                    } finally {
                        currentFileTOCNode = null;
                    }
                } catch (freemarker.core.StopException e) {
                    throw new DocgenException(e.getMessage());
                } catch (DocgenTagException e) {
                    throw new DocgenException("Docgen tag evaluation in document text failed; see cause exception", e);
                } catch (TemplateException e) {
                    throw new BugException(e);
                }
            }
        }

        if (!offline && searchKey != null) {
            try {
                generateSearchResultsHTMLFile(doc);
                htmlFileCounter++;
            } catch (freemarker.core.StopException e) {
                throw new DocgenException(e.getMessage());
            } catch (TemplateException e) {
                throw new BugException(e);
            }
        }

        // - Copy the standard statics:
        logger.info("Copying common static files...");
        copyCommonStatic("docgen.min.css");
        copyCommonStatic("img/patterned-bg.png");

        copyCommonStatic("fonts/icomoon.eot");
        copyCommonStatic("fonts/icomoon.svg");
        copyCommonStatic("fonts/icomoon.ttf");
        copyCommonStatic("fonts/icomoon.woff");
        copyCommonStatic("fonts/NOTICE");

        if (showXXELogo) {
            copyCommonStatic("img/xxe.png");
        }
        if (!disableJavaScript) {
          copyCommonStatic("main.min.js");
        }

        // - Copy the custom statics:
        logger.info("Copying custom static files...");
        int bookSpecStaticFileCounter = FileUtil.copyDir(contentDir, destDir, ignoredFilePathPatterns);

        // - Eclipse ToC:
        if (generateEclipseTOC) {
            if (simpleNavigationMode) {
                throw new DocgenException("Eclipse ToC generation is untested/unsupported with simpleNavigationMode=true.");
            }

            logger.info("Generating Eclipse ToC...");
            Template template = fmConfig.getTemplate(FILE_ECLIPSE_TOC_TEMPLATE);
            try (Writer wr = FileUtil.newFileWriter(new File(destDir, FILE_ECLIPSE_TOC_OUTPUT))) {
                try {
                    SimpleHash dataModel = new SimpleHash(fmConfig.getObjectWrapper());
                    if (eclipseLinkTo != null) {
                        dataModel.put(VAR_ECLIPSE_LINK_TO, eclipseLinkTo);
                    }
                    template.process(dataModel, wr, null, NodeModel.wrap(doc));
                } catch (TemplateException e) {
                    throw new BugException("Failed to generate Eclipse ToC "
                            + "(see cause exception).", e);
                }
            }
        }

        // - Report summary:
        logger.info(
                "Done: "
                + htmlFileCounter + " HTML-s + "
                + bookSpecStaticFileCounter + " custom statics + commons"
                + (generateEclipseTOC ? " + Eclipse ToC" : ""));
    }

    private Map<String, Object> computeCustomVariables() throws DocgenException {
        for (String varName : customVariableOverrides.keySet()) {
            if (!customVariablesFromSettingsFile.containsKey(varName)) {
                throw new DocgenException("Attempt to override custom variable " + StringUtil.jQuote(varName)
                        + ", when it was not set in the settings file (" + cfgFile + ").");
            }
        }

        Map<String, Object> customVariables = new HashMap<>();
        customVariables.putAll(customVariablesFromSettingsFile);
        customVariables.putAll(customVariableOverrides);

        for (Entry<String, Object> entry : customVariables.entrySet()) {
            if (entry.getValue() == null) {
                throw new DocgenException("The custom variable " + StringUtil.jQuote(entry.getKey())
                        + " was set to null, which is not allowed. Probably you are supposed to override its value.");
            }
        }

        return customVariables;
    }

    private Map<String, Path> computeInsertableFiles() throws DocgenException {
        for (String varName : insertableFilesOverrides.keySet()) {
            if (!insertableFilesFromSettingsFile.containsKey(varName)) {
                throw new DocgenException("Attempt to set insertable path with symbolic name "
                        + StringUtil.jQuote(varName)
                        + ", when same was not set in the settings file (" + FILE_SETTINGS + ").");
            }
        }

        Map<String, String> unresolvedInsertableFiles = new HashMap<>();
        unresolvedInsertableFiles.putAll(insertableFilesFromSettingsFile);
        unresolvedInsertableFiles.putAll(insertableFilesOverrides);

        for (Entry<String, String> entry : unresolvedInsertableFiles.entrySet()) {
            if (entry.getValue() == null) {
                throw new DocgenException("The insertable path with symbolic name "
                        + StringUtil.jQuote(entry.getKey()) + " was set to path null, which is not allowed. "
                        + "Probably you are supposed to override its path.");
            }
        }

        Map<String, Path> insertableFiles = new HashMap<>();
        for (Entry<String, String> entry : unresolvedInsertableFiles.entrySet()) {
            String symbolicName = entry.getKey();
            String unresolvedPath = entry.getValue();

            Path path;
            if (unresolvedPath.endsWith("/**") || unresolvedPath.endsWith("\\**")) {
                path = srcDir.toPath().resolve(unresolvedPath.substring(0, unresolvedPath.length() - 3));
                if (!Files.isDirectory(path)) {
                    throw new DocgenException(
                            "Insertable file with symbolic name " + StringUtil.jQuote(symbolicName)
                            + " points to a directory that doesn't exist: " + StringUtil.jQuote(path));
                }
            } else {
                path = srcDir.toPath().resolve(unresolvedPath);
                if (!Files.isRegularFile(path)) {
                    if (Files.isDirectory(path)) {
                        throw new DocgenException(
                                "Insertable file with symbolic name " + StringUtil.jQuote(symbolicName)
                                + " points to a directory, not a file: " + StringUtil.jQuote(path) + "."
                                + " If you want to point to a directory, end the path with \"/**\".");
                    } else {
                        throw new DocgenException(
                                "Insertable file with symbolic name " + StringUtil.jQuote(symbolicName)
                                        + " points to a file that doesn't exist: " + StringUtil.jQuote(path));
                    }
                }
            }

            insertableFiles.put(symbolicName, path);
        }

        return insertableFiles;
    }

    private void resolveLogoHref(Logo logo) throws DocgenException {
        String logoHref = logo.getHref();
        if (logoHref != null) {
            logo.setHref(resolveDocgenURL(SETTING_LOGO, logoHref));
        }
    }

    /**
     * Resolves the URL if it uses the {@code "olink:"} or {@code "id:"} schema, returns it as if otherwise.
     */
    private String resolveDocgenURL(String settingName, String url) throws DocgenException {
        if (url.startsWith(OLINK_SCHEMA_START)) {
            String oLinkName = url.substring(OLINK_SCHEMA_START.length());
            String resolvedOLink = olinks.get(oLinkName);
            if (resolvedOLink == null) {
                throw new DocgenException("Undefined olink used inside configuration setting "
                        + StringUtil.jQuote(settingName)
                        + ": " + StringUtil.jQuote(oLinkName));
            }
            return resolveDocgenURL(settingName, resolvedOLink);
        } else if (url.startsWith(ID_SCHEMA_START)) {
            String id = url.substring(ID_SCHEMA_START.length());
            try {
                return createLinkFromId(id);
            } catch (DocgenException e) {
                throw new DocgenException("Can't resolve id inside configuration setting "
                        + StringUtil.jQuote(settingName)
                        + ": " + StringUtil.jQuote(id),
                        e);
            }
        } else {
            return url;
        }
    }

    private static Logo castMapToLogo(SettingName settingName, Object settingValue) {
        Map<String, String> logoMap = castSetting(
                settingName,
                settingValue, null,
                Map.class,
                new MapEntryType(String.class, SETTING_LOGO_MAP_KEYS, String.class));
        return new Logo(
                logoMap.get(SETTING_LOGO_KEY_SRC),
                logoMap.get(SETTING_LOGO_KEY_HREF),
                logoMap.get(SETTING_LOGO_KEY_ALT));
    }

    private String getFileContentForSetting(SettingName settingName, Object settingValue) {
        String settingValueStr = castSetting(settingName, settingValue, String.class);
        File f = new File(getSourceDirectory(), settingValueStr);
        if (!f.exists()) {
            throw newCfgFileException(
                    settingName,
                    "File not found: " + f.toPath());
        }
        try {
            return FileUtil.loadString(f, UTF_8);
        } catch (IOException e) {
            throw newCfgFileException(
                    settingName,
                    "Error while reading file: " + f.toPath(),
                    e);
        }
    }

    private void copyCommonStatic(String staticFileName) throws IOException {
        String resourcePath = "statics/" + staticFileName;
        try (InputStream in = Transform.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Failed to open class-loader resource: " + resourcePath + " relatively to "
                        + Transform.class.getPackage().getName());
            }

            if (staticFileName.endsWith(".css") || staticFileName.endsWith(".js")) {
                // ISO-8859-1 will be good enough as far as the resource isn't UTF-16 or EBCDIC:
                final Charset fileCharset = StandardCharsets.ISO_8859_1;
                String content = FileUtil.loadString(in, fileCharset);
                final String eol = TextUtil.detectEOL(content, "\n");

                // If we have an initial comment, then that must be a copyright header, which we will remove.
                if (content.startsWith("/*")) {
                    int commentEnd = content.indexOf("*/");
                    if (commentEnd == -1) {
                        throw new BugException("Unclosed initial \"/*\" in resource " + resourcePath);
                    }
                    commentEnd += 2;
                    String comment = content.substring(0, commentEnd);
                    if (!comment.contains("Copyright") && !comment.contains("copyright")
                            && !comment.contains("License") && !comment.contains("license")) {
                        throw new BugException("The initial /*...*/ comments doesn't look like a copyright header "
                                + "in resource " + resourcePath);
                    }

                    // Include an EOL after the comment, if there's any.
                    for (int i = 0; i < 2; i++) {
                        if (commentEnd < content.length()) {
                            char c = content.charAt(commentEnd);
                            if (c == '\n') {
                                commentEnd++;
                            } else if (c == '\r') {
                                commentEnd++;
                                if (commentEnd < content.length() && content.charAt(commentEnd) == '\n') {
                                    commentEnd++;
                                }
                            }
                        }
                    }

                    // Remove existing copyright header:
                    content = content.substring(commentEnd);
                }

                if (offline && copyrightComment != null) {
                    // Add copyright comment:
                    StringBuilder sb = new StringBuilder(TextUtil.normalizeEOL(copyrightJavaComment, eol));
                    sb.append(eol);
                    if (content.length() > 0 && content.charAt(0) != '\n' && content.charAt(0) != '\r') {
                        sb.append(eol);
                    }
                    sb.append(content);
                    content = sb.toString();
                }

                Path destSubdir = destDir.toPath().resolve("docgen-resources");
                Files.createDirectories(destSubdir);
                Files.write(destSubdir.resolve(staticFileName), content.getBytes(fileCharset));
            } else {
                FileUtil.copyResourceIntoFile(
                        Transform.class, "statics", staticFileName,
                        new File(destDir, "docgen-resources"));
            }
        }
    }

    /**
     * Adds attribute <tt>id</tt> to elements that are in
     * <code>idAttrElements</code>, but has no id attribute yet.
     * Adding id-s is useful to create more precise HTML cross-links later.
     */
    private void preprocessDOM(Document doc)
            throws SAXException, DocgenException {
        NodeModel.simplify(doc);
        preprocessDOM_applyRemoveNodesWhenOnlineSetting(doc);
        preprocessDOM_addRanks(doc);
        preprocessDOM_misc(doc);
        preprocessDOM_buildTOC(doc);
    }

    private static final class PreprocessDOMMisc_GlobalState {
        private int lastId;

        /** Style silencer:  notAUtiltiyClass() never used */
        private PreprocessDOMMisc_GlobalState() {
            notAUtiltiyClass();
        }

        /** CheckStyle silencer */
        void notAUtiltiyClass() {
            // Nop
        }
    }

    private static final class PreprocessDOMMisc_ParentSectState {
        private int upperRomanNumber = 1;
        private int lowerRomanNumber = 1;
        private int arabicNumber = 1;
        private int upperLatinNumber = 1;
        private int unitedNumber = 1;

        /** Style silencer:  notAUtiltiyClass() never used */
        private PreprocessDOMMisc_ParentSectState() {
            notAUtiltiyClass();
        }

        /** CheckStyle silencer */
        void notAUtiltiyClass() {
            // Nop
        }
    }

    private void preprocessDOM_misc(Document doc)
            throws SAXException, DocgenException {
        preprocessDOM_misc_inner(doc,
                new PreprocessDOMMisc_GlobalState(),
                new PreprocessDOMMisc_ParentSectState());
        indexEntries = new ArrayList<>(primaryIndexTermLookup.keySet());
        Collections.sort(indexEntries, Collator.getInstance(locale));
    }

    private void preprocessDOM_misc_inner(
            Node node,
            PreprocessDOMMisc_GlobalState globalState,
            PreprocessDOMMisc_ParentSectState parentSectState)
            throws SAXException, DocgenException {
        if (node instanceof Element) {
            Element elem = (Element) node;

            // xml:id -> id:
            String id = XMLUtil.getAttribute(elem, "xml:id");
            if (id != null) {
                if (id.startsWith(AUTO_ID_PREFIX)) {
                    throw new DocgenException(
                            XMLUtil.theSomethingElement(elem, true)
                            + " uses a reserved xml:id, "
                            + TextUtil.jQuote(id) + ". All ID-s starting with "
                            + "\"" + AUTO_ID_PREFIX + "\" are reserved for "
                            + "Docgen.");
                }
                if (id.startsWith(DOCGEN_ID_PREFIX)) {
                    throw new DocgenException(
                            XMLUtil.theSomethingElement(elem, true)
                            + " uses a reserved xml:id, "
                            + TextUtil.jQuote(id) + ". All ID-s starting with "
                            + "\"" + DOCGEN_ID_PREFIX + "\" are reserved for "
                            + "Docgen.");
                }
                elem.setAttribute("id", id);
            }

            final String elemName = node.getNodeName();

            // Add auto id-s:
            if (id == null && GUARANTEED_ID_ELEMENTS.contains(elemName)) {
                globalState.lastId++;
                id = AUTO_ID_PREFIX + globalState.lastId;
                elem.setAttribute("id", id);
            }
            if (id != null) {
                elementsById.put(id, elem);
            }

            // Add default titles:
            if (elemName.equals(E_PREFACE)
                    || elemName.equals(E_GLOSSARY)
                    || elemName.equals(E_INDEX)) {
                ensureTitleExists(
                        elem,
                        Character.toUpperCase(elemName.charAt(0))
                        + elemName.substring(1));

            // Simplify tables:
            } else if (
                    (elemName.equals(E_INFORMALTABLE)
                            || elemName.equals(E_TABLE))
                    && elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                TableSimplifier.simplify(elem);
            // Collect index terms:
            } else if (elemName.equals(E_INDEXTERM)) {
                addIndexTerm(node);
            } else if (elemName.equals(E_IMAGEDATA)) {
                String ref = XMLUtil.getAttribute(elem, A_FILEREF);
                String loRef = ref.toLowerCase();
                if (!loRef.startsWith("http://")
                        && !loRef.startsWith("https://")
                        && !ref.startsWith("/")) {
                    if (!new File(contentDir, ref.replace('/', File.separatorChar)).isFile()) {
                        throw new DocgenException(
                                XMLUtil.theSomethingElement(elem) + " refers "
                                + "to a missing file: \""
                                + ref.replace("\"", "&quot;") + "\"");
                    }
                }
                if (loRef.endsWith(".svg")) {
                    String pngRef = ref.substring(0, ref.length() - 4) + ".png";
                    if (!new File(contentDir, pngRef.replace('/', File.separatorChar)).isFile()) {
                        throw new DocgenException(
                                XMLUtil.theSomethingElement(elem)
                                + " refers to an SVG file for which the fallback PNG file is missing: \""
                                + pngRef.replace("\"", "&quot;") + "\"");
                    }
                }
            }

            // Adding title prefixes to document structure elements:
            if (DOCUMENT_STRUCTURE_ELEMENTS.contains(elemName)) {
                final String prefix;
                if (elem.getParentNode() instanceof Document) {
                    // The document element is never prefixed
                    prefix = null;
                } else if (hasPrefaceLikeParent(elem)) {
                    prefix = null;
                } else if (numberedSections
                        && elemName.equals(E_SECTION)) {
                    prefix = String.valueOf(
                            parentSectState.arabicNumber++);
                } else if (elemName.equals(E_CHAPTER)) {
                    prefix = String.valueOf(
                            parentSectState.arabicNumber++);
                } else if (elemName.equals(E_PART)) {
                    prefix = TextUtil.toUpperRomanNumber(
                            parentSectState.upperRomanNumber++);
                } else if (elemName.equals(E_APPENDIX)) {
                    prefix = TextUtil.toUpperLatinNumber(
                            parentSectState.upperLatinNumber++);
                } else if (elemName.equals(E_ARTICLE)) {
                    prefix = TextUtil.toLowerRomanNumber(
                            parentSectState.lowerRomanNumber++);
                } else {
                    prefix = null;
                }

                if (prefix != null) {
                    final String fullPrefix;
                    final Node parent = elem.getParentNode();
                    if (parent instanceof Element
                            // Don't inherit prefix from "part" rank:
                            && !parent.getLocalName().equals(E_PART)
                            // Don't inherit prefix from "article":
                            && !parent.getLocalName().equals(E_ARTICLE)) {
                        String inhPrefix = XMLUtil.getAttribute(
                                (Element) parent, A_DOCGEN_TITLE_PREFIX);
                        if (inhPrefix != null) {
                            if (inhPrefix.endsWith(".")) {
                                fullPrefix = inhPrefix + prefix;
                            } else {
                                fullPrefix = inhPrefix + "." + prefix;
                            }
                        } else {
                            fullPrefix = prefix;
                        }
                    } else {
                        fullPrefix = prefix;
                    }

                    elem.setAttribute(A_DOCGEN_TITLE_PREFIX, fullPrefix);
                } // if prefix != null

                elem.setAttribute(
                        A_DOCGEN_UNITED_NUMBERING,
                        String.valueOf(parentSectState.unitedNumber++));

                // We will be the parent document structure element of the soon
                // processed children:
                parentSectState = new PreprocessDOMMisc_ParentSectState();
            } // if document structure element
        } // if element

        NodeList children = node.getChildNodes();
        int ln = children.getLength();
        for (int i = 0; i < ln; i++) {
            preprocessDOM_misc_inner(
                    children.item(i),
                    globalState, parentSectState);
        }
    }

    private void preprocessDOM_applyRemoveNodesWhenOnlineSetting(Document doc) {
        if (offline || removeNodesWhenOnline == null || removeNodesWhenOnline.isEmpty()) return;

        HashSet<String> idsToRemoveLeft = new HashSet<>(removeNodesWhenOnline);
        preprocessDOM_applyRemoveNodesWhenOnlineSetting_inner(
                doc.getDocumentElement(), idsToRemoveLeft);
        if (!idsToRemoveLeft.isEmpty()) {
            throw new DocgenException(
                    "These xml:id-s, specified in the \"" + SETTING_REMOVE_NODES_WHEN_ONLINE
                    + "\" configuration setting, wasn't found in the document: " + idsToRemoveLeft);
        }
    }

    private void preprocessDOM_applyRemoveNodesWhenOnlineSetting_inner(Element elem, Set<String> idsToRemoveLeft) {
        Node child = elem.getFirstChild();
        while (child != null && !idsToRemoveLeft.isEmpty()) {
            Element childElemToBeRemoved = null;
            if (child instanceof Element) {
                Element childElem = (Element) child;
                String id = XMLUtil.getAttribute(childElem, "xml:id");
                if (id != null && idsToRemoveLeft.remove(id)) {
                    childElemToBeRemoved = childElem;
                }
                if (!idsToRemoveLeft.isEmpty()) {
                    preprocessDOM_applyRemoveNodesWhenOnlineSetting_inner(childElem, idsToRemoveLeft);
                }
            }
            child = child.getNextSibling();
            if (childElemToBeRemoved != null) {
                elem.removeChild(childElemToBeRemoved);
            }
        }
    }

    /**
     * Annotates the document structure nodes with so called ranks.
     * About ranks see: {@link #setting_lowestFileElementRank}.
     */
    private void preprocessDOM_addRanks(Document doc) {
        Element root = doc.getDocumentElement();
        String rootName = root.getLocalName();
        if (rootName.equals(E_BOOK)) {
            root.setAttribute(
                    A_DOCGEN_RANK, DocumentStructureRank.BOOK.toString());
            preprocessDOM_addRanks_underBookRank(root);
        } else if (rootName.equals(E_ARTICLE)) {
            root.setAttribute(
                    A_DOCGEN_RANK, DocumentStructureRank.CHAPTER.toString());
            preprocessDOM_addRanks_underChapterRankOrDeeper(root, 0);
        } else {
            throw new DocgenException("The \"" + rootName + "\" element is "
                    + "unsupported as root element.");
        }
    }

    private void preprocessDOM_addRanks_underBookRank(Element root) {
        // Find the common rank:
        DocumentStructureRank commonRank = null;
        for (Element child : XMLUtil.childrenElementsOf(root)) {
            String name = child.getLocalName();
            if (name.equals(E_PART)) {
                if (commonRank != null
                        && !commonRank.equals(DocumentStructureRank.PART)) {
                    throw new DocgenException("Bad document structure: "
                            + XMLUtil.theSomethingElement(child) + " is on the "
                            + "same ToC level with a \"" + E_CHAPTER
                            + "\" element.");
                }
                commonRank = DocumentStructureRank.PART;
            } else if (name.equals(E_CHAPTER)) {
                if (commonRank != null
                        && !commonRank.equals(DocumentStructureRank.CHAPTER)) {
                    throw new DocgenException("Bad document structure: "
                            + XMLUtil.theSomethingElement(child) + " is on the "
                            + "same ToC level with a \"" + E_PART
                            + "\" element.");
                }
                commonRank = DocumentStructureRank.CHAPTER;
            }
        }
        if (commonRank == null) {
            commonRank = DocumentStructureRank.CHAPTER;
        }

        // Apply the common rank plus go deeper:
        for (Element child : XMLUtil.childrenElementsOf(root)) {
            if (DOCUMENT_STRUCTURE_ELEMENTS.contains(child.getLocalName())) {
                child.setAttribute(
                        A_DOCGEN_RANK, commonRank.toString());
                // Even if this node received part rank, its children will not
                // "feel like" being the children of a true part, unless its
                // indeed a part:
                if (child.getLocalName().equals(E_PART)) {
                    preprocessDOM_addRanks_underTruePart(child);
                } else {
                    preprocessDOM_addRanks_underChapterRankOrDeeper(
                            child, 0);
                }
            }
        }
    }

    private void preprocessDOM_addRanks_underTruePart(Node parent) {
        for (Element child : XMLUtil.childrenElementsOf(parent)) {
            if (DOCUMENT_STRUCTURE_ELEMENTS.contains(child.getLocalName())) {
                child.setAttribute(
                        A_DOCGEN_RANK,
                        DocumentStructureRank.CHAPTER.toString());
                preprocessDOM_addRanks_underChapterRankOrDeeper(child, 0);
            }
        }
    }

    private void preprocessDOM_addRanks_underChapterRankOrDeeper(
            Element parent, int underSectionRank) {
        for (Element child : XMLUtil.childrenElementsOf(parent)) {
            if (DOCUMENT_STRUCTURE_ELEMENTS.contains(child.getLocalName())) {
                if (child.getLocalName().equals(E_SIMPLESECT)) {
                    child.setAttribute(
                            A_DOCGEN_RANK,
                            DocumentStructureRank.SIMPLESECT.toString());
                    // Note: simplesection-s are leafs in the ToC hierarchy.
                } else {
                    if (underSectionRank + 1 > DocgenRestrictionsValidator
                            .MAX_SECTION_NESTING_LEVEL) {
                        throw new DocgenException("Too deep ToC nesting for "
                                + XMLUtil.theSomethingElement(child)
                                + ": rank bellow "
                                + DocumentStructureRank.sectionToString(
                                        DocgenRestrictionsValidator
                                                .MAX_SECTION_NESTING_LEVEL));
                    }

                    child.setAttribute(
                            A_DOCGEN_RANK,
                            DocumentStructureRank.sectionToString(
                                    underSectionRank + 1));

                    preprocessDOM_addRanks_underChapterRankOrDeeper(
                            child, underSectionRank + 1);
                }
            }
        }
    }

    private void preprocessDOM_buildTOC(Document doc) {
        preprocessDOM_buildTOC_inner(doc, 0, null);
        if (tocNodes.size() > 0) {
            preprocessDOM_buildTOC_checkEnsureHasIndexHhml(tocNodes);

            preprocessDOM_buildTOC_checkTOCTopology(tocNodes.get(0));

            if (!tocNodes.get(0).isFileElement()) {
                throw new BugException(
                        "The root ToC node must be a file-element.");
            }
            preprocessDOM_buildTOC_checkFileTopology(tocNodes.get(0));

            if (simpleNavigationMode) {
                // Must do it at the end: We need the docgen_... XML attributes here, and we must be past the
                // TOC topology checks.
                for (TOCNode tocNode : tocNodes) {
                    // Don't generate a file for pages that would only contain a table of contents
                    if (tocNode.isFileElement()
                            && (tocNode.getParent() == null || !hasContentInTheSameFile(tocNode))) {
                        tocNode.setOutputFileName(null);
                        tocNode.getElement().setAttribute(A_DOCGEN_NOT_ADDRESSABLE, "true");
                    }
                }
            }

            if (!validationOps.getOutputFilesCanUseAutoID()) {
                for (TOCNode tocNode : tocNodes) {
                    String outputFileName = tocNode.getOutputFileName();
                    if (outputFileName != null && outputFileName.startsWith(AUTO_ID_PREFIX)) {
                        throw new DocgenException(XMLUtil.theSomethingElement(tocNode.getElement(), true)
                                + " has automatically generated ID that is not allowed as the ID "
                                + "is used for generating a file name. (Related setting: \"" + SETTING_VALIDATION
                                + "\" per \"" + SETTING_VALIDATION_OUTPUT_FILES_CAN_USE_AUTOID + "\")");
                    }
                }
            }
        }
    }

    private static final String COMMON_TOC_TOPOLOGY_ERROR_HINT
            = " (Hint: Review the \"" + SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK
              + "\" setting. Maybe it's incompatible with the structure of "
              + "this document.)";

    private void preprocessDOM_buildTOC_checkTOCTopology(TOCNode tocNode) {
        // Check parent-child relation:
        TOCNode parent = tocNode.getParent();
        if (parent != null && !parent.getElement().isSameNode(
                tocNode.getElement().getParentNode())) {
            throw new DocgenException(
                    "Bad ToC-element topology: In the ToC "
                    + parent.theSomethingElement()
                    + " is the parent of "
                    + tocNode.theSomethingElement()
                    + ", yet they are not in parent-child relation in the XML "
                    + "document (but maybe in grandparent-nephew relation or "
                    + "like)."
                    + COMMON_TOC_TOPOLOGY_ERROR_HINT);
        }

        // Check following-sibling relation:
        TOCNode next = tocNode.getNext();
        Element relevantSibling = preprocessDOM_buildTOC_getSectionLikeSibling(
                tocNode.getElement(), true);
        if (next != null) {
            if (relevantSibling == null) {
                throw new DocgenException(
                        "Bad ToC-element topology: In the ToC "
                        + next.theSomethingElement()
                        + " is the following sibling of "
                        + tocNode.theSomethingElement()
                        + ", yet they are not siblings in the XML document."
                        + COMMON_TOC_TOPOLOGY_ERROR_HINT);
            }
            if (!relevantSibling.isSameNode(next.getElement())) {
                throw new DocgenException(
                        "Bad ToC-element topology: In the ToC "
                        + next.theSomethingElement()
                        + " is the immediate following sibling of "
                        + tocNode.theSomethingElement()
                        + ", but in the XML document there is a \""
                        + relevantSibling.getLocalName()
                        + "\" element between them, or they aren't siblings "
                        + "at all."
                        + COMMON_TOC_TOPOLOGY_ERROR_HINT);
            }
        } else {
            // next == null
            if (relevantSibling != null) {
                throw new DocgenException(
                        "Bad ToC-element topology: In the ToC hierarchy "
                        + tocNode.theSomethingElement()
                        + "\" is a last-child, but in the XML document it has "
                        + "a \"" + relevantSibling.getLocalName() + "\" "
                        + "element as its following sibling."
                        + COMMON_TOC_TOPOLOGY_ERROR_HINT);
            }
        }

        // Check preceding-sibling relation:
        TOCNode prev = tocNode.getPrevious();
        relevantSibling = preprocessDOM_buildTOC_getSectionLikeSibling(
                tocNode.getElement(), false);
        if (prev == null && relevantSibling != null) {
            throw new DocgenException(
                    "Bad ToC-element topology: In the ToC hierarchy "
                    + tocNode.theSomethingElement() + " is a first-child, "
                    + "but in the XML document it has a "
                    + "\"" + relevantSibling.getLocalName() + "\" "
                    + "element as its preceding sibling."
                    + COMMON_TOC_TOPOLOGY_ERROR_HINT);
        }

        TOCNode child = tocNode.getFirstChild();
        while (child != null) {
            preprocessDOM_buildTOC_checkTOCTopology(child);
            child = child.getNext();
        }
    }

    private Element preprocessDOM_buildTOC_getSectionLikeSibling(
            Element elem, boolean next) {
        Node relevantSibling = elem;
        do {
            if (next) {
                relevantSibling = relevantSibling.getNextSibling();
            } else {
                relevantSibling = relevantSibling.getPreviousSibling();
            }
        } while (relevantSibling != null
                && !(relevantSibling instanceof Element
                        && DOCUMENT_STRUCTURE_ELEMENTS.contains(
                                relevantSibling.getLocalName())));
        return (Element) relevantSibling;
    }

    private static final String COMMON_FILE_TOPOLOGY_ERROR_HINT
            = " (Hint: Review the \"" + SETTING_LOWEST_FILE_ELEMENT_RANK
              + "\" setting. Maybe it's incompatible with the structure of "
              + "this document.)";

    private void preprocessDOM_buildTOC_checkFileTopology(TOCNode tocNode) {
        TOCNode firstChild  = tocNode.getFirstChild();
        if (firstChild != null) {
            boolean firstIsFileElement = firstChild.isFileElement();

            TOCNode child = firstChild;
            do {
                if (child.isFileElement() != firstIsFileElement) {
                    throw new DocgenException("Bad file-element topology: "
                            + "The first child element of "
                            + tocNode.theSomethingElement()
                            + ", " + firstChild.theSomethingElement()
                            + ", is " + (firstIsFileElement ? "a" : "not a")
                            + " file-element, while another child, "
                            + child.theSomethingElement()
                            + (firstIsFileElement ? " isn't" : " is")
                            + ". Either all relevant children elements must be "
                            + "file-elements or neither can be."
                            + COMMON_FILE_TOPOLOGY_ERROR_HINT);
                }

                preprocessDOM_buildTOC_checkFileTopology(child);

                child = child.getNext();
            } while (child != null);

            if (firstIsFileElement && !tocNode.isFileElement()) {
                throw new DocgenException("Bad file-element topology: "
                        + tocNode.theSomethingElement() + " is not a "
                        + "file-element, yet it has file-element children, "
                        + firstChild.theSomethingElement() + ". Only "
                        + "file-elements can have children that are "
                        + "file-elements.");
            }
        }
    }

    private TOCNode preprocessDOM_buildTOC_inner(Node node,
            final int sectionLevel, TOCNode parentTOCNode) {
        TOCNode curTOCNode = null;
        int newSectionLevel = sectionLevel;

        if (node instanceof Element) {
            final Element elem = (Element) node;
            final String nodeName = node.getNodeName();

            if (DOCUMENT_STRUCTURE_ELEMENTS.contains(nodeName)) {
                DocumentStructureRank rank = DocumentStructureRank.valueOf(
                        XMLUtil.getAttribute(elem, A_DOCGEN_RANK)
                                .toUpperCase());
                final boolean isTheDocumentElement
                        = elem.getParentNode() instanceof Document;
                if (isTheDocumentElement
                        || rank.compareTo(lowestPageTOCElemenRank) >= 0) {
                    curTOCNode = new TOCNode(elem, tocNodes.size());
                    tocNodes.add(curTOCNode);

                    if ((isTheDocumentElement
                            || rank.compareTo(lowestFileElemenRank) >= 0)
                            && !hasPrefaceLikeParent(elem)) {
                        elem.setAttribute(A_DOCGEN_FILE_ELEMENT, "true");
                        curTOCNode.setFileElement(true);

                        if (isTheDocumentElement) {
                            curTOCNode.setOutputFileName(FILE_TOC_HTML);
                            elem.setAttribute(A_DOCGEN_ROOT_ELEMENT, "true");
                        } else if (getExternalLinkTOCNodeURLOrNull(elem) != null) {
                            curTOCNode.setOutputFileName(null);
                        } else if (AV_INDEX_ROLE.equals(elem.getAttribute(DocBook5Constants.A_ROLE))) {
                            curTOCNode.setOutputFileName(FILE_INDEX_HTML);
                        } else {
                            String id = XMLUtil.getAttribute(elem, "id");
                            if (id == null) {
                                throw new BugException("Missing id attribute");
                            }
                            String fileName = id + ".html";
                            if (fileName.equals(FILE_TOC_HTML) || fileName.equals(FILE_DETAILED_TOC_HTML)
                                    || fileName.equals(FILE_INDEX_HTML) || fileName.equals(FILE_SEARCH_RESULTS_HTML)) {
                                throw new DocgenException(
                                        XMLUtil.theSomethingElement(elem, true)
                                        + " has an xml:id that is deduced to "
                                        + "a reserved output file name, \""
                                        + fileName + "\". (Hint: Change the "
                                        + "xml:id.)");
                            }
                            curTOCNode.setOutputFileName(fileName);
                        }
                    } else { // of: if file element
                        elem.setAttribute(A_DOCGEN_PAGE_TOC_ELEMENT, "true");
                    }
                    elem.setAttribute(A_DOCGEN_DETAILED_TOC_ELEMENT, "true");
                } // if ToC element
            }  // if document structure element
        }  // if Element

        if (curTOCNode != null) {
            parentTOCNode = curTOCNode;
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            TOCNode child = preprocessDOM_buildTOC_inner(
                    children.item(i),
                    newSectionLevel,
                    parentTOCNode);

            if (child != null && parentTOCNode != null) {
                child.setParent(parentTOCNode);
                TOCNode lastChild = parentTOCNode.getLastChild();
                if (lastChild != null) {
                    child.setPrevious(lastChild);
                    lastChild.setNext(child);
                }

                if (parentTOCNode.getFirstChild() == null) {
                    parentTOCNode.setFirstChild(child);
                }
                parentTOCNode.setLastChild(child);
            }
        }

        return curTOCNode;
    }

    private String getExternalLinkTOCNodeURLOrNull(Element elem) {
        if (elem.getParentNode() instanceof Document) {
            // The document element is never an external link ToC node.
            return null;
        }

        Element title = getTitle(elem);
        if (title == null) {
            // An element without title can't be an external link ToC node
            return null;
        }

        Iterator<Element> it = XMLUtil.childrenElementsOf(title).iterator();
        if (it.hasNext()) {
            Element firstChild = it.next();
            if (!it.hasNext()) { // It's the only child
                String firstChildName = firstChild.getLocalName();
                if (firstChildName.equals(E_LINK)) {
                    String href = XMLUtil.getAttributeNS(firstChild, XMLNS_XLINK, A_XLINK_HREF);
                    if (href == null) {
                        throw new DocgenException(XMLUtil.theSomethingElement(firstChild, true)
                                + " inside a title has no xlink:" + A_XLINK_HREF + " attribute, thus it can't be "
                                + "used as ToC link.");
                    }
                    return href;
                } else if (firstChildName.equals(E_OLINK)) {
                    String targetdoc = XMLUtil.getAttributeNS(firstChild, null, A_TARGETDOC);
                    if (targetdoc == null) {
                        throw new DocgenException(XMLUtil.theSomethingElement(firstChild, true)
                                + " has no xlink:" + A_TARGETDOC + " attribute");
                    }
                    String url = olinks.get(targetdoc);
                    if (url == null) {
                        throw new DocgenException(XMLUtil.theSomethingElement(firstChild, true)
                                + " refers to undefined olink name " + StringUtil.jQuote(targetdoc)
                                + "; check configuration.");
                    }
                    return url;
                }
            }
        }
        return null;
    }

    /**
     * Ensures that
     * @param tocNodes
     * @throws DocgenException
     */
    private void preprocessDOM_buildTOC_checkEnsureHasIndexHhml(List<TOCNode> tocNodes) {
        for (TOCNode tocNode : tocNodes) {
            if (tocNode.getOutputFileName() != null && tocNode.getOutputFileName().equals(FILE_INDEX_HTML)) {
                return;
            }
        }
        // If we had no index.html, the ToC HTML will be renamed to it:
        for (TOCNode tocNode : tocNodes) {
            if (tocNode.getOutputFileName() != null && tocNode.getOutputFileName().equals(FILE_TOC_HTML)) {
                tocNode.setOutputFileName(FILE_INDEX_HTML);
                return;
            }
        }
        throw new DocgenException(
                "No " + FILE_INDEX_HTML + " output file would be generated. Add " + DocBook5Constants.A_ROLE + "=\""
                + AV_INDEX_ROLE + "\" to one of the elements for which a separate file is generated.");
    }

    private boolean hasPrefaceLikeParent(Element elem) {
        while (true) {
            Node parent = elem.getParentNode();
            if (parent != null && parent instanceof Element) {
                elem = (Element) parent;
                if (elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)
                        && PREFACE_LIKE_ELEMENTS.contains(
                                elem.getLocalName())) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private Element getTitle(Element elem) {
        NodeList children = elem.getChildNodes();
        int ln = children.getLength();
        for (int i = 0; i < ln; i++) {
            Node child = children.item(i);
            if (child instanceof Element
                    && child.getLocalName().equals("title")) {
                return (Element) child;  // !! found it
            }
        }
        return null;
    }

    private void ensureTitleExists(Element elem, String defaultTitle) {
        if (getTitle(elem) != null) {
            return;
        }

        // Retrieve a document node:
        Node node =  elem;
        do {
            node = node.getParentNode();
            if (node == null) {
                throw new BugException("Can't find Document node.");
            }
        } while (node.getNodeType() != Node.DOCUMENT_NODE);
        Document doc = (Document) node;

        // Create the title node:
        Element title = doc.createElementNS(XMLNS_DOCBOOK5, E_TITLE);
        title.appendChild(doc.createTextNode(defaultTitle));

        // Insert it into the tree:
        elem.insertBefore(title, elem.getFirstChild());
    }

    /**
     * Returns the {@link TOCNode} that corresponds to the element, or
     * {@link null} if it's not a file element. Can be called only
     * after {@link #createLookupTables(Node, LookupCreatingState)}.
     */
    private TOCNode getFileTOCNodeFor(Element elem) {
        for (TOCNode tocNode : tocNodes) {
            if (tocNode.isFileElement()
                    && tocNode.getElement().isSameNode(elem)) {
                return tocNode;
            }
        }
        return null;
    }

    private void addIndexTerm(Node node) {
        Node primary = null;
        Node secondary = null;

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (child.getNodeName().equals(E_PRIMARY)) {
                    primary = child;
                } else if (child.getNodeName().equals(E_SECONDARY)) {
                    secondary = child;
                }
            }
        }

        String primaryText = primary.getFirstChild().getNodeValue().trim();
        if (!primaryIndexTermLookup.containsKey(primaryText)) {
            primaryIndexTermLookup.put(primaryText, new ArrayList<>());
        }

        if (secondary != null) {
            if (!secondaryIndexTermLookup.containsKey(primaryText)) {
                secondaryIndexTermLookup.put(
                        primaryText, new TreeMap<>());
            }
            Map<String, List<NodeModel>> m = secondaryIndexTermLookup.get(
                    primaryText);
            String secondaryText = secondary.getFirstChild().getNodeValue()
                    .trim();
            List<NodeModel> nodes = m.get(secondaryText);
            if (nodes == null) {
                nodes = new ArrayList<>();
                m.put(secondaryText, nodes);
            }
            nodes.add(NodeModel.wrap(node));
        } else {
            primaryIndexTermLookup.get(primaryText).add(NodeModel.wrap(node));
        }
    }

    /**
     * Generates a HTML file for the {@link #currentFileTOCNode}, maybe with
     * some accompanying HTML-s.
     */
    private int generateHTMLFile()
            throws IOException, TemplateException {
        SimpleHash dataModel = new SimpleHash(fmConfig.getObjectWrapper());

        TOCNode otherTOCNode;

        otherTOCNode = currentFileTOCNode;
        do {
            otherTOCNode = otherTOCNode.getPreviousInTraversarOrder();
        } while (!(otherTOCNode == null || otherTOCNode.isFileElement()));
        dataModel.put(
                VAR_PREVIOUS_FILE_ELEMENT,
                otherTOCNode != null ? otherTOCNode.getElement() : null);

        otherTOCNode = currentFileTOCNode;
        do {
            otherTOCNode = otherTOCNode.getNextInTraversarOrder();
        } while (!(otherTOCNode == null || otherTOCNode.isFileElement()));
        dataModel.put(
                VAR_NEXT_FILE_ELEMENT,
                otherTOCNode != null ? otherTOCNode.getElement() : null);

        otherTOCNode = currentFileTOCNode.getParent();
        dataModel.put(
                VAR_PARENT_FILE_ELEMENT,
                otherTOCNode != null ? otherTOCNode.getElement() : null);

        Element curElem = currentFileTOCNode.getElement();
        final boolean isTheDocumentElement
                = curElem.getParentNode() instanceof Document;
        dataModel.put(
                VAR_TOC_DISPLAY_DEPTH,
                isTheDocumentElement
                        ? maxTOFDisplayDepth : maxMainTOFDisplayDepth);

        if (seoMeta != null) {
            Map<String, String> seoMetaMap = seoMeta.get("file:" + currentFileTOCNode.getOutputFileName());
            if (seoMetaMap == null) {
                String id = XMLUtil.getAttribute(currentFileTOCNode.getElement(), "id");
                if (id != null) {
                    seoMetaMap = seoMeta.get(id);
                }
            }
            if (seoMetaMap != null) {
                dataModel.put(
                        VAR_SEO_META_TITLE_OVERRIDE,
                        seoMetaMap.get(SETTING_SEO_META_KEY_TITLE));
                dataModel.put(
                        VAR_SEO_META_FULL_TITLE_OVERRIDE,
                        seoMetaMap.get(SETTING_SEO_META_KEY_FULL_TITLE));
                dataModel.put(
                        VAR_SEO_META_DESCRIPTION,
                        seoMetaMap.get(SETTING_SEO_META_KEY_DESCRIPTION));
            }
        }

        boolean generateDetailedTOC = false;
        if (isTheDocumentElement) {
            // Find out if a detailed ToC will be useful:
            int mainTOFEntryCount = countTOFEntries(
                    currentFileTOCNode, maxMainTOFDisplayDepth);
            if (mainTOFEntryCount != 0  // means, not a single-page output
                    && mainTOFEntryCount < tocNodes.size() * 0.75) {
                generateDetailedTOC = true;
                dataModel.put(
                        VAR_ALTERNATIVE_TOC_LINK,
                        FILE_DETAILED_TOC_HTML);
                dataModel.put(
                        VAR_ALTERNATIVE_TOC_LABEL,
                        "show detailed");
            }
        }

        generateHTMLFile_inner(dataModel, currentFileTOCNode.getOutputFileName());

        if (generateDetailedTOC) {
            dataModel.put(VAR_PAGE_TYPE, PAGE_TYPE_DETAILED_TOC);
            dataModel.put(
                    VAR_ALTERNATIVE_TOC_LINK,
                    currentFileTOCNode.getOutputFileName());
            dataModel.put(
                    VAR_ALTERNATIVE_TOC_LABEL,
                    "show simplified");
            generateHTMLFile_inner(dataModel, FILE_DETAILED_TOC_HTML);
            return 2;
        } else {
            return 1;
        }
    }

    private void generateSearchResultsHTMLFile(Document doc) throws TemplateException, IOException, DocgenException {
        SimpleHash dataModel = new SimpleHash(fmConfig.getObjectWrapper());

        dataModel.put(VAR_PAGE_TYPE, PAGE_TYPE_SEARCH_RESULTS);
        dataModel.put(VAR_TOC_DISPLAY_DEPTH, maxMainTOFDisplayDepth);

        // Create docgen:searchresults element that's no really in the XML file:
        Element searchresultsElem = doc.createElementNS(XMLNS_DOCGEN, E_SEARCHRESULTS);
        {
            // Docgen templates may expect page-elements to have an id:
            if (elementsById.containsKey(SEARCH_RESULTS_ELEMENT_ID)) {
                throw new DocgenException("Reserved element id \"" + SEARCH_RESULTS_ELEMENT_ID + "\" was already taken");
            }
            searchresultsElem.setAttribute("id", SEARCH_RESULTS_ELEMENT_ID);

            searchresultsElem.setAttribute(A_DOCGEN_RANK, E_SECTION);

            // Docgen templates may expect page-elements to have a title:
            Element titleElem = doc.createElementNS(XMLNS_DOCBOOK5, E_TITLE);
            titleElem.setTextContent(SEARCH_RESULTS_PAGE_TITLE);
            searchresultsElem.appendChild(titleElem);
        }

        // We must add it to the document so that .node?root and such will work.
        doc.getDocumentElement().appendChild(searchresultsElem);
        try {
            TOCNode searchresultsTOCNode = new TOCNode(searchresultsElem, 0);
            searchresultsTOCNode.setFileElement(true);
            searchresultsTOCNode.setOutputFileName(FILE_SEARCH_RESULTS_HTML);
            currentFileTOCNode = searchresultsTOCNode;

            generateHTMLFile_inner(dataModel, currentFileTOCNode.getOutputFileName());
        } finally {
            doc.getDocumentElement().removeChild(searchresultsElem);
        }
    }

    private void generateHTMLFile_inner(SimpleHash dataModel, String fileName)
            throws TemplateException, IOException {
        Template template = fmConfig.getTemplate("page.ftlh");
        File outputFile = new File(destDir, fileName);
        try (Writer writer = FileUtil.newFileWriter(outputFile)) {
            template.process(
                    dataModel,
                    writer, null,
                    NodeModel.wrap(currentFileTOCNode.getElement()));
        }
    }

    private int countTOFEntries(TOCNode parent,
            int displayDepth) {
        int sum = 0;
        TOCNode child = parent.getFirstChild();
        while (child != null) {
            if (child.isFileElement()) {
                sum++;
                if (displayDepth > 1) {
                    sum += countTOFEntries(child, displayDepth - 1);
                }
            }
            child = child.getNext();
        }
        return sum;
    }

    /**
     * Returns if the TOC node contains anything (other than generated content) in the same file where the TOC node
     * is displayed.
     */
    private boolean hasContentInTheSameFile(TOCNode tocNode) {
        for (Element elem : XMLUtil.childrenElementsOf(tocNode.getElement())) {
            if (elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                String name = elem.getLocalName();
                if (!elem.hasAttribute(A_DOCGEN_FILE_ELEMENT)
                        && VISIBLE_TOP_LEVEL_ELEMENTS.contains(name)
                        && !name.equals(E_TITLE) && !name.equals(E_SUBTITLE)
                        && !name.equals(E_INFO)
                        && !name.equals(E_FOOTNOTE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String createElementLinkURL(final Element elem) {
        if (elem.hasAttribute(A_DOCGEN_NOT_ADDRESSABLE)) {
            return null;
        }

        String extLink = getExternalLinkTOCNodeURLOrNull(elem);
        if (extLink != null) {
            return extLink;
        }

        // Find the closest id:
        String id = null;
        Node node = elem;
        while (node != null) {
            if (node instanceof Element) {
                id = XMLUtil.getAttribute((Element) node, "id");
                if (id != null) {
                    break;
                }
            }
            node = node.getParentNode();
        }
        if (id == null) {
            throw new DocgenException(
                    "Can't create link for the \"" + elem.getLocalName()
                    + "\" element: Nor this element nor its ascendants have an "
                    + "id.");
        }
        final Element idElem = (Element) node;

        String fileName = null;
        Element curElem = idElem;
        do {
            TOCNode fileTOCNode = getFileTOCNodeFor(curElem);
            if (fileTOCNode == null) {
                curElem = (Element) curElem.getParentNode();
            } else {
                fileName = fileTOCNode.getOutputFileName();
                if (fileName == null) throw new IllegalStateException("fileTOCNode with null outputFileName");
            }
        } while (fileName == null);

        String link;
        if (currentFileTOCNode != null
                && fileName.equals(currentFileTOCNode.getOutputFileName())) {
            link = "";
        } else {
            link = fileName;
        }

        if (getFileTOCNodeFor(idElem) == null) {
            link = link + "#" + id;
        }

        // IE6 doesn't like empty href-s:
        if (link.length() == 0) {
            link = fileName;
        }

        return link;
    }

    private String getArgString(List<?> args, int argIdx) throws TemplateModelException {
        Object value = args.get(argIdx);
        if (value instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) value).getAsString();
        }
        if (value instanceof TemplateModel) {
            throw new TemplateModelException("Argument #" + (argIdx + 1) + " should be a string, but it was: "
                    + ClassUtil.getFTLTypeDescription((TemplateModel) value));
        }
        throw new IllegalArgumentException("\"value\" must be " + TemplateModel.class.getName());
    }

    private TemplateMethodModelEx createLinkFromID = new TemplateMethodModelEx() {

        @Override
        public Object exec(@SuppressWarnings("rawtypes") final List args)
                throws TemplateModelException {
            if (args.size() != 1) {
                throw new TemplateModelException(
                        "Method CreateLinkFromID should have exactly one "
                        + "parameter.");
            }
            String id = getArgString(args, 0);

            try {
                return createLinkFromId(id);
            } catch (DocgenException e) {
                throw new TemplateModelException("Can't resolve id " + StringUtil.jQuote(id) + " to URL", e);
            }
        }

    };

    private String createLinkFromId(String id) {
        if (elementsById == null) {
            throw new IllegalStateException("Can't resolve ID as elementsById is still null: " + id);
        }
        Element elem = elementsById.get(id);
        if (elem == null) {
            throw new DocgenException(
                    "No element exists with this id: \"" + id + "\"");
        }

        return createElementLinkURL(elem);
    }

    private TemplateMethodModelEx createLinkFromNode
            = new TemplateMethodModelEx() {

        @Override
        public Object exec(@SuppressWarnings("rawtypes") final List args)
                throws TemplateModelException {

            if (args.size() != 1) {
                throw new TemplateModelException(
                        "Method CreateLinkFromNode should have exactly one "
                        + "parameter.");
            }
            Object arg1 = args.get(0);
            if (!(arg1 instanceof NodeModel)) {
                throw new TemplateModelException(
                        "The first parameter to CreateLinkFromNode must be a "
                        + "node, but it wasn't. (Class: "
                        + arg1.getClass().getName() + ")");
            }
            Node node = ((NodeModel) arg1).getNode();
            if (!(node instanceof Element)) {
                throw new TemplateModelException(
                        "The first parameter to CreateLinkFromNode must be an "
                        + "element node, but it wasn't. (Class: "
                        + arg1.getClass().getName() + ")");
            }

            try {
                String url = createElementLinkURL((Element) node);
                return url != null ? new SimpleScalar(url) : null;
            } catch (DocgenException e) {
                throw new TemplateModelException(
                        "CreateLinkFromNode falied to create link.", e);
            }
        }

    };

    private TemplateMethodModelEx nodeFromID = new TemplateMethodModelEx() {

        @Override
        public Object exec(@SuppressWarnings("rawtypes") List args)
                throws TemplateModelException {
            Node node = elementsById.get(getArgString(args, 0));
            return NodeModel.wrap(node);
        }

    };

    // -------------------------------------------------------------------------

    Map<String, Path> getInsertableFiles() {
        return insertableFiles;
    }

    TOCNode getCurrentFileTOCNode() {
        return currentFileTOCNode;
    }

    // -------------------------------------------------------------------------

    public Map<String, InsertableOutputCommandProperties> getInsertableOutputCommands() {
        return insertableOutputCommands;
    }

    public File getDestinationDirectory() {
        return destDir;
    }

    /**
     * Sets the directory where all the output files will go.
     */
    public void setDestinationDirectory(File destDir) {
        this.destDir = destDir;
    }

    public File getSourceDirectory() {
        return srcDir;
    }

    public void setSourceDirectory(File srcDir) {
        this.srcDir = srcDir;
    }

    public File getCustomVariableFileDirectory() {
        return customVariableFileDir;
    }

    public void setCustomVariableFileDirectory(File customVariableFileDir) {
        this.customVariableFileDir = customVariableFileDir;
    }

    public Boolean getOffline() {
        return offline;
    }

    public void setOffline(Boolean offline) {
        this.offline = offline;
    }

    public boolean getSimpleNavigationMode() {
        return simpleNavigationMode;
    }

    public void setSimpleNavigationMode(boolean simpleNavigationMode) {
        this.simpleNavigationMode = simpleNavigationMode;
    }

    public boolean getShowEditoralNotes() {
        return showEditoralNotes;
    }

    public void setShowEditoralNotes(boolean showEditoralNotes) {
        this.showEditoralNotes = showEditoralNotes;
    }

    public boolean getValidate() {
        return validate;
    }

    /**
     * Specifies if the DocBook XML should be validated against the DocBook 5
     * RELAX NG Schema; defaults to {@code true}. Setting this to {@code false}
     * can have whatever random effects later if the DocBook isn't valid,
     * since the transformation written with the assumption that source is
     * valid XML.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public boolean getPrintProgress() {
        return printProgress;
    }

    /**
     * Sets if {@link #execute()} should print feedback to the stdout.
     * Note that errors (exceptions) will never be printed, just thrown.
     */
    public void setPrintProgress(boolean printProgress) {
        this.printProgress = printProgress;
    }

    public boolean getGenerateEclipseToC() {
        return generateEclipseTOC;
    }

    public void setGenerateEclipseToC(boolean eclipseToC) {
        this.generateEclipseTOC = eclipseToC;
    }

    /**
     * Adds to the {@link Map} of custom variables, that will be available in templates with variable name
     * {@link #VAR_CUSTOM_VARIABLES}. You can only set variables that are also set in ({@link #FILE_SETTINGS}) (even
     * if you {@code null}), or else {@link #execute()} will throw exception. The values set here has precedence over
     * the values coming from the settings file.
     */
    public void addCustomVariableOverrides(Map<String, Object> customVariables) {
        this.customVariableOverrides.putAll(customVariables);
    }

    public void addInsertableFileOverrides(Map<String, String> insertableFilesOverrides) {
        this.insertableFilesOverrides.putAll(insertableFilesOverrides);
    }

    // -------------------------------------------------------------------------

    /**
     * A node in the XML document for which a ToC entry should be shown.
     * These nodes form a tree that exists in parallel with the the tree of DOM
     * nodes.
     */
    public class TOCNode {

        private final Element element;
        private final int traversalIndex;
        private TOCNode parent;
        private TOCNode next;
        private TOCNode previous;
        private TOCNode firstChild;
        private TOCNode lastChild;
        private boolean fileElement;
        private String outputFileName;

        public TOCNode(Element element, int traversalIndex) {
            this.element = element;
            this.traversalIndex = traversalIndex;
        }

        public TOCNode getFirstChild() {
            return firstChild;
        }

        public void setFirstChild(TOCNode firstChild) {
            this.firstChild = firstChild;
        }

        public TOCNode getLastChild() {
            return lastChild;
        }

        public void setLastChild(TOCNode lastChild) {
            this.lastChild = lastChild;
        }

        public void setParent(TOCNode parent) {
            this.parent = parent;
        }

        public TOCNode getNext() {
            return next;
        }

        public void setNext(TOCNode next) {
            this.next = next;
        }

        public TOCNode getPrevious() {
            return previous;
        }

        public void setPrevious(TOCNode previous) {
            this.previous = previous;
        }

        public TOCNode getParent() {
            return parent;
        }

        public void setOutputFileName(String outputFileName) {
            if (!fileElement) {
                throw new BugException("Can't set outputFileName before setting fileElement to true");
            }
            this.outputFileName = outputFileName;
        }

        /**
         * {@code null} if no file will be generated for this node, despite its "rank". This is the case for nodes that
         * are external links, or when {@link Transform#simpleNavigationMode} is {@code true} and the file would only
         * contain a ToC.
         */
        public String getOutputFileName() {
            return outputFileName;
        }

        public Element getElement() {
            return element;
        }

        public void setFileElement(boolean fileElement) {
            this.fileElement = fileElement;
        }

        public boolean isFileElement() {
            return fileElement;
        }

        public String theSomethingElement() {
            return XMLUtil.theSomethingElement(element);
        }

        public TOCNode getNextInTraversarOrder() {
            return traversalIndex + 1 < tocNodes.size()
                    ? tocNodes.get(traversalIndex + 1) : null;
        }

        public TOCNode getPreviousInTraversarOrder() {
            return traversalIndex > 0
                    ? tocNodes.get(traversalIndex - 1) : null;
        }

    }

    enum DocumentStructureRank {
        SIMPLESECT, SECTION3, SECTION2, SECTION1, CHAPTER, PART, BOOK;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        static String sectionToString(int level) {
            return DocumentStructureRank.SECTION1.toString().substring(
                    0,
                    DocumentStructureRank.SECTION1.toString().length() - 1)
                    + level;
        }
    }

    static class InsertableOutputCommandProperties {
        private final String mainClassName;
        private final String mainMethodName;
        private final Map<String, String> systemProperties;
        private final List<String> prependedArguments;
        private final List<String> appendedArguments;
        private final Path wdSubstitution;

        public InsertableOutputCommandProperties(
                String mainClassName, String mainMethodName,
                Map<String, String> systemProperties,
                List<String> prependedArguments, List<String> appendedArguments, Path wdSubstitution) {
            this.mainClassName = mainClassName;
            this.mainMethodName = mainMethodName;
            this.systemProperties = systemProperties;
            this.prependedArguments = prependedArguments;
            this.appendedArguments = appendedArguments;
            this.wdSubstitution = wdSubstitution;
        }

        public String getMainClassName() {
            return mainClassName;
        }

        public String getMainMethodName() {
            return mainMethodName;
        }

        public Map<String, String> getSystemProperties() {
            return systemProperties;
        }

        public List<String> getPrependedArguments() {
            return prependedArguments;
        }

        public List<String> getAppendedArguments() {
            return appendedArguments;
        }

        public Path getWdSubstitution() {
            return wdSubstitution;
        }

        @Override
        public String toString() {
            return "InsertableOutputCommandProperties{" +
                    "mainClassName='" + mainClassName + '\'' +
                    ", mainMethodName='" + mainMethodName + '\'' +
                    ", systemProperties=" + systemProperties +
                    ", prependedArguments=" + prependedArguments +
                    ", appendedArguments=" + appendedArguments +
                    ", wdSubstitution=" + wdSubstitution +
                    '}';
        }
    }

    @FunctionalInterface
    interface CJSONFunction {
        Object run(Transform context, CJSONInterpreter.FunctionCall fc);
    }

    private static final Map<String, CJSONFunction> CJSON_FUNCTIONS = ImmutableMap.of(
            "getCustomVariable",
            (ctx, fc) -> {
                List<Object> params = fc.getParams();
                if (params.size() != 1) {
                    throw new DocgenException(
                            "CJSON function " + fc.getName() + "(name) "
                                    + "should have 1 arguments, but had " + params.size() + ".");
                }

                Object varName = params.get(0);
                if (!(varName instanceof String)) {
                    throw new DocgenException(
                            "CJSON function " + fc.getName() + "(name) "
                                    + "argument should be a string, but was a(n) "
                                    + CJSONInterpreter.cjsonTypeNameOfValue(varName) + ".");
                }

                Object result = ctx.customVariableOverrides.get(varName);
                if (result == null) {
                    result = ctx.customVariablesFromSettingsFile.get(varName);
                }
                if (result == null) {
                    throw new DocgenException(
                            "The custom variable " + StringUtil.jQuote(varName) + " is not set (or was set to null).");
                }
                return result;
            },
            "concat",
            (ctx, fc) -> {
                return fc.getParams().stream()
                        .filter(it -> it != null)
                        .map(Object::toString)
                        .collect(Collectors.joining());
            }
    );

    class DocgenCJSONEvaluationEnvironment implements CJSONInterpreter.EvaluationEnvironment {
        @Override
        public Object evalFunctionCall(CJSONInterpreter.FunctionCall fc, CJSONInterpreter ip) {
            String name = fc.getName();
            CJSONFunction f = CJSON_FUNCTIONS.get(name);
            if (f == null) {
                throw new DocgenException("Unknown CJSON function: " + name
                        + "\nSupported functions are: " + String.join(", ", CJSON_FUNCTIONS.keySet()));
            }
            return f.run(Transform.this, fc);
        }

        @Override
        public Object notify(CJSONInterpreter.EvaluationEvent event, CJSONInterpreter ip, String name, Object extra) throws
                Exception {
            return null;
        }
    }

}
