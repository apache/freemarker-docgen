<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>
<#-- Avoid inital empty line! -->
<#import "util.ftl" as u>
<#import "navigation.ftl" as nav>
<#import "node-handlers.ftl" as defaultNodeHandlers>
<#import "customizations.ftl" as customizations>
<#assign nodeHandlers = [customizations, defaultNodeHandlers]>
<#-- Avoid inital empty line! -->
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>
    <#assign titleElement = u.getRequiredTitleElement(.node)>
    <#assign title = u.titleToString(titleElement)>
    <#assign topLevelTitle = u.getRequiredTitleAsString(.node?root.*)>
    ${topLevelTitle?html}<#if title != topLevelTitle> - ${title?html}</#if>
  </title>
  <link rel="stylesheet" href="docgen-resources/docgen.css" type="text/css">
  <meta name="generator" content="FreeMarker Docgen (DocBook 5)">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="format-detection" content="telephone=no">
  <#if !disableJavaScript>
    <script type="text/javascript" src="docgen-resources/jquery.js"></script>
    <script type="text/javascript" src="docgen-resources/linktargetmarker.js"></script>
  </#if>
</head>
<body>

<@nav.navigationBar top=true />

<div id="mainContent">
  <#assign pageType = pageType!.node?node_name>

  <#if pageType == "index" || pageType == "glossary">
    <#visit .node using nodeHandlers>
  <#elseif pageType == "docgen:detailed_toc">
    <@toc att="docgen_detailed_toc_element" maxDepth=99 title="Detailed Table of Contents" />
  <#else>
    <#-- Normal page content: -->

    <#-- - Render page title: -->
    <#visit titleElement using nodeHandlers>

    <#-- - Render either ToF (Table of Files) or Page ToC; -->
    <#--   both is called, but at least one of them will be empty: -->
    <@toc att="docgen_file_element" maxDepth=maxTOFDisplayDepth />
    <@toc att="docgen_page_toc_element" maxDepth=99 title="Page Contents" minLength=2 />

    <#-- - Render the usual content, like <para>-s etc.: -->
    <#list .node.* as child>
      <#if child.@docgen_file_element?size == 0
          && child?node_name != "title"
          && child?node_name != "subtitle">
        <#visit child using nodeHandlers>
      </#if>
    </#list>
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

<@nav.navigationBar top=false />

<table width="100%">
  <#assign pageGenTimeHTML = "HTML generated: ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')?html}">
  <#assign footerTitleHTML = topLevelTitle?html>
  <#assign bookSubtitle = u.getOptionalSubtitleAsString(.node?root.book)>
  <#if bookSubtitle?has_content>
    <#assign footerTitleHTML = footerTitleHTML + " -- " + bookSubtitle?html>
  </#if>
  <#if !showXXELogo>
      <td align="left" valign="top"><span class="footer">
          ${footerTitleHTML}
      </span></td>
      <td align="right" valign="top"><span class="footer">
          ${pageGenTimeHTML}
      </span></td>
    </tr>
  <#else>
      <td align="left" valign="top"><span class="smallFooter">
          <#if footerTitleHTML != "">
            ${footerTitleHTML}
            <br>
          </#if>
          ${pageGenTimeHTML}
      </span></td>
      <td align="right" valign="top"><span class="smallFooter">
          <a href="http://www.xmlmind.com/xmleditor/">
            <img src="docgen-resources/img/xxe.gif" alt="Edited with XMLMind XML Editor">
          </a>
      </span></td>
    </tr>
  </#if>
</table>
<#if !disableJavaScript>
  <#-- Put pre-loaded images here: -->
  <div style="display: none">
    <img src="docgen-resources/img/linktargetmarker.gif" alt="Here!" />
  </div>
</#if>
</body>
</html>

<#macro toc att maxDepth title='' minLength=1>
  <#local tocElems = .node["*[@${att}]"]>
  <#if (tocElems?size >= minLength)>
    <div class="toc">
      <p>
        <strong>
          <#if !title?has_content>
            <#if .node?parent?node_type == "document">
              Table of Contents
            <#else>
              ${pageType?cap_first} Contents
            </#if>
          <#else>
            ${title}
          </#if>
        </strong>
        <#if alternativeTOCLink??>
          &nbsp;&nbsp;<#t>
          <#-- @todo: removing font tag -->
          <font size="-1">[<#t>
          <a href="${alternativeTOCLink?html}"><#t>
            ${alternativeTOCLabel?cap_first?html}...<#t>
          </a><#t>
          ]</font><#t>
        </#if>
      </p>

      <@toc_inner tocElems att maxDepth />
    </div>
    <a name="docgen_afterTheTOC"></a>
  </#if>
</#macro>

<#macro toc_inner tocElems att maxDepth curDepth=1>
  <#if tocElems?size == 0><#return></#if>
  <ul <#if curDepth == 1>class="noMargin"</#if>>
    <#if curDepth==1 && startsWithTopLevelContent>
      <li style="padding-bottom: 0.5em"><em><a href="#docgen_afterTheTOC">Intro.</a></em></li>
    </#if>
    <#list tocElems as tocElem>
      <li>
        ${u.getTitlePrefix(tocElem, true)?html}<#rt>
        <a href="${CreateLinkFromID(tocElem.@id)?html}"><#t>
          <#recurse u.getRequiredTitleElement(tocElem) using nodeHandlers><#t>
        </a><#lt>
        <#if curDepth < maxDepth>
          <@toc_inner tocElem["*[@${att}]"], att, maxDepth, curDepth + 1 />
        </#if>
      </li>
    </#list>
  </ul>
</#macro>
