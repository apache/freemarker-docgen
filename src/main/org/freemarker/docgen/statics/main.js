var LEVEL = 0;

(function(toc, breadcrumb) {
  //var breadcrumb = ['Reference', 'Built-in Reference', 'Built-ins for strings'];
  var skipList = ['Preface', 'XML Processing Guide', 'Search'];

  createMenu(toc);

  window.addEventListener('hashchange', hashChange);
  document.addEventListener('DOMContentLoaded', contentLoaded);

  function createMenu(data) {
      var menuPlaceholder = document.getElementById('table-of-contents-wrapper');

      var finishedToc = menuChildren(data.children, 0, true);
      finishedToc.classList.add('table-of-contents');

      menuPlaceholder.appendChild(finishedToc);
  }

  function menuChildren(children, depth, onPath) {

    var ul = document.createElement('ul');
    ul.classList.add('depth-' + LEVEL);

    for (var x = 0; x < children.length; x++) {
      var node = children[x];

      var li = document.createElement('li');
      var isLast = checkIfLast(node);

      if (LEVEL === 0) {
        li.classList.add('section');
      }

      li.addEventListener('click', menuClick);
      li.addEventListener('keydown', keyboardNavigation);

      // skip certain nodes
      if (skipList.indexOf(node.title) >= 0) {
        li.style.display = 'none';
      }

      // add menu link
      li.appendChild(menuLink(node));

      if (node.title === breadcrumb[depth + 1] && onPath) {

        if (depth + 2 === breadcrumb.length) {
          li.classList.add('current');
        }

        // 'section' is always open
        if (LEVEL !== 0) {
          li.classList.add('open');
        }

        depth++;

      } else if (LEVEL > 0) {
        li.classList.add('closed');
      } else {
      }



      if (isLast) {

        li.classList.add('last');

        // @todo: add flags to docgen
        if (typeof node.flags !== 'undefined') {
          li.classList.add(node.flags.join(' '));
        }
      } else if (LEVEL > 0) {
        // don't add for top level elements
        li.classList.add('has-children');
      }

      if (!isLast) {
        LEVEL++;

        li.appendChild(menuChildren(node.children, depth, (node.title === breadcrumb[depth])));

        LEVEL--;
      }

      ul.appendChild(li);
    }

    return ul;
  }

  function checkIfLast(nodeData) {
    if (!nodeData.children.length) {
      return true;
    } else {

      // don't print out children if they are only anchors
      for (var x = 0; x < nodeData.children.length; x++) {
        if (nodeData.children[x].isFile) {
          return false;
        }
      }

      return true;
    }
  }

  function menuLink(nodeData) {
    var a = document.createElement('a');

    a.innerHTML = nodeData.title;
    a.href = nodeData.url;
    a.className = 'depth-' + LEVEL + '-link';

    return a;
  }

  function menuClick(e) {
    e.stopPropagation();

    var node = e.target;

    if (node.tagName.toUpperCase() === 'LI') {
      node.classList.toggle('closed');
      node.classList.toggle('open');
    }
  }

  function keyboardNavigation(e) {
    e.stopPropagation();

    var node = e.target.parentNode;

    // right arrow, wants to open node
    if (e.which === 39) {
      node.classList.remove('closed');
      node.classList.add('open');
    }
    // left arrow, wants to close node
    else if (e.which === 37) {
      node.classList.add('closed');
      node.classList.remove('open');
    }
  }

  function hashChange(e) {
    highlightNode(getHash());
  }

  function getHash() {
    if (window.location.hash) {
      return window.location.hash.substring(1);
    } else {
      return '';
    }
  }

  function contentLoaded(e) {
    highlightNode(getHash());
  }

  function highlightNode(id) {
    if (id === '') {
      return;
    }

    var node = document.getElementById(id);

    if (node) {
      node.classList.add('active');
    }
  }

})(toc, breadcrumb);
