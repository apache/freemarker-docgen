<#import "ui.ftl" as ui>


<#macro footer topLevelTitle>

  <#local footerTitleHTML = topLevelTitle?html>
  <#local bookSubtitle = u.getOptionalSubtitleAsString(.node?root.book)>
  <#if bookSubtitle?has_content>
    <#local footerTitleHTML = footerTitleHTML + " – " + bookSubtitle?html>
  </#if>

  <div class="site-footer">
    <#-- keep site-width inside so background extends -->
    <div class="site-width">
      <div class="footer-top">
        <div class="col-left sitemap">
          <#if footerSiteMap??>
            <@siteMap links=footerSiteMap />
          </#if>
        </div>
        <div class="col-right">
          <@ui.social />
          <#if showXXELogo>
            <a class="xxe" href="http://www.xmlmind.com/xmleditor/" rel="nofollow" title="Edited with XMLMind XML Editor">
              <span>Edited with XMLMind XML Editor</span>
            </a>
          </#if>
        </div>
      </div>
      <div class="footer-bottom">
          <p>
            ${footerTitleHTML}
            <span class="last-updated"><@lastUpdated /></span>
          </p>
          <@copyright />
      </div>
    </div>
  </div>
</#macro>


<#macro lastUpdated>
  Last updated:
  <time itemprop="dateModified" datetime="${transformStartTime?datetime?iso_utc}" title="${transformStartTime?datetime?string.full}"><#t>
    ${transformStartTime?string('yyyy-MM-dd HH:mm:ss z')?html}<#t>
  </time>
</#macro>


<#macro copyright>
  <#-- @todo: this should be generic and not hardcoded -->
  <p class="copyright">
    © <span itemprop="copyrightYear">1999</span>–${transformStartTime?string('yyyy')}
    <a itemtype="http://schema.org/Person" itemprop="copyrightHolder" href="http://freemarker.org">The FreeMarker Project</a>. All rights reserved.
  </p>
</#macro>


<#macro siteMap links>
  <#list links?keys as column>
    <div class="column">
      <h3 class="column-header">${column}</h3>
      <ul>
        <#list links[column] as link>
          <li><a href="${link.href}">${link.text}</a></li>
        </#list>
      </ul>
    </div>
  </#list>
</#macro>
