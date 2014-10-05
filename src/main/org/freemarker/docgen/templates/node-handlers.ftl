<#ftl ns_prefixes={"D":"http://docbook.org/ns/docbook"}>

<#import "util.ftl" as u>

<#-- Constants: -->
<#assign forProgrammersStyle = "color:#333399; font-style:italic">

<#-- State variables: -->
<#assign inHtmlP = false, compactPara = false, disableAnchors = false, inlineMonospacedColorisation=false>
<#assign footnotes = []>

<#macro @text>${.node?html}</#macro>

<#macro @element>
  <#stop "This DocBook element is not supported by the Docgen transformer, "
      + "or wasn't expected where it occured: "
      + .node?node_name>
</#macro>

<#macro Anchor node=.node>
  <#if !disableAnchors && node.@id[0]??>
    <a name="${node.@id}"></a><#t>
  </#if>
</#macro>

<#macro anchor>
  <@Anchor/>
</#macro>

<#macro answer>
<dd class="answer">
  <#recurse>
</dd>
</#macro>

<#macro emphasis>
    <#local role=.node.@role[0]!"none">
    <#if role = "term" || role = "bold" || .node?ancestors("programlisting")?has_content>
      <strong><#recurse></strong><#t>
    <#else>
      <em><#recurse></em><#t>
    </#if>
</#macro>

<#macro glossentry><#recurse></#macro>

<#macro glossdef>
   <dd><#recurse>
   <#local seealsos=.node.glossseealso>
   <#if seealsos?has_content>
    <p>See Also
     <#list seealsos as also>
       <#local otherTermID=also.@otherterm>
       <#local otherNode=NodeFromID(otherTermID)>
       <#local term=otherNode.glossterm>
       <a href="${CreateLinkFromID(also.@otherterm)}">${term}</a><#if also_has_next>,</#if>
     </#list>
    </p>
   </#if>
   </dd>
</#macro>

<#macro glosssee>
    <dd>See
       <#local otherTermID=.node.@otherterm>
       <#local otherNode=NodeFromID(otherTermID)>
       <#local term=otherNode.glossterm>
       <a href="${CreateLinkFromID(otherTermID)}">${term}</a>
    </dd>
</#macro>

<#macro glossseealso>
  <#-- This is dealt with in the glossdef routine -->
</#macro>

<#macro glossterm>
  <dt><@Anchor .node?parent/><#recurse></dt>
</#macro>

<#macro graphic>
  <#local role=.node.@role[0]!?string>
  <#if role?starts_with("alt:")>
    <#local alt = role[4.. .node.@role?length-1]?trim>
  <#else>
    <#local alt = "figure">
  </#if>
  <img src="${.node.@fileref}" alt="${alt?html}"><#t>
</#macro>

<#assign imagedata=graphic>

<#macro indexterm>
  <@Anchor/>
</#macro>

<#macro itemizedlist>
    <#local packed=.node.@spacing[0]! = "compact">
    <#local prevCompactPara=compactPara>
    <#if packed>
       <#assign compactPara = true>
    </#if>
    <@CantBeNestedIntoP>
    <div class="itemizedlist">
        <@Anchor/>
        <#local mark=.node.@mark[0]!>
        <#if mark = "bullet">
            <ul type="disc"><#t>
        <#elseif mark = "box">
            <ul type="square"><#t>
        <#elseif mark = "ring">
            <ul type="circle"><#t>
        <#elseif mark = "">
            <ul><#t>
        <#else>
            <ul type="${mark?html}"><#t>
        </#if>
        <#recurse>
        </ul><#t>
    </div>
    </@CantBeNestedIntoP>
    <#assign compactPara = prevCompactPara>
</#macro>

<#macro link>
   <#if .node.@linkend?has_content>
      <a href="${CreateLinkFromID(.node.@linkend)?html}"><#recurse></a><#t>
   <#else>
      <a href="${.node["@xlink:href"]?html}"><#recurse></a><#t>
   </#if>
</#macro>

<#macro listitem>
   <#local mark=.node?parent.@mark[0]!>
   <#if mark != "">
       <li style="list-style-type: ${mark?html}"><@Anchor/><#t>
   <#else>
       <li><@Anchor/><#t>
   </#if>
   <#recurse>
   </li><#t>
</#macro>

<#macro _inlineMonospaced>
    <#local moreStyle="" color="#A03D10">
    <#if .node?ancestors("link")?has_content>
        <#-- If we are within a link, we don't change color, just use the regular link color -->
        <code><#recurse></code><#t>
    <#else>
        <#if fontBgColor! != "">
            <#local moreStyle = "; background-color:${fontBgColor}">
        </#if>
        <#-- @todo: convert 'moreStyle' to class names -->
        <code class="inline-code"<#if moreStyle?has_content> style="${moreStyle}"</#if>><#t>
        <#local saved_inlineMonospacedColorisation = inlineMonospacedColorisation>
        <#assign inlineMonospacedColorisation = true>
        <#recurse><#t>
        <#assign inlineMonospacedColorisation = saved_inlineMonospacedColorisation>
        </code><#t>
    </#if>
</#macro>

<#assign classname = _inlineMonospaced>
<#assign code = _inlineMonospaced>
<#assign command=_inlineMonospaced>
<#assign constant = _inlineMonospaced>
<#assign envar = _inlineMonospaced>
<#assign errorcode = _inlineMonospaced>
<#assign errorname = _inlineMonospaced>
<#assign errortext = _inlineMonospaced>
<#assign errortype = _inlineMonospaced>
<#assign exceptionname = _inlineMonospaced>
<#assign filename = _inlineMonospaced>
<#assign function = _inlineMonospaced>
<#assign interfacename = _inlineMonospaced>
<#assign literal = _inlineMonospaced>
<#assign markup = _inlineMonospaced>
<#assign methodname = _inlineMonospaced>
<#assign package = _inlineMonospaced>
<#assign parameter = _inlineMonospaced>
<#assign prompt = _inlineMonospaced>
<#assign property = _inlineMonospaced>
<#assign returnvalue = _inlineMonospaced>
<#assign sgmltag = _inlineMonospaced>
<#assign structfield = _inlineMonospaced>
<#assign structname = _inlineMonospaced>
<#assign symbol = _inlineMonospaced>
<#assign token = _inlineMonospaced>
<#assign type = _inlineMonospaced>
<#assign uri = _inlineMonospaced>
<#assign varname = _inlineMonospaced>

<#macro note>
  <div class="callout note">
    <div class="callout-inner">
       <strong class="callout-label">Note</strong>
       <#recurse>
     </div>
  </div>
</#macro>

<#macro warning>
  <div class="callout warning">
    <div class="callout-inner">
      <strong class="callout-label">Warning!</strong>
      <#recurse>
    </div>
  </div>
</#macro>

<#macro olink>
    <#if !(olinks[.node.@targetdoc]??)>
      <#stop "The olink element refers to an unknown targetdoc: \""
             + .node.@targetdoc?xml
             + "\". Ensure this target is defined in the docgen.cfg file.">
    </#if>
    <a href="${olinks[.node.@targetdoc]}"><#recurse></a><#t>
</#macro>

<#macro orderedlist>
    <#local packed=(.node.@spacing[0]! = "compact")>
    <#local prevCompactPara=compactPara>
    <#if packed>
       <#assign compactPara = true>
    </#if>
    <@CantBeNestedIntoP>
    <div class="orderedlist"><@Anchor/><ol type="1"><#recurse></ol></div><#t>
    </@CantBeNestedIntoP>
    <#assign compactPara = prevCompactPara>
</#macro>

<#macro para>
  <#if .node.@role[0]! = "forProgrammers">
    <#local style = forProgrammersStyle>
  <#else>
    <#local style = ''>
  </#if>
  <#if compactPara!>
    <#if style != ''>
      <span style="${style}"><#t>
    </#if>
    <@Anchor/><#t>
    <#recurse>
    <#if style != ''>
      </span><#t>
    </#if>
  <#else>
    <#assign inHtmlP = true>
    <p<#if style != ''> style="${style}"</#if>><#t>
    <#local content><@Anchor/><#recurse></#local><#t>
    <#-- Avoid empty p element when closing para directly after orderedlist or itemizedlist. -->
    <#if !content?matches(r".*<p>\s*$", "s")>
        ${content}</p><#t>
    <#else>
        ${content?substring(0, content?last_index_of("<p>"))}<#t>
    </#if>
    <#assign inHtmlP = false>
  </#if>
</#macro>

<#macro CantBeNestedIntoP>
<#if inHtmlP>
  </p><#t>
  <#assign inHtmlP = false>
  <#nested>
  <p><#t>
  <#assign inHtmlP = true>
<#else>
  <#nested>
</#if>
</#macro>

<#macro phrase>
  <#local lastFontBgColor=fontBgColor!>
  <#local moreStyle="">
  <#local role=.node.@role[0]!>
  <#local bgcolors={"markedComment" : "#6af666", "markedTemplate" : "#D8D8D8", "markedDataModel" : "#99CCFF", "markedOutput" : "#CCFFCC", "markedText" : "#8acbfa", "markedInterpolation" : "#ffb85d", "markedFTLTag" : "#dbfe5e"}>
  <#if role != "">
    <#if role = "homepage">
      http://freemarker.org<#t>
    <#elseif role = "markedInvisibleText">
      <#if fontBgColor! != "">
        <#local moreStyle = ";background-color:${fontBgColor}">
      </#if>
      <i><span style="color: #999999 ${moreStyle}"><#recurse></span></i><#t>
    <#elseif role = "forProgrammers">
      <#if fontBgColor! != "">
        <#local moreStyle = ";background-color:${fontBgColor}">
      </#if>
      <span style="${forProgrammersStyle}${moreStyle}"><#recurse></span><#t>
    <#else>
      <#local lastFontBgColor = fontBgColor!>
      <#if !bgcolors[role]??>
        <#stop "Invalid role attribute value, \"" + role + "\"">
      </#if>
      <#assign fontBgColor = bgcolors[role]>
      <span style="background-color:${bgcolors[role]}"><#recurse></span><#t>
      <#assign fontBgColor = lastFontBgColor>
    </#if>
  </#if>
</#macro>

<#macro programlisting>
  <@Anchor/>
  <#local role=.node.@role[0]!?string>
  <#local dotidx=role?index_of(".")>
  <#if dotidx != -1>
    <#local role = role[0..dotidx-1]>
  </#if>

  <#switch role>
    <#case "output">
      <#local codeType = "code-output">
      <#break>
    <#case "dataModel">
      <#local codeType = "code-data-model">
      <#break>
    <#case "template">
      <#local codeType = "code-template">
      <#break>
    <#case "unspecified">
      <#local codeType = "code-unspecified">
      <#break>
    <#case "metaTemplate">
      <pre class="metaTemplate"><#t>
        <#recurse>
      </pre><#lt>
      <#return>
    <#default>
      <#local codeType = "code-default">
  </#switch>
  <#--
    We will use a table instead of a div, because div-s has to problems:
    - If their content is wider than the main content div, the bgcolor and
      border will finish but the text will flow out of it.
    - The above can be avoided if the div has "position: absolute" (and then
      the proper horizontal space is ensured with other tricks), but then if a
      program line is so wide that it doesn't fit the screen width, there will
      be no be horizontal scrollbar, so it becomes unreadable.
  -->
  <@CantBeNestedIntoP>
    <pre class="code-block ${codeType}"><#t><#-- XXE and usual FO-stylesheet-compatible interpretation of inital line-breaks -->
      <#local content><#recurse></#local><#t>
      ${content?chop_linebreak}</pre><#t>
  </@CantBeNestedIntoP>
</#macro>

<#macro qandaset>
  <div class="qandaset">
  <#local prevCompactPara=compactPara!>
  <#assign compactPara = true>
  <#assign qaIndex = 1>
  <#-- @todo: remove table, fix spacing -->

  <ol>
  <#list .node.qandaentry as qandaentry>
    <li>
      <#local prevdisableAnchors=disableAnchors!>
      <#assign disableAnchors = true>
      <a href="#${qandaentry.@id[0]!("faq_question_" + qaIndex)}">
        <#recurse qandaentry.question>
      </a>
      <#assign disableAnchors = prevdisableAnchors>
    <#assign qaIndex = qaIndex+1>
    </li>
  </#list>
  </ol>
  <#assign compactPara = prevCompactPara>

  <#assign qaIndex = 1>
  <dl>
  <#recurse>
  </dl>

  </div>
  </#macro>

  <#macro question>
  <#local prevCompactPara=compactPara!>
  <#assign compactPara = true>

  <#local nodeParent = .node?parent>

  <#if nodeParent.@id[0]??>
    <#local questionId = nodeParent.@id />
  </#if>
  <dt class="question" id="${questionId!''}">
    ${qaIndex}.&nbsp; <#recurse>
  </dt>
  <#assign qaIndex = qaIndex+1>
  <#assign compactPara = prevCompactPara>
</#macro>

<#macro qandaentry><#recurse></#macro>

<#macro remark>
  <#if showEditoralNotes>
    <p style="background-color:#FFFF00">[<#recurse>]</p><#t>
  </#if>
</#macro>

<#macro replaceable>
  <#local moreStyle="">
  <#if inlineMonospacedColorisation>
    <#if fontBgColor! != "">
      <#local moreStyle = "; background-color:${fontBgColor}">
    </#if>
    <#-- @todo: check if class name is necessary. probably able to cascade under .inline-code -->
    <em class="code-color"<#if moreStyle?has_content> style="${moreStyle}"</#if>><#recurse></em><#t>
  <#else>
    <em><#recurse></em><#t>
  </#if>
</#macro>

<#macro subtitle>
  <#-- We do nothing here because this is dealt with in the title macro -->
</#macro>

<#macro sectionLikeElement>
  <#recurse>
</#macro>

<#assign article = sectionLikeElement>
<#assign part = sectionLikeElement>
<#assign chapter = sectionLikeElement>
<#assign appendix = sectionLikeElement>
<#assign preface = sectionLikeElement>
<#assign section = sectionLikeElement>
<#assign simplesect = sectionLikeElement>

<#macro index>
  <#visit u.getRequiredTitleElement(.node)>

  <#-- ABC links -->
  <#local lastLetter = "">
  <p>
    <#list indexEntries as key>
      <#local letter = key[0]?upper_case>
      <#if lastLetter != letter>
        <#if lastLetter != "">&nbsp;| </#if><a href="#${index_safeID(letter)?html}">${letter?html}</a><#t>
        <#local lastLetter = letter>
      </#if>
    </#list>
  </p>

  <#-- Index list -->
  <#local lastLetter = "">
  <#list indexEntries as key>
    <#local letter = key[0]?upper_case>
    <#if letter != lastLetter>
      <#if lastLetter != "">
        </dl></div><#lt>
      </#if>
      <div class="indexdiv"><#lt>
      <a name="${index_safeID(letter)?html}"></a><#lt>
      <h2 class="indexLabel">${letter?html}</h2><#lt>
      <dl><#lt>
      <#local lastLetter = letter>
    </#if>
    <#local entryNodes = primaryIndexTermLookup[key]>
    <dt>
      ${key?html}<#if entryNodes?has_content>,&nbsp;&nbsp;</#if><#rt>
      <#list entryNodes as entryNode>
        <a href="${CreateLinkFromNode(entryNode)}"><#t><@index_entryText entryNode/></a><#t>
        <#if entryNode_has_next>,</#if><#lt>
      </#list>
    </dt>
    <#if secondaryIndexTermLookup[key]?has_content>
      <#local secondaryTerms = secondaryIndexTermLookup[key]>
      <dd><dl>
      <#list secondaryTerms?keys?sort as secondary>
        <dt><#rt>
        ${secondary?html}, <#t>
        <#list secondaryTerms[secondary] as secondaryNode>
          <a href="${CreateLinkFromNode(secondaryNode)}"><#t>
            <@index_entryText secondaryNode/><#t>
          </a><#if secondaryNode_has_next>, </#if><#t>
        </#list>
        </dt><#lt>
      </#list>
      </dl></dd>
    </#if>
    <#if !key_has_next>
      </dl></div><#lt>
    </#if>
  </#list>
</#macro>

<#macro index_entryText node>
  <#list 1..100 as i>
    <#if node?node_type != "element">
      entry<#t>
      <#return>
    </#if>
    <#if node.title?has_content>
      <#local title=node.title>
      <#if !node.@id[0]!?starts_with("autoid_")>
        ${title?trim?html}<#t>
        <#return>
      </#if>
    </#if>
    <#local node = node?parent>
  </#list>
  No title<#t>
</#macro>

<#function index_safeID id>
  <#return "idx_" + id?url('UTF-8')?replace('%', "x")?replace('+', "_")>
</#function>

<#macro glossary>
  <#visit u.getRequiredTitleElement(.node)>

  <#local ges = .node.glossentry?sort_by("glossterm")>

  <#-- Print alphabetical index links: -->
  <#local lgtl = "">
  <p>
    <#list ges as ge>
      <#local fullgt = ge.glossterm>
      <#if fullgt?size != 0>
        <#local gtl = fullgt.@@text[0]?upper_case>
        <#if gtl != lgtl>
          <#if lgtl != "">&nbsp;| </#if><a href="#${ge.@id?html}">${gtl?html}</a><#t>
          <#local lgtl = gtl>
        </#if>
      </#if>
    </#list>
  </p>

  <#-- Print glossentry-es: -->
  <dl>
    <#list ges as ge>
      <#visit ge using nodeHandlers>
    </#list>
  </dl>
</#macro>

<#assign partintro = simplesect>

<#macro title>
  <#local hierarElem = .node?parent>
  <#if hierarElem?node_name == "info">
    <#local hierarElem = hierarElem?parent>
  </#if>

  <#local type = hierarElem?node_name>
  <#local titleInitial = u.getTitlePrefix(hierarElem, true, true)>

  <#-- Calculate htmlHLevel: ToC-deeph compared to the enclosing file-element -->
  <#local htmlHLevel = 1>
  <#local cur = hierarElem>
  <#list 1..100000 as _>
    <#if cur.@docgen_file_element?size != 0>
      <#break>
    </#if>
    <#if cur.@docgen_rank?size != 0>
      <#local htmlHLevel = htmlHLevel + 1>
    </#if>
    <#local cur = cur?parent>
  </#list>

  <#-- HTML only defines h-s up to h6 -->
  <#if htmlHLevel <= 6>
    <#local htmlHElem = "h${htmlHLevel}">
  <#else>
    <#local htmlHElem = "p">
  </#if>

  <#local classAtt = "">

  <${htmlHElem} class="rank_${hierarElem.@docgen_rank}"
        <#if htmlHLevel == 1>id="pageTopTitle"</#if>>
      <@Anchor hierarElem/><#t>
      ${titleInitial?html}<#recurse><#t>
      <#-- <font size="-4" color="#D0D0D0">[TR=${hierarElem.@docgen_rank}]</font> --><#t>
    <#local subtitleElem = u.getOptionalSubtitleElement(hierarElem)>
    <#if subtitleElem?has_content>
      <span style="font-size: 50%"><br><#recurse subtitleElem></span>
    </#if>
  </${htmlHElem}>
</#macro>

<#macro subtitle>
  <#-- Handled by "title" macro -->
</#macro>

<#macro ulink>
  <a href="${.node.@url?html}"><#recurse></a><#t>
</#macro>

<#macro xref>
  <#local xrefID=.node.@linkend>
  <#local targetNode = NodeFromID(xrefID)>
  <#local targetLink = CreateLinkFromID(xrefID)>

  <#local label = targetNode.@xreflabel[0]!>
  <#if label?has_content>
    <a href="${targetLink?html}">${label?html}</a><#t>
  <#else>
    <#local labelHTMLs = buildTitleHTMLChain(targetNode)>
    <#if labelHTMLs?size == 0>
      <#stop "\"xref\" target element with xml:id \"" + targetNode.@id
          + "\" has no \"title\" element in it nor \"xreflabel\" attribute.">
    </#if>
    <#local ctxLabelHTMLs = buildTitleHTMLChain(.node, true)>
    <a href="${targetLink?html}"><#t>
      <#local started = false>
      <#list labelHTMLs as labelHTML>
        <#if started || !(
              labelHTML_has_next
              && ctxLabelHTMLs[labelHTML_index]??
              && labelHTML == ctxLabelHTMLs[labelHTML_index]
            )
        >
          ${labelHTML}<#if labelHTML_has_next>/</#if><#t>
          <#local started = true>
        </#if>
      </#list>
    </a><#t>
  </#if>
</#macro>

<#function buildTitleHTMLChain targetNode allowFallback=false>
  <#local result = []>
  <#list 1..1000000 as _>
     <#if targetNode.@docgen_root_element?size != 0><#break></#if>

     <#local title = u.getOptionalTitleElement(targetNode)>
     <#if title?has_content>
       <#local titleHTML><#recurse title></#local>
       <#local result = [titleHTML] + result>
       <#local allowFallback = true>
     <#elseif !allowFallback>
       <#break>
     </#if>

     <#local targetNode = targetNode?parent>
  </#list>
  <#return result>
</#function>

<#macro quote>"<#recurse>"</#macro>

<#macro footnote>
  ${' '}[<a href="#autoid_footnote_${footnotes?size + 1}">${footnotes?size + 1}</a>]${' '}<#t>
  <#local capturedContent><#recurse></#local><#t>
  <#assign footnotes = footnotes + [capturedContent]>
</#macro>

<#macro informaltable>
   <div class="informal-table">
      <@Anchor/>
      <#-- @todo: remove table, fix spacing -->
      <table>
          <#recurse>
      </table>
   </div>
</#macro>

<#-- Re-prints the original tag as is, but restricts the allowed attributes -->
<#macro _HTMLTableElement supportedAtts empty=false>
  <#if !supportedAtts??><#stop 'XXX ' + .node?node_name></#if>
  <${.node?node_name}<#t>
    <#list .node.@@ as att>
      <#if supportedAtts[att?node_name]??>
        ${' '}${att?node_name}="${att?html}"<#t>
      <#else>
        <#stop 'Unimplemented attribute for "${.node?node_name}": ' + att?node_name>
      </#if>
    </#list>
  ><#t>
  <#if !empty>
    <#recurse><#t>
    </${.node?node_name}><#t>
  </#if>
  ${"\n"}<#t>
</#macro>

<#assign htmlAlignAtts = {"align":true, "valign":true}>

<#macro tr><@_HTMLTableElement htmlAlignAtts /></#macro>

<#macro td><@_HTMLTableElement htmlAlignAtts + {"colspan":true, "rowspan":true} /></#macro>
<#assign th = td>

<#macro thead><@_HTMLTableElement htmlAlignAtts /></#macro>
<#assign tbody = thead>
<#assign tfoot = thead>

<#macro colgroup>
  <#-- This element should be resolved and deleted from the DOM before we get here -->
  <#stop 'This element is only supported directly inside tables.'>
</#macro>

<#macro col>
  <#-- This element should be resolved and deleted from the DOM before we get here -->
  <#stop 'This element is only supported directly inside a "colgroup".'>
</#macro>

<#macro mediaobject>
  <#list .node.* as imageobject>
    <#if imageobject?node_name == "imageobject">
      <#list imageobject.* as imagedata>
        <#if imagedata?node_name == "imagedata">
          <p align="center"><@Anchor /><img src="${imagedata.@fileref?html}" alt="figure"></p>
        <#else>
          <#stop "Unexpected element when \"imagedata\" was expected: "
              + imagedata?node_name>
        </#if>
      </#list>
    <#else>
      <#stop "Unexpected element when \"imageobject\" was expected: "
          + imageobject?node_name>
    </#if>
  </#list>
</#macro>
