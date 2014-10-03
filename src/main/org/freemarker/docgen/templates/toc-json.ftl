<#import "util.ftl" as u>
<#macro tocNodeToJSON node>
    {
        "title": "${u.getRequiredTitleAsString(node.element)?json_string}",
        "url": "${CreateLinkFromNode(node.element)?json_string}",
        "isFile": ${node.fileElement?c},
        "children": [
            <#local child = node.firstChild!>
            <#list 1.. as _>
                <#if !child?has_content><#break></#if>
                <#if child.previous??>, </#if>
                <@tocNodeToJSON child />
                <#local child = child.next!>
            </#list>
        ]
    }
</#macro>

<#compress><@tocNodeToJSON tocRoot /></#compress>