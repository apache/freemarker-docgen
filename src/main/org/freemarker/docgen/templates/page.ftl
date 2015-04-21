<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>
<#-- Avoid inital empty line! -->
<#import "util.ftl" as u>
<#import "ui.ftl" as ui>
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
  <@ui.siteHeader logo=logo />


  <div class="site-content site-width">
    <#--<@nav.breadcrumb />-->
    <#assign pageType = pageType!.node?node_name>

    <#if pageType == "index" || pageType == "glossary">
      <#visit .node using nodeHandlers>
    <#elseif pageType == "docgen:detailed_toc">
      <@toc att="docgen_detailed_toc_element" maxDepth=99 />
    <#else>

      <div class="page-content">
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

  <@footer />
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

  <ul<#if tocClass?has_content> class="${tocClass}"</#if>>
    <#list tocElems as tocElem>
      <li><#t>
        <a href="${CreateLinkFromID(tocElem.@id)?html}"><#t>
          <#recurse u.getRequiredTitleElement(tocElem) using nodeHandlers><#t>
        </a><#lt>
        <#if (curDepth < maxDepth)>
          <@toc_inner tocElem["*[@${att}]"], att, maxDepth, curDepth + 1 />
        </#if>
      </li><#t>
    </#list>
  </ul><#t>
</#macro>


<#macro footer>

  <#local footerTitleHTML = topLevelTitle?html>
  <#local bookSubtitle = u.getOptionalSubtitleAsString(.node?root.book)>
  <#if bookSubtitle?has_content>
    <#local footerTitleHTML = footerTitleHTML + " – " + bookSubtitle?html>
  </#if>

  <#-- @todo: externalize links to manual -->
  <#local socialLinks = [
    {
      "url": "https://github.com/freemarker",
      "class": "github",
      "title": "Github"
    }, {
      "url": "https://twitter.com/freemarker",
      "class": "twitter",
      "title": "Twitter"
    }, {
      "url": "https://stackoverflow.com/questions/tagged/freemarker",
      "class": "stack-overflow",
      "title": "Stack Overflow"
    }
  ]>

  <div class="site-footer">
    <#-- keep site-width inside so background extends -->
    <div class="site-width">
      <div class="footer-inner">
        <p class="footer-title">
          ${footerTitleHTML}<br>
          Last updated:
          <time itemprop="dateModified" datetime="${transformStartTime?datetime?iso_utc}" title="${transformStartTime?datetime?string.full}"><#t>
            ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')?html}<#t>
          </time>
        </p>

        <ul class="social-icons"><#t>
          <#list socialLinks as link>
            <li><#t>
              <a class="${link.class}" href="${link.url}">${link.title}</a><#t>
            </li><#t>
          </#list>
        </ul><#t>
      </div>

      <#-- @todo: this should be generic and not hardcoded -->
      <div class="copyright">
        <p>© <span itemprop="copyrightYear">1999</span>–${transformStartTime?string('yyyy')} <a itemtype="http://schema.org/Person" itemprop="copyrightHolder" href="http://freemarker.org">The FreeMarker Project</a>. All rights reserved.</p>
        <#-- @todo: make license generic -->
        <ul class="legal"><#t>
          <li><#t>
            <a href="app_license.html" itemprop="license">License</a><#t>
          </li><#t>
          <li><#t>
            <a href="http://sourceforge.net/p/freemarker/bugs/">Report a bug</a><#t>
          </li><#t>
        </ul><#t>
      </div>
    </div>
  </div>
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
