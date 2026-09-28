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

  /* ---- status filters (class & diagram rows) ---- */
  var active = {};
  function applyFilters() {
    var any = Object.keys(active).some(function (k) { return active[k]; });
    $$('.node.cls, .node.dia').forEach(function (n) {
      var s = $(':scope > .row', n).dataset.status;
      n.classList.toggle('filtered-out', any && !active[s]);
    });
    $$('.grp').forEach(function (g) {
      var vis = $$('.node', g).some(function (n) { return !n.classList.contains('filtered-out'); });
      g.classList.toggle('filtered-out', !vis);
    });
    $$('.summary .cnt').forEach(function (c) { c.classList.toggle('on', !!active[c.dataset.status]); });
  }
  $$('.summary .cnt').forEach(function (c) {
    c.addEventListener('click', function () { active[c.dataset.status] = !active[c.dataset.status]; applyFilters(); });
  });
  $('#flt-clear').addEventListener('click', function () { active = {}; applyFilters(); });

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

  /* ---- lightbox ---- */
  var lb = $('#lightbox');
  document.addEventListener('click', function (e) {
    var img = e.target.closest('.diag img'); if (!img) return;
    $('img', lb).src = img.src; lb.classList.add('on');
  });
  lb.addEventListener('click', function () { lb.classList.remove('on'); });
})();
