<#assign searchKey = "014728049242975963158:8awjt03uofm">

<#macro search>
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
</#macro>


<#macro searchResults>
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
</#macro>
