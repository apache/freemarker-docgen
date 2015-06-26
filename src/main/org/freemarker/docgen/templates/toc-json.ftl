<#import "util.ftl" as u>
<#macro tocNodeToJSON node>
    {
        "title": "${u.getRequiredTitleAsString(node.element)?jsonString}",
        <#if !(node.showsToCOnly && simpleNavigationMode)>
        "url": "${CreateLinkFromNode(node.element)?jsonString}",
        </#if>
        "isFile": ${node.fileElement?c},
        "showsToCOnly": ${node.showsToCOnly?c},
        "children": [
            <#local child = node.firstChild!>
            <#list 1.. as _>
                <#if !child?hasContent><#break></#if>
                <#if child.previous??>, </#if>
                <@tocNodeToJSON child />
                <#local child = child.next!>
            </#list>
        ]
    }
</#macro>

<#compress>var toc = <@tocNodeToJSON tocRoot />;</#compress>
