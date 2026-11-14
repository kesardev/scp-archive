/* Reader interactions. Page scripts are stripped; everything here is ours. */
(function () {
  'use strict';

  function post(type, payload) {
    var message = { type: type };
    if (payload) {
      for (var key in payload) {
        if (Object.prototype.hasOwnProperty.call(payload, key)) {
          message[key] = payload[key];
        }
      }
    }
    try {
      if (window.Reader && window.Reader.postMessage) {
        window.Reader.postMessage(JSON.stringify(message));
      }
    } catch (error) {
      /* preview mode has no native bridge */
    }
  }

  function closest(node, selector) {
    while (node && node.nodeType === 1) {
      if (node.matches && node.matches(selector)) {
        return node;
      }
      node = node.parentNode;
    }
    return null;
  }

  function wrapTables() {
    var tables = document.querySelectorAll('#page-content table');
    for (var i = 0; i < tables.length; i++) {
      var table = tables[i];
      if (closest(table.parentNode, '.reader-table-wrap')) {
        continue;
      }
      var wrap = document.createElement('div');
      wrap.className = 'reader-table-wrap';
      table.parentNode.insertBefore(wrap, table);
      wrap.appendChild(table);
    }
  }

  function initTabs() {
    var sets = document.querySelectorAll('.yui-navset');
    for (var i = 0; i < sets.length; i++) {
      (function (set) {
        var nav = set.querySelector('ul.yui-nav');
        var content = set.querySelector('.yui-content');
        if (!nav || !content) {
          return;
        }
        var tabs = nav.querySelectorAll('li');
        var panes = [];
        for (var p = 0; p < content.children.length; p++) {
          if (content.children[p].tagName === 'DIV') {
            panes.push(content.children[p]);
          }
        }
        if (tabs.length === 0 || panes.length === 0) {
          return;
        }
        function show(index) {
          for (var t = 0; t < tabs.length; t++) {
            if (t === index) {
              tabs[t].className = tabs[t].className.replace(/\s*selected/g, '') + ' selected';
            } else {
              tabs[t].className = tabs[t].className.replace(/\s*selected/g, '');
            }
          }
          for (var q = 0; q < panes.length; q++) {
            panes[q].style.display = q === index ? '' : 'none';
          }
        }
        for (var t = 0; t < tabs.length; t++) {
          (function (index) {
            tabs[index].addEventListener('click', function (event) {
              event.preventDefault();
              show(index);
            });
          })(t);
        }
        show(0);
      })(sets[i]);
    }
  }

  function initCollapsibles() {
    var blocks = document.querySelectorAll('.collapsible-block');
    for (var i = 0; i < blocks.length; i++) {
      (function (block) {
        var folded = block.querySelector('.collapsible-block-folded');
        var unfolded = block.querySelector('.collapsible-block-unfolded');
        if (!unfolded) {
          return;
        }
        unfolded.style.display = 'none';
        if (folded) {
          folded.style.display = '';
          folded.addEventListener('click', function (event) {
            event.preventDefault();
            unfolded.style.display = 'block';
            folded.style.display = 'none';
          });
        }
        var closeLink = block.querySelector('.collapsible-block-unfolded-link');
        if (closeLink) {
          closeLink.addEventListener('click', function (event) {
            event.preventDefault();
            unfolded.style.display = 'none';
            if (folded) {
              folded.style.display = '';
            }
          });
        }
      })(blocks[i]);
    }
  }

  function footnoteNumber(link) {
    var id = link.id || '';
    if (id.indexOf('footnoteref-') === 0) {
      return id.substring('footnoteref-'.length);
    }
    var href = link.getAttribute('href') || '';
    var match = href.match(/footnote-([0-9]+)/);
    return match ? match[1] : '';
  }

  function closeFootnotePop() {
    var existing = document.querySelector('.reader-footnote-pop');
    if (existing) {
      existing.parentNode.removeChild(existing);
    }
  }

  function showFootnote(number) {
    var source = document.getElementById('footnote-' + number);
    if (!source) {
      return;
    }
    closeFootnotePop();
    var pop = document.createElement('div');
    pop.className = 'reader-footnote-pop';

    var title = document.createElement('div');
    title.className = 'reader-pop-title';
    title.textContent = '脚注 ' + number;
    pop.appendChild(title);

    var close = document.createElement('button');
    close.className = 'reader-pop-close';
    close.type = 'button';
    close.textContent = '×';
    close.addEventListener('click', closeFootnotePop);
    pop.appendChild(close);

    var body = document.createElement('div');
    var clone = source.cloneNode(true);
    var numberLink = clone.querySelector('a');
    if (numberLink) {
      numberLink.parentNode.removeChild(numberLink);
    }
    body.innerHTML = clone.innerHTML;
    pop.appendChild(body);

    document.body.appendChild(pop);
  }

  function initFootnotes() {
    var refs = document.querySelectorAll('sup.footnoteref a, a.footnoteref');
    for (var i = 0; i < refs.length; i++) {
      (function (link) {
        link.addEventListener('click', function (event) {
          event.preventDefault();
          var number = footnoteNumber(link);
          if (number) {
            showFootnote(number);
          }
        });
      })(refs[i]);
    }
  }

  function initImages() {
    var nodes = document.querySelectorAll('#page-content img');
    var items = [];

    function captionOf(image) {
      var block = closest(image, '.scp-image-block');
      if (!block) {
        return '';
      }
      var caption = block.querySelector('.scp-image-caption');
      return caption ? caption.textContent.trim() : '';
    }

    function closeGallery() {
      var existing = document.querySelector('.reader-lightbox');
      if (existing) {
        existing.parentNode.removeChild(existing);
      }
    }

    function openGallery(index) {
      closeGallery();
      var box = document.createElement('div');
      box.className = 'reader-lightbox';

      var close = document.createElement('button');
      close.className = 'reader-lightbox-close';
      close.type = 'button';
      close.textContent = '×';
      close.addEventListener('click', closeGallery);
      box.appendChild(close);

      var image = document.createElement('img');
      box.appendChild(image);

      var counter = document.createElement('div');
      counter.className = 'reader-lightbox-counter';
      box.appendChild(counter);

      var caption = document.createElement('div');
      caption.className = 'reader-lightbox-cap';
      box.appendChild(caption);

      var prev = document.createElement('button');
      prev.className = 'reader-lightbox-nav reader-lightbox-prev';
      prev.type = 'button';
      prev.textContent = '‹';
      box.appendChild(prev);

      var next = document.createElement('button');
      next.className = 'reader-lightbox-nav reader-lightbox-next';
      next.type = 'button';
      next.textContent = '›';
      box.appendChild(next);

      function render(current) {
        var item = items[current];
        if (!item) {
          return;
        }
        image.src = item.src;
        image.alt = item.caption;
        caption.textContent = item.caption;
        counter.textContent = (current + 1) + ' / ' + items.length;
        prev.style.display = items.length > 1 ? '' : 'none';
        next.style.display = items.length > 1 ? '' : 'none';
      }

      prev.addEventListener('click', function (event) {
        event.stopPropagation();
        index = (index - 1 + items.length) % items.length;
        render(index);
      });
      next.addEventListener('click', function (event) {
        event.stopPropagation();
        index = (index + 1) % items.length;
        render(index);
      });
      box.addEventListener('click', function (event) {
        if (event.target === box) {
          closeGallery();
        }
      });
      document.body.appendChild(box);
      render(index);
    }

    for (var i = 0; i < nodes.length; i++) {
      var src = nodes[i].currentSrc || nodes[i].src;
      if (!src) {
        continue;
      }
      items.push({ src: src, caption: captionOf(nodes[i]) });
      (function (node, index) {
        node.style.cursor = 'zoom-in';
        node.addEventListener('click', function (event) {
          event.preventDefault();
          openGallery(index);
        });
      })(nodes[i], items.length - 1);
    }
  }

  function initToc() {
    var heads = document.querySelectorAll('#page-content h1, #page-content h2, #page-content h3');
    var items = [];
    for (var i = 0; i < heads.length; i++) {
      var head = heads[i];
      var text = head.textContent.replace(/\s+/g, ' ').trim();
      if (!text) {
        continue;
      }
      if (!head.id) {
        head.id = 'reader-heading-' + i;
      }
      items.push({
        id: head.id,
        text: text,
        level: parseInt(head.tagName.substring(1), 10)
      });
    }
    if (items.length > 0) {
      post('toc', { items: items });
    }
  }

  function initProgress() {
    var bar = document.createElement('div');
    bar.className = 'reader-progress';
    document.body.appendChild(bar);
    var ticking = false;
    function update(persistPosition) {
      ticking = false;
      var height = document.documentElement.scrollHeight - window.innerHeight;
      var value = height <= 0 ? 0 : Math.min(100, Math.max(0, (window.scrollY / height) * 100));
      bar.style.width = value + '%';
      if (persistPosition) {
        post('progress', { value: Math.round(value), ratio: height <= 0 ? 0 : (window.scrollY / height) });
      } else {
        post('progress', { value: Math.round(value) });
      }
    }
    window.addEventListener('scroll', function () {
      if (!ticking) {
        ticking = true;
        window.requestAnimationFrame(function () { update(true); });
      }
    }, { passive: true });
    update(false);
  }

  function initLinks() {
    document.addEventListener('click', function (event) {
      var link = closest(event.target, 'a');
      if (!link) {
        return;
      }
      if (link.hasAttribute('data-reader-noop')) {
        event.preventDefault();
        return;
      }
      var href = link.getAttribute('href') || '';
      if (!href || href.charAt(0) === '#') {
        return;
      }
      if (href.indexOf('javascript:') === 0) {
        event.preventDefault();
        return;
      }
      var absolute;
      try {
        absolute = new URL(href, document.baseURI);
      } catch (error) {
        return;
      }
      event.preventDefault();
      var host = absolute.hostname.toLowerCase();
      if (host === 'scp-wiki-cn.wikidot.com' || host === 'scp-wiki.wikidot.com') {
        post('openInternal', { path: absolute.pathname.replace(/^\//, '') + absolute.search });
      } else {
        post('openExternal', { url: absolute.href });
      }
    });
  }

  function fixAvatars() {
    var images = document.querySelectorAll('img[src*="avatar.php"]');
    for (var i = 0; i < images.length; i++) {
      var src = images[i].getAttribute('src') || '';
      if (src.indexOf('&amp;') >= 0) {
        images[i].setAttribute('src', src.replace(/&amp;/g, '&'));
      }
    }
  }

  window.__readerApplySettings = function (settings) {
    if (!settings) {
      return;
    }
    var root = document.documentElement;
    if (settings.fontSize) {
      root.style.setProperty('--reader-font-size', settings.fontSize + 'px');
    }
    if (settings.lineHeight) {
      root.style.setProperty('--reader-line-height', String(settings.lineHeight));
    }
    document.body.classList.toggle('reader-hide-images', settings.showImages === false);
    var main = document.getElementById('main-content');
    if (main && settings.margin) {
      main.style.paddingLeft = settings.margin + 'px';
      main.style.paddingRight = settings.margin + 'px';
    }
  };

  window.__readerTocGo = function (id) {
    var target = document.getElementById(id);
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  window.__readerRestorePosition = function (ratio) {
    var value = Number(ratio);
    if (!isFinite(value) || value <= 0) {
      return;
    }
    var max = document.documentElement.scrollHeight - window.innerHeight;
    if (max > 0) {
      window.scrollTo(0, max * Math.min(1, Math.max(0, value)));
    }
  };

  function init() {
    wrapTables();
    initTabs();
    initCollapsibles();
    initFootnotes();
    initImages();
    initToc();
    initProgress();
    initLinks();
    fixAvatars();
    window.__readerReady = true;
    post('ready', { title: document.title });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
