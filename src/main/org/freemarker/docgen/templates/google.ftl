<#macro search>
  <#if searchKey??>
    <script>
      (function() {
        var cx = '${searchKey}';
        var gcse = document.createElement('script');
        gcse.type = 'text/javascript';
        gcse.async = true;
        gcse.src = (document.location.protocol == 'https:' ? 'https:' : 'http:') +
            '//cse.google.com/cse.js?cx=' + cx;
        var s = document.getElementsByTagName('script')[0];
        s.parentNode.insertBefore(gcse, s);
      })();
    </script>
    <gcse:searchbox></gcse:searchbox>
  </#if>
</#macro>


<#macro searchResults>
  <#if searchKey??>
    <div class="search-results">
      <script>
        (function() {
          var cx = '${searchKey}';
          var gcse = document.createElement('script');
          gcse.type = 'text/javascript';
          gcse.async = true;
          gcse.src = (document.location.protocol == 'https:' ? 'https:' : 'http:') +
              '//cse.google.com/cse.js?cx=' + cx;
          var s = document.getElementsByTagName('script')[0];
          s.parentNode.insertBefore(gcse, s);
        })();
      </script>
      <gcse:searchresults-only>Loading…</gcse:searchresults-only>
    </div>
  </#if>
</#macro>
