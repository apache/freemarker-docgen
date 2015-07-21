<#import "util.ftl" as u>
<#macro sitemapUrls node>
    <#attempt>
      <#-- @todo: why won't this work... -->
      <#local url = CreateLinkFromNode(node.element)>
    <#recover>
    </#attempt>
    <#if url?hasContent && node.fileElement?c == "true" && !(url?startsWith("http"))>
      <url>
        <loc>${deployUrl}${url}</loc>
      </url>
    </#if>
    <#local child = node.firstChild!>
    <#list 1.. as _>
        <#if !child?hasContent><#break></#if>
        <@sitemapUrls child />
        <#local child = child.next!>
    </#list>
</#macro>
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <@sitemapUrls tocRoot />
</urlset>
