<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<#import "util.ftl" as u>
<#if offline && copyrightComment?hasContent>
  ${copyrightJavaComment}<#lt>
</#if>
<#macro tocNodeToJSON node>
    {
        "title": "${u.getRequiredTitleAsString(node.element)?jsonString}",
        "url": ${('"' + CreateLinkFromNode(node.element)?jsonString + '"')!'null'},
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
