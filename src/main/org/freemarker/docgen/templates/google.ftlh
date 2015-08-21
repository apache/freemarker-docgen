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
