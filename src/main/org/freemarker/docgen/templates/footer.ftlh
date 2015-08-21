<#ftl nsPrefixes={"D":"http://docbook.org/ns/docbook"}>

<#import "util.ftl" as u>

<#macro footer>
  <#compress>
    <#local book = .node?root.*>

    <div class="site-footer"><#t>
      <#-- keep site-width inside so background extends -->
      <div class="site-width"><#t>
        <#if footerSiteMap?? || socialLinks?? || showXXELogo>
          <div class="footer-top"><#t>
            <div class="col-left sitemap"><#t>
              <#if footerSiteMap??>
                <@siteMap columns=footerSiteMap /><#t>
              </#if>
            </div><#t>
            <div class="col-right"><#t>
              <#if socialLinks??>
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
            <p><#t>
              <#if book.info.productname?hasContent>
                <span class="generated-for-product">Generated for: ${book.info.productname}</span><#t>
              </#if>
              <span class="last-updated"><@lastGenerated /></span><#t>
            </p><#t>
            <@copyright /><#t>
        </div><#t>
      </div><#t>
    </div><#t>
  </#compress>
</#macro>


<#macro lastGenerated>
  Last generated:
  <time itemprop="dateModified" datetime="${transformStartTime?datetime?isoUtc}" title="${transformStartTime?datetime?string.full}"><#t>
    ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')}<#t>
  </time><#t>
</#macro>


<#macro copyright>
  <p class="copyright">
    © <span itemprop="copyrightYear">${copyrightStartYear?string('0')}</span><#rt>
    <#lt><#if transformStartTime?string('yyyy')?number != copyrightStartYear>–${transformStartTime?string('yyyy')}</#if>
    <a itemtype="http://schema.org/Organization" itemprop="copyrightHolder" href="http://freemarker.org">${copyrightHolder}</a>. All rights reserved.<#t>
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
