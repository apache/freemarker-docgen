<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>
<#escape x as x?html>

<#import "util.ftl" as u>

<#import "node-handlers.ftl" as defaultNodeHandlers>
<#import "customizations.ftl" as customizations>
<#assign nodeHandlers = [customizations, defaultNodeHandlers]>

<#macro navigationBar top>
  <#local captured>
    <#if showNavigationBar>
      <#if top>
        <@pagers />
        <@bookmarks />
      <#else>
        <@pagers />
      </#if>
    </#if>
  </#local>
  <#if captured?trim?length == 0>
    <#if !top>
      <div class="missingNavigationFooterLine"></div>
    </#if>
  <#else>
    <div class="navigation">
      <#-- keep site-width inside navigation so that the background extends -->
      <div class="site-width">
        <#noescape>${captured}</#noescape><#t>
      </div>
    </div>
  </#if>
</#macro>

<#macro breadcrumb>
  <#if !showBreadCrumb>
    <#return>
  </#if>
  <#local path = []>
  <#local curNode = .node>
  <#list 1..99 as _>
    <#local path = [curNode] + path>
    <#if curNode.@docgen_root_element?size != 0>
      <#break>
    </#if>
    <#local curNode = curNode?parent>
  </#list>
  <#if (path?size > 1)>
    <ul class="breadcrumb"><#t>
      <#list path as step>
        <#if step_has_next>
          <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><#t>
            <a itemprop="url" href="${CreateLinkFromNode(step)}"><#t>
              <span itemprop="title"><#recurse u.getRequiredTitleElement(step) using nodeHandlers></span><#t>
            </a><#t>
          </li><#t>
        </#if>
      </#list>
    </ul><#t>
  </#if>
</#macro>

<#macro bookmarks>
  <#if internalBookmarks?size != 0 || externalBookmarks?size != 0>
    <div class="bookmarks"><#t>
        Bookmarks:<#t>
        <ul class="bookmark-list"><#t>
          <#local curHref = CreateLinkFromNode(.node)>
          <#list internalBookmarks?keys as k>
            <li><#t>
              <#local target = CreateLinkFromID(internalBookmarks[k])>
              <#if target != curHref>
                <a href="${target}">${k}</a><#t>
              <#else>
                ${k}<#t>
              </#if>
            </li><#t>
          </#list>
          <#list externalBookmarks?keys as k>
            <li><#t>
              <a href="${externalBookmarks[k]}">${k}</a><#t>
            </li><#t>
          </#list>
        </ul><#t>
    </div>
  </#if>
</#macro>

<#macro tabs>
    <#local tabs = .data_model.tabs>
    <#if tabs?size != 0>
        <ul class="tabs"><#t>
            <#list tabs?keys as tabTitle>
                <#if tabs[tabTitle]?has_content>
                    <#-- @todo: this should be dynamci -->
                    <li><a<#if tabTitle == "Java API"> class="external"</#if> href="${tabs[tabTitle]}">${tabTitle}</a></li><#t>
                <#else>
                    <li class="current">${tabTitle}</li><#t>
                </#if>
            </#list>
        </ul><#t>
    </#if>
</#macro>

<#macro pagers full=true>
  <#-- Render pager panel only if it's not a single-HTML-file output: -->
  <#if nextFileElement?? || previousFileElement?? || parentFileElement??>
    <ul class="pagers"><#t>
      <#if full>
        <@pagerButton "Previous", previousFileElement! />

        <@pagerButton "Parent page", parentFileElement! />
        <@pagerButton "Contents", rootElement />
      <#else>
        <#--><@pagerButton "Previous", previousFileElement!, false />-->
      </#if>
      <@pagerButton "Next", nextFileElement!, false />
    </ul><#t>
  </#if>
</#macro>

<#macro pagerButton label element labelOnly=true>
  <li><#t>
    <#if element?has_content>
      <#local href = CreateLinkFromNode(element)>
      <#local curHref = CreateLinkFromNode(.node)>
    </#if>
    <#if element?has_content && href != curHref>
      <#if !labelOnly>
        <span class="pager-label">${label}:</span><#t>
        <a href="${href}"><#t>
          <#recurse u.getRequiredTitleElement(element) using nodeHandlers><#t>
        </a><#t>
      <#else>
        <a href="${href}"><#t>
          ${label}<#t>
        </a><#t>
      </#if>
      <#--
      <a href="${href}"><#t>
        <#if !labelOnly>
          <span class="hideA">${label}:${' '}</span><#t>
          <#recurse u.getRequiredTitleElement(element) using nodeHandlers><#t>
        <#else>
          ${label}<#t>
        </#if>
      </a><#t>-->
    <#else>
      ${label}<#t>
    </#if>
  </li><#t>
</#macro>

</#escape>