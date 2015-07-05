package org.freemarker.docgen;

import static org.freemarker.docgen.DocBook5Constants.A_FILEREF;
import static org.freemarker.docgen.DocBook5Constants.A_TARGETDOC;
import static org.freemarker.docgen.DocBook5Constants.A_XLINK_HREF;
import static org.freemarker.docgen.DocBook5Constants.DOCUMENT_STRUCTURE_ELEMENTS;
import static org.freemarker.docgen.DocBook5Constants.E_APPENDIX;
import static org.freemarker.docgen.DocBook5Constants.E_ARTICLE;
import static org.freemarker.docgen.DocBook5Constants.E_BOOK;
import static org.freemarker.docgen.DocBook5Constants.E_CHAPTER;
import static org.freemarker.docgen.DocBook5Constants.E_FOOTNOTE;
import static org.freemarker.docgen.DocBook5Constants.E_GLOSSARY;
import static org.freemarker.docgen.DocBook5Constants.E_GLOSSENTRY;
import static org.freemarker.docgen.DocBook5Constants.E_IMAGEDATA;
import static org.freemarker.docgen.DocBook5Constants.E_INDEX;
import static org.freemarker.docgen.DocBook5Constants.E_INDEXTERM;
import static org.freemarker.docgen.DocBook5Constants.E_INFO;
import static org.freemarker.docgen.DocBook5Constants.E_INFORMALTABLE;
import static org.freemarker.docgen.DocBook5Constants.E_LINK;
import static org.freemarker.docgen.DocBook5Constants.E_OLINK;
import static org.freemarker.docgen.DocBook5Constants.E_PART;
import static org.freemarker.docgen.DocBook5Constants.E_PREFACE;
import static org.freemarker.docgen.DocBook5Constants.E_PRIMARY;
import static org.freemarker.docgen.DocBook5Constants.E_SECONDARY;
import static org.freemarker.docgen.DocBook5Constants.E_SECTION;
import static org.freemarker.docgen.DocBook5Constants.E_SIMPLESECT;
import static org.freemarker.docgen.DocBook5Constants.E_SUBTITLE;
import static org.freemarker.docgen.DocBook5Constants.E_TABLE;
import static org.freemarker.docgen.DocBook5Constants.E_TITLE;
import static org.freemarker.docgen.DocBook5Constants.VISIBLE_TOPLEVEL_ELEMENTS;
import static org.freemarker.docgen.DocBook5Constants.XMLNS_DOCBOOK5;
import static org.freemarker.docgen.DocBook5Constants.XMLNS_XLINK;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import freemarker.template.utility.StringUtil;

/**
 * Generates complete HTML-format documentation from a DocBook 5 (XML) book.
 *
 * <p>Usage: First set the JavaBean properties, then call {@link #execute()};
 * These must be set:
 * <ul>
 *   <li>{@link #setSourceDirectory(File)}
 *   <li>{@link #setDestinationDirectory(File)}
 *   <li>{@link #setOffline(Boolean)}, unless the configuration file specifies this
 * </ul>
 *
 * <p>All files and directories in the source directory will be copied into the
 * destination directory as is (recursively), except these, which will be
 * ignored:
 * <ul>
 *   <li>file or directory whose name starts with <tt>"docgen-"</tt> or
 *       <tt>"docgen."</tt>, or whose name is <tt>"docgen"</tt>.
 *   <li>file directly in the source directory (not in a sub-directory)
 *       with <tt>xml</tt> file extension
 *   <li>file or directory whose name looks like it's just backup, temporary,
 *       SVN-related or CVS-related entry.
 * </ul>
 *
 * <p>The following files/directories are treated specially:
 * <ul>
 *   <li><p><tt>book.xml</tt> or <tt>article.xml</tt> (<b>required</b>): the
 *       DocBook XML that we want to transform. It may contains XInclude-s so
 *       it doesn't have to store the whole book or article.
 *
 *   <li><p><tt>docgen.cjson</tt> file (optional):
 *       contains Docgen settings. It uses an extended JSON syntax; see more
 *       <a href="#cjsonLanguage">later</a>. The supported settings are
 *       (all optional):
 *       <ul>
 *         <li>
 *             <p><tt>tabs</tt> (map): Defines the tabs on the top of the page.
 *             It associates labels with URL-s to which the tab will lead to.
 *             The URL of one of the tabs should be <tt>""</tt>, which means that
 *             it's the tab that belongs to the documentation generated by this
 *             configuration. The order of the map entries specifies the order in
 *             which the tabs will appear.
 *         <li><p><tt>secondaryTabs</tt> (object): An array of objects for the
 *             with the secondary tabs, like [{ href: "", text: "", class: ""}]
 *         <li><p><tt>socialLinks</tt> (object): An array of objects for the
 *             social links, like [{ href: "", text: "", class: ""}]
 *         <li><p><tt>searchKey</tt> (string): A Google custom search key. If not
 *             present, the search box will not show.
 *         <li><p><tt>footerSiteMap</tt> (object): Defines the list of links to
 *             display in the footer as columns
 *         <li><p><tt>internalBookmarks</tt> (map): Specifies the first part
 *             of the book-mark link list that appears in the navigation bar.
 *             Associates labels with element ID-s (<tt>xml:id</tt> attribute
 *             values). The order of the map entries specifies the order in
 *             which the book-marks will appear.
 *
 *         <li><p><tt>externalBookmarks</tt> (map): Specifies the second part
 *             of the book-mark link list. Associates labels with arbitrary
 *             URL-s or paths. Again, order matters. External bookmarks should
 *             be used to link to resources outside the documentation generated
 *             by this configuration. If the target resource is controlled by
 *             you, and you can use the same set of tabs there are here,
 *             consider using <tt>tabs</tt> instead.
 *
 *         <li><p><tt>offline</tt> (boolean):
 *            Specifies if the documentation will be generated for offline use.
 *            If it was already specified via {@link #setOffline(Boolean)}, then
 *            that has priority. If it wasn't specified via {@link #setOffline(Boolean)},
 *            then it's mandatory to set.
 *         <li><p><tt>deployUrl</tt> (string): Url the page is deployed to (used
 *            for canonical urls)
 *         <li><p><tt>olinks</tt> (map):
 *            Maps <tt>olink</tt> <tt>targetdoc</tt> attribute values to
 *            actual URL-s.
 *
 *         <li><p><tt>validation</tt> (map):
 *            This is where you can configure the optional Docgen-specific
 *            DocBook validation restrictions. Accepted map entries are:
 *            <ul>
 *              <li><tt>programlistingsRequireRole</tt> (boolean):
 *                  defaults to {@code false}.
 *              <li><tt>programlistingsRequireLanguage</tt> (boolean):
 *                  defaults to {@code false}.
 *              <li><tt>outputFilesCanUseAutoID</tt> (boolean):
 *                  defaults to {@code false}.
 *              <li><tt>maximumProgramlistingWidth</tt> (int): defaults to
 *                  {@link Integer#MAX_VALUE}. The maximum number of characters
 *                  per line in <tt>programlisting</tt>-s.
 *            </ul>
 *
 *         <li><p><tt>contentDirectory</tt> (string): By default the Docgen
 *            configuration files and the files that store the actual book
 *            content (DocBook XML-s, images, etc.) are in the same directory,
 *            the so called source directory. By setting this setting, the last
 *            can be separated from the directory of the configuration files.
 *            If it's not an absolute path then it will be interpreted
 *            relatively to the source directory.
 *
 *         <li><p><a name="setting_lowestFileElementRank"></a>
 *            <tt>lowestFileElementRank</tt> (string): The lowest document
 *            structure element "rank" for which an own output file will be
 *            created. Note that possibly not all such elements are shown in a
 *            given TOF (Table of Files) due to the <tt>maxTOFDisplayDepth</tt>
 *            or <tt>maxMainTOFDisplayDepth</tt> setting.
 *
 *            <p>"rank" symbolizes how big structural unit the element stands
 *            for. The valid ranks are, from the lowest to the highest:
 *            {@code simplesect}, {@code section3}, {@code section2},
 *            {@code section1}, {@code chapter}, {@code part},
 *            {@code book}.
 *            If the name of an element is the same as one of the rank names
 *            then that will be its rank. For <tt>section</tt>-s, the number in
 *            the rank name tells how deeply the <tt>section</tt> is nested into
 *            other <tt>section</tt>-s (1 means a <tt>section</tt> that is not
 *            nested into any other <tt>section</tt>-s). For the other document
 *            structure elements (e.g. <tt>appendix</tt>, <tt>preface</tt>,
 *            etc.) the rank will be decided based on its surroundings. For
 *            example, <tt>book/appendix</tt>, if it has <tt>part</tt> siblings,
 *            will receive <tt>part</tt> rank, but if it has <tt>chapter</tt>
 *            siblings, then it will only receive <tt>chapter</tt> rank. Again
 *            the same kind of element, <tt>appendix</tt>, inside a
 *            <tt>chapter</tt> will only receive <tt>section1</tt> rank.
 *            It's good to know that nothing will receive <tt>chapter</tt> rank
 *            unless it's directly under a <tt>part</tt> element (not just a
 *            {@code part}-ranked element!) or <tt>book</tt> element. However,
 *            if the root element of the document is <tt>article</tt>, that
 *            will receive <tt>chapter</tt> rank.
 *
 *            <p>Note that the content of some elements, like of
 *            <tt>preface</tt>-s, is kept in a single file regardless of this
 *            setting.
 *
 *            <p>The default value is <tt>section1</tt>.
 *
 *         <li><p><tt>lowestPageTOCElementRank</tt> (string):
 *            The lowest document structure element "rank" for which a
 *            "Page Contents" ToC entry will be created.
 *
 *            <p>About "ranks" see <a href="#setting_lowestFileElementRank">the
 *            <tt>lowestFileElementRank</tt> setting</a>.
 *
 *            <p>The default value is <tt>section3</tt>.

 *         <li><p><tt>maxTOFDisplayDepth</tt> (int): In a given TOF
 *            (Table of Files) (because there can be multiple TOF-s, like there
 *            can be a book-level TOF, and then there can be chapter-level
 *            TOF-s), this is the nesting level until TOF entries are actually
 *            displayed.
 *            Depth level 0 is considered to by the level where the
 *            file-element of the HTML page which contains the TOF is.
 *            Defaults to {@link Integer#MAX_VALUE}. Must be at least {@code 1}.
 *
 *         <li><p><tt>maxMainTOFDisplayDepth</tt> (int): Same as
 *            <tt>maxTOFDisplayDepth</tt>, but only applies to the TOF on the
 *            first (index) page. Defaults to the value of
 *            <tt>maxTOFDisplayDepth</tt>.
 *
 *         <li><p><tt>numberedSections</tt> (boolean): Specifies if
 *            <tt>section</tt> element titles should be shown with numbering.
 *            This will result in titles like "2 something" "2.1 Something" or
 *            "2.1.3 Something", even "B.1 Something" (last is a
 *            <tt>section</tt> under Appendix B).
 *
 *            <p>Note that within some elements, like inside <tt>preface</tt>-s,
 *            nothing has prefixes (labels) so this setting is ignored there.
 *
 *         <li><p><tt>generateEclipseTOC</tt> (boolean): Sets whether an Eclipse
 *            ToC XML is generated for the generated HTML-s. Defaults to
 *            <tt>false</tt>.
 *
 *         <li><p><tt>eclipse</tt> (map):
 *            Stores the settings of the Eclipse-ToC-generation.
 *            (Note that you still must turn that on with
 *            <tt>generateEclipseTOC</tt>; the mere presence
 *            of this setting will not do that.).
 *            Accepted map entries are:
 *            <ul>
 *              <li><tt>link_to</tt> (string): The value of
 *                  <tt>toc.@link_to</tt> in the generated ToC file. If not
 *                  specified, there will not be any <tt>link_to</tt> attribute.
 *            </ul>
 *
 *         <li><p><tt>locale</tt> (string): The "nationality" used for
 *            lexical shorting, number formatting and such things.
 *            Defaults to <tt>"en_US"</tt>.
 *
 *         <li><p><tt>timeZone</tt> (string): The time zone used for the
 *            date/time shown. Defaults to <tt>"GMT"</tt>.
 *
 *         <li><p><tt>disableJavaScript</tt> (boolean): Disallow JavaScript in
 *           the generated pages. Defaults to <tt>false</tt> (i.e., JavaScript
 *           is allowed). The pages are more functional with JavaScript, but
 *           MSIE 6 and 7 (didn't tried 8) will show a security alert and block
 *           JavaScript if the page is opened from the local file-system (i.e.,
 *           as <tt>file://...</tt> or <tt>C:\...</tt>, etc). So if the
 *           generated content is often read locally and the target audience is
 *           not IT-people (who know this thing very well, since even Javadoc
 *           output does this), you better set this to <tt>true</tt>. Note that
 *           even with JavaScript blocked by MSIE, the page will remain as
 *           functional as if you were generating it with
 *           <tt>disableJavaScript</tt> set to <tt>true</tt>, only the security
 *           warning is annoying.
 *
 *         <li><p><tt>onlineTrackerHTML</tt> (string): The path of a HTML file
 *         whose content will be inserted before the <tt>body</tt> tag, unless
 *         <tt>offline</tt> was set to <tt>true</tt>. This is typically used to
 *         insert the Google Analytics <tt>script</tt> element. If this path is
 *         relative, it's relative to the source directory.
 *
 *         <li><p><tt>showXXELogo</tt> (boolean): Specifies if an
 *           "Edited with XXE" logo should be shown on the generated pages.
 *           Defaults to <tt>false</tt>.
 *
 *       </ul>
 *
 *       <li><p><tt>docgen-templates</tt> directory:
 *           The templates here will have priority over the ones in the
 *           {@code org.freemarker.docgen.templates} package.
 *           This is mostly used for overriding <tt>customizations.ftl</tt>;
 *           that FTL is <tt>#import</tt>-ed at the beginning of all
 *           template files, and searched first for the
 *           <tt>#visit</tt>/<tt>#recurse</tt> calls.
 * </ul>
 *
 *
 * <p><b><font size="+1"><a name="cjsonLanguage"></a>
 *   The CJSON language
 * </font></b></p>
 *
 * <p>It's JSON extended with some features that make it more convenient for
 * configuration files:
 * <ul>
 *   <li>String literals whose value only contains letters (UNICODE), digits
 *       (UNICODE) and characters {@code '.'}, {@code '_'}, {@code '$'},
 *       {@code '@'}, and {@code '-'}, but don't start with
 *       characters 0-9 or is {@code true} or {@code false}, need not be
 *       quoted. Thus instead of
 *       <tt>{"name": "Big Joe", "color": "red"}</tt> you can just
 *       write <tt>{name: "Big Joe", color: red}</tt>. (There are no
 *       variable references in CJSON.)
 *   <li>In key-value pairs the value defaults to {@code true}. Like, instead
 *       of <tt>{showLogo: true}</tt> you can just write <tt>{showLogo}</tt>.
 *   <li>You can omit the commas that otherwise would be at the end of the line.
 *   <li>JavaScript comments are supported (<tt>/* ... *<!-- -->/</tt> and
 *       <tt>// ...</tt>)
 *   <li>If a file is expected to contain a map, like most configuration
 *       files are, putting the whole thing between <tt>{</tt> and <tt>}</tt> is
 *       optional.
 *   <li>Maps remember the order in which the entries were specified in the
 *       expression. The consumer of the configuration file will not utilize
 *       this for most settings anyway, but for certain kind of settings it's
 *       just more intuitive than getting the entries in a some random order.
 *   <li>A comma may be used after the last item of a list or map.
 *   <li>Supports FTL raw string literals (e.g. {@code r"C:\Windows\System32"}).
 *   <li>Supports function calls (e.g. {@code f(1, 2)}), although it's up to the
 *       consumer to resolve them; the CJSON language itself doesn't define any
 *       functions.
 * </ul>
 *
 * <p>When CJSON is stored in a file, the file extension should be
 * <tt>cjson</tt> and UTF-8 charset should be is used. However, the charset can
 * be overridden with a initial
 * <tt>//&nbsp;charset:&nbsp;<i>charsetName</i></tt> comment [*].
 * Initial BOM is silently ignored.
 *
 * <blockquote>
 * <p>* The comment is considered to be a charset override only if when it's
 *      decoded with ISO-8859-1 it stands that:
 * <ul>
 *   <li>Apart from white-space (and an initial BOM) it's the first thing in
 *       the file.
 *   <li>It's a <tt>//</tt> comment, not a <tt>/* ... *<!-- -->/</tt> comment.
 *   <li>Ignoring white-space, the first word inside the comment is
 *       <tt>charset</tt> or <tt>encoding</tt> (they are equivalent). That's
 *       followed by optional whitespace, then a colon, then optional
 *       whitespace again. Then a non-whitespace character (the first letter of
 *       the charset name). At this point the comment already counts as a
 *       charset override. Starting from there, until the end of the line or
 *       of the file (whichever comes first) all kind of characters can occur,
 *       and they will all belong to the charset name (which will be
 *       interpreted after trimming surrounding whitespace).
 * </ul>
 * </blockquote>
 */
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
    static final String FILE_ECLIPSE_TOC_TEMPLATE = "eclipse-toc.ftl";
    static final String FILE_ECLIPSE_TOC_OUTPUT = "eclipse-toc.xml";
    static final String DIR_TEMPLATES = "docgen-templates";
    
    static final String SETTING_VALIDATION = "validation";
    static final String SETTING_OFFLINE = "offline";
    static final String SETTING_SIMPLE_NAVIGATION_MODE = "simpleNavigationMode";
    static final String SETTING_DEPLOY_URL = "deployUrl";
    static final String SETTING_ONLINE_TRACKER_HTML = "onlineTrackerHTML";
    static final String SETTING_INTERNAL_BOOKMARKS = "internalBookmarks";
    static final String SETTING_EXTERNAL_BOOKMARKS = "externalBookmarks";
    static final String SETTING_LOGO = "logo";
    static final String SETTING_COPYRIGHT_HOLDER = "copyrightHolder";
    static final String SETTING_COPYRIGHT_START_YEAR = "copyrightStartYear";
    static final String SETTING_LOGO_SRC = "src";
    static final String SETTING_LOGO_ALT = "alt";
    static final String SETTING_LOGO_HREF = "href";
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
    static final String SETTING_CATEGORY_TEXT = "text";
    static final String SETTING_CATEGORY_HREF = "href";
    static final String SETTING_CONTENT_DIRECTORY = "contentDirectory";
    static final String SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK
            = "lowestPageTOCElementRank";
    static final String SETTING_LOWEST_FILE_ELEMENT_RANK
            = "lowestFileElementRank";
    static final String SETTING_MAX_TOF_DISPLAY_DEPTH = "maxTOFDisplayDepth";
    static final String SETTING_MAX_MAIN_TOF_DISPLAY_DEPTH
            = "maxMainTOFDisplayDepth";
    static final String SETTING_NUMBERED_SECTIONS = "numberedSections";

    static final String SETTING_VALIDATION_PROGRAMLISTINGS_REQ_ROLE
            = "programlistingsRequireRole";
    static final String SETTING_VALIDATION_PROGRAMLISTINGS_REQ_LANG
            = "programlistingsRequireLanguage";
    static final String SETTING_VALIDATION_OUTPUT_FILES_CAN_USE_AUTOID
            = "outputFilesCanUseAutoID";
    static final String SETTING_VALIDATION_MAXIMUM_PROGRAMLISTING_WIDTH
            = "maximumProgramlistingWidth";
    static final String SETTING_ECLIPSE_LINK_TO = "link_to";

    private static final String VAR_OFFLINE
            = SETTING_OFFLINE;
    private static final String VAR_SIMPLE_NAVIGATION_MODE
    		= SETTING_SIMPLE_NAVIGATION_MODE;
    private static final String VAR_DEPLOY_URL
            = SETTING_DEPLOY_URL;
    private static final String VAR_ONLINE_TRACKER_HTML
            = SETTING_ONLINE_TRACKER_HTML;
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
    private static final String VAR_COPYRIGHT_HOLDER = SETTING_COPYRIGHT_HOLDER;
    private static final String VAR_COPYRIGHT_START_YEAR = SETTING_COPYRIGHT_START_YEAR;
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
    private static final String VAR_INDEX_ENTRIES
            = "indexEntries";
    private static final String VAR_STARTS_WITH_TOP_LEVEL_CONTENT
            = "startsWithTopLevelContent";
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
        Set<String> idAttElems = new HashSet<String>();

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
        Set<String> sinlgeFileElems = new HashSet<String>();

        sinlgeFileElems.add(E_PREFACE);

        PREFACE_LIKE_ELEMENTS = Collections.unmodifiableSet(sinlgeFileElems);
    }

	private static final String XMLNS_DOCGEN = "http://freemarker.org/docgen";
    private static final String E_SEARCHRESULTS = "searchresults";
	private static final String SEARCH_RESULTS_PAGE_TITLE = "Search results";
	private static final String SEARCH_RESULTS_ELEMENT_ID = "searchresults";
    
    // -------------------------------------------------------------------------
    // Settings:

    private File destDir;

    private File srcDir;

    private File contentDir;

    private Boolean offline;

    private String deployUrl;

    private String onlineTrackerHTML;

    /** Element types for which a new output file is created  */
    private DocumentStructureRank lowestFileElemenRank
            = DocumentStructureRank.SECTION1;

    private DocumentStructureRank lowestPageTOCElemenRank
            = DocumentStructureRank.SECTION3;

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

    private LinkedHashMap<String, String> internalBookmarks = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> externalBookmarks = new LinkedHashMap<>();
    private Map<String, Map<String, String>> footerSiteMap;

    private LinkedHashMap<String, String> tabs = new LinkedHashMap<>();

    private Map<String, Map<String, String>> secondaryTabs;
    private Map<String, Map<String, String>> socialLinks;

    private HashMap<String, String> logo;
    
    private String copyrightHolder;
    private Integer copyrightStartYear;

    private DocgenValidationOptions validationOps
            = new DocgenValidationOptions();

    // -------------------------------------------------------------------------
    // Global transformation state:

    private boolean executed;

    private Map<String, String> olinks = new HashMap<String, String>();
    private Map<String, List<NodeModel>> primaryIndexTermLookup;
    private Map<String, SortedMap<String, List<NodeModel>>>
            secondaryIndexTermLookup;
    private Map<String, Element> elementsById;
    private List<TOCNode> tocNodes;
    private List<String> indexEntries;
    private Configuration fmConfig;

    // -------------------------------------------------------------------------
    // Output-file-specific state:

    private TOCNode currentFileTOCNode;

    // -------------------------------------------------------------------------
    // Misc. fields:

    private DocgenLogger logger = new DocgenLogger() {

        public void info(String message) {
            if (printProgress) {
                System.out.println(message);
            }
        }

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
        String eclipseLinkTo = null;

        File cfgFile = new File(srcDir, FILE_SETTINGS);
        if (cfgFile.exists()) {
            Map<String, Object> cfg;
            try {
                cfg = CJSONInterpreter.evalAsMap(cfgFile);
            } catch (CJSONInterpreter.EvaluationException e) {
                throw new DocgenException(e.getMessage(),
                        e.getCause());
            }

            for (Entry<String, Object> cfgEnt : cfg.entrySet()) {
                final String settingName = cfgEnt.getKey();
                final Object settingValue = cfgEnt.getValue();

                if (settingName.equals(SETTING_OLINKS)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String name = ent.getKey();
                        String target = castSettingValueMapValueToString(
                                cfgFile, settingName, ent.getValue());
                        olinks.put(name, target);
                    }
                } else if (settingName.equals(SETTING_INTERNAL_BOOKMARKS)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String name = ent.getKey();
                        String target = castSettingValueMapValueToString(
                                cfgFile, settingName, ent.getValue());
                        internalBookmarks.put(name, target);
                    }
                    // Book-mark targets will be checked later, when the XML
                    // document is already loaded.
                } else if (settingName.equals(SETTING_EXTERNAL_BOOKMARKS)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String name = ent.getKey();
                        String target = castSettingValueMapValueToString(
                                cfgFile, settingName, ent.getValue());
                        externalBookmarks.put(name, target);
                    }
                } else if (settingName.equals(SETTING_LOGO)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    logo = new HashMap<>();
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String k = ent.getKey();
                        String v = castSettingValueMapValueToString(cfgFile, settingName, ent.getValue());
                        if (!(k.equals(SETTING_LOGO_SRC) || k.equals(SETTING_LOGO_ALT)
                                || k.equals(SETTING_LOGO_HREF))) {
                            throw newCfgFileException(cfgFile, SETTING_LOGO, "Unknown logo option: " + k);
                        }
                        logo.put(k, v);
                    }
                    if (!logo.containsKey(SETTING_LOGO_SRC)) {
                        throw newCfgFileException(cfgFile, SETTING_LOGO, "Missing logo option: " + SETTING_LOGO_SRC);
                    }
                    if (!logo.containsKey(SETTING_LOGO_ALT)) {
                        throw newCfgFileException(cfgFile, SETTING_LOGO, "Missing logo option: " + SETTING_LOGO_ALT);
                    }
                    if (!logo.containsKey(SETTING_LOGO_HREF)) {
                        throw newCfgFileException(cfgFile, SETTING_LOGO, "Missing logo option: " + SETTING_LOGO_HREF);
                    }
                } else if (settingName.equals(SETTING_COPYRIGHT_HOLDER)) {
                    copyrightHolder = castSettingToString(cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_COPYRIGHT_START_YEAR)) {
                    copyrightStartYear = castSettingToInt(cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_TABS)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String k = ent.getKey();
                        String v = castSettingValueMapValueToString(cfgFile, settingName, ent.getValue());
                        tabs.put(k, v);
                    }
                } else if (settingName.equals(SETTING_SECONDARY_TABS)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    secondaryTabs = new LinkedHashMap<>();
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String k = ent.getKey();
                        Map<String, String> v = castSettingValueMapValueToMapOfStringString(
                                cfgFile, settingName, ent.getValue(),
                                "class", "href");
                        secondaryTabs.put(k, v);
                    }
                } else if (settingName.equals(SETTING_SOCIAL_LINKS)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, settingName, settingValue);
                    socialLinks = new LinkedHashMap<>();
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String entName = ent.getKey();
                        Map<String, String> entValue = castSettingValueMapValueToMapOfStringString(
                                cfgFile, settingName, ent.getValue(),
                                "class", "href");
                        socialLinks.put(entName, entValue);
                    }
                } else if (settingName.equals(SETTING_FOOTER_SITEMAP)) {
                    // TODO Check value in more details
                    footerSiteMap = (Map) castSettingToMap(
                            cfgFile, settingName, settingValue);
                }else if (settingName.equals(SETTING_VALIDATION)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, SETTING_VALIDATION, settingValue);
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String name = ent.getKey();
                        if (name.equals(
                                SETTING_VALIDATION_PROGRAMLISTINGS_REQ_ROLE)) {
                            validationOps.setProgramlistingRequiresRole(
                                    caseSettingToBoolean(
                                            cfgFile,
                                            settingName + "." + name,
                                            ent.getValue()));
                        } else if (name.equals(
                                SETTING_VALIDATION_PROGRAMLISTINGS_REQ_LANG)) {
                            validationOps.setProgramlistingRequiresLanguage(
                                    caseSettingToBoolean(
                                            cfgFile,
                                            settingName + "." + name,
                                            ent.getValue()));
                        } else if (name.equals(
                                SETTING_VALIDATION_OUTPUT_FILES_CAN_USE_AUTOID)
                                ) {
                            validationOps.setOutputFilesCanUseAutoID(
                                    caseSettingToBoolean(
                                            cfgFile,
                                            settingName + "." + name,
                                            ent.getValue()));
                        } else if (name.equals(
                                SETTING_VALIDATION_MAXIMUM_PROGRAMLISTING_WIDTH)
                                ) {
                            validationOps.setMaximumProgramlistingWidth(
                                    castSettingToInt(
                                            cfgFile,
                                            settingName + "." + name,
                                            ent.getValue()));
                        } else {
                            throw newCfgFileException(
                                    cfgFile, SETTING_VALIDATION,
                                    "Unknown validation option: " + name);
                        }
                    }
                } else if (settingName.equals(SETTING_OFFLINE)) {
                    if (offline == null) {  // Ignore if the caller has already set this
                        offline = caseSettingToBoolean(cfgFile, settingName, settingValue);
                    }
                } else if (settingName.equals(SETTING_SIMPLE_NAVIGATION_MODE)) {
                	simpleNavigationMode = caseSettingToBoolean(cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_DEPLOY_URL)) {
                    deployUrl = castSettingToString(cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_ONLINE_TRACKER_HTML)) {
                    String onlineTrackerHtmlPath = castSettingToString(cfgFile, settingName, settingValue);
                    File f = new File(getSourceDirectory(), onlineTrackerHtmlPath);
                    if (!f.exists()) {
                        throw newCfgFileException(
                                cfgFile, SETTING_ONLINE_TRACKER_HTML,
                                "File not found: " + f.toPath());
                    }
                    onlineTrackerHTML = FileUtil.loadString(f, UTF_8);
                } else if (settingName.equals(SETTING_ECLIPSE)) {
                    Map<String, Object> m = castSettingToMap(
                            cfgFile, SETTING_ECLIPSE, settingValue);
                    for (Entry<String, Object> ent : m.entrySet()) {
                        String name = ent.getKey();
                        if (name.equals(SETTING_ECLIPSE_LINK_TO)) {
                            String value = castSettingToString(
                                    cfgFile,
                                    settingName + "." + name,
                                    ent.getValue());
                            eclipseLinkTo = value;
                        } else {
                            throw newCfgFileException(
                                    cfgFile, settingName,
                                    "Unknown Eclipse option: " + name);
                        }
                    }
                } else if (settingName.equals(SETTING_LOCALE)) {
                    String s = castSettingToString(
                            cfgFile, settingName, settingValue);
                    locale = StringUtil.deduceLocale(s);
                } else if (settingName.equals(SETTING_TIME_ZONE)) {
                    String s = castSettingToString(
                            cfgFile, settingName, settingValue);
                    timeZone = TimeZone.getTimeZone(s);
                } else if (settingName.equals(SETTING_GENERATE_ECLIPSE_TOC)) {
                    generateEclipseTOC = caseSettingToBoolean(
                            cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_SHOW_EDITORAL_NOTES)) {
                    showEditoralNotes = caseSettingToBoolean(
                            cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_SHOW_XXE_LOGO)) {
                    showXXELogo = caseSettingToBoolean(
                            cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_SEARCH_KEY)) {
                    searchKey = castSettingToString(
                            cfgFile, settingName, settingValue);
                }else if (settingName.equals(SETTING_DISABLE_JAVASCRIPT)) {
                    disableJavaScript = caseSettingToBoolean(
                            cfgFile, settingName, settingValue);
                } else if (settingName.equals(SETTING_CONTENT_DIRECTORY)) {
                    String s = castSettingToString(
                            cfgFile, settingName, settingValue);
                    contentDir = new File(srcDir, s);
                    if (!contentDir.isDirectory()) {
                        throw newCfgFileException(cfgFile, settingName,
                                "It's not an existing directory: "
                                + contentDir.getAbsolutePath());
                    }
                } else if (settingName.equals(SETTING_LOWEST_FILE_ELEMENT_RANK)
                        || settingName.equals(
                                SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK)) {
                    DocumentStructureRank rank;
                    String strRank = castSettingToString(
                            cfgFile, settingName, settingValue);
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
                        throw newCfgFileException(cfgFile, settingName,
                                msg);
                    }

                    if (settingName.equals(
                            SETTING_LOWEST_FILE_ELEMENT_RANK)) {
                        lowestFileElemenRank = rank;
                    } else if (settingName.equals(
                            SETTING_LOWEST_PAGE_TOC_ELEMENT_RANK)) {
                        lowestPageTOCElemenRank = rank;
                    } else {
                        throw new BugException("Unexpected setting name.");
                    }
                } else if (settingName.equals(SETTING_MAX_TOF_DISPLAY_DEPTH)) {
                    maxTOFDisplayDepth = castSettingToInt(
                            cfgFile, settingName, settingValue);
                    if (maxTOFDisplayDepth < 1) {
                        throw newCfgFileException(cfgFile, settingName,
                                "Value must be at least 1.");
                    }
                } else if (settingName.equals(
                        SETTING_MAX_MAIN_TOF_DISPLAY_DEPTH)) {
                    maxMainTOFDisplayDepth = castSettingToInt(
                            cfgFile, settingName, settingValue);
                    if (maxTOFDisplayDepth < 1) {
                        throw newCfgFileException(cfgFile, settingName,
                                "Value must be at least 1.");
                    }
                } else if (settingName.equals(SETTING_NUMBERED_SECTIONS)) {
                    numberedSections = caseSettingToBoolean(
                            cfgFile, settingName, settingValue);
                } else {
                    throw newCfgFileException(cfgFile, "Unknown setting: \""
                            + settingName
                            + "\". (Hint: See the list of available "
                            + "settings in the Java API documentation of "
                            + Transform.class.getName() + ". Also, note that "
                            + "setting names are case-sensitive.)");
                }
            } // for each cfg settings

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

        primaryIndexTermLookup = new HashMap<String, List<NodeModel>>();
        secondaryIndexTermLookup
                = new HashMap<String, SortedMap<String, List<NodeModel>>>();
        elementsById = new HashMap<String, Element>();
        tocNodes = new ArrayList<TOCNode>();
        indexEntries = new ArrayList<String>();

        // Setup FreeMarker:

        try {
            Logger.selectLoggerLibrary(Logger.LIBRARY_NONE);
        } catch (ClassNotFoundException e) {
            throw new BugException(e);
        }

        fmConfig = new Configuration(Configuration.VERSION_2_3_23);

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

        // - Post-edit and examine the DOM:
        preprocessDOM(doc);
        
        // Resolve Docgen URL schemes in setting values:
        if (tabs != null) {
            for (Entry<String, String> tabEnt : tabs.entrySet()) {
                tabEnt.setValue(resolveDocgenURL(SETTING_TABS, tabEnt.getValue()));
            }
        }
        if (secondaryTabs != null) {
            for (Map<String, String> tab : secondaryTabs.values()) {
                tab.put("href", resolveDocgenURL(SETTING_SECONDARY_TABS, tab.get("href")));
            }
        }
        if (socialLinks != null) {
            for (Map<String, String> tab : socialLinks.values()) {
                tab.put("href", resolveDocgenURL(SETTING_SOCIAL_LINKS, tab.get("href")));
            }
        }
        if (footerSiteMap != null) {
            for (Map<String, String> links : footerSiteMap.values()) {
                for (Map.Entry<String, String> link : links.entrySet()) {
                    link.setValue(resolveDocgenURL(SETTING_FOOTER_SITEMAP, link.getValue()));
                }
            }
        }

        // - Create destination directory:
        if (!destDir.isDirectory() && !destDir.mkdirs()) {
            throw new IOException("Failed to create destination directory: "
                    + destDir.getAbsolutePath());
        }

        // - Check internal book-marks:
        for (Entry<String, String> ent : internalBookmarks.entrySet()) {
            String id = ent.getValue();
            if (!elementsById.containsKey(id)) {
                throw newCfgFileException(cfgFile,
                        SETTING_INTERNAL_BOOKMARKS,
                        "No element with id \"" + id
                        + "\" exists in the book.");
            }
        }

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
                    VAR_COPYRIGHT_HOLDER, copyrightHolder);
            fmConfig.setSharedVariable(
                    VAR_COPYRIGHT_START_YEAR, copyrightStartYear);
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

            // Calculated data:
            fmConfig.setSharedVariable(
                    VAR_TRANSFORM_START_TIME, new Date());
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
            try (Writer wr = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    new File(destDir, FILE_TOC_JSON_OUTPUT)),
                            UTF_8))) {
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
        copyCommonStatic("docgen.css");
        copyCommonStatic("docgen.min.css");
        copyCommonStatic("img/patterned-bg.png");

        copyCommonStatic("fonts/icomoon.eot");
        copyCommonStatic("fonts/icomoon.svg");
        copyCommonStatic("fonts/icomoon.ttf");
        copyCommonStatic("fonts/icomoon.woff");

        for (int i = 1; i < 15; i++) {
            copyCommonStatic("img/callouts/" + i + ".gif");
        }

        if (showXXELogo) {
            copyCommonStatic("img/xxe.png");
        }
        if (!disableJavaScript) {
          copyCommonStatic("main.js");
          copyCommonStatic("main.min.js");
        }

        // - Copy the custom statics:
        logger.info("Copying custom static files...");
        int bookSpecStaticFileCounter = FileUtil.copyDir(contentDir, destDir, true);

        // - Eclipse ToC:
        if (generateEclipseTOC) {
        	if (simpleNavigationMode) {
        		throw new DocgenException("Eclipse ToC generation is untested/unsupported with simpleNavigationMode=true.");
        	}
        	
            logger.info("Generating Eclipse ToC...");
            Template template = fmConfig.getTemplate(FILE_ECLIPSE_TOC_TEMPLATE);
            try (Writer wr = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    new File(destDir, FILE_ECLIPSE_TOC_OUTPUT)),
                            UTF_8))) {
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

    /**
     * Resolves the URL if it uses the {@code "olink:"} schema, returns it as if otherwise.
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
            return resolvedOLink;
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

    private DocgenException newCfgFileException(
            File cfgFile, String settingName, String desc) {
        settingName = settingName.replace(".", "\" per \"");
        return newCfgFileException(cfgFile, "Wrong value for setting \""
                + settingName + "\": " + desc);
    }

    private DocgenException newCfgFileException(File cfgFile, String desc) {
        return newCfgFileException(cfgFile, desc, (Throwable) null);
    }

    private DocgenException newCfgFileException(File cfgFile, String desc,
            Throwable cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("Wrong configuration");
        if (cfgFile != null) {
            sb.append(" file \"");
            sb.append(cfgFile.getAbsolutePath());
            sb.append("\"");
        }
        sb.append(": ");
        sb.append(desc);
        return new DocgenException(sb.toString(), cause);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castSettingToMap(
            File cfgFile, String settingName, Object settingValue)
            throws DocgenException {
        if (!(settingValue instanceof Map)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be a map (like {key1: value1, key2: value2}), but "
                    + "it's a " + CJSONInterpreter.cjsonTypeOf(settingValue)
                    + ".");
        }
        return (Map<String, Object>) settingValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castSettingToList(
            File cfgFile, String settingName, Object settingValue)
            throws DocgenException {
        if (!(settingValue instanceof List)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be a list (like [value1, value2, ... valueN]), but "
                    + "it's a " + CJSONInterpreter.cjsonTypeOf(settingValue)
                    + ".");
        }
        return (Map<String, Object>) settingValue;
    }
    
    private String castSettingToString(File cfgFile,
            String settingName, Object settingValue) throws DocgenException {
        if (!(settingValue instanceof String)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be a string, but it's a "
                    + CJSONInterpreter.cjsonTypeOf(settingValue) + ".");
        }
        return (String) settingValue;
    }

    private boolean caseSettingToBoolean(File cfgFile,
            String settingName, Object settingValue) throws DocgenException {
        if (!(settingValue instanceof Boolean)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be a boolean (i.e., true or false), but it's a "
                    + CJSONInterpreter.cjsonTypeOf(settingValue) + ".");
        }
        return (Boolean) settingValue;
    }

    private int castSettingToInt(File cfgFile,
            String settingName, Object settingValue)
            throws DocgenException {

        if (!(settingValue instanceof Number)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be an number, but it's a "
                    + CJSONInterpreter.cjsonTypeOf(settingValue) + ".");
        }
        if (!(settingValue instanceof Integer)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be an integer number (32 bits max), but it's: "
                    + settingValue);
        }
        return ((Integer) settingValue).intValue();
    }

    /* Unused at the moment
    @SuppressWarnings("unchecked")
    private List<String> castSettingToListOfStrings(File cfgFile,
            String settingName, Object settingValue) throws DocgenException {
        if (!(settingValue instanceof List)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be a list, but it's a "
                    + CJSONInterpreter.cjsonTypeOf(settingValue) + ".");
        }
        List ls = (List) settingValue;

        for (Object i : ls) {
            if (!(i instanceof String)) {
            throw newCfgFileException(
                    cfgFile, settingName,
                    "Should be a list of strings, but one if the list items "
                    + "is a " + CJSONInterpreter.cjsonTypeOf(i) + ".");
            }
        }

        return ls;
    }
    */

    private String castSettingValueMapValueToString(File cfgFile,
            String settingName, Object mapEntryValue) throws DocgenException {
        if (!(mapEntryValue instanceof String)) {
            throw newCfgFileException(cfgFile, settingName,
                    "The values in the key-value pairs of this map must be "
                    + "strings, but some of them is a "
                    + CJSONInterpreter.cjsonTypeOf(mapEntryValue) + ".");
        }
        return (String) mapEntryValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castSettingValueMapValueToMapOfStringString(File cfgFile,
            String settingName, Object mapEntryValue, String... expectedKeys) throws DocgenException {
        if (!(mapEntryValue instanceof Map)) {
            throw newCfgFileException(cfgFile, settingName,
                    "The values in the key-value pairs of this map must be "
                    + "Map-s, but some of them is a "
                    + CJSONInterpreter.cjsonTypeOf(mapEntryValue) + ".");
        }
        Map<?, ?> mapEntryValueAsMap = (Map<?, ?>) mapEntryValue;
        for (Entry<?, ?> valueEnt : mapEntryValueAsMap.entrySet()) {
            Object key = valueEnt.getKey();
            if (!(key instanceof String)) {
                throw newCfgFileException(cfgFile, settingName,
                        "The values in the key-value pairs of this map must be "
                        + "Map<String, String>-s, but some of the keys is a "
                        + CJSONInterpreter.cjsonTypeOf(mapEntryValue) + ".");
            }
            if (!(valueEnt.getValue() instanceof String)) {
                throw newCfgFileException(cfgFile, settingName,
                        "The values in the key-value pairs of this map must be "
                                + "Map<String, String>-s, but some of the values is a "
                        + CJSONInterpreter.cjsonTypeOf(valueEnt.getValue()) + ".");
            }
            if (!Arrays.asList(expectedKeys).contains(key)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unsupported key: ");
                sb.append(StringUtil.jQuote(key));
                sb.append(". Supported keys are: ");
                for (int i = 0; i < expectedKeys.length; i++) {
                    String expectedKey = expectedKeys[i];
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(StringUtil.jQuote(expectedKey));
                }
                throw newCfgFileException(cfgFile, settingName, sb.toString());
            }
        }
        for (String expectedKey : expectedKeys) {
            if (!mapEntryValueAsMap.containsKey(expectedKey)) {
                throw newCfgFileException(cfgFile, settingName,
                        "Missing map key from nested Map: " + expectedKey);
            }
        }
        return (Map<String, String>) mapEntryValue;
    }
    
    private void copyCommonStatic(String path) throws IOException {
        FileUtil.copyResourceIntoFile(
                Transform.class, "statics", path,
                new File(destDir, "docgen-resources"));
    }

    /**
     * Adds attribute <tt>id</tt> to elements that are in
     * <code>idAttrElements</code>, but has no id attribute yet.
     * Adding id-s is useful to create more precise HTML cross-links later.
     */
    private void preprocessDOM(Document doc)
            throws SAXException, DocgenException {
        NodeModel.simplify(doc);
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
        indexEntries = new ArrayList<String>(primaryIndexTermLookup.keySet());
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
                    if (!new File(contentDir,
                                ref.replace('/', File.separatorChar))
                            .isFile()) {
                        throw new DocgenException(
                                XMLUtil.theSomethingElement(elem) + " contains "
                                + "a broken file reference: \""
                                + ref.replace("\"", "&quot;") + "\"");
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

    /**
     * Annotates the document structure nodes with so called ranks.
     * About ranks see: {@link #setting_lowestFileElementRank}.
     */
    private void preprocessDOM_addRanks(Document doc)
            throws DocgenException {
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

    private void preprocessDOM_addRanks_underBookRank(
            Element root) throws DocgenException {

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

    private void preprocessDOM_addRanks_underTruePart(
            Node parent) throws DocgenException {
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
            Element parent, int underSectionRank) throws DocgenException {
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

    private void preprocessDOM_buildTOC(Document doc) throws DocgenException {
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
                    if (tocNode.isFileElement()
                            && (tocNode.getParent() == null || !hasTopLevelContent(tocNode.getElement()))) {
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

    private void preprocessDOM_buildTOC_checkTOCTopology(TOCNode tocNode)
    throws DocgenException {
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

    private void preprocessDOM_buildTOC_checkFileTopology(TOCNode tocNode)
            throws DocgenException {
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
            final int sectionLevel, TOCNode parentTOCNode)
            throws DocgenException {
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

    private String getExternalLinkTOCNodeURLOrNull(Element elem) throws DocgenException {
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
    private void preprocessDOM_buildTOC_checkEnsureHasIndexHhml(List<TOCNode> tocNodes) throws DocgenException {
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
            primaryIndexTermLookup.put(primaryText, new ArrayList<NodeModel>());
        }

        if (secondary != null) {
            if (!secondaryIndexTermLookup.containsKey(primaryText)) {
                secondaryIndexTermLookup.put(
                        primaryText, new TreeMap<String, List<NodeModel>>());
            }
            Map<String, List<NodeModel>> m = secondaryIndexTermLookup.get(
                    primaryText);
            String secondaryText = secondary.getFirstChild().getNodeValue()
                    .trim();
            List<NodeModel> nodes = m.get(secondaryText);
            if (nodes == null) {
                nodes = new ArrayList<NodeModel>();
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
        dataModel.put(
                VAR_STARTS_WITH_TOP_LEVEL_CONTENT,
                hasTopLevelContent(currentFileTOCNode.getElement()));

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
        Template template = fmConfig.getTemplate("page.ftl");
        File outputFile = new File(destDir, fileName);
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, UTF_8);
        Writer writer = new BufferedWriter(osw, 2048);
        try {
            template.process(
                    dataModel,
                    writer, null,
                    NodeModel.wrap(currentFileTOCNode.getElement()));
        } finally {
            writer.close();
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
     * Checks if a document-structure-element has top-level content.
     * Top-level content is visible content that is outside the nested
     * document-structure-element-s that have enough rank to get into the
     * Page Contents table.
     */
    private boolean hasTopLevelContent(Element element) {
        for (Element elem : XMLUtil.childrenElementsOf(element)) {
            if (elem.getNamespaceURI().equals(XMLNS_DOCBOOK5)) {
                if (elem.hasAttribute(A_DOCGEN_FILE_ELEMENT)
                        || elem.hasAttribute(A_DOCGEN_PAGE_TOC_ELEMENT)) {
                    return false;
                }
                String name = elem.getLocalName();
                if (!name.equals(E_TITLE) && !name.equals(E_SUBTITLE)
                        && !name.equals(E_INFO)
                        && !name.equals(E_FOOTNOTE)) {
                    if (VISIBLE_TOPLEVEL_ELEMENTS.contains(name)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String createElementLinkURL(final Element elem)
            throws DocgenException {
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

    private String createLinkFromId(String id) throws DocgenException {
        Element elem = elementsById.get(id);
        if (elem == null) {
            throw new DocgenException(
                    "No element exists with this id: \"" + id + "\"");
        }

        return createElementLinkURL(elem);
    }
    
    private TemplateMethodModelEx createLinkFromNode
            = new TemplateMethodModelEx() {

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

        public Object exec(@SuppressWarnings("rawtypes") List args)
                throws TemplateModelException {
            Node node = elementsById.get(getArgString(args, 0));
            return NodeModel.wrap(node);
        }

    };

    // -------------------------------------------------------------------------

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

}
