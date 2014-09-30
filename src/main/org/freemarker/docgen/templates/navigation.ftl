<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>

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
      ${captured}<#t>
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
          <#if step_has_next><a href="${CreateLinkFromNode(step)?html}"></#if><#rt>
            <#if step_index != 0>
              <#recurse u.getRequiredTitleElement(step) using nodeHandlers><#t>
            <#else>
              ${step?node_name?cap_first}<#t>
            </#if>
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
            <a href="${target?html}">${k?html}</a><#t>
          <#else>
            <span class="disabledBookmark">${k?html}</span><#t>
          </#if>
          <#if k_has_next>, </#if><#t>
        </#list>
        <#if internalBookmarks?size != 0 && externalBookmarks?size != 0>, </#if><#t>
        <#list externalBookmarks?keys as k>
          <a href="${externalBookmarks[k]?html}">${k?html}</a><#if k_has_next>, </#if><#t>
        </#list>
      </span><#t>
    </div>
  </#if>
</#macro>

<#macro pagers>
  <#-- Render pager panel only if it's not a single-HTML-file output: -->
  <#if nextFileElement?? || previousFileElement?? || parentFileElement??>
    <div class="pagers">
      <div class="pagersVerticalSpacer"><@u.invisible1x1Img /></div>
      <@pagerButton "Next page", nextFileElement!null, false />
      <@pagerButton "Previous page", previousFileElement!null />
      <@pagerButton "Parent page", parentFileElement!null />
      <@pagerButton "Contents", rootElement />
      <div class="pagersVerticalSpacer"><@u.invisible1x1Img /></div>
    </div>
  </#if>
</#macro>

<#macro pagerButton label element labelOnly=true>
  <div class="pagerButton"><#t>
    <#if element??>
      <#local href = CreateLinkFromNode(element)>
      <#local curHref = CreateLinkFromNode(.node)>
    </#if>
    <#if element?? && href != curHref>
      <a href="${href?html}"><#t>
        <#if !labelOnly>
          <span class="hideA">${label?html}:${' '}</span><#t>
          <#recurse u.getRequiredTitleElement(element) using nodeHandlers><#t>
        <#else>
          ${label?html}<#t>
        </#if>
      </a><#t>
    <#else>
      <span class="disabledPager">${label?html}</span><#t>
    </#if>
  </div><#t>
</#macro>
