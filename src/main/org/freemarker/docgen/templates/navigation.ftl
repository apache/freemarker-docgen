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
        <@breadcrumb />
        <@bookmarks />
        <@pagers />
      <#else>
        <@pagers />
        <@breadcrumb />
      </#if>
    </#if>
  </#local>
  <#if captured?trim?length == 0>
    <#if !top>
      <div class="missingNavigationFooterLine"></div>
    </#if>
  <#else>
    <div class="navigation">
      <#noescape>${captured}</#noescape><#t>
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
    <div class="breadcrumb">
      <span class="breadcrumb"><#t>
        You are here:
        <#list path as step>
          <#if step_has_next><a href="${CreateLinkFromNode(step)}"></#if><#rt>
            <#recurse u.getRequiredTitleElement(step) using nodeHandlers><#t>
          <#if step_has_next></a></#if><#lt>
          <#if step_has_next>
            <b>></b>
          </#if>
        </#list>
      </span><#t>
    </div>
  </#if>
</#macro>

<#macro bookmarks>
  <#if internalBookmarks?size != 0 || externalBookmarks?size != 0>
    <div class="bookmarks">
      <span class="bookmarks"><#t>
        Bookmarks:<#lt>
        <#local curHref = CreateLinkFromNode(.node)>
        <#list internalBookmarks?keys as k>
          <#local target = CreateLinkFromID(internalBookmarks[k])>
          <#if target != curHref>
            <a href="${target}">${k}</a><#t>
          <#else>
            <span class="disabledBookmark">${k}</span><#t>
          </#if>
          <#if k_has_next>, </#if><#t>
        </#list>
        <#if internalBookmarks?size != 0 && externalBookmarks?size != 0>, </#if><#t>
        <#list externalBookmarks?keys as k>
          <a href="${externalBookmarks[k]}">${k}</a><#if k_has_next>, </#if><#t>
        </#list>
      </span><#t>
    </div>
  </#if>
</#macro>

<#macro tabs>
    <#local tabs = .data_model.tabs>
    <#if tabs?size != 0>
        <ul class="tabs">
            <#list tabs?keys as tabTitle>
                <#if tabs[tabTitle]?has_content>
                    <li><a href="${tabs[tabTitle]}">${tabTitle}</a></li>
                <#else>
                    <li class="current">${tabTitle}</li>
                </#if>
            </#list>
        </ul>
    </#if>
</#macro>

<#macro pagers>
  <#-- Render pager panel only if it's not a single-HTML-file output: -->
  <#if nextFileElement?? || previousFileElement?? || parentFileElement??>
    <div class="pagers">
      <div class="pagersVerticalSpacer"><@u.invisible1x1Img /></div>
      <@pagerButton "Next page", nextFileElement!, false />
      <@pagerButton "Previous page", previousFileElement! />
      <@pagerButton "Parent page", parentFileElement! />
      <@pagerButton "Contents", rootElement />
      <div class="pagersVerticalSpacer"><@u.invisible1x1Img /></div>
    </div>
  </#if>
</#macro>

<#macro pagerButton label element labelOnly=true>
  <div class="pagerButton"><#t>
    <#if element?has_content>
      <#local href = CreateLinkFromNode(element)>
      <#local curHref = CreateLinkFromNode(.node)>
    </#if>
    <#if element?has_content && href != curHref>
      <a href="${href}"><#t>
        <#if !labelOnly>
          <span class="hideA">${label}:${' '}</span><#t>
          <#recurse u.getRequiredTitleElement(element) using nodeHandlers><#t>
        <#else>
          ${label}<#t>
        </#if>
      </a><#t>
    <#else>
      <span class="disabledPager">${label}</span><#t>
    </#if>
  </div><#t>
</#macro>

</#escape>