// fix search population
(function() {

  var SEARCH_VARIABLE = 'q'; // this must be the same as google analytics
  var searchField;

  function populationSearchField(query) {
    // replace '+' signs with spaces
    query = query.replace(/\+/g, ' ');
    query = decodeURIComponent(query); // decode so value stays as expected

    searchField.value = query;
  }

  function checkForSearchQuery() {
    var query = window.location.search;

    if (query !== '') {
      var params = query.split('&'); // note: if the user's search term
                                     // includes & then this won't work as
                                     // expected for them

      // extract the search query
      for (var x = 0; x < params.length; x++) {
        var param = params[x];
        var parts = param.split('=');

        if (parts[0] === '?' + SEARCH_VARIABLE) {
          populationSearchField(parts[1]); // second part is the query
        }
      }
    }
  }

  function init() {
    searchField = document.getElementById('search-field');

    if (searchField !== null) {
      checkForSearchQuery();
    }
  }

  init();
})();
