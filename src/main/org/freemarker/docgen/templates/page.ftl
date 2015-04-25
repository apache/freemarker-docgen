<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>
<#-- Avoid inital empty line! -->
<#import "util.ftl" as u>
<#import "ui.ftl" as ui>
<#import "footer.ftl" as footer>
<#import "header.ftl" as header>
<#import "navigation.ftl" as nav>
<#import "node-handlers.ftl" as defaultNodeHandlers>
<#import "customizations.ftl" as customizations>
<#assign nodeHandlers = [customizations, defaultNodeHandlers]>
<#-- Avoid inital empty line! -->
<!doctype html>
<html lang="en">
<#compress>
<head prefix="og: http://ogp.me/ns#">
  <meta charset="utf-8">
  <#assign titleElement = u.getRequiredTitleElement(.node)>
  <#assign title = u.titleToString(titleElement)>
  <#assign topLevelTitle = u.getRequiredTitleAsString(.node?root.*)>
  <#assign pageTitle = topLevelTitle />
  <#if title != topLevelTitle>
    <#assign pageTitle = topLevelTitle + " - " + title>
  </#if>
  <title>${pageTitle?html}</title>
  <link rel="stylesheet" type="text/css" href="http://fonts.googleapis.com/css?family=Roboto:500,700,400" >
  <link rel="stylesheet" type="text/css" href="docgen-resources/docgen.css">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="format-detection" content="telephone=no">
  <meta property="og:title" content="${pageTitle?html}">
  <meta property="og:locale" content="en_US">
  <#-- @todo: improve this logic -->
  <#assign nodeId = .node.@id>
  <#if nodeId == "autoid_1">
    <#assign nodeId = "index">
  </#if>
  <#assign canonicalUrl = "http://freemarker.org/docs/${nodeId}.html"><#-- @todo: remove hard-coded domain -->
  <meta property="og:url" content="${canonicalUrl}">
  <link rel="canoical" href="${canonicalUrl}">
  <#--
  <#if !disableJavaScript>
    <script src="docgen-resources/jquery.js"></script>
    <script src="docgen-resources/linktargetmarker.js"></script>
    <script src="docgen-resources/toc.js"></script>
  </#if>
  -->
</head>
</#compress>

<body itemscope itemtype="http://schema.org/Article">
  <@header.header logo=logo />

  <div class="site-content">
    <div class="site-width">
      <#--<@nav.breadcrumb />-->
      <#assign pageType = pageType!.node?node_name>

      <div class="page-content<#if pageType == "index" || pageType == "glossary" || pageType == "docgen:detailed_toc"> no-toc</#if>">
        <#if pageType == "index" || pageType == "glossary">
          <div class="page-title">
            <#visit titleElement using nodeHandlers>
            <@nav.pagers class="top" />
          </div>
          <#visit .node using nodeHandlers>
        <#elseif pageType == "docgen:detailed_toc">
          <@toc att="docgen_detailed_toc_element" maxDepth=99 />
        <#else>
          <div class="col-left">
            <#-- - Render either ToF (Table of Files) or Page ToC; -->
            <#--   both are called, but at least one of them will be empty: -->
            <div id="table-of-contents"<#if .node?parent?node_type == "document"> class="expanded"</#if>>
              <@toc att="docgen_file_element" maxDepth=maxTOFDisplayDepth />
              <@toc att="docgen_page_toc_element" maxDepth=99 minLength=2 />
            </div>
          </div>

          <div class="col-right">
            <#-- - Render page title: -->
            <div class="page-title">
              <#visit titleElement using nodeHandlers>
              <@nav.pagers class="top" />
            </div>

            <#-- @todo: remove this and fix anchors
            <a name="docgen_afterTheTOC"></a> -->
            <#-- - Render the usual content, like <para>-s etc.: -->
            <#list .node.* as child>
              <#if child.@docgen_file_element?size == 0
                  && child?node_name != "title"
                  && child?node_name != "subtitle">
                <#visit child using nodeHandlers>
              </#if>
            </#list>

            <@nav.pagers class="bottom" />
          </div>
        </#if>

        <#-- Render footnotes, if any: -->
        <#assign footnotes = defaultNodeHandlers.footnotes>
        <#if footnotes?size != 0>
          <div id="footnotes">
            Footnotes:
            <ol>
              <#list footnotes as footnote>
                <li><a name="autoid_footnote_${footnote_index + 1}"></a>${footnote}</li>
              </#list>
            </ol>
          </div>
        </#if>
      </div>
    </div>
  </div>

  <@footer.footer topLevelTitle=topLevelTitle />
  <#if !disableJavaScript>
    <#-- Put pre-loaded images here:
    <div style="display: none">
      <img src="docgen-resources/img/linktargetmarker.gif" alt="Here!" />
    </div> -->
  </#if>
  <#if !offline && onlineTrackerHTML??>
    <#--${onlineTrackerHTML}-->
  </#if>

  <script>
    <@nav.breadcrumbJs />
  </script>
  <#--<script src="docgen-resources/main.js"></script>-->
</body>
</html>

<#macro toc att maxDepth minLength=1>
  <#local tocElems = .node["*[@${att}]"]>
  <#if (tocElems?size >= minLength)>
      <@toc_inner tocElems att maxDepth />
  </#if>
</#macro>

<#macro toc_inner tocElems att maxDepth curDepth=1>

  <#if tocElems?size == 0><#return></#if>

  <#if curDepth == 1>
    <#local tocClass = "table-of-contents">
  </#if>

  <ul class="depth-${curDepth}<#if tocClass?has_content> ${tocClass}</#if>">
    <#list tocElems as tocElem>
      <li<#if curDepth == 1> class="section"</#if>><#t>
        <a<#if curDepth == 1> class="header"</#if> href="${CreateLinkFromID(tocElem.@id)?html}"><#t>
          <#recurse u.getRequiredTitleElement(tocElem) using nodeHandlers><#t>
        </a><#lt>
        <#if (curDepth < maxDepth)>
          <@toc_inner tocElem["*[@${att}]"], att, maxDepth, curDepth + 1 />
        </#if>
      </li><#t>
    </#list>
  </ul><#t>
</#macro>


<#---
  Old Site footer for backup
-->
<#macro Oldfooter>
  <div class="site-footer">

    <#-- keep site-width inside so background extends -->
    <div class="site-width">

      <div class="footer-inner">
        <#local pageGenTimeHTML>
          HTML generated:
          <time itemprop="dateModified" datetime="${transformStartTime?datetime?iso_utc}" title="${transformStartTime?datetime?string.full}"><#t>
            ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')?html}<#t>
          </time><#t>
        </#local>

        <#local footerTitleHTML = topLevelTitle?html>
        <#local bookSubtitle = u.getOptionalSubtitleAsString(.node?root.book)>
        <#if bookSubtitle?has_content>
          <#local footerTitleHTML = footerTitleHTML + " – " + bookSubtitle?html>
        </#if>
        <#if !showXXELogo>
          <div class="footer-left">
              ${footerTitleHTML}
          </div>
          <div class="footer-right">
              ${pageGenTimeHTML}
          </div>
        <#else>
          <div class="footer-left">
            <#if footerTitleHTML != "">
              ${footerTitleHTML}
              <br>
            </#if>
            ${pageGenTimeHTML}
          </div>
          <div class="footer-right">
            <a href="http://www.xmlmind.com/xmleditor/" rel="nofollow">
              Edited with XMLMind XML Editor
              <#--><img src="docgen-resources/img/xxe.gif" alt="Edited with XMLMind XML Editor">-->
            </a>
          </div>
        </#if>
      </div>
    </div>
  </div>
</#macro>
