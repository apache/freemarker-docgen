<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>

<#function getOptionalTitleAsString node>
  <#return titleToString(getOptionalTitleElement(node))>
</#function>

<#function getOptionalTitleElement node>
  <#local result = node.title>
  <#if !result?has_content><#local result = node.info.title></#if>
  <#if !result?has_content>
     <#return ''>
  </#if>
  <#return result>
</#function>

<#function getRequiredTitleElement node>
  <#local result = getOptionalTitleElement(node)>
  <#if !result?has_content>
    <#stop "Required \"title\" child element missing for element \""
        + node?node_name + "\".">
  </#if>
  <#return result>
</#function>

<#function getRequiredTitleAsString node>
  <#return titleToString(getRequiredTitleElement(node))>
</#function>

<#function getOptionalSubtitleElement node>
  <#local result = node.subtitle>
  <#if !result?has_content><#local result = node.info.subtitle></#if>
  <#if !result?has_content>
    <#return ''>
  </#if>
  <#return result>
</#function>

<#function getOptionalSubtitleAsString node>
  <#return titleToString(getOptionalSubtitleElement(node))>
</#function>

<#function titleToString titleNode>
  <#if !titleNode?has_content>
    <#-- Used for optional title -->
    <#return ''>
  </#if>
  <#if !titleNode?is_node>
    <#-- Just a string... -->
    <#return titleNode>
  </#if>

  <#local res = "">
  <#list titleNode?children as child>
    <#if child?node_type == "text">
      <#local res = res + child>
    <#elseif child?node_type == "element">
      <#local name = child?node_name>
      <#if name == "literal"
          || name == "classname" || name == "methodname" || name == "package"
          || name == "replaceable"
          || name == "emphasis"
          || name == "phrase">
        <#local res = res + titleToString(child)>
      <#elseif name == "quote">
        <#local res = "\x201C" + titleToString(child) + "\x201D">
      <#elseif name != "subtitle">
        <#stop 'The "${name}" in titles is not supported by Docgen.'>
      </#if>
    </#if>
  </#list>

  <#return res>
</#function>

<#-- "docStructElem" is a part, chapter, section, etc., NOT a title element -->
<#function getTitlePrefix docStructElem, extraSpacing=false, longForm=false>
  <#local prefix = docStructElem.@docgen_title_prefix[0]!>
  <#if !prefix?has_content>
    <#return "">
  </#if>

  <#local type = docStructElem?node_name>

  <#if extraSpacing>
    <#local spacer = "\xA0\xA0\xA0">
  <#else>
    <#local spacer = " ">
  </#if>

  <#if type = "chapter">
    <#return longForm?string("Chapter ", "") + prefix + spacer>
  <#elseif type = "appendix">
    <#return longForm?string("Appendix ", "") + prefix + spacer>
  <#elseif type = "part">
    <#return longForm?string("Part ", "") + prefix + spacer>
  <#elseif type = "article">
    <#return longForm?string("Article ", "") + prefix + spacer>
  <#else>
    <#return prefix + spacer>
  </#if>
</#function>

<#macro invisible1x1Img>
  <img src="docgen-resources/img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"/><#t>
</#macro>