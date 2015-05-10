(function() {

  // remove highlight class so animation can be repeated on same node again
  function unHighlightNode(nodeId) {
    var node = document.getElementById(nodeId);

    if (node) {
      node.classList.remove('active');
    }
  }

  function highlightNode(nodeId) {
    var node = document.getElementById(nodeId);

    if (node) {
      node.classList.add('active');

      window.setTimeout(function() {
        unHighlightNode(nodeId);
      }, 1000);
    }
  }

  function onPageMenuClick(e) {
    var node = e.target;

    if (node.classList.contains('page-menu-link')) {
      highlightNode(node.getAttribute('data-menu-target'));
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
    document.addEventListener('click', onPageMenuClick);
    document.addEventListener('DOMContentLoaded', onDocReady);
  }

  init();

})();
