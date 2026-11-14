/* Preview-only validation: reads the rendered DOM and reports measurable facts. */
(function () {
  'use strict';

  function rectOf(selector) {
    var el = document.querySelector(selector);
    if (!el) {
      return null;
    }
    var rect = el.getBoundingClientRect();
    var style = getComputedStyle(el);
    return {
      width: Math.round(rect.width),
      height: Math.round(rect.height),
      display: style.display,
      fontSize: style.fontSize,
      lineHeight: style.lineHeight,
      float: style.float,
      color: style.color
    };
  }

  function run() {
    var images = [];
    var imageNodes = document.querySelectorAll('#page-content img');
    for (var i = 0; i < imageNodes.length; i++) {
      var image = imageNodes[i];
      var imageRect = image.getBoundingClientRect();
      images.push({
        src: (image.currentSrc || image.src || '').substring(0, 120),
        naturalWidth: image.naturalWidth,
        naturalHeight: image.naturalHeight,
        renderedWidth: Math.round(imageRect.width),
        complete: image.complete
      });
    }

    var tables = [];
    var tableNodes = document.querySelectorAll('#page-content table');
    for (var t = 0; t < tableNodes.length; t++) {
      var table = tableNodes[t];
      var wrap = table.closest('.reader-table-wrap');
      tables.push({
        width: Math.round(table.getBoundingClientRect().width),
        wrapWidth: wrap ? Math.round(wrap.getBoundingClientRect().width) : 0,
        scrollWidth: wrap ? wrap.scrollWidth : 0
      });
    }

    var result = {
      viewport: window.innerWidth + 'x' + window.innerHeight,
      documentWidth: document.documentElement.scrollWidth,
      horizontalOverflow: document.documentElement.scrollWidth - window.innerWidth,
      styleSheets: document.styleSheets.length,
      pageContent: rectOf('#page-content'),
      pageTitle: rectOf('#page-title'),
      mainContent: rectOf('#main-content'),
      imageBlock: rectOf('.scp-image-block'),
      images: images,
      tables: tables,
      blockquotes: document.querySelectorAll('#page-content blockquote').length,
      footnotes: document.querySelectorAll('#page-content .footnote-footer').length,
      footnoteRefs: document.querySelectorAll('sup.footnoteref a, a.footnoteref').length,
      headings: document.querySelectorAll('#page-content h1, #page-content h2, #page-content h3').length,
      tabSets: document.querySelectorAll('.yui-navset').length,
      collapsibles: document.querySelectorAll('#page-content .collapsible-block').length,
      tabTitles: Array.prototype.slice.call(document.querySelectorAll('.yui-navset .yui-nav li')).map(function (item) {
        return (item.innerText || '').trim();
      }),
      ratingText: (document.querySelector('.creditRate') ? document.querySelector('.creditRate').innerText : '').replace(/\s+/g, ' ').trim(),
      permissionText: (document.querySelector('#page-content').innerText || '').indexOf('以下内容需要5/CN-1000级权限') >= 0,
      textLength: (document.querySelector('#page-content').innerText || '').length
    };

    var collapsed = document.querySelector('#page-content .collapsible-block');
    if (collapsed) {
      var folded = collapsed.querySelector('.collapsible-block-folded');
      var unfolded = collapsed.querySelector('.collapsible-block-unfolded');
      result.collapsibleBefore = unfolded ? getComputedStyle(unfolded).display : null;
      if (folded) {
        folded.click();
      }
      result.collapsibleAfterOpen = unfolded ? getComputedStyle(unfolded).display : null;
      result.collapsibleContent = unfolded ? (unfolded.innerText || '').substring(0, 40) : '';
      var closeCollapse = collapsed.querySelector('.collapsible-block-unfolded-link');
      if (closeCollapse) {
        closeCollapse.click();
      }
      result.collapsibleAfterClose = unfolded ? getComputedStyle(unfolded).display : null;
    }

    var secondTab = document.querySelectorAll('.yui-navset .yui-nav li')[1];
    if (secondTab) {
      secondTab.click();
      var visible = null;
      var panes = document.querySelectorAll('.yui-navset .yui-content > div');
      for (var p = 0; p < panes.length; p++) {
        if (panes[p].style.display !== 'none') {
          visible = panes[p];
          break;
        }
      }
      result.tabSwitch = visible ? {
        visibleId: visible.id,
        text: (visible.innerText || '').substring(0, 40)
      } : null;
      var firstTab = document.querySelectorAll('.yui-navset .yui-nav li')[0];
      if (firstTab) {
        firstTab.click();
      }
    }

    var ref = document.querySelector('sup.footnoteref a, a.footnoteref');
    if (ref) {
      ref.click();
      var pop = document.querySelector('.reader-footnote-pop');
      result.footnotePopover = pop ? {
        visible: pop.getBoundingClientRect().height > 0,
        text: (pop.innerText || '').substring(0, 80)
      } : null;
      var close = document.querySelector('.reader-pop-close');
      if (close) {
        close.click();
      }
    }

    var image = document.querySelector('#page-content img');
    if (image) {
      image.click();
      var box = document.querySelector('.reader-lightbox');
      result.lightbox = box ? {
        visible: box.getBoundingClientRect().height > 0,
        hasImage: !!box.querySelector('img')
      } : null;
      var lightClose = document.querySelector('.reader-lightbox-close');
      if (lightClose) {
        lightClose.click();
      }
    }

    var pre = document.createElement('pre');
    pre.id = 'reader-validation';
    pre.textContent = JSON.stringify(result);
    document.body.appendChild(pre);
    document.title = 'VALIDATION ' + JSON.stringify(result);
  }

  if (document.readyState === 'complete') {
    setTimeout(run, 1200);
  } else {
    window.addEventListener('load', function () {
      setTimeout(run, 1200);
    });
  }
})();
