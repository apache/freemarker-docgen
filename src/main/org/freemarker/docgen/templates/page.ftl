<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>
<#-- Avoid inital empty line! -->
<#import "util.ftl" as u>
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
  <link rel="stylesheet" href="docgen-resources/docgen.css" type="text/css">
  <meta name="generator" content="FreeMarker Docgen (DocBook 5)">
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
  <#if !disableJavaScript>
    <script type="text/javascript" src="docgen-resources/jquery.js"></script>
    <script type="text/javascript" src="docgen-resources/linktargetmarker.js"></script>
  </#if>
</head>
</#compress>

<body itemscope itemtype="http://schema.org/Article">
  <div class="site-header">
    <#-- keep site-width inside site-header so that the background extends -->
    <div class="site-width header-top">
      <div class="logo-nav-wrapper">
        <#if logo??>
          <a class="logo" href="${logo.href?html}" role="banner"><#t>
            FreeMarker<#t>
            <#-- @todo: replace with new logo -->
            <#--<img src="${logo.src?html}" alt="${logo.alt?html}">-->
          </a><#t><#t>
        </#if>
        <@nav.tabs />
      </div>
      <#-- @todo: replace with google search -->
      <#-- @todo: move to better file -->
      <#-- @todo: need flag to disable or enable search -->
      <form class="search-form">
        <fieldset>
          <legend class="sr-only">Search form</legend>
          <label for="search-field" class="sr-only">Search query</label>
          <input id="search-field" type="search" class="search-box" placeholder="Search" spellcheck="false" autocorrect="off">
          <button type="submit" class="search-btn"><span class="sr-only">Search</span></button>
        </fieldset>
      </form>
    </div>
    <@nav.navigationBar top=true />
  </div>

  <div class="site-content site-width">
    <#--<@nav.breadcrumb />-->
    <#assign pageType = pageType!.node?node_name>

    <#if pageType == "index" || pageType == "glossary">
      <#visit .node using nodeHandlers>
    <#elseif pageType == "docgen:detailed_toc">
      <@toc att="docgen_detailed_toc_element" maxDepth=99 title="Detailed Table of Contents" />
    <#else>
      <div class="page-content">
        <#-- Normal page content: -->

        <#-- - Render either ToF (Table of Files) or Page ToC; -->
        <#--   both is called, but at least one of them will be empty: -->
        <@toc att="docgen_file_element" maxDepth=maxTOFDisplayDepth />
        <@toc att="docgen_page_toc_element" maxDepth=99 title="Page Contents" minLength=2 />

        <#-- - Render page title: -->
        <div class="col-right">
          <@nav.breadcrumb />
          <#-- @todo: remove this and fix anchors -->
          <a name="docgen_afterTheTOC"></a>

          <#visit titleElement using nodeHandlers>

          <#-- - Render the usual content, like <para>-s etc.: -->
          <#list .node.* as child>
            <#if child.@docgen_file_element?size == 0
                && child?node_name != "title"
                && child?node_name != "subtitle">
              <#visit child using nodeHandlers>
            </#if>
          </#list>
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

  <#--<@nav.navigationBar top=false />-->

  <@footer />
  <#if !disableJavaScript>
    <#-- Put pre-loaded images here: -->
    <div style="display: none">
      <img src="docgen-resources/img/linktargetmarker.gif" alt="Here!" />
    </div>
  </#if>
  <#if !offline && onlineTrackerHTML??>
    ${onlineTrackerHTML}
  </#if>
</body>
</html>

<#macro toc att maxDepth title='' minLength=1>
  <#local tocElems = .node["*[@${att}]"]>
  <#if (tocElems?size >= minLength)>
    <div class="table-of-contents-wrapper"><#t>
      <p><#t>
        <strong><#t>
          <#if !title?has_content>
            <#if .node?parent?node_type == "document">
              Table of Contents
            <#else>
              ${pageType?cap_first} Contents
            </#if>
          <#else>
            ${title}
          </#if>
        </strong><#t>
        <#if alternativeTOCLink??>
          <a class="alt-toc" href="${alternativeTOCLink?html}">${alternativeTOCLabel?cap_first?html}</a><#t>
        </#if>
      </p>
      <@toc_inner tocElems att maxDepth />
    </div>
  </#if>
</#macro>

<#macro toc_inner tocElems att maxDepth curDepth=1>

  <#if tocElems?size == 0><#return></#if>

  <#if curDepth == 1>
    <#local tocClass = "table-of-contents">

    <#if .node?parent?node_type == "document">
      <#local tocClass = tocClass + " main-toc">
    </#if>
  </#if>

  <ol<#if tocClass?has_content> class="${tocClass}"</#if>>
    <#if curDepth == 1 && startsWithTopLevelContent>
      <li class="empty"><a href="#docgen_afterTheTOC">Intro.</a></li><#t>
    </#if>
    <#list tocElems as tocElem>
      <#local prefixClass = titlePrefixClass(tocElem)>
      <li<#if prefixClass?has_content> class="${prefixClass}"</#if>><#t>
        <a href="${CreateLinkFromID(tocElem.@id)?html}"><#t>


          <#recurse u.getRequiredTitleElement(tocElem) using nodeHandlers><#t>
        </a><#lt>
        <#if (curDepth < maxDepth)>
          <@toc_inner tocElem["*[@${att}]"], att, maxDepth, curDepth + 1 />
        </#if>
      </li><#t>
    </#list>
  </ol><#t>
</#macro>


<#---
  return appropriate css class based on title prefix
  @fixme can this be on the backend?
-->
<#function titlePrefixClass tocElem>

  <#local titlePrefix = u.getTitlePrefix(tocElem, false)?trim>

  <#if !titlePrefix?has_content>
    <#return "empty">

  <#elseif "01234567890"?contains(titlePrefix)>
    <#return "decimal">

  <#elseif "IIIVVI"?contains(titlePrefix)>
    <#return "roman">

  <#elseif "ABCDEFGHIJKLMNOPQRSTUVWXYZ"?contains(titlePrefix)>
    <#return "latin">

  </#if>

  <#return "">

</#function>



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
