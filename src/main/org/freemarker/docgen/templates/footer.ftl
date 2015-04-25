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
      <#--
      <div class="footer-inner">
        <p class="footer-title">
          ${footerTitleHTML}<br>
          <@lastUpdated />
        </p>

        <@ui.social />
      </div>
      -->

      <div class="footer-bottom">
        <div class="col-left">
          <p>${footerTitleHTML}</p>
          <p class="last-updated"><@lastUpdated /></p>
        </div>
        <div class="col-right">
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
  <div class="copyright">
    <p>© <span itemprop="copyrightYear">1999</span>–${transformStartTime?string('yyyy')} <a itemtype="http://schema.org/Person" itemprop="copyrightHolder" href="http://freemarker.org">The FreeMarker Project</a>. All rights reserved.</p>
    <#-- @todo: make license generic
    <ul class="legal"><#t>
      <li><#t>
        <a href="app_license.html" itemprop="license">License</a><#t>
      </li><#t>
      <li><#t>
        <a href="http://sourceforge.net/p/freemarker/bugs/">Report a bug</a><#t>
      </li><#t>
    </ul><#t> -->
  </div>
</#macro>

<#macro siteMap>
  <#local links = {
      "Overview": [
        "What is FreeMarker?",
        "Download",
        "Version history",
        "About us",
        "License"
      ],
      "Community": [
        "Github project",
        "Follow us on Twitter",
        "Report a bug",
        "Ask a question",
        "Mailing lists"
      ],
      "Shortcuts": [
        "Expressions cheatsheet",
        ".special_vars",
        "#directives",
        "?built_ins",
        "FAQ"
      ]
  } />

  <#list links?keys as key>
    <div class="column">
      <h3 class="column-header">${key}</h3>
      <ul>
        <#list links[key] as link>
          <li><a href="javascript:;">${link}</a></li>
        </#list>
      </ul>
    </div>
  </#list>
</#macro>
