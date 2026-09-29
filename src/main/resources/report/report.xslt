<?xml version="1.0" encoding="UTF-8"?>
<!--
  cim-compare 2.0 report stylesheet (XSLT 3.0, Saxon-HE).

  Input:  the enriched comparison XML written by ReportPreProcessor
          (see that class for the element/attribute contract).
  Output: one self-contained HTML5 file. The CSS and JS are passed in as
          parameters by ReportRenderer and inlined; nothing is fetched at
          view time, and nothing is computed in the browser at load time.

  Layout follows the "Variant B" design agreed in the design brief:
    package tree with indent rails → class rows → detail with
    Description → Metadata → Attributes → Links (Generalization,
    Association, Aggregation) → and Diagrams per package.
-->
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:r="urn:cim-compare:report"
    exclude-result-prefixes="xs r">

  <xsl:output method="html" html-version="5" encoding="UTF-8" indent="no" omit-xml-declaration="yes"/>

  <xsl:param name="css" as="xs:string" select="''"/>
  <xsl:param name="js" as="xs:string" select="''"/>
  <xsl:param name="logo" as="xs:string" select="''"/>
  <xsl:param name="include-diagrams" as="xs:string" select="'true'"/>
  <xsl:param name="image-type" as="xs:string" select="'jpg'"/>

  <xsl:variable name="diagrams" as="xs:boolean" select="$include-diagrams = 'true'"/>
  <xsl:variable name="full" as="xs:boolean" select="/ComparisonReport/@full = 'true'"/>

  <!-- status groups in the order the 1.x report used -->
  <xsl:variable name="group-order" as="xs:string*" select="('identical', 'deleted', 'added', 'moved', 'changed')"/>
  <xsl:variable name="group-label" as="map(xs:string, xs:string)"
      select="map { 'identical': 'Identical', 'deleted': 'Deleted', 'added': 'Added', 'moved': 'Moved', 'changed': 'Changed' }"/>
  <xsl:variable name="badge" as="map(xs:string, xs:string)"
      select="map { 'identical': 'IDENTICAL', 'deleted': 'DELETED', 'added': 'ADDED', 'moved': 'MOVED', 'changed': 'CHANGED' }"/>
  <xsl:variable name="link-order" as="xs:string*" select="('Generalization', 'Association', 'Aggregation')"/>

  <!-- ====================================================== helpers -->

  <xsl:function name="r:path" as="xs:string">
    <xsl:param name="e" as="element()"/>
    <xsl:sequence select="string-join($e/ancestor::Package/@name, '::')"/>
  </xsl:function>

  <xsl:function name="r:prop" as="xs:string">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="name" as="xs:string"/>
    <xsl:param name="side" as="xs:string"/>
    <xsl:sequence select="string($e/Properties/Property[@name = $name]/@*[local-name() = $side])"/>
  </xsl:function>

  <xsl:function name="r:pstatus" as="xs:string">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="name" as="xs:string"/>
    <xsl:sequence select="(string($e/Properties/Property[@name = $name]/@status), 'identical')[1]"/>
  </xsl:function>

  <!-- value of a text property for one side; redlined on the destination side when changed -->
  <xsl:template name="r:text-cell">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="name" as="xs:string"/>
    <xsl:param name="side" as="xs:string"/>
    <xsl:variable name="p" select="$e/Properties/Property[@name = $name]"/>
    <xsl:choose>
      <xsl:when test="$side = 'model' and $p/@status = 'changed' and $p/Redline">
        <xsl:copy-of select="$p/Redline/node()"/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$p/@*[local-name() = $side]"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- a bound, role or cardinality: destination side shows old→new redlined when changed -->
  <xsl:template name="r:token">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="name" as="xs:string"/>
    <xsl:param name="side" as="xs:string"/>
    <xsl:variable name="p" select="$e/Properties/Property[@name = $name]"/>
    <xsl:choose>
      <xsl:when test="$side = 'model' and $p/@status = 'changed'">
        <del><xsl:value-of select="$p/@baseline"/></del><ins><xsl:value-of select="$p/@model"/></ins>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$p/@*[local-name() = $side]"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- link to a class row by id, or plain text -->
  <xsl:template name="r:class-link">
    <xsl:param name="name" as="xs:string"/>
    <xsl:param name="id" as="xs:string?"/>
    <xsl:choose>
      <xsl:when test="$id"><a href="#{$id}"><xsl:value-of select="$name"/></a></xsl:when>
      <xsl:otherwise><xsl:value-of select="$name"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- property | baseline | destination table -->
  <xsl:template name="r:prop-table">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="only-changed" as="xs:boolean" select="true()"/>
    <xsl:param name="skip" as="xs:string*" select="()"/>
    <xsl:param name="one-sided" as="xs:string?" select="()"/>
    <xsl:param name="sides" as="xs:string*" select="('Baseline', 'Destination')"/>
    <!-- properties whose changed destination value is shown as plain text, not redlined (#67) -->
    <xsl:param name="plain" as="xs:string*" select="()"/>
    <!-- one-sided tables omit properties that have no value on the side that exists (no information content) -->
    <xsl:variable name="rows" select="$e/Properties/Property[(not($only-changed) or @status != 'identical') and not(@name = $skip)
        and (not($one-sided) or string(if ($one-sided = 'deleted') then @baseline else @model) != '')]"/>
    <xsl:if test="$rows">
      <div class="tw">
        <table class="t">
          <xsl:choose>
            <xsl:when test="$one-sided">
              <thead><tr><th></th><th>
                <xsl:value-of select="if ($one-sided = 'deleted') then 'Baseline — removed from model' else 'Destination — new in model'"/>
              </th></tr></thead>
              <tbody>
                <xsl:for-each select="$rows">
                  <tr data-status="{@status}"><th class="k"><xsl:value-of select="@name"/></th>
                    <td><xsl:value-of select="if ($one-sided = 'deleted') then @baseline else @model"/></td></tr>
                </xsl:for-each>
              </tbody>
            </xsl:when>
            <xsl:otherwise>
              <thead><tr><th></th><th><xsl:value-of select="$sides[1]"/></th><th><xsl:value-of select="$sides[2]"/></th></tr></thead>
              <tbody>
                <xsl:for-each select="$rows">
                  <tr data-status="{@status}">
                    <th class="k"><xsl:value-of select="@name"/></th>
                    <xsl:choose>
                      <xsl:when test="@status = 'deleted'"><td><xsl:value-of select="@baseline"/></td><td class="void">—</td></xsl:when>
                      <xsl:when test="@status = 'added'"><td class="void">—</td><td><xsl:value-of select="@model"/></td></xsl:when>
                      <xsl:otherwise>
                        <td><xsl:value-of select="@baseline"/></td>
                        <td><xsl:choose>
                          <xsl:when test="@status = 'changed' and Redline and not(@name = $plain)"><xsl:copy-of select="Redline/node()"/></xsl:when>
                          <xsl:otherwise><xsl:value-of select="@model"/></xsl:otherwise>
                        </xsl:choose></td>
                      </xsl:otherwise>
                    </xsl:choose>
                  </tr>
                </xsl:for-each>
              </tbody>
            </xsl:otherwise>
          </xsl:choose>
        </table>
      </div>
    </xsl:if>
  </xsl:template>

  <!-- Description block from Notes -->
  <xsl:template name="r:notes-block">
    <xsl:param name="e" as="element()"/>
    <xsl:variable name="n" select="$e/Notes"/>
    <xsl:if test="$n">
      <xsl:choose>
        <xsl:when test="$n/@status = 'changed'">
          <div class="desc">
            <div><div class="lbl">Baseline description</div><xsl:value-of select="$n/Baseline"/></div>
            <div><div class="lbl">Destination description</div><xsl:copy-of select="$n/Redline/node()"/></div>
          </div>
        </xsl:when>
        <!-- unchanged descriptions are also available from the ⓘ popup; the inline block is
             shown only in "ⓘ inline" mode (body.meta-inline), changed ones always -->
        <xsl:when test="$n/@status = 'deleted'">
          <div class="desc one plain"><div><div class="lbl">Description (baseline)</div><xsl:value-of select="$n/Baseline"/></div></div>
        </xsl:when>
        <xsl:otherwise>
          <div class="desc one plain"><div><div class="lbl">Description</div>
            <xsl:value-of select="if (string($n/Destination) != '') then $n/Destination else $n/Baseline"/></div></div>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

  <!-- hidden copy of the notes for the ⓘ hover popup -->
  <xsl:template name="r:notes-src">
    <xsl:param name="e" as="element()"/>
    <xsl:variable name="n" select="$e/Notes"/>
    <div class="notes-src">
      <xsl:choose>
        <xsl:when test="$n/@status = 'changed'">
          <div class="lbl">Description (destination, redlined)</div><xsl:copy-of select="$n/Redline/node()"/>
        </xsl:when>
        <xsl:when test="$n">
          <div class="lbl">Description</div>
          <xsl:value-of select="if (string($n/Destination) != '') then $n/Destination else $n/Baseline"/>
        </xsl:when>
        <xsl:otherwise><div class="lbl">Description</div>No notes available.</xsl:otherwise>
      </xsl:choose>
    </div>
  </xsl:template>

  <!-- the element's current name: the destination Name property when there is
       one (it carries a «deprecated» prefix that @name may not), else @name -->
  <xsl:function name="r:new-name" as="xs:string">
    <xsl:param name="e" as="element()"/>
    <xsl:sequence select="(r:prop($e, 'Name', 'model')[. != ''], string($e/@name))[1]"/>
  </xsl:function>

  <!-- name with rename / deprecated prefix. A renamed element shows its new name
       only, with "renamed from <old>" in grey beside it (Todd, 2026-09-28). -->
  <xsl:template name="r:display-name">
    <xsl:param name="e" as="element()"/>
    <xsl:choose>
      <xsl:when test="$e/@renamedFrom">
        <xsl:value-of select="r:new-name($e)"/><span class="renamed-from">renamed from <xsl:value-of select="$e/@renamedFrom"/></span>
      </xsl:when>
      <xsl:when test="starts-with($e/@name, '«deprecated»')">
        <span class="stereo">«deprecated»</span><xsl:text> </xsl:text><xsl:value-of select="normalize-space(substring-after($e/@name, '»'))"/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$e/@name"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- tree row -->
  <xsl:template name="r:row">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="ico" as="xs:string"/>
    <xsl:param name="leaf" as="xs:boolean" select="false()"/>
    <xsl:variable name="st" select="string($e/@status)"/>
    <div class="row{if ($leaf) then ' leaf' else ''}" data-status="{$st}" tabindex="0"
         data-name="{$e/@name}" data-kind="{$ico}" data-path="{r:path($e)}">
      <span class="chev"><xsl:value-of select="if ($leaf) then '' else '▶'"/></span>
      <span class="ico"><xsl:value-of select="$ico"/></span>
      <span class="name"><xsl:call-template name="r:display-name"><xsl:with-param name="e" select="$e"/></xsl:call-template></span>
      <xsl:if test="$e/@stereotype and $e/@stereotype != 'deprecated'"><span class="stereo">«<xsl:value-of select="$e/@stereotype"/>»</span></xsl:if>
      <xsl:if test="$e/Properties/Property[@name = 'Notes']">
        <span class="info" data-id="{$e/@id}" title="notes">i</span>
        <xsl:call-template name="r:notes-src"><xsl:with-param name="e" select="$e"/></xsl:call-template>
      </xsl:if>
      <xsl:if test="$st != 'identical'"><span class="badge" data-status="{$st}"><xsl:value-of select="$badge($st)"/></span></xsl:if>
      <xsl:if test="$e/@fromPackage"><span class="from">from <code><xsl:value-of select="$e/@fromPackage"/></code></span></xsl:if>
      <span class="guid"><xsl:value-of select="$e/@guid"/></span>
    </div>
  </xsl:template>

  <!-- ====================================================== document -->

  <xsl:template match="/ComparisonReport">
    <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;&#10;</xsl:text>
    <html lang="en">
      <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <title>CIM Model Comparison <xsl:value-of select="@baselineVersion"/> → <xsl:value-of select="@destinationVersion"/></title>
        <meta name="generator" content="{@generator}"/>
        <style><xsl:value-of select="$css" disable-output-escaping="yes"/></style>
      </head>
      <body>
        <header class="hdr">
          <xsl:if test="$logo != ''"><a class="brand" href="https://github.com/cimug-org/cim-compare" target="_blank" rel="noopener" title="cim-compare on GitHub" aria-label="cim-compare on GitHub"><xsl:value-of select="$logo" disable-output-escaping="yes"/></a></xsl:if>
          <div>
            <div class="title">CIM Model Comparison <span class="arrow">·</span>
              <span class="ver"><xsl:value-of select="@baselineVersion"/></span><xsl:text> </xsl:text><span class="lbl">baseline</span>
              <xsl:text> </xsl:text><span class="arrow">→</span><xsl:text> </xsl:text>
              <span class="ver"><xsl:value-of select="@destinationVersion"/></span><xsl:text> </xsl:text><span class="lbl">destination</span></div>
            <div class="sub">Scope: <xsl:value-of select="@scope"/> · compared <xsl:value-of select="@comparedOn"/> · <a class="gen" href="https://github.com/cimug-org/cim-compare" target="_blank" rel="noopener"><xsl:value-of select="@generator"/></a><xsl:if test="$full"> · full (identical items included)</xsl:if></div>
          </div>
          <div class="tools">
            <div class="search"><input id="q" type="search" placeholder="Find class, package, diagram or attribute…  (/)" autocomplete="off"/><div id="q-results" class="results"></div></div>
            <button type="button" onclick="expandAll()">Expand all</button>
            <button type="button" onclick="collapseAll()">Collapse all</button>
            <label class="chk chk-rail"><input id="chk-rail" type="checkbox" checked="checked"/> Outline <kbd>o</kbd></label>
            <label class="chk"><input id="chk-guids" type="checkbox" checked="checked"/> GUIDs <kbd>g</kbd></label>
            <label class="chk"><input id="chk-clean" type="checkbox"/> Hide redline</label>
            <xsl:if test="$diagrams"><label class="chk" title="Highlight added, removed, changed, moved and restyled elements on the diagram images"><input id="chk-hl" type="checkbox" checked="checked"/> Diagram highlights</label></xsl:if>
            <span class="seg" id="seg-meta" title="How ⓘ metadata is shown"><button type="button" class="on" data-mode="hover">ⓘ hover</button><button type="button" data-mode="inline">ⓘ inline</button></span>
          </div>
        </header>
        <div class="wrap">
          <div class="summary">
            <xsl:call-template name="r:summary-group"><xsl:with-param name="kind" select="'package'"/><xsl:with-param name="label" select="'Packages'"/></xsl:call-template>
            <xsl:call-template name="r:summary-group"><xsl:with-param name="kind" select="'class'"/><xsl:with-param name="label" select="'Classes'"/></xsl:call-template>
            <xsl:if test="$diagrams">
              <xsl:call-template name="r:summary-group"><xsl:with-param name="kind" select="'diagram'"/><xsl:with-param name="label" select="'Diagrams'"/></xsl:call-template>
            </xsl:if>
            <span id="flt-clear" class="clear">clear filters</span>
          </div>
          <div class="legend">
            <span><i class="sw s-added"></i><b>Added</b> — only in destination</span>
            <span><i class="sw s-deleted"></i><b>Deleted</b> — only in baseline</span>
            <span><i class="sw s-moved"></i><b>Moved</b> — same GUID, different package</span>
            <span><i class="sw s-changed"></i><b>Changed</b> — same GUID and package; properties or members differ</span>
            <span><i class="sw s-identical"></i><b>Identical</b></span>
            <span><i class="sw s-pkg"></i><b>Package</b> (unchanged itself)</span>
            <span><del>struck</del><ins>inserted</ins> — destination text with baseline wording struck (descriptions, names, role names, multiplicities); the ⓘ metadata view shows both texts verbatim</span>
            <span><b class="chg">Blue</b> — a changed attribute type (the new type)</span>
          </div>
          <div class="layout">
            <nav id="rail" class="rail" aria-label="Package outline">
              <div class="r-h"><span>Packages</span><span class="r-hn" title="changed classes and diagrams in each package, including sub-packages">changes</span></div>
              <ul class="r-list"><xsl:apply-templates select="Package" mode="rail"/></ul>
            </nav>
            <div class="tree">
              <xsl:apply-templates select="Package"/>
            </div>
          </div>
        </div>
        <div id="lightbox" class="lightbox"><img alt=""/></div>
        <script><xsl:value-of select="$js" disable-output-escaping="yes"/></script>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="r:summary-group">
    <xsl:param name="kind" as="xs:string"/>
    <xsl:param name="label" as="xs:string"/>
    <xsl:variable name="cs" select="/ComparisonReport/Summary/Count[@kind = $kind]"/>
    <xsl:if test="$cs[@status != 'identical' or $full]">
      <div class="grp"><b><xsl:value-of select="$label"/></b>
        <xsl:for-each select="('added', 'deleted', 'moved', 'changed', 'identical')">
          <xsl:variable name="s" select="."/>
          <xsl:variable name="c" select="$cs[@status = $s]"/>
          <xsl:if test="$c and ($s != 'identical' or $full)">
            <span class="cnt s-{$s}" data-status="{$s}" title="filter: {$s}"><xsl:value-of select="$c/@n"/><xsl:text> </xsl:text><xsl:value-of select="$s"/></span>
          </xsl:if>
        </xsl:for-each>
      </div>
    </xsl:if>
  </xsl:template>

  <!-- ====================================================== package outline (left rail) -->

  <!-- One entry per rendered package, nested as in the tree and opened to the
       same depth as the tree on load. The number is the count of changed
       classes and diagrams in the package's whole subtree. -->
  <xsl:template match="Package" mode="rail">
    <xsl:variable name="depth" select="count(ancestor::Package)"/>
    <xsl:variable name="n" select="xs:integer(@changedClasses) + (if ($diagrams) then xs:integer(@changedDiagrams) else 0)"/>
    <li class="r-item{if (Package) then ' r-kids' else ''}{if ($depth ge 2) then ' closed' else ''}" data-target="{@id}" data-status="{@status}">
      <div class="r-row">
        <span class="r-chev"><xsl:value-of select="if (Package) then '▸' else ''"/></span>
        <a class="r-name" href="#{@id}">
          <xsl:attribute name="title">
            <xsl:value-of select="if (@renamedFrom) then concat(@name, ' (renamed from ', @renamedFrom, ')') else @name"/>
            <xsl:if test="not(@status = ('changed', 'identical'))"><xsl:value-of select="concat(' (', @status, ')')"/></xsl:if>
          </xsl:attribute>
          <xsl:value-of select="@name"/>
          <!-- a renamed package: "[renamed from <old>]" in grey on the next line (Todd, 2026-09-28, option D) -->
          <xsl:if test="@renamedFrom"><span class="r-renamed">[renamed from <xsl:value-of select="@renamedFrom"/>]</span></xsl:if>
        </a>
        <xsl:if test="$n gt 0">
          <span class="r-n" title="{@changedClasses} changed classes{if ($diagrams) then concat(', ', @changedDiagrams, ' changed diagrams') else ''}"><xsl:value-of select="$n"/></span>
        </xsl:if>
      </div>
      <xsl:if test="Package">
        <ul><xsl:apply-templates select="Package" mode="rail"/></ul>
      </xsl:if>
    </li>
  </xsl:template>

  <!-- ====================================================== packages -->

  <xsl:template match="Package">
    <xsl:variable name="depth" select="count(ancestor::Package)"/>
    <div class="node pkg{if ($depth > 1) then ' closed' else ''}" id="{@id}" data-depth="{$depth}">
      <xsl:call-template name="r:row"><xsl:with-param name="e" select="."/><xsl:with-param name="ico" select="'📁'"/></xsl:call-template>
      <div class="kids">
        <xsl:apply-templates select="Package"/>
        <xsl:call-template name="r:groups">
          <xsl:with-param name="items" select="Class"/>
          <xsl:with-param name="label" select="'classes'"/>
          <xsl:with-param name="total" select="xs:integer(@classCount)"/>
        </xsl:call-template>
        <xsl:if test="$diagrams">
          <xsl:call-template name="r:groups">
            <xsl:with-param name="items" select="Diagram"/>
            <xsl:with-param name="label" select="'diagrams'"/>
            <xsl:with-param name="total" select="xs:integer(@diagramCount)"/>
          </xsl:call-template>
        </xsl:if>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="r:groups">
    <xsl:param name="items" as="element()*"/>
    <xsl:param name="label" as="xs:string"/>
    <xsl:param name="pkg" select="."/>
    <xsl:param name="total" as="xs:integer" select="count($items)"/>
    <xsl:variable name="shown" select="$items[$full or @status != 'identical']"/>
    <xsl:choose>
      <xsl:when test="$shown">
        <xsl:for-each select="$group-order">
          <xsl:variable name="s" select="."/>
          <xsl:variable name="g" select="$shown[@status = $s]"/>
          <xsl:if test="$g">
            <div class="grp">
              <div class="grp-h"><xsl:value-of select="$group-label($s)"/><xsl:text> </xsl:text><xsl:value-of select="$label"/><xsl:text> </xsl:text><span class="n">(<xsl:value-of select="count($g)"/>)</span></div>
              <xsl:for-each select="$g">
                <xsl:sort select="lower-case(@name)"/>
                <xsl:apply-templates select="."/>
              </xsl:for-each>
            </div>
          </xsl:if>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="$total > 0">
        <div class="empty">Package '<xsl:value-of select="$pkg/@name"/>' has no changes to the <xsl:value-of select="$total"/> <xsl:text> </xsl:text><xsl:value-of select="$label"/> it contains.</div>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================== classes -->

  <xsl:template match="Class">
    <xsl:variable name="depth" select="count(ancestor::Package)"/>
    <div class="node cls closed" id="{@id}" data-depth="{$depth}">
      <xsl:call-template name="r:row"><xsl:with-param name="e" select="."/><xsl:with-param name="ico" select="'⚙'"/></xsl:call-template>
      <div class="detail">
        <xsl:choose>
          <xsl:when test="@status = ('added', 'deleted')">
            <xsl:call-template name="r:class-compact"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="r:class-detail"/>
          </xsl:otherwise>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <!-- full detail: Description → Metadata → Attributes → Links -->
  <xsl:template name="r:class-detail">
    <xsl:variable name="cls" select="."/>
    <xsl:variable name="nb"><xsl:call-template name="r:notes-block"><xsl:with-param name="e" select="."/></xsl:call-template></xsl:variable>
    <xsl:variable name="meta">
      <xsl:call-template name="r:prop-table">
        <xsl:with-param name="e" select="."/>
        <xsl:with-param name="only-changed" select="not($full)"/>
        <xsl:with-param name="skip" select="if ($nb/*) then 'Notes' else ()"/>
        <xsl:with-param name="one-sided" select="if (@status = ('added', 'deleted')) then string(@status) else ()"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$nb/*"><div class="sec"><xsl:copy-of select="$nb"/></div></xsl:if>
    <xsl:if test="$meta/*"><div class="sec"><div class="sec-h"><span class="ico">▤</span>Metadata</div><xsl:copy-of select="$meta"/></div></xsl:if>
    <xsl:variable name="attrs" select="Attribute[$full or @status != 'identical']"/>
    <xsl:if test="$attrs">
      <div class="sec"><div class="sec-h"><span class="ico">≡</span>Attributes</div>
        <div class="tw"><table class="t attrsB attrs">
          <thead><tr><th>Attribute</th><th>Baseline</th><th>Destination</th><th>Notes</th></tr></thead>
          <tbody><xsl:apply-templates select="$attrs" mode="row"/></tbody>
        </table></div>
      </div>
    </xsl:if>
    <xsl:variable name="links" select="Link[$full or @status != 'identical']"/>
    <xsl:if test="$links">
      <div class="sec"><div class="sec-h"><span class="ico">⇄</span>Links</div>
        <xsl:for-each select="$links">
          <xsl:sort select="index-of($link-order, string(@kind))[1]" data-type="number"/>
          <xsl:apply-templates select="." mode="card"/>
        </xsl:for-each>
      </div>
    </xsl:if>
    <xsl:if test="not($nb/*) and not($meta/*) and not($attrs) and not($links)">
      <div class="empty">No changes occurred to the properties or links for this class.</div>
    </xsl:if>
  </xsl:template>

  <!-- compact detail for a wholly added / deleted class: description, plain
       attribute and link lists; the full tables behind a disclosure -->
  <xsl:template name="r:class-compact">
    <xsl:variable name="side" select="if (@status = 'deleted') then 'baseline' else 'model'"/>
    <xsl:variable name="tid" select="if (@status = 'deleted') then 'baselineTypeId' else 'modelTypeId'"/>
    <xsl:variable name="cid" select="if (@status = 'deleted') then 'baselineClassId' else 'modelClassId'"/>
    <xsl:variable name="nb"><xsl:call-template name="r:notes-block"><xsl:with-param name="e" select="."/></xsl:call-template></xsl:variable>
    <xsl:if test="$nb/*"><div class="sec"><xsl:copy-of select="$nb"/></div></xsl:if>
    <xsl:if test="Attribute">
      <div class="sec"><div class="sec-h"><span class="ico">≡</span>Attributes<xsl:text> </xsl:text><span class="n">(<xsl:value-of select="count(Attribute)"/>)</span></div>
        <ul class="plain">
          <xsl:for-each select="Attribute">
            <xsl:sort select="lower-case(@name)"/>
            <li id="{@id}" data-name="{@name}" data-kind="•" data-path="{r:path(.)}::{../@name}">
              <code class="nm"><xsl:value-of select="@name"/></code>
              <xsl:text> : </xsl:text>
              <xsl:call-template name="r:class-link"><xsl:with-param name="name" select="r:prop(., 'Type', $side)"/><xsl:with-param name="id" select="@*[local-name() = $tid]"/></xsl:call-template>
              <xsl:text> </xsl:text><span class="card">[<xsl:value-of select="r:prop(., 'LowerBound', $side)"/>..<xsl:value-of select="r:prop(., 'UpperBound', $side)"/>]</span>
              <xsl:variable name="nt" select="r:prop(., 'Notes', $side)"/>
              <xsl:if test="$nt != ''"><span class="nt"> — <xsl:value-of select="$nt"/></span></xsl:if>
              <span class="guid"><xsl:value-of select="@guid"/></span>
            </li>
          </xsl:for-each>
        </ul>
      </div>
    </xsl:if>
    <xsl:if test="Link">
      <div class="sec"><div class="sec-h"><span class="ico">⇄</span>Links<xsl:text> </xsl:text><span class="n">(<xsl:value-of select="count(Link)"/>)</span></div>
        <ul class="plain">
          <xsl:for-each select="Link">
            <xsl:sort select="index-of($link-order, string(@kind))[1]" data-type="number"/>
            <xsl:variable name="src" select="End[@side = 'source']"/>
            <xsl:variable name="dst" select="End[@side = 'target']"/>
            <li>
              <span class="kind"><xsl:value-of select="@kind"/></span><xsl:text> </xsl:text>
              <xsl:call-template name="r:sig-compact"><xsl:with-param name="link" select="."/><xsl:with-param name="side" select="$side"/><xsl:with-param name="redline" select="false()"/></xsl:call-template>
              <span class="guid"><xsl:value-of select="@guid"/></span>
            </li>
          </xsl:for-each>
        </ul>
      </div>
    </xsl:if>
    <details class="more">
      <summary>Show full metadata for this <xsl:value-of select="if (@status = 'deleted') then 'removed' else 'new'"/> class <span class="n">(properties, attributes, links; empty properties omitted)</span></summary>
      <xsl:call-template name="r:prop-table">
        <xsl:with-param name="e" select="."/>
        <xsl:with-param name="only-changed" select="false()"/>
        <xsl:with-param name="skip" select="if ($nb/*) then 'Notes' else ()"/>
        <xsl:with-param name="one-sided" select="string(@status)"/>
      </xsl:call-template>
      <xsl:for-each select="Attribute">
        <xsl:sort select="lower-case(@name)"/>
        <div class="sub">Attribute <code><xsl:value-of select="@name"/></code> <span class="guid"><xsl:value-of select="@guid"/></span></div>
        <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="one-sided" select="string(../@status)"/></xsl:call-template>
      </xsl:for-each>
      <xsl:for-each select="Link">
        <xsl:sort select="index-of($link-order, string(@kind))[1]" data-type="number"/>
        <div class="sub"><xsl:value-of select="@kind"/> <span class="guid"><xsl:value-of select="@guid"/></span></div>
        <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="one-sided" select="string(../@status)"/></xsl:call-template>
        <div class="endsB">
          <xsl:for-each select="End">
            <div><div class="sub"><xsl:value-of select="if (@side = 'source') then 'Source' else 'Target'"/> end</div>
              <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="one-sided" select="string(../../@status)"/></xsl:call-template>
            </div>
          </xsl:for-each>
        </div>
      </xsl:for-each>
    </details>
  </xsl:template>

  <!-- ====================================================== attributes (variant B rows) -->

  <xsl:template match="Attribute" mode="row">
    <xsl:variable name="st" select="string(@status)"/>
    <xsl:variable name="nb" select="r:prop(., 'Name', 'baseline')"/>
    <xsl:variable name="nm" select="r:prop(., 'Name', 'model')"/>
    <tr data-status="{$st}" id="{@id}" data-name="{@name}" data-kind="•" data-path="{r:path(.)}::{../@name}">
      <td class="nm c-{$st}">
        <xsl:choose>
          <xsl:when test="@renamedFrom"><xsl:value-of select="r:new-name(.)"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="@name"/></xsl:otherwise>
        </xsl:choose>
        <span class="info" data-id="{@guid}" title="attribute metadata">i</span>
        <xsl:if test="@renamedFrom"><span class="renamed-from">renamed from <xsl:value-of select="@renamedFrom"/></span></xsl:if>
        <span class="guid"><xsl:value-of select="@guid"/></span>
      </td>
      <xsl:choose>
        <xsl:when test="$st = 'added'"><td class="nx">does not exist</td></xsl:when>
        <xsl:otherwise>
          <td class="sig">
            <xsl:call-template name="r:class-link"><xsl:with-param name="name" select="r:prop(., 'Type', 'baseline')"/><xsl:with-param name="id" select="@baselineTypeId"/></xsl:call-template>
            <xsl:text> </xsl:text><span class="card">[<xsl:value-of select="r:prop(., 'LowerBound', 'baseline')"/>..<xsl:value-of select="r:prop(., 'UpperBound', 'baseline')"/>]</span>
          </td>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="$st = 'deleted'"><td class="gone">removed from model</td></xsl:when>
        <xsl:otherwise>
          <td class="sig">
            <xsl:choose>
              <xsl:when test="r:pstatus(., 'Type') = 'changed'">
                <!-- a changed type shows only the new type, in blue (not redlined) -->
                <span class="chg"><xsl:call-template name="r:class-link"><xsl:with-param name="name" select="r:prop(., 'Type', 'model')"/><xsl:with-param name="id" select="@modelTypeId"/></xsl:call-template></span>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="r:class-link"><xsl:with-param name="name" select="r:prop(., 'Type', 'model')"/><xsl:with-param name="id" select="@modelTypeId"/></xsl:call-template>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:text> </xsl:text><span class="card">[<xsl:call-template name="r:token"><xsl:with-param name="e" select="."/><xsl:with-param name="name" select="'LowerBound'"/><xsl:with-param name="side" select="'model'"/></xsl:call-template>..<xsl:call-template name="r:token"><xsl:with-param name="e" select="."/><xsl:with-param name="name" select="'UpperBound'"/><xsl:with-param name="side" select="'model'"/></xsl:call-template>]</span>
          </td>
        </xsl:otherwise>
      </xsl:choose>
      <td class="nt">
        <xsl:choose>
          <xsl:when test="$st = 'deleted'"><xsl:value-of select="r:prop(., 'Notes', 'baseline')"/></xsl:when>
          <xsl:when test="r:pstatus(., 'Notes') = 'changed'"><xsl:call-template name="r:text-cell"><xsl:with-param name="e" select="."/><xsl:with-param name="name" select="'Notes'"/><xsl:with-param name="side" select="'model'"/></xsl:call-template></xsl:when>
          <xsl:otherwise><xsl:value-of select="if (r:prop(., 'Notes', 'model') != '') then r:prop(., 'Notes', 'model') else r:prop(., 'Notes', 'baseline')"/></xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
    <tr class="meta"><td colspan="4">
      <div class="lbl">Attribute metadata — <xsl:value-of select="@name"/></div>
      <!-- the ⓘ view shows the new name as it is, like the baseline (#67) -->
      <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="plain" select="'Name'"/></xsl:call-template>
    </td></tr>
  </xsl:template>

  <!-- ====================================================== links (variant B cards) -->

  <!-- one-line signature: srcRole [card] SrcClass → dstRole [card] DstClass -->
  <xsl:template name="r:sig-compact">
    <xsl:param name="link" as="element()"/>
    <xsl:param name="side" as="xs:string"/>
    <xsl:param name="redline" as="xs:boolean" select="true()"/>
    <xsl:variable name="src" select="$link/End[@side = 'source']"/>
    <xsl:variable name="dst" select="$link/End[@side = 'target']"/>
    <xsl:variable name="rolep" select="if ($link/@kind = 'Generalization') then 'End' else 'Role'"/>
    <xsl:variable name="rl" select="$redline and $side = 'model'"/>
    <xsl:choose>
    <xsl:when test="$link/@kind = 'Generalization'">
      <xsl:for-each select="($src, $dst)">
        <xsl:call-template name="r:class-link">
          <xsl:with-param name="name" select="r:prop(., 'End', $side)"/>
          <xsl:with-param name="id" select="if ($side = 'baseline') then @baselineClassId else @modelClassId"/>
        </xsl:call-template>
        <xsl:if test="position() = 1"><xsl:text> → </xsl:text></xsl:if>
      </xsl:for-each>
    </xsl:when>
    <xsl:otherwise>
    <!-- each end: the class (a link), then a grey "role" label, the role name
         and its multiplicity, e.g. "FuseCharacteristicCurve role TotalClearingTimeCurve [0..1]" -->
    <xsl:for-each select="($src, $dst)">
      <xsl:variable name="end" select="."/>
      <xsl:variable name="role" select="r:prop($end, $rolep, $side)"/>
      <span class="end-sig">
      <xsl:call-template name="r:class-link">
        <xsl:with-param name="name" select="r:prop($end, 'End', $side)"/>
        <xsl:with-param name="id" select="if ($side = 'baseline') then $end/@baselineClassId else $end/@modelClassId"/>
      </xsl:call-template>
      <xsl:text> </xsl:text><span class="role-part"><span class="role-lbl">role</span><xsl:text>&#160;</xsl:text>
      <xsl:choose>
        <xsl:when test="$rl and r:pstatus($end, $rolep) = 'changed'">
          <del><xsl:value-of select="r:prop($end, $rolep, 'baseline')"/></del><ins><xsl:value-of select="r:prop($end, $rolep, 'model')"/></ins>
        </xsl:when>
        <xsl:when test="$role = ''"><i>unspecified</i></xsl:when>
        <xsl:otherwise><xsl:value-of select="$role"/></xsl:otherwise>
      </xsl:choose>
      <xsl:text>&#160;</xsl:text><span class="card">[<xsl:choose>
        <xsl:when test="$rl"><xsl:call-template name="r:token"><xsl:with-param name="e" select="$end"/><xsl:with-param name="name" select="'Cardinality'"/><xsl:with-param name="side" select="'model'"/></xsl:call-template></xsl:when>
        <xsl:otherwise><xsl:value-of select="r:prop($end, 'Cardinality', $side)"/></xsl:otherwise>
      </xsl:choose>]</span><xsl:if test="position() = 1"><xsl:text>&#160;→</xsl:text></xsl:if></span></span>
      <xsl:if test="position() = 1"><xsl:text> </xsl:text></xsl:if>
    </xsl:for-each>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="Link" mode="card">
    <xsl:variable name="st" select="string(@status)"/>
    <xsl:variable name="src" select="End[@side = 'source']"/>
    <xsl:variable name="dst" select="End[@side = 'target']"/>
    <div class="linkB" data-status="{$st}">
      <div class="link-h"><span class="badge" data-status="{$st}"><xsl:value-of select="$badge($st)"/></span><xsl:value-of select="@kind"/><span class="guid"><xsl:value-of select="@guid"/></span></div>
      <div class="sigline">
        <div><span class="lbl">Baseline</span>
          <xsl:choose>
            <xsl:when test="$st = 'added'"><span class="nx" style="color:var(--muted);font-style:italic">does not exist</span></xsl:when>
            <xsl:otherwise><xsl:call-template name="r:sig-compact"><xsl:with-param name="link" select="."/><xsl:with-param name="side" select="'baseline'"/></xsl:call-template></xsl:otherwise>
          </xsl:choose>
        </div>
        <div class="mid">→</div>
        <div><span class="lbl">Destination</span>
          <xsl:choose>
            <xsl:when test="$st = 'deleted'"><span class="gone" style="color:var(--deleted-ink);font-weight:700">REMOVED FROM MODEL</span></xsl:when>
            <xsl:otherwise><xsl:call-template name="r:sig-compact"><xsl:with-param name="link" select="."/><xsl:with-param name="side" select="'model'"/><xsl:with-param name="redline" select="$st = 'changed'"/></xsl:call-template></xsl:otherwise>
          </xsl:choose>
        </div>
      </div>
      <xsl:variable name="one" select="if ($st = ('added', 'deleted')) then $st else ()"/>
      <xsl:variable name="meta"><xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full)"/><xsl:with-param name="one-sided" select="$one"/></xsl:call-template></xsl:variable>
      <xsl:if test="$meta/* or End">
        <div class="rows">
          <xsl:if test="$meta/*"><div class="sub">Link metadata</div><xsl:copy-of select="$meta"/></xsl:if>
          <xsl:if test="$one and End">
            <div class="endsB">
              <xsl:for-each select="End">
                <div><div class="sub"><xsl:value-of select="if (@side = 'source') then 'Source' else 'Target'"/> end</div>
                  <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="one-sided" select="$one"/></xsl:call-template>
                </div>
              </xsl:for-each>
            </div>
          </xsl:if>
          <xsl:if test="$st = ('changed', 'moved', 'identical')">
            <div class="endsB">
              <xsl:for-each select="($src, $dst)">
                <xsl:variable name="which" select="if (@side = 'source') then 'Source' else 'Target'"/>
                <xsl:variable name="rolep" select="if (../@kind = 'Generalization') then 'End' else 'Role'"/>
                <div>
                  <xsl:choose>
                    <xsl:when test="Properties/Property[@status != 'identical']">
                      <div class="sub"><xsl:value-of select="$which"/> end — <xsl:value-of select="(r:prop(., $rolep, 'model'), r:prop(., 'End', 'model'))[. != ''][1]"/></div>
                      <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full)"/></xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                      <div class="sub"><xsl:value-of select="$which"/> end</div><div class="none">no metadata changes</div>
                    </xsl:otherwise>
                  </xsl:choose>
                </div>
              </xsl:for-each>
            </div>
          </xsl:if>
        </div>
      </xsl:if>
    </div>
  </xsl:template>

  <!-- ====================================================== diagrams -->

  <xsl:template match="Diagram">
    <xsl:variable name="st" select="string(@status)"/>
    <xsl:variable name="depth" select="count(ancestor::Package)"/>
    <xsl:variable name="nb"><xsl:call-template name="r:notes-block"><xsl:with-param name="e" select="."/></xsl:call-template></xsl:variable>
    <xsl:variable name="meta">
      <xsl:call-template name="r:prop-table">
        <xsl:with-param name="e" select="."/>
        <xsl:with-param name="only-changed" select="not($full)"/>
        <xsl:with-param name="skip" select="if ($nb/*) then 'Notes' else ()"/>
        <xsl:with-param name="one-sided" select="if ($st = ('added', 'deleted')) then $st else ()"/>
      </xsl:call-template>
    </xsl:variable>
    <div class="node dia closed" id="{@id}" data-depth="{$depth}">
      <xsl:call-template name="r:row"><xsl:with-param name="e" select="."/><xsl:with-param name="ico" select="'▦'"/></xsl:call-template>
      <div class="detail">
        <xsl:if test="$nb/*"><div class="sec"><xsl:copy-of select="$nb"/></div></xsl:if>
        <xsl:if test="$meta/*"><div class="sec"><div class="sec-h"><span class="ico">▤</span>Metadata</div><xsl:copy-of select="$meta"/></div></xsl:if>
        <xsl:if test="$st = 'identical'"><div class="empty">Diagram has no material changes made to it.</div></xsl:if>
        <!-- #37: the images are shown only when there is something on them to point
             at (a highlight or a changed connector), or when the diagram was added or
             removed. A diagram whose only changes are properties (name, notes,
             modified date) shows its metadata alone. -->
        <xsl:if test="$st = ('added', 'deleted') or Highlight or Connector">
          <div class="sec"><div class="sec-h"><span class="ico">▦</span>Diagram</div>
            <xsl:if test="Highlight">
              <xsl:variable name="kinds" select="distinct-values(Highlight/@kind)"/>
              <div class="hl-legend">
                <xsl:for-each select="('added', 'removed', 'changed', 'moved', 'restyled')[. = $kinds]">
                  <span class="hl-key"><span class="hl-sw hl-{.}"></span><xsl:value-of select="if (. = 'moved') then 'moved or resized' else ."/></span>
                </xsl:for-each>
              </div>
            </xsl:if>
            <div class="diag">
              <div><div class="lbl">Baseline</div>
                <xsl:choose>
                  <xsl:when test="$st = 'added'"><div class="nx">Diagram does not exist in the baseline model.</div></xsl:when>
                  <xsl:otherwise><xsl:call-template name="r:diagram-image"><xsl:with-param name="side" select="'baseline'"/></xsl:call-template></xsl:otherwise>
                </xsl:choose>
              </div>
              <div><div class="lbl">Destination</div>
                <xsl:choose>
                  <xsl:when test="$st = 'deleted'"><div class="gone">Diagram was removed from the model.</div></xsl:when>
                  <xsl:otherwise><xsl:call-template name="r:diagram-image"><xsl:with-param name="side" select="'destination'"/></xsl:call-template></xsl:otherwise>
                </xsl:choose>
              </div>
            </div>
            <xsl:if test="Connector">
              <xsl:variable name="cx" select="Connector"/>
              <div class="cx-list">
                <div class="cx-h">Connectors <span class="cx-n"><xsl:value-of select="string-join(for $k in ('added', 'removed', 'rerouted', 'labels moved', 'restyled') return (if ($cx[@kind = $k]) then concat(count($cx[@kind = $k]), ' ', $k) else ()), ' · ')"/></span>
                  <span class="cx-hint">hover a connector to outline the elements it joins</span></div>
                <xsl:for-each select="$cx">
                  <xsl:sort select="index-of(('added', 'removed', 'rerouted', 'labels moved', 'restyled'), @kind)[1]" data-type="number"/>
                  <xsl:sort select="@name"/>
                  <div class="cx-row" tabindex="0" data-side="{@side}" data-ends="{@ends}"><span class="cx-k cx-{translate(@kind, ' ', '-')}"><xsl:value-of select="@kind"/></span><span class="cx-name"><xsl:value-of select="@name"/></span></div>
                </xsl:for-each>
              </div>
            </xsl:if>
          </div>
        </xsl:if>
      </div>
    </div>
  </xsl:template>

  <!-- One diagram image with its #37 highlight boxes. The boxes are in the
       image's own pixels (data-box); report.js places them once the image
       has loaded and its natural size is known. -->
  <xsl:template name="r:diagram-image">
    <xsl:param name="side" as="xs:string"/>
    <div class="dimg" data-side="{$side}">
      <img loading="lazy" src="Images-{$side}/{@eaid}.{$image-type}" alt="{@name} ({$side})"/>
      <xsl:for-each select="Highlight[@side = $side]">
        <span class="hl hl-{@kind}" data-box="{@box}" title="{if (@name != '') then concat(@name, ': ') else ''}{if (@kind = 'moved') then 'moved or resized' else @kind}"></span>
      </xsl:for-each>
    </div>
  </xsl:template>

</xsl:stylesheet>
