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
(function(toc, breadcrumb) {

  var menuLevel = 0;

  function menuItemInnerHTML(nodeData) {
    var isLink = nodeData.url != null;
    var a = document.createElement(isLink ? 'a' : 'span');

    a.innerHTML = nodeData.title;

    if (isLink) {
      a.href = nodeData.url;
    }

    a.className = 'depth-' + menuLevel + '-link';

    return a;
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

  function menuClick(e) {
    e.stopPropagation();

    var node = e.target;

    if (node.tagName.toUpperCase() === 'LI') {
      node.classList.toggle('closed');
      node.classList.toggle('open');
    }
  }

  function menuChildren(children, depth, onPath) {

    var ul = document.createElement('ul');
    ul.classList.add('depth-' + menuLevel);

    for (var x = 0; x < children.length; x++) {
      var node = children[x];

      var li = document.createElement('li');
      var isLast = checkIfLast(node);

      if (menuLevel === 0) {
        li.classList.add('section');
      }

      // @todo: hide this until we can figure out a solution for search
      if (node.title === 'Search') {
        li.style.display = 'none';
      }

      li.addEventListener('click', menuClick);
      li.addEventListener('keydown', keyboardNavigation);

      // add menu link
      li.appendChild(menuItemInnerHTML(node));

      if (node.title === breadcrumb[depth + 1] && onPath) {

        if (depth + 2 === breadcrumb.length) {
          li.classList.add('current');
        }

        // 'section' is always open
        if (menuLevel !== 0) {
          li.classList.add('open');
        }

        depth++;

      } else if (menuLevel > 0) {
        li.classList.add('closed');
      }

      if (isLast) {
        li.classList.add('last');

        // @todo: add flags to docgen
        if (typeof node.flags !== 'undefined') {
          li.classList.add(node.flags.join(' '));
        }
      } else if (menuLevel > 0) {
        // don't add for top menuLevel elements
        li.classList.add('has-children');
      }

      if (!isLast) {
        menuLevel++;

        li.appendChild(menuChildren(node.children, depth, (node.title === breadcrumb[depth])));

        menuLevel--;
      }

      ul.appendChild(li);
    }

    return ul;
  }

  function createMenu(data) {
    var menuPlaceholder = document.getElementById('table-of-contents-wrapper');

    var finishedToc = menuChildren(data.children, 0, true);
    finishedToc.classList.add('table-of-contents');

    menuPlaceholder.appendChild(finishedToc);
  }

  createMenu(toc);

})(toc, breadcrumb);
