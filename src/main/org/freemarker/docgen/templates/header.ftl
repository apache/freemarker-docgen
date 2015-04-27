<#ftl strip_text = true />

<#import "navigation.ftl" as nav>
<#import "google.ftl" as google>


<#macro header logo>
  <div class="header-top-bg"><#t>
    <div class="site-width header-top"><#t>
      <#if logo??>
        <a class="logo" href="${logo.href?html}" role="banner"><#t>
          &lt;FreeMarker&gt;<#t>
          <#-- @todo: replace with new logo -->
          <#--<img src="${logo.src?html}" alt="${logo.alt?html}">-->
        </a><#t>
      </#if>
      <@nav.tabs /><#t>
      <@notices /><#t>
    </div><#t>
  </div><#t>
  <@categoryHeader /><#t>
</#macro>


<#macro categoryHeader>
  <div class="header-bottom-bg">
    <div class="site-width header-bottom">
      <div class="header-left">
        <div class="category">Manual</div>
        <@nav.breadcrumb /><#t>
      </div>
      <div class="header-right">
        <@nav.bookmarks /><#t>
        <@searchForm /><#t>
      </div>
    </div>
  </div>
</#macro>


<#macro searchForm>
  <#-- @todo: replace with google search -->
  <#-- @todo: need flag to disable or enable search -->

  <form class="search-form<#if offline> offline</#if>"><#t>
    <fieldset><#t>
      <legend class="sr-only">Search form</legend><#t>
      <label for="search-field" class="sr-only">Search query</label><#t>
      <input id="search-field" type="search" class="search-input" placeholder="Search" spellcheck="false" autocorrect="off"><#t>
      <button type="submit" class="search-btn"><span class="sr-only">Search</span></button><#t>
    </fieldset><#t>
  </form><#t>
  <#if !offline>
    <@google.search />
  </#if>
</#macro>


<#macro notices>
  <p class="notices">
    <a href="javascript:;">Love FreeMarker? <strong>Help it grow!</strong></a>
  </p>
</#macro>
