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

<#import "util.ftl" as u>

<#macro footer>
  <#compress>
    <div class="site-footer"><#t>
      <#-- keep site-width inside so background extends -->
      <div class="site-width"><#t>
        <#if footerSiteMap?hasContent || socialLinks?hasContent || showXXELogo>
          <div class="footer-top"><#t>
            <div class="col-left sitemap"><#t>
              <#if footerSiteMap?hasContent>
                <@siteMap columns=footerSiteMap /><#t>
              </#if>
            </div><#t>
            <div class="col-right"><#t>
              <#if socialLinks?hasContent>
                <@social links=socialLinks />
              </#if>
              <#if showXXELogo>
                <a class="xxe" href="http://www.xmlmind.com/xmleditor/" rel="nofollow" title="Edited with XMLMind XML Editor"><#t>
                  <span>Edited with XMLMind XML Editor</span><#t>
                </a><#t>
              </#if>
            </div><#t>
          </div><#t>
        </#if>
        <div class="footer-bottom"><#t>
          <@lastGenerated />
          <@copyright />
        </div><#t>
      </div><#t>
    </div><#t>
  </#compress>
</#macro>


<#macro lastGenerated>
  <p class="last-generated">
    Last generated:
    <time itemprop="dateModified" datetime="${transformStartTime?datetime?isoUtc}" title="${transformStartTime?datetime?string.full}"><#t>
      ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')}<#t>
    </time><#t>
    <#local book = .node?root.*>
    <#if book.info.productname?hasContent>
      , for <@printTextWithDocgenSubstitutions text=book.info.productname /><#t>
    </#if>
  </p>
</#macro>


<#macro copyright>
  <p class="copyright">
    © <span itemprop="copyrightYear">${copyrightStartYear?string('0')}</span><#rt>
    <#lt><#if transformStartTime?string('yyyy')?number != copyrightStartYear>–${transformStartTime?string('yyyy')}</#if>
    <a itemtype="http://schema.org/Organization" itemprop="copyrightHolder" href="${copyrightHolderSite}">${copyrightHolder}</a><#t>
    <#if copyrightSuffix??>. ${copyrightSuffix}</#if><#t>
  </p>
</#macro>


<#macro social links>
  <ul class="social-icons"><#t>
    <#list links?keys as linkTitle>
      <#local link = links[linkTitle]>
      <li><#t>
        <a class="${link.class}" href="${link.href}">${linkTitle}</a><#t>
      </li><#t>
    </#list>
  </ul><#t>
</#macro>


<#macro siteMap columns>
  <#list columns?keys as columnTitle>
    <div class="column"><#t>
      <h3 class="column-header">${columnTitle}</h3><#t>
      <ul><#t>
        <#local links = columns[columnTitle]>
        <#list links?keys as linkTitle>
          <#if linkTitle == "License">
            <li><a itemprop="license" href="${links[linkTitle]}">${linkTitle}</a></li><#t>
          <#else>
          <li><a href="${links[linkTitle]}">${linkTitle}</a></li><#t>
          </#if>
        </#list>
      </ul><#t>
    </div><#t>
  </#list>
</#macro>
