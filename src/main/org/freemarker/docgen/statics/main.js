var LEVEL = 1;

(function(toc, breadcrumb) {
  //var breadcrumb = ['Reference', 'Built-in Reference', 'Built-ins for strings'];
  var skipList = ['Preface', 'XML Processing Guide'];

  createMenu(toc);

  window.addEventListener('hashchange', hashChange);
  document.addEventListener('DOMContentLoaded', contentLoaded);

  function createMenu(data) {
      var menuPlaceholder = document.getElementById('table-of-contents-wrapper');

      var finishedToc = menuChildren(data.children, 1, false);
      finishedToc.classList.add('table-of-contents');

      menuPlaceholder.appendChild(finishedToc);
  }

  function menuChildren(children, depth, isLast) {

    var ul = document.createElement('ul');
    ul.classList.add('depth-' + LEVEL);

    for (var x = 0; x < children.length; x++) {
      var node = children[x];

      var li = document.createElement('li');

      if (LEVEL === 1) {
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

      // determine node class
      if (node.title === breadcrumb[depth]) {
        li.classList.add('current');

        if (depth === 0) {
          li.classList.add('active-section');
        }

        /*
        if (depth === (breadcrumb.length - 1)) {
          li.classList.add('active-menu');
        }*/

        depth++;

      } else if (!isLast) {
        //li.classList.add('closed');
      }

      if (isLast || !node.children.length) {
        li.classList.add('last');

        if (typeof node.flags !== 'undefined') {
          li.classList.add(node.flags.join(' '));
        }
      } else {
        li.classList.add('has-children');
      }

      if (node.children.length) {
        LEVEL++;

        li.appendChild(menuChildren(node.children, depth,
            (depth === breadcrumb.length)));

        LEVEL--;
      }

      ul.appendChild(li);
    }

    return ul;
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
    }
  }

  function keyboardNavigation(e) {
    e.stopPropagation();

    var node = e.target.parentNode;

    // right arrow, wants to open node
    if (e.which === 39) {
      node.classList.remove('closed');
    }
    // left arrow, wants to close node
    else if (e.which === 37) {
      node.classList.add('closed');
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
    console.log('doc loaded', e);
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
