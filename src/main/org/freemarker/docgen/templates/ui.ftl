<#ftl strip_text = true />

<#macro social links>
  <ul class="social-icons"><#t>
    <#list links as link>
      <li><#t>
        <a class="${link.class}" href="${link.href}">${link.text}</a><#t>
      </li><#t>
    </#list>
  </ul><#t>
</#macro>
