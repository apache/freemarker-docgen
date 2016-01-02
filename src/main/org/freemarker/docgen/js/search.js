/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
