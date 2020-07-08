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
<#macro search>
  <#if searchKey??>
    <script>
      <#noAutoEsc>
      (function() {
        var cx = '${searchKey?jsString}';
        var gcse = document.createElement('script');
        gcse.type = 'text/javascript';
        gcse.async = true;
        gcse.src = (document.location.protocol == 'https:' ? 'https:' : 'http:') +
            '//cse.google.com/cse.js?cx=' + cx;
        var s = document.getElementsByTagName('script')[0];
        s.parentNode.insertBefore(gcse, s);
      })();
      </#noAutoEsc>
    </script>
    <gcse:searchbox></gcse:searchbox>
  </#if>
</#macro>


<#macro searchResults>
  <#if searchKey??>
    <div class="search-results">
      <script>
        <#noAutoEsc>
        (function() {
          var cx = '${searchKey?jsString}';
          var gcse = document.createElement('script');
          gcse.type = 'text/javascript';
          gcse.async = true;
          gcse.src = (document.location.protocol == 'https:' ? 'https:' : 'http:') +
              '//cse.google.com/cse.js?cx=' + cx;
          var s = document.getElementsByTagName('script')[0];
          s.parentNode.insertBefore(gcse, s);
        })();
        </#noAutoEsc>
      </script>
      <gcse:searchresults-only>Loading…</gcse:searchresults-only>
    </div>
  </#if>
</#macro>
