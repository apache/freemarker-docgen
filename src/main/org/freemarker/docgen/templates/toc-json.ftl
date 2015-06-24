<#import "util.ftl" as u>
<#macro tocNodeToJSON node>
    {
        "title": "${u.getRequiredTitleAsString(node.element)?jsonString}",
        "url": "${CreateLinkFromNode(node.element)?jsonString}",
        "isFile": ${node.fileElement?c},
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
