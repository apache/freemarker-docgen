<#ftl nsPrefixes={"D":"http://docbook.org/ns/docbook"} stripText = true>

<#import "ui.ftl" as ui>
<#import "util.ftl" as u>

<#macro footer>
  <#compress>
    <#local book = .node?root.*>
    <#local footerTitleHTML = u.getRequiredTitleAsString(book)>
    <#local bookSubtitle = u.getOptionalSubtitleAsString(book)>
    <#if bookSubtitle?hasContent>
      <#local footerTitleHTML += " – " + bookSubtitle?html>
    </#if>

    <div class="site-footer"><#t>
      <#-- keep site-width inside so background extends -->
      <div class="site-width"><#t>
        <#if footerSiteMap?? || socialLinks?? || showXXELogo>
          <div class="footer-top"><#t>
            <div class="col-left sitemap"><#t>
              <#if footerSiteMap??>
                <@siteMap links=footerSiteMap /><#t>
              </#if>
            </div><#t>
            <div class="col-right"><#t>
              <#if socialLinks??>
                <@ui.social links=socialLinks />
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
    ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')?html}<#t>
  </time><#t>
</#macro>


<#macro copyright>
  <#-- @todo: this should be generic and not hardcoded -->
  <p class="copyright"><#t>
    © <span itemprop="copyrightYear">1999</span>–${transformStartTime?string('yyyy')}<#lt>
    <a itemtype="http://schema.org/Person" itemprop="copyrightHolder" href="http://freemarker.org">The Freemarker Project</a>. All rights reserved.<#t>
  </p><#t>
</#macro>


<#macro siteMap links>
  <#list links?keys as column>
    <div class="column"><#t>
      <h3 class="column-header">${column}</h3><#t>
      <ul><#t>
        <#list links[column] as link>
          <li><a href="${link.href}">${link.text}</a></li><#t>
        </#list>
      </ul><#t>
    </div><#t>
  </#list>
</#macro>
