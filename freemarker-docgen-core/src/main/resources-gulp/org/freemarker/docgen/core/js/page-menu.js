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
(function() {
  function isAtTop(node) {
    var nodeOffsetTop = node.offsetTop + node.offsetParent.offsetTop - 5;
    var windowOffsetTop = window.pageYOffset;

    // @todo: figure out why less than isn't working when they are equal
    return (nodeOffsetTop === windowOffsetTop || nodeOffsetTop < windowOffsetTop);
  }

  // remove highlight class so animation can be repeated on same node again
  function unHighlightNode(nodeId) {
    var node = document.getElementById(nodeId);

    if (node) {
      node.classList.remove('active');
    }
  }

  function highlightNode(nodeId) {
    var node = document.getElementById(nodeId);

    // wrap in a setTimeout so that window.scrollY is accurate when we poll it
    window.setTimeout(function() {
      if (node && !isAtTop(node)) {
        node.classList.add('active');

        window.setTimeout(function() {
          unHighlightNode(nodeId);
        }, 1000);
      }
    }, 1);
  }

  function onPageMenuRelatedClick(e) {
    var node = e.target;

    var toc = document.getElementById('table-of-contents-wrapper');
    if (node.classList.contains('page-menu-link')) {
      highlightNode(node.getAttribute('data-menu-target'));
    } else if (node.id == 'hamburger-menu') {
      toc.classList.toggle("visible-hamburger-menu");
    } else if (toc.classList.contains("visible-hamburger-menu") && !toc.contains(event.target)) {
      toc.classList.remove("visible-hamburger-menu");
    }
  }

  function getNodeIdFromHash() {
    if (window.location.hash) {
      return window.location.hash.substring(1);
    } else {
      return '';
    }
  }

  function onDocReady() {
    var nodeId = getNodeIdFromHash();

    if (nodeId !== '') {
      highlightNode(nodeId);
    }
  }

  function init() {
    document.addEventListener('click', onPageMenuRelatedClick);
    document.addEventListener('DOMContentLoaded', onDocReady);
  }

  init();

})();
