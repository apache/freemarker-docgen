<#ftl nsPrefixes={"D":"http://docbook.org/ns/docbook"}>
<#import "util.ftl" as u>
<#escape x as x?html>
<?xml version="1.0" encoding="utf-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>

<#assign
    part = hierarElemHandler,
    chapter = hierarElemHandler,
    appendix = hierarElemHandler,
    specialPart = hierarElemHandler,
    section =  hierarElemHandler,
    preface = hierarElemHandler,
    glossary = hierarElemHandler,
    index = hierarElemHandler
>

<#assign docElem = .node.*>
<toc label="${u.getRequiredTitleAsString(docElem)}"<#if link_to??> link_to="${link_to}"</#if>>
  <#recurse docElem>
</toc>

<#macro hierarElemHandler>
  <#if .node.@docgen_file_element?size == 0><#return></#if>
  
  <topic<#lt>
      label="${u.getTitlePrefix(.node)}${u.getRequiredTitleAsString(.node)}"
      href="${.node.@id}.html"
  ><#rt>
    <#recurse>
  </topic><#t>
</#macro>

<#macro "@element">
  <#recurse>
</#macro>

<#macro "@text"></#macro>

</#escape>
