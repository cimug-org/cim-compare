/* cim-compare report — mockup script (Phase 1). No dependencies; nothing runs on load except wiring. */
(function () {
  'use strict';
  var body = document.body;
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  /* ---- expand / collapse ---- */
  function setOpen(node, open) {
    node.classList.toggle('closed', !open);
    var chev = $(':scope > .row > .chev', node);
    if (chev && chev.textContent.trim()) chev.textContent = open ? '▼' : '▶';
  }
  function toggle(node) { setOpen(node, node.classList.contains('closed')); }
  function expandAncestors(el) {
    var n = el.parentElement;
    while (n) { if (n.classList && n.classList.contains('node')) setOpen(n, true); n = n.parentElement; }
  }
  window.expandAll = function () { $$('.node').forEach(function (n) { setOpen(n, true); }); };
  window.collapseAll = function () { $$('.node').forEach(function (n) { if (n.dataset.depth !== '0') setOpen(n, false); }); };

  document.addEventListener('click', function (e) {
    var t = e.target;
    if (t.closest('a, .info, input, button, label, .guid')) return;
    var row = t.closest('.row');
    if (!row || row.classList.contains('leaf')) return;
    var node = row.parentElement;
    if (e.altKey) { $$('.node', node).forEach(function (n) { setOpen(n, true); }); setOpen(node, true); }
    else toggle(node);
  });

  /* ---- GUID toggle ---- */
  var guidChk = $('#chk-guids');
  function applyGuids() { body.classList.toggle('hide-guids', !guidChk.checked); }
  guidChk.addEventListener('change', applyGuids);
  function toggleGuids() { guidChk.checked = !guidChk.checked; applyGuids(); }

  /* ---- metadata mode: hover popup vs inline strip ---- */
  var metaMode = 'hover';
  $$('#seg-meta button').forEach(function (b) {
    b.addEventListener('click', function () {
      metaMode = b.dataset.mode;
      body.classList.toggle('meta-inline', metaMode === 'inline');
      $$('#seg-meta button').forEach(function (x) { x.classList.toggle('on', x === b); });
      if (metaMode === 'hover') $$('tr.meta.open').forEach(function (r) { r.classList.remove('open'); });
      hidePop();
    });
  });
  var pop = null;
  function showPop(anchor, html) {
    hidePop();
    pop = document.createElement('div'); pop.className = 'pop'; pop.innerHTML = html;
    document.body.appendChild(pop);
    var r = anchor.getBoundingClientRect();
    var top = r.bottom + window.scrollY + 6, left = r.left + window.scrollX;
    if (left + pop.offsetWidth > window.scrollX + window.innerWidth - 12) left = window.scrollX + window.innerWidth - pop.offsetWidth - 12;
    pop.style.top = top + 'px'; pop.style.left = Math.max(8, left) + 'px';
  }
  function hidePop() { if (pop) { pop.remove(); pop = null; } }
  function metaSource(info) {
    /* the ⓘ points at a hidden source: an attribute's tr.meta, or a row's .notes-src */
    var tr = info.closest('tr');
    if (tr && tr.nextElementSibling && tr.nextElementSibling.classList.contains('meta')) return tr.nextElementSibling;
    var row = info.closest('.row');
    if (row) return $('.notes-src', row);
    return null;
  }
  document.addEventListener('mouseover', function (e) {
    var info = e.target.closest('.info'); if (!info || metaMode !== 'hover') return;
    var src = metaSource(info); if (!src) return;
    showPop(info, src.classList.contains('meta') ? $('td', src).innerHTML : src.innerHTML);
  });
  document.addEventListener('mouseout', function (e) { if (e.target.closest && e.target.closest('.info')) hidePop(); });
  document.addEventListener('click', function (e) {
    var info = e.target.closest('.info'); if (!info) return;
    e.stopPropagation();
    var src = metaSource(info); if (!src) return;
    if (metaMode === 'hover') { /* click pins/unpins the popup */
      if (pop && pop.dataset.pinned === info.dataset.id) { hidePop(); return; }
      showPop(info, src.classList.contains('meta') ? $('td', src).innerHTML : src.innerHTML);
      pop.dataset.pinned = info.dataset.id; pop.style.pointerEvents = 'auto';
      return;
    }
    if (src.classList.contains('meta')) src.classList.toggle('open');
    else { /* row notes in inline mode: expand the row, its description block is first */
      var node = info.closest('.node'); if (node) setOpen(node, true);
      var d = $(':scope > .detail .desc', node); if (d) d.scrollIntoView({ block: 'nearest' });
    }
  });
  document.addEventListener('click', function (e) { if (pop && !e.target.closest('.info, .pop')) hidePop(); });

  /* ---- redline clean toggle ---- */
  $('#chk-clean').addEventListener('change', function () { body.classList.toggle('clean', this.checked); });

  /* ---- diagram highlights toggle (#37); only present when diagrams are included ---- */
  var hlChk = $('#chk-hl');
  if (hlChk) {
    body.classList.toggle('no-hl', !hlChk.checked);   /* a reload may restore it unchecked */
    hlChk.addEventListener('change', function () { body.classList.toggle('no-hl', !this.checked); });
  }

  /* ---- status filters: a summary count shows only the items of its own kind and status (#78) ---- */
  var active = {};            // "class:added" -> true
  var savedClosed = null;     // nodes closed before the first filter; restored when the filters are cleared
  var NOUN = { 'package': ['package', 'packages'], 'class': ['class', 'classes'], 'diagram': ['diagram', 'diagrams'] };
  function kindOf(n) { return n.classList.contains('cls') ? 'class' : n.classList.contains('dia') ? 'diagram' : 'package'; }
  function statusOf(n) { var r = $(':scope > .row', n); return r ? r.dataset.status : ''; }
  function applyFilters() {
    var any = Object.keys(active).some(function (k) { return active[k]; });
    var nodes = $$('.tree .node');
    if (any && !savedClosed) savedClosed = nodes.filter(function (n) { return n.classList.contains('closed'); });
    nodes.forEach(function (n) { n.classList.remove('filtered-out', 'flt-hit'); });
    $$('.tree .grp, .tree .empty').forEach(function (g) { g.classList.remove('filtered-out'); });
    if (any) {
      var keep = [];
      nodes.forEach(function (n) {
        if (!active[kindOf(n) + ':' + statusOf(n)]) return;
        n.classList.add('flt-hit'); keep.push(n);
        for (var a = n.parentElement.closest('.node'); a; a = a.parentElement.closest('.node')) { keep.push(a); setOpen(a, true); }
      });
      // a matching package is shown with everything in it; anything else not on the way to a match is hidden
      nodes.forEach(function (n) {
        if (keep.indexOf(n) < 0 && !n.parentElement.closest('.node.pkg.flt-hit')) n.classList.add('filtered-out');
      });
      $$('.tree .empty').forEach(function (e) { e.classList.add('filtered-out'); });
      $$('.tree .grp').forEach(function (g) {
        if (!$$(':scope > .node', g).some(function (n) { return !n.classList.contains('filtered-out'); })) g.classList.add('filtered-out');
      });
    } else if (savedClosed) {
      nodes.forEach(function (n) { setOpen(n, savedClosed.indexOf(n) < 0); });
      savedClosed = null;
    }
    var parts = [];
    $$('.summary .cnt').forEach(function (c) {
      var on = !!active[c.dataset.kind + ':' + c.dataset.status];
      c.classList.toggle('on', on);
      if (on) { var n = parseInt(c.textContent, 10); parts.push(n + ' ' + c.dataset.status + ' ' + NOUN[c.dataset.kind][n === 1 ? 0 : 1]); }
    });
    var st = $('#flt-status');
    st.classList.toggle('active', parts.length > 0);
    st.innerHTML = parts.length ? 'Showing ' + parts.join(', ') + ' · <a href="#" id="flt-clear">clear filters</a>'
                                : 'Click a count to show only those items';
    var clr = $('#flt-clear');
    if (clr) clr.addEventListener('click', function (e) { e.preventDefault(); active = {}; applyFilters(); });
  }
  $$('.summary .cnt').forEach(function (c) {
    c.addEventListener('click', function () { var k = c.dataset.kind + ':' + c.dataset.status; active[k] = !active[k]; applyFilters(); });
  });

  /* ---- search ---- */
  var index = $$('[data-name]').map(function (el) {
    return { el: el, name: el.dataset.name, lower: el.dataset.name.toLowerCase(), path: el.dataset.path || '', kind: el.dataset.kind || '' };
  });
  var inp = $('#q'), res = $('#q-results'), sel = -1, hits = [];
  function render() {
    res.innerHTML = '';
    hits.slice(0, 40).forEach(function (h, i) {
      var d = document.createElement('div'); if (i === sel) d.className = 'sel';
      d.innerHTML = '<span class="kind">' + h.kind + '</span><code>' + h.name + '</code><span class="path">' + h.path + '</span>';
      d.addEventListener('mousedown', function (ev) { ev.preventDefault(); go(h); });
      res.appendChild(d);
    });
  }
  function go(h) {
    res.innerHTML = ''; inp.blur();
    var el = h.el.classList.contains('row') ? h.el.parentElement : h.el;   /* .node or <tr> or <li> */
    if (el.id) { reveal(el.id); history.replaceState(null, '', '#' + el.id); }
  }
  inp.addEventListener('input', function () {
    var q = inp.value.trim().toLowerCase(); sel = -1;
    if (!q) { hits = []; render(); return; }
    var starts = [], contains = [];
    index.forEach(function (it) { var p = it.lower.indexOf(q); if (p === 0) starts.push(it); else if (p > 0) contains.push(it); });
    hits = starts.concat(contains); render();
  });
  inp.addEventListener('keydown', function (e) {
    if (e.key === 'ArrowDown') { sel = Math.min(sel + 1, hits.length - 1); render(); e.preventDefault(); }
    else if (e.key === 'ArrowUp') { sel = Math.max(sel - 1, 0); render(); e.preventDefault(); }
    else if (e.key === 'Enter') { if (hits.length) go(hits[Math.max(sel, 0)]); }
    else if (e.key === 'Escape') { inp.value = ''; hits = []; render(); inp.blur(); }
  });
  inp.addEventListener('blur', function () { setTimeout(function () { res.innerHTML = ''; }, 150); });

  /* ---- deep links and in-page links (#EAID_...) ---- */
  function reveal(id) {
    var el = document.getElementById(id); if (!el) return false;
    if (el.closest('.tree .node.filtered-out, .tree .grp.filtered-out')) { active = {}; applyFilters(); }   /* a link to a filtered-out item clears the filters (#78) */
    var node = el.classList.contains('node') ? el : el.closest('.node');
    if (node) setOpen(node, true);           /* open the target itself ... */
    expandAncestors(el);                     /* ... and everything above it */
    var row = el.classList.contains('node') ? $(':scope > .row', el) : el;
    if (row.tagName === 'TR') row.classList.remove('filtered-out');
    $$('.target').forEach(function (x) { x.classList.remove('target'); });
    row.classList.add('target');
    row.scrollIntoView({ block: 'start' });
    if (row.focus) row.focus({ preventScroll: true });
    return true;
  }
  function openHash() { var id = location.hash.replace(/^#/, ''); if (id) reveal(id); }
  window.addEventListener('hashchange', openHash);
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a[href^="#"]'); if (!a) return;
    var id = a.getAttribute('href').slice(1); if (!id) return;
    e.preventDefault();
    if (reveal(id) && location.hash !== '#' + id) history.pushState(null, '', '#' + id);
  });
  openHash();

  /* ---- keyboard ---- */
  document.addEventListener('keydown', function (e) {
    if (e.target.matches('input, textarea')) return;
    if (e.key === '/') { e.preventDefault(); inp.focus(); inp.select(); }
    else if (e.key === 'g') { toggleGuids(); }
    else if (e.key === 'o') { toggleRail(); }
    else if (e.key === 'Home') {
      /* back to the top; a history entry lets Back return to where you were */
      e.preventDefault();
      if (location.hash) history.pushState(null, '', location.pathname + location.search);
      $$('.target').forEach(function (x) { x.classList.remove('target'); });
      window.scrollTo(0, 0);
      var first = $$('.row').filter(function (r) { return r.offsetParent !== null; })[0];
      if (first) first.focus({ preventScroll: true });
    }
    else if (e.key === 'Escape') { hidePop(); $('#lightbox').classList.remove('on'); }
    else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      var rows = $$('.row').filter(function (r) { return r.offsetParent !== null; });
      var cur = document.activeElement.closest ? document.activeElement.closest('.row') : null;
      var i = rows.indexOf(cur); i = e.key === 'ArrowDown' ? Math.min(i + 1, rows.length - 1) : Math.max(i - 1, 0);
      rows[i].focus(); e.preventDefault();
    } else if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      var r = document.activeElement.closest ? document.activeElement.closest('.row') : null;
      if (r && !r.classList.contains('leaf')) { setOpen(r.parentElement, e.key === 'ArrowRight'); e.preventDefault(); }
    } else if (e.key === 'Enter') {
      var r2 = document.activeElement.closest ? document.activeElement.closest('.row') : null;
      if (r2 && !r2.classList.contains('leaf')) toggle(r2.parentElement);
    }
  });

  /* ---- package outline (left rail) ---- */
  var hdr = $('.hdr');
  function setHeaderHeight() { document.documentElement.style.setProperty('--hdr-h', hdr.offsetHeight + 'px'); }
  setHeaderHeight();
  window.addEventListener('resize', setHeaderHeight);

  var rail = $('#rail'), railChk = $('#chk-rail');
  function applyRail() { body.classList.toggle('no-rail', !railChk.checked); if (!body.classList.contains('no-rail')) scheduleSpy(); }
  function toggleRail() { railChk.checked = !railChk.checked; applyRail(); }
  railChk.addEventListener('change', applyRail);

  var railItems = {};
  $$('.r-item', rail).forEach(function (li) { railItems[li.dataset.target] = li; });
  rail.addEventListener('click', function (e) {
    var chev = e.target.closest('.r-chev'); if (!chev) return;
    var li = chev.closest('.r-item'); if (li.classList.contains('r-kids')) li.classList.toggle('closed');
  });

  /* Scroll-spy: the current package is the innermost package whose subtree
     contains the line just below the sticky header. */
  var pkgRows = $$('.node.pkg > .row'), curId = null, spyQueued = false;
  function spy() {
    spyQueued = false;
    if (body.classList.contains('no-rail') || !rail.offsetParent) return;
    var line = hdr.getBoundingClientRect().bottom + 20, best = null;   /* below the rows' 12px scroll-margin */
    for (var i = 0; i < pkgRows.length; i++) {
      var r = pkgRows[i]; if (!r.offsetParent) continue;       /* inside a collapsed package */
      if (r.getBoundingClientRect().top <= line) best = r; else break;
    }
    var node = best ? best.parentElement : (pkgRows[0] && pkgRows[0].parentElement);
    while (node && node.getBoundingClientRect().bottom < line) {  /* scrolled past this package's subtree */
      node = node.parentElement ? node.parentElement.closest('.node.pkg') : null;
    }
    setCurrent(node ? node.id : null);
  }
  function setCurrent(id) {
    if (id === curId) return;
    if (curId && railItems[curId]) railItems[curId].classList.remove('cur');
    curId = id;
    var li = id && railItems[id]; if (!li) return;
    li.classList.add('cur');
    for (var p = li.parentElement.closest('.r-item'); p; p = p.parentElement.closest('.r-item')) p.classList.remove('closed');
    var rr = rail.getBoundingClientRect(), lr = $('.r-row', li).getBoundingClientRect();
    if (lr.top < rr.top + 30 || lr.bottom > rr.bottom - 10) rail.scrollTop += (lr.top - rr.top) - rr.height / 3;
  }
  function scheduleSpy() { if (!spyQueued) { spyQueued = true; requestAnimationFrame(spy); } }
  window.addEventListener('scroll', scheduleSpy, { passive: true });
  window.addEventListener('resize', scheduleSpy);
  document.addEventListener('click', scheduleSpy);   /* expanding/collapsing changes the layout */
  document.addEventListener('keyup', scheduleSpy);
  scheduleSpy();

  /* ---- diagram highlights (#37): boxes are in image pixels; place them as
     percentages of the image's natural size, so they scale with it ---- */
  var PAD = 4;   /* image pixels between an element and its highlight */
  function placeBoxes(wrap) {
    var img = $('img', wrap); if (!img || !img.naturalWidth) return;
    var w = img.naturalWidth, h = img.naturalHeight;
    [].forEach.call(wrap.querySelectorAll('.hl'), function (s) {
      var b = s.getAttribute('data-box').split(',').map(Number);
      var l = Math.max(0, b[0] - PAD), t = Math.max(0, b[1] - PAD);
      var r = Math.min(w, b[2] + PAD), bt = Math.min(h, b[3] + PAD);
      s.style.left = (100 * l / w) + '%'; s.style.top = (100 * t / h) + '%';
      s.style.width = (100 * (r - l) / w) + '%'; s.style.height = (100 * (bt - t) / h) + '%';
    });
    wrap.classList.add('placed');
  }
  [].forEach.call(document.querySelectorAll('.diag .dimg'), function (wrap) {
    if (!wrap.querySelector('.hl')) return;
    var img = $('img', wrap);
    if (img.complete && img.naturalWidth) placeBoxes(wrap);
    else img.addEventListener('load', function () { placeBoxes(wrap); });
  });

  /* ---- connectors (#37): hovering (or focusing) a row outlines the two elements
     it joins on the image it belongs to; a click keeps the outline ---- */
  /* drawn in the image's own pixels, so the outline scales with the image */
  var ENDPAD = 14, ENDLINE = 5, ENDDASH = '16 10', SVGNS = 'http://www.w3.org/2000/svg';
  function showEnds(row) {
    var node = row.closest('.detail'); if (!node) return;
    var wrap = node.querySelector('.dimg[data-side="' + row.dataset.side + '"]'); if (!wrap) return;
    var img = $('img', wrap); if (!img || !img.naturalWidth) return;
    var w = img.naturalWidth, h = img.naturalHeight;
    var svg = document.createElementNS(SVGNS, 'svg');
    svg.setAttribute('class', 'cx-end');
    svg.setAttribute('viewBox', '0 0 ' + w + ' ' + h);
    svg.setAttribute('preserveAspectRatio', 'none');
    (row.dataset.ends || '').split(';').forEach(function (bx) {
      if (!bx) return;
      var b = bx.split(',').map(Number), r = document.createElementNS(SVGNS, 'rect');
      r.setAttribute('x', b[0] - ENDPAD); r.setAttribute('y', b[1] - ENDPAD);
      r.setAttribute('width', b[2] - b[0] + 2 * ENDPAD); r.setAttribute('height', b[3] - b[1] + 2 * ENDPAD);
      r.setAttribute('fill', 'none'); r.setAttribute('stroke', '#1f2933');
      r.setAttribute('stroke-width', ENDLINE); r.setAttribute('stroke-dasharray', ENDDASH);
      svg.appendChild(r);
    });
    wrap.appendChild(svg);
  }
  function clearEnds(detail) { [].forEach.call(detail.querySelectorAll('.cx-end'), function (s) { s.remove(); }); }
  function refreshEnds(detail) {
    clearEnds(detail);
    var r = detail.querySelector('.cx-row:hover') || detail.querySelector('.cx-row:focus') || detail.querySelector('.cx-row.pinned');
    if (r) showEnds(r);
  }
  document.addEventListener('mouseover', function (e) { var r = e.target.closest('.cx-row'); if (r) refreshEnds(r.closest('.detail')); });
  document.addEventListener('mouseout', function (e) {
    var r = e.target.closest('.cx-row'); if (r) setTimeout(function () { refreshEnds(r.closest('.detail')); }, 0);
  });
  document.addEventListener('focusin', function (e) { var r = e.target.closest('.cx-row'); if (r) refreshEnds(r.closest('.detail')); });
  document.addEventListener('focusout', function (e) {
    var r = e.target.closest('.cx-row'); if (r) setTimeout(function () { refreshEnds(r.closest('.detail')); }, 0);
  });
  document.addEventListener('click', function (e) {
    var r = e.target.closest('.cx-row'); if (!r) return;
    var was = r.classList.contains('pinned');
    [].forEach.call(r.parentNode.querySelectorAll('.cx-row.pinned'), function (x) { x.classList.remove('pinned'); });
    if (!was) r.classList.add('pinned');
    refreshEnds(r.closest('.detail'));
  });

  /* ---- lightbox: the image and its highlights ---- */
  var lb = $('#lightbox');
  document.addEventListener('click', function (e) {
    var wrap = e.target.closest('.diag .dimg'); if (!wrap) return;
    var copy = wrap.cloneNode(true);
    copy.querySelector('img').removeAttribute('loading');
    lb.replaceChildren(copy); lb.classList.add('on');
  });
  lb.addEventListener('click', function () { lb.classList.remove('on'); });
})();
