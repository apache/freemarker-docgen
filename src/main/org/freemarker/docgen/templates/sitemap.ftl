<#import "util.ftl" as u>
<#macro sitemapUrls node>
  <#local url = CreateLinkFromNode(node.element)?xml>
  <#if url?? && node.fileElement?c == "true">
    <url>
      <loc>${deployUrl}${url}</loc>
      <lastmod>${.now?string.iso_m_u}</lastmod>
    </url>
    <#local child = node.firstChild!>
    <#list 1.. as _>
        <#if !child?hasContent><#break></#if>
        <@sitemapUrls child />
        <#local child = child.next!>
    </#list>    
  </#if>
</#macro>
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <@sitemapUrls tocRoot />
</urlset>
