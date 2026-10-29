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
      // the notes ("has no changes to the N classes it contains") belong to their package:
      // they stay in a package that matches, so expanding it never shows nothing (#148)
      $$('.tree .empty').forEach(function (e) { if (!e.closest('.node.pkg.flt-hit')) e.classList.add('filtered-out'); });
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

  /* ---- search: by name, or (#133) by GUID in any common form ---- */
  /* a GUID reduced to 32 lower-case hex digits: no quotes, braces, EAID_ prefix, hyphens or underscores */
  function normGuid(s) {
    return s.trim().replace(/^["']+|["']+$/g, '').replace(/[{}]/g, '').replace(/^EAID_/i, '').replace(/[-_\s]/g, '').toLowerCase();
  }
  var index = $$('[data-name]').map(function (el) {
    var host = el.classList.contains('row') ? el.parentElement : el;   /* .node or <tr> or <li> carries the EAID */
    return { el: el, name: el.dataset.name, lower: el.dataset.name.toLowerCase(), path: el.dataset.path || '', kind: el.dataset.kind || '',
             guid: /^EAID_/.test(host.id || '') ? normGuid(host.id) : '',
             shown: /^EAID_/.test(host.id || '') ? '{' + host.id.slice(5).replace(/_/g, '-') + '}' : '' };   /* as EA writes it */
  });
  /* links have a GUID but no name: searchable by GUID only, once for each class they are listed under */
  var links = $$('[data-link]').map(function (el) {
    var cls = el.closest('.node.cls'), crow = cls ? $(':scope > .row', cls) : null;
    return { el: el, link: el.dataset.link, name: el.dataset.ends, kind: el.dataset.link,
             path: crow ? 'in ' + crow.dataset.name + ' · ' + crow.dataset.path : '', guid: normGuid(el.dataset.guid || ''), shown: el.dataset.guid || '' };
  });
  var ICO_ASSOC = '<svg width="16" height="10" viewBox="0 0 16 10" aria-hidden="true"><line x1="1" y1="5" x2="15" y2="5" stroke="currentColor" stroke-width="1.4"/></svg>';
  var ICO_GEN = '<svg width="16" height="10" viewBox="0 0 16 10" aria-hidden="true"><line x1="1" y1="5" x2="9" y2="5" stroke="currentColor" stroke-width="1.4"/><path d="M9 1.5 L15 5 L9 8.5 Z" fill="var(--surface)" stroke="currentColor" stroke-width="1.2"/></svg>';
  var inp = $('#q'), res = $('#q-results'), sel = -1, hits = [], gq = '';
  function kindIcon(h) {
    if (h.link) return h.link === 'Generalization' ? ICO_GEN : ICO_ASSOC;
    return h.kind === '📁' ? '<span class="fold"></span>' : h.kind;
  }
  function render() {
    res.innerHTML = '';
    if (gq && !hits.length) { res.innerHTML = '<div class="gnone">No element with this GUID in this report</div>'; return; }
    hits.slice(0, 40).forEach(function (h, i) {
      var d = document.createElement('div'); d.className = (gq ? 'ghit' : '') + (i === sel ? ' sel' : '');
      if (gq) {
        /* the GUID as the report shows it (EA's mixed case), with the typed part in bold */
        var full = h.shown, cut = 0, k = 0;
        for (; cut < full.length && k < gq.length; cut++) if (/[0-9a-f]/i.test(full[cut])) k++;
        d.innerHTML = '<span class="kind">' + kindIcon(h) + '</span><span class="gmain"><span class="gname">'
          + (h.link ? '<span class="lk">' + h.link + '</span> ' : '') + '<code>' + h.name + '</code></span>'
          + '<span class="gid"><b>' + full.slice(0, cut) + '</b>' + full.slice(cut) + '</span></span><span class="path">' + h.path + '</span>';
      } else {
        d.innerHTML = '<span class="kind">' + kindIcon(h) + '</span><code>' + h.name + '</code><span class="path">' + h.path + '</span>';
      }
      d.addEventListener('mousedown', function (ev) { ev.preventDefault(); go(h); });
      res.appendChild(d);
    });
  }
  function go(h) {
    res.innerHTML = ''; inp.blur();
    if (h.link) {
      var cls = h.el.closest('.node.cls');
      if (revealEl(h.el) && cls && cls.id) history.replaceState(null, '', '#' + cls.id);
      return;
    }
    var el = h.el.classList.contains('row') ? h.el.parentElement : h.el;   /* .node or <tr> or <li> */
    if (el.id) { reveal(el.id); history.replaceState(null, '', '#' + el.id); }
  }
  inp.addEventListener('input', function () {
    var raw = inp.value.trim(), q = raw.toLowerCase(), g = normGuid(raw); sel = -1;
    if (!q) { hits = []; gq = ''; render(); return; }
    if (g.length >= 8 && /^[0-9a-f]+$/.test(g)) {
      /* a GUID, whole or its start (8+ hex digits) */
      gq = g;
      hits = index.concat(links).filter(function (it) { return it.guid && it.guid.indexOf(g) === 0; });
      render(); return;
    }
    gq = '';
    var starts = [], contains = [];
    index.forEach(function (it) { var p = it.lower.indexOf(q); if (p === 0) starts.push(it); else if (p > 0) contains.push(it); });
    hits = starts.concat(contains); render();
  });
  inp.addEventListener('keydown', function (e) {
    if (e.key === 'ArrowDown') { sel = Math.min(sel + 1, hits.length - 1); render(); e.preventDefault(); }
    else if (e.key === 'ArrowUp') { sel = Math.max(sel - 1, 0); render(); e.preventDefault(); }
    else if (e.key === 'Enter') { if (hits.length) go(hits[Math.max(sel, 0)]); }
    else if (e.key === 'Escape') { inp.value = ''; hits = []; gq = ''; render(); inp.blur(); }
  });
  inp.addEventListener('blur', function () { setTimeout(function () { res.innerHTML = ''; }, 150); });

  /* ---- deep links and in-page links (#EAID_...) ---- */
  function reveal(id) { return revealEl(document.getElementById(id)); }
  function revealEl(el) {
    if (!el) return false;
    if (el.closest('.tree .node.filtered-out, .tree .grp.filtered-out')) { active = {}; applyFilters(); }   /* a link to a filtered-out item clears the filters (#78) */
    var node = el.classList.contains('node') ? el : el.closest('.node');
    if (node) setOpen(node, true);           /* open the target itself ... */
    expandAncestors(el);                     /* ... and everything above it */
    var row = el.classList.contains('node') ? $(':scope > .row', el) : el;
    if (row.tagName === 'TR') row.classList.remove('filtered-out');
    $$('.target').forEach(function (x) { x.classList.remove('target'); });
    row.classList.add('target');
    row.scrollIntoView({ block: row.hasAttribute('data-link') ? 'center' : 'start' });   /* a link: keep its class in view */
    if (row.focus) row.focus({ preventScroll: true });
    placeRing();
    return true;
  }

  /* #133 a table row cannot have rounded corners, so an attribute row jumped to gets a ring laid
     over it; it follows the row as tables open, close or reflow, and goes when the target does */
  var ring = null, ringQueued = false;
  function placeRing() {
    var tr = $('tr.target');
    if (!tr || !tr.offsetParent) { if (ring) ring.style.display = 'none'; return; }
    /* the ring sits outside the table's scroll box (.tw scrolls sideways and would clip it), spans
       the part of the table in view, and follows a sideways scroll */
    var table = tr.closest('table'), box = table.parentElement, host = box.parentElement;
    if (getComputedStyle(host).position === 'static') host.style.position = 'relative';
    if (!ring) { ring = document.createElement('span'); ring.className = 'tr-ring'; ring.setAttribute('aria-hidden', 'true'); }
    if (ring.parentElement !== host) host.appendChild(ring);
    var h = host.getBoundingClientRect(), b = box.getBoundingClientRect(), r = tr.getBoundingClientRect();
    ring.style.display = 'block';
    ring.style.left = (b.left - h.left - 3) + 'px'; ring.style.width = (Math.min(b.width, table.offsetWidth) + 2) + 'px';
    ring.style.top = (r.top - h.top - 3) + 'px'; ring.style.height = (r.height + 2) + 'px';
  }
  function queueRing() { if (!ringQueued) { ringQueued = true; requestAnimationFrame(function () { ringQueued = false; placeRing(); }); } }
  if (window.MutationObserver) new MutationObserver(function () { if (ring) queueRing(); })
    .observe(body, { subtree: true, attributes: true, attributeFilter: ['class', 'open'] });
  window.addEventListener('resize', function () { if (ring) queueRing(); });
  document.addEventListener('scroll', function (e) { if (ring && e.target.classList && e.target.classList.contains('tw')) queueRing(); }, true);
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
    else if (e.key === 'Escape') { hidePop(); }
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

  /* ---- lightbox: the image and its highlights; #153 zoom and pan ----
     The image is zoomed by setting its size (not a CSS scale), so the browser draws an SVG
     sharp at every zoom; the highlights are placed as percentages of the image, so they
     follow it, and keep their line width. */
  var lb = $('#lightbox');
  var Z = null;   /* the open lightbox: { stage, copy, img, lvl, w, h, s, x, y, fit, max } */
  var HINT = 'Scroll to zoom · drag to move · Esc to close';
  function lbButton(text, title, fn) {
    var b = document.createElement('button'); b.type = 'button'; b.textContent = text;
    b.title = title; b.setAttribute('aria-label', title);
    b.addEventListener('click', function (e) { e.stopPropagation(); fn(); });
    return b;
  }
  function lbSep() { var s = document.createElement('span'); s.className = 'sep'; return s; }
  function lbApply() {
    Z.img.style.width = (Z.w * Z.s) + 'px'; Z.img.style.height = (Z.h * Z.s) + 'px';
    Z.copy.style.transform = 'translate(' + Z.x + 'px,' + Z.y + 'px)';
    Z.lvl.textContent = Math.round(Z.s * 100) + '%';
  }
  function lbFitScale() {
    var r = Z.stage.getBoundingClientRect();
    return Math.min(1, (r.width - 40) / Z.w, (r.height - 40) / Z.h);
  }
  /* zoom to scale s, keeping the point (cx, cy) of the stage where it is */
  function lbZoomAt(s, cx, cy) {
    s = Math.max(Math.min(Z.fit, 1) / 2, Math.min(Z.max, s));
    Z.x = cx - (cx - Z.x) * s / Z.s; Z.y = cy - (cy - Z.y) * s / Z.s; Z.s = s;
    lbKeepInView(); lbApply();
  }
  function lbCentre(s) { var r = Z.stage.getBoundingClientRect(); lbZoomAt(s, r.width / 2, r.height / 2); }
  function lbFit() {
    var r = Z.stage.getBoundingClientRect();
    Z.fit = lbFitScale(); Z.s = Z.fit;
    Z.x = (r.width - Z.w * Z.s) / 2; Z.y = (r.height - Z.h * Z.s) / 2; lbApply();
  }
  /* at least 80 px of the image stays in view */
  function lbKeepInView() {
    var r = Z.stage.getBoundingClientRect(), w = Z.w * Z.s, h = Z.h * Z.s, m = 80;
    Z.x = Math.min(r.width - m, Math.max(m - w, Z.x)); Z.y = Math.min(r.height - m, Math.max(m - h, Z.y));
  }
  function lbClose() { lb.classList.remove('on'); lb.replaceChildren(); Z = null; }
  function lbOpen(wrap) {
    var copy = wrap.cloneNode(true);
    var img = copy.querySelector('img'); img.removeAttribute('loading');
    var stage = document.createElement('div'); stage.className = 'lb-stage'; stage.appendChild(copy);
    var tools = document.createElement('div'); tools.className = 'lb-tools';
    var lvl = document.createElement('span'); lvl.className = 'lvl';
    tools.append(lbButton('−', 'Zoom out (−)', function () { lbCentre(Z.s / 1.25); }), lvl,
      lbButton('+', 'Zoom in (+)', function () { lbCentre(Z.s * 1.25); }), lbSep(),
      lbButton('Fit', 'Fit the whole diagram (0)', lbFit),
      lbButton('100%', 'The diagram at its own size', function () { lbCentre(1); }), lbSep(),
      lbButton('✕', 'Close (Esc)', lbClose));
    var hint = document.createElement('div'); hint.className = 'lb-hint'; hint.textContent = HINT;
    lb.replaceChildren(stage, tools, hint); lb.classList.add('on');
    function start() {
      if (!img.naturalWidth) return;
      /* SVG stays sharp: zoom far; JPG/PNG/GIF/BMP blur past their own size: up to 200% */
      var svg = /\.svg(\?|#|$)/i.test(img.getAttribute('src') || '');
      Z = { stage: stage, copy: copy, img: img, lvl: lvl, w: img.naturalWidth, h: img.naturalHeight,
            s: 1, x: 0, y: 0, fit: 1, max: svg ? 8 : 2 };
      placeBoxes(copy); lbFit();
    }
    if (img.complete && img.naturalWidth) start(); else img.addEventListener('load', start);
    lbGestures(stage);
  }
  function lbGestures(stage) {
    var pts = new Map(), moved = 0, last = null, pinch = null, onImage = false;
    function local(e) { var r = stage.getBoundingClientRect(); return { x: e.clientX - r.left, y: e.clientY - r.top }; }
    stage.addEventListener('wheel', function (e) {
      if (!Z) return;
      e.preventDefault();
      var dy = e.deltaY * (e.deltaMode === 1 ? 16 : e.deltaMode === 2 ? 400 : 1);
      var p = local(e);   /* a trackpad pinch arrives as a wheel event with ctrlKey */
      lbZoomAt(Z.s * Math.exp(-dy * (e.ctrlKey ? 0.01 : 0.0015)), p.x, p.y);
    }, { passive: false });
    /* Safari's trackpad pinch */
    var g0 = null;
    stage.addEventListener('gesturestart', function (e) { if (Z) { e.preventDefault(); g0 = Z.s; } });
    stage.addEventListener('gesturechange', function (e) {
      if (!Z || g0 === null) return; e.preventDefault(); var p = local(e); lbZoomAt(g0 * e.scale, p.x, p.y);
    });
    stage.addEventListener('gestureend', function () { g0 = null; });
    stage.addEventListener('pointerdown', function (e) {
      if (!Z) return;
      try { stage.setPointerCapture(e.pointerId); } catch (ignored) { /* a pointer the browser no longer tracks */ }
      pts.set(e.pointerId, local(e));
      if (pts.size === 1) { moved = 0; last = local(e); onImage = !!e.target.closest('.dimg'); stage.classList.add('dragging'); }
      if (pts.size === 2) {
        var a = Array.from(pts.values());
        pinch = { d: Math.hypot(a[0].x - a[1].x, a[0].y - a[1].y), s: Z.s };
      }
    });
    stage.addEventListener('pointermove', function (e) {
      if (!Z || !pts.has(e.pointerId)) return;
      var p = local(e); pts.set(e.pointerId, p);
      if (pts.size >= 2 && pinch) {   /* two fingers: zoom about their midpoint */
        var a = Array.from(pts.values()), d = Math.hypot(a[0].x - a[1].x, a[0].y - a[1].y);
        moved += 10; lbZoomAt(pinch.s * d / Math.max(1, pinch.d), (a[0].x + a[1].x) / 2, (a[0].y + a[1].y) / 2);
      } else if (last) {
        moved += Math.abs(p.x - last.x) + Math.abs(p.y - last.y);
        Z.x += p.x - last.x; Z.y += p.y - last.y; last = p; lbKeepInView(); lbApply();
      }
    });
    function up(e) {
      if (!pts.has(e.pointerId)) return;
      pts.delete(e.pointerId);
      if (pts.size < 2) pinch = null;
      if (pts.size === 1) last = Array.from(pts.values())[0];
      if (pts.size === 0) {
        stage.classList.remove('dragging');
        /* a click (not a drag) outside the image closes the lightbox; where the press began
           decides, since a captured pointer's events all go to the stage */
        if (e.type === 'pointerup' && moved < 4 && !onImage) lbClose();
        last = null;
      }
    }
    stage.addEventListener('pointerup', up); stage.addEventListener('pointercancel', up);
  }
  document.addEventListener('click', function (e) {
    if (lb.contains(e.target)) return;
    var wrap = e.target.closest('.diag .dimg'); if (!wrap || !wrap.querySelector('img')) return;
    lbOpen(wrap);
  });
  document.addEventListener('keydown', function (e) {
    if (!Z || !lb.classList.contains('on')) return;
    if (e.key === '+' || e.key === '=') { lbCentre(Z.s * 1.25); e.preventDefault(); e.stopImmediatePropagation(); }
    else if (e.key === '-' || e.key === '_') { lbCentre(Z.s / 1.25); e.preventDefault(); e.stopImmediatePropagation(); }
    else if (e.key === '0') { lbFit(); e.preventDefault(); e.stopImmediatePropagation(); }
    else if (e.key === 'Escape') { lbClose(); }
  }, true);
  window.addEventListener('resize', function () { if (Z && Math.abs(Z.s - Z.fit) < 1e-6) lbFit(); });

  /* ---- #128 printing: load every diagram image first (images not yet scrolled into view
     would otherwise print blank). "Show full metadata" sections print as they are on screen. ---- */
  var pb = $('#btn-print'); if (pb) pb.addEventListener('click', function () { window.print(); });
  window.addEventListener('beforeprint', function () {
    $$('img[loading="lazy"]').forEach(function (i) { i.loading = 'eager'; });
  });
})();
