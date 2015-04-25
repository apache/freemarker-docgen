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
          <@siteMap  />
        </div>
        <div class="col-right">
          <@ui.social />
        </div>
      </div>
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
  <p class="copyright">© <span itemprop="copyrightYear">1999</span>–${transformStartTime?string('yyyy')} <a itemtype="http://schema.org/Person" itemprop="copyrightHolder" href="http://freemarker.org">The FreeMarker Project</a>. All rights reserved.</p>
</#macro>

<#macro siteMap>
  <#-- @todo: make this dynamic instead of hardcoded -->
  <#local links = {
      "Overview": [{
        "text": "What is FreeMarker?",
        "href": "http://freemarker.org/features.html"
      },{
        "text": "Download",
        "href": "http://freemarker.org/freemarkerdownload.html"
      },{
        "text": "Version history",
        "href": "app_versions.html"
      },{
        "text": "About us",
        "href": "http://freemarker.org/whoWeAre.html"
      },{
        "text": "License",
        "href": "app_license.html"
      }],
      "Community": [{
        "text": "FreeMarker on Github",
        "href": "https://github.com/freemarker"
      },{
        "text": "Follow us on Twitter",
        "href": "https://twitter.com/freemarker"
      },{
        <#-- @todo: enable issues on Github, and update link -->
        "text": "Report a bug",
        "href": "https://sourceforge.net/p/freemarker/bugs/new/"
      },{
        "text": "Ask a question",
        "href": "http://stackoverflow.com/questions/tagged/freemarker"
      },{
        "text": "Mailing lists",
        "href": "http://freemarker.org/mailing-lists.html"
      }],
      "Shortcuts": [{
        "text": "Expressions cheatsheet",
        "href": "dgui_template_exp.html#exp_cheatsheet"
      }, {
        "text": ".special_vars",
        "href": "ref_specvar.html"
      }, {
        "text": "#directives",
        "href": "ref_directive_alphaidx.html"
      }, {
        "text": "?built_ins",
        "href": "ref_builtins_alphaidx.html"
      }, {
        "text": "FAQ",
        "href": "app_faq.html"
      }]
  } />

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
