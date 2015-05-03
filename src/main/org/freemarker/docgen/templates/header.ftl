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
    <div class="site-width search-row"><#t>
      <a href="index.html" class="category">Manual</a><#t>
      <@searchForm /><#t>
    </div><#t>
    <div class="site-width breadcrumb-row"><#t>
      <@nav.breadcrumb /><#t>
      <@nav.bookmarks /><#t>
    </div>
  </div>
</#macro>


<#macro searchForm>
  <form method="get" class="search-form<#if offline> offline</#if>" action="search.html"><#t>
    <fieldset><#t>
      <legend class="sr-only">Search form</legend><#t>
      <label for="search-field" class="sr-only">Search query</label><#t>
      <input id="search-field" name="q" type="search" class="search-input" placeholder="Search" spellcheck="false" autocorrect="off" autocomplete="off"><#t>
      <button type="submit" class="search-btn"><span class="sr-only">Search</span></button><#t>
    </fieldset><#t>
  </form><#t>
</#macro>


<#macro notices>
  <p class="notices">
    <a href="javascript:;">Love FreeMarker? <strong>Help it grow!</strong></a>
  </p>
</#macro>
