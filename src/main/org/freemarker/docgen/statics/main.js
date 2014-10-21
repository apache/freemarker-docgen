"use strict";

(function(toc, breadcrumb) {

  // ignore first node
  breadcrumb.shift();

  var breadcrumb = breadcrumb;
  createMenu(toc);

  function createMenu(data) {

    var tableOfContents = document.getElementById('table-of-contents');
    var tocParent = tableOfContents.parentNode;

    tocParent.removeChild(tableOfContents);

    var newTableOfContents = menuChildren(data.children, '', 0);
    newTableOfContents.className = 'table-of-contents';

    tocParent.appendChild(newTableOfContents);
  }

  function menuChildren(children, parentTitle, nodeDepth) {

    var ul = document.createElement('ul');

    for (var x = 0; x < children.length; x++) {
      var node = children[x];
      var li = document.createElement('li');

      li.addEventListener('click', menuClick);
      li.addEventListener('keydown', keyboardNavigation);

      li.appendChild(menuLink(node));

      // always check node parent title so that sections with the same titles and depth
      // aren't highlighted by accident
      if (node.title === breadcrumb[nodeDepth] && (nodeDepth === 0 || parentTitle === breadcrumb[nodeDepth - 1])) {

        li.className = 'current';

        //last current section (the title of the page we're on)
        if (nodeDepth === breadcrumb.length - 1) {
          li.classList.add('open-section');
        }
      } else {
        li.className = 'closed';
      }

      if (node.children.length) {
        li.appendChild(
            menuChildren(node.children, node.title, nodeDepth + 1)
          );
      } else {
        li.className = 'childless';
      }

      li.classList.add('depth-' + nodeDepth);

      ul.appendChild(li);
    }

    return ul;
  }

  function menuLink(node) {
    var a = document.createElement('a');
    var span = document.createElement('span');

    a.href = node.url;

    // create span inside anchor for greater control over styles
    span.innerHTML = node.title;
    a.appendChild(span);

    return a;
  }

  function menuClick(e) {
    e.stopPropagation();

    var node = e.target;

    // prevent flicker when user clicks on a link instead of an li
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

})(toc, breadcrumb);