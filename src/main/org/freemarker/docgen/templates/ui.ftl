<#ftl strip_text = true />

<#macro social>
  <#-- @todo: externalize links to manual -->
  <#local socialLinks = [
    {
      "url": "https://github.com/freemarker",
      "class": "github",
      "title": "Github"
    }, {
      "url": "https://twitter.com/freemarker",
      "class": "twitter",
      "title": "Twitter"
    }, {
      "url": "https://stackoverflow.com/questions/tagged/freemarker",
      "class": "stack-overflow",
      "title": "Stack Overflow"
    }
  ]>

  <ul class="social-icons"><#t>
    <#list socialLinks as link>
      <li><#t>
        <a class="${link.class}" href="${link.url}">${link.title}</a><#t>
      </li><#t>
    </#list>
  </ul><#t>
</#macro>
