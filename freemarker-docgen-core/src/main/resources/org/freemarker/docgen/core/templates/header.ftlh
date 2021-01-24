<#ftl nsPrefixes={"D":"http://docbook.org/ns/docbook"}>
<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

<#import "navigation.ftlh" as nav>
<#import "google.ftlh" as google>

<#macro header>
  <#if logo?? || tabs?hasContent || secondaryTabs?hasContent>
    <div class="header-top-bg"><#t>
      <div class="site-width header-top"><#t>
        <div id="hamburger-menu" role="button"></div><#t>
        <#if logo??>
          <div class="logo">
            <a href="${logo.href}" role="banner"><#t>
              <img itemprop="image" src="${logo.src}" alt="${logo.alt}"><#t>
            </a><#t>
          </div>
        </#if>
        <@nav.tabs /><#t>
        <#if secondaryTabs?hasContent>
          <ul class="secondary-tabs"><#t>
            <#list secondaryTabs as tabTitle, tab>
              <li><#t>
                <#if tab.href??>
                  <a class="tab<#if tab.class??> ${tab.class}</#if>" href="${tab.href}" title="${tabTitle}"><#t>
                    <span>${tabTitle}</span><#t>
                  </a><#t>
                <#else>
                  <div class="tab<#if tab.class??> ${tab.class}</#if>" title="${tabTitle}"><#t>
                    <span>${tabTitle}</span><#t>
                  </div><#t>
                </#if>
              </li><#t>
            </#list>
          </ul><#t>
        </#if>
      </div><#t>
    </div><#t>
  </#if>
  <#if !simpleNavigationMode>
    <@navigationHeader /><#t>
  </#if>
</#macro>

<#macro navigationHeader>
  <div class="header-bottom-bg"><#t>
    <div class="site-width search-row"><#t>
      <#local book = .node?root.*>
      <a href="${CreateLinkFromNode(book)}" class="navigation-header"><#t>
        <#recurse u.getRequiredTitleElement(book, true) using nodeHandlers><#t>
      </a><#t>
      <#-- empty div to maintain layout -->
      <div class="navigation-header"></div><#t>
      <@searchForm /><#t>
    </div><#t>
    <div class="site-width breadcrumb-row"><#t>
      <@nav.breadcrumb /><#t>
      <@nav.bookmarks /><#t>
    </div><#t>
  </div><#t>
</#macro>

<#macro searchForm>
  <#if searchKey?? && !offline>
    <form method="get" class="search-form<#if offline> offline</#if>" action="search-results.html"><#t>
      <fieldset><#t>
        <legend class="sr-only">Search form</legend><#t>
        <label for="search-field" class="sr-only">Search query</label><#t>
        <input id="search-field" name="q" type="search" class="search-input" placeholder="Search" spellcheck="false" autocorrect="off" autocomplete="off"><#t>
        <button type="submit" class="search-btn"><span class="sr-only">Search</span></button><#t>
      </fieldset><#t>
    </form><#t>
  </#if>
</#macro>
