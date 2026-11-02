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
  <!-- #139 e.g. "Diagrams are JPG: SVG images were not available from this version of EA." -->
  <xsl:param name="image-note" as="xs:string" select="''"/>

  <xsl:variable name="diagrams" as="xs:boolean" select="$include-diagrams = 'true'"/>
  <xsl:variable name="full" as="xs:boolean" select="/ComparisonReport/@full = 'true'"/>

  <!-- status groups in the order the 1.x report used -->
  <xsl:variable name="group-order" as="xs:string*" select="('identical', 'deleted', 'added', 'moved', 'changed')"/>
  <xsl:variable name="group-label" as="map(xs:string, xs:string)"
      select="map { 'identical': 'Identical', 'deleted': 'Deleted', 'added': 'Added', 'moved': 'Moved', 'changed': 'Changed' }"/>
  <xsl:variable name="badge" as="map(xs:string, xs:string)"
      select="map { 'identical': 'IDENTICAL', 'deleted': 'DELETED', 'added': 'ADDED', 'moved': 'MOVED', 'changed': 'CHANGED' }"/>
  <!-- #137 the ⓘ in running text: drawn, so it sits on the line the same in every browser and font
       (the Unicode ⓘ came out small and below the line); sized to the text by .ii in report.css -->
  <xsl:variable name="ii"><svg class="ii" viewBox="0 0 16 16" aria-hidden="true"><circle cx="8" cy="8" r="7.1" fill="none" stroke="currentColor" stroke-width="1.1"/><circle cx="8.9" cy="4.5" r="1.05" fill="currentColor"/><path d="M8.3 6.9 L7.2 11.3 Q7 12.1 7.8 11.8 L8.6 11.3" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/></svg></xsl:variable>
  <xsl:variable name="link-order" as="xs:string*" select="('Generalization', 'Association', 'Aggregation')"/>
  <!-- #169: a hanging luggage tag with its string, for "Tagged values" -->
  <xsl:variable name="tag-ico"><svg class="tg" width="14" height="14" viewBox="0 0 16 16" aria-hidden="true"><g transform="rotate(90 8 8)"><path d="M1.5 8 L5.5 3.5 H14.5 V12.5 H5.5 Z" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><circle cx="6" cy="8" r="1.3" fill="none" stroke="currentColor" stroke-width="1.2"/></g><path d="M8 4.7 C 6.5 2.5, 9.5 1.6, 10.5 0.6" fill="none" stroke="currentColor" stroke-width="1.1" stroke-linecap="round"/></svg></xsl:variable>

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

  <!-- #133 a link's two end classes for the search list, e.g. "Plant ▷ PowerSystemResource" -->
  <xsl:function name="r:link-ends" as="xs:string">
    <xsl:param name="link" as="element()"/>
    <xsl:param name="side" as="xs:string"/>
    <xsl:variable name="end" select="$link/End/Properties/Property[@name = 'End']"/>
    <xsl:sequence select="concat(string(($end[../../@side = 'source']/@*[local-name() = $side])[1]),
                                 if ($link/@kind = 'Generalization') then ' ▷ ' else ' — ',
                                 string(($end[../../@side = 'target']/@*[local-name() = $side])[1]))"/>
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

  <!-- #199 a word-break opportunity between the words of a long name (FooBarBaz → Foo|Bar|Baz) -->
  <xsl:template name="r:wbr">
    <xsl:param name="s" as="xs:string"/>
    <xsl:analyze-string select="$s" regex="([a-z0-9])([A-Z])">
      <xsl:matching-substring><xsl:value-of select="regex-group(1)"/><wbr/><xsl:value-of select="regex-group(2)"/></xsl:matching-substring>
      <xsl:non-matching-substring><xsl:value-of select="."/></xsl:non-matching-substring>
    </xsl:analyze-string>
  </xsl:template>

  <xsl:template name="r:cell-text">
    <xsl:param name="s" as="xs:string"/>
    <xsl:param name="wbr" as="xs:boolean"/>
    <xsl:choose>
      <xsl:when test="$wbr"><xsl:call-template name="r:wbr"><xsl:with-param name="s" select="$s"/></xsl:call-template></xsl:when>
      <xsl:otherwise><xsl:value-of select="$s"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- #199 true when every property and tag of an element is unchanged (so a full report shows its values) -->
  <xsl:function name="r:unchanged" as="xs:boolean">
    <xsl:param name="e" as="element()"/>
    <xsl:sequence select="$full and string($e/@status) = 'identical'"/>
  </xsl:function>

  <!-- property | baseline | destination table.
       #199: one rule everywhere. A side that does not exist is hatched (void) and
       the table keeps both columns; rows empty on both sides are left out; an
       unchanged element in a full report shows one Value column; with
       always = true an empty table still shows its header and "no changes". -->
  <xsl:template name="r:prop-table">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="only-changed" as="xs:boolean" select="true()"/>
    <xsl:param name="skip" as="xs:string*" select="()"/>
    <xsl:param name="one-sided" as="xs:string?" select="()"/>
    <xsl:param name="sides" as="xs:string*" select="('Baseline', 'Destination')"/>
    <xsl:param name="always" as="xs:boolean" select="false()"/>
    <xsl:param name="values" as="xs:boolean" select="false()"/>
    <xsl:param name="wbr" as="xs:boolean" select="false()"/>
    <xsl:variable name="rows" select="$e/Properties/Property[(not($only-changed) or @status != 'identical') and not(@name = $skip)
        and (string(@baseline) != '' or string(@model) != '')
        and (not($one-sided) or string(if ($one-sided = 'deleted') then @baseline else @model) != '')]"/>
    <xsl:choose>
      <xsl:when test="$values">
        <div class="tw">
          <table class="t one-col">
            <thead><tr><th></th><th>Value</th></tr></thead>
            <tbody>
              <xsl:for-each select="$rows">
                <tr data-status="identical"><th class="k"><xsl:value-of select="@name"/></th>
                  <td><xsl:call-template name="r:cell-text"><xsl:with-param name="s" select="string((@model[. != ''], @baseline)[1])"/><xsl:with-param name="wbr" select="$wbr"/></xsl:call-template></td></tr>
              </xsl:for-each>
              <xsl:if test="not($rows)"><tr><td colspan="2" class="none-cell"><i>none</i></td></tr></xsl:if>
            </tbody>
          </table>
        </div>
      </xsl:when>
      <xsl:when test="$rows or $always">
        <div class="tw">
          <table class="t">
            <thead><tr><th></th><th><xsl:value-of select="$sides[1]"/></th><th><xsl:value-of select="$sides[2]"/></th></tr></thead>
            <tbody>
              <xsl:for-each select="$rows">
                <tr data-status="{@status}">
                  <th class="k"><xsl:value-of select="@name"/></th>
                  <xsl:choose>
                    <xsl:when test="$one-sided = 'deleted' or @status = 'deleted'"><td><xsl:call-template name="r:cell-text"><xsl:with-param name="s" select="string(@baseline)"/><xsl:with-param name="wbr" select="$wbr"/></xsl:call-template></td><td class="void"></td></xsl:when>
                    <xsl:when test="$one-sided = 'added' or @status = 'added'"><td class="void"></td><td><xsl:call-template name="r:cell-text"><xsl:with-param name="s" select="string(@model)"/><xsl:with-param name="wbr" select="$wbr"/></xsl:call-template></td></xsl:when>
                    <xsl:otherwise>
                      <!-- a value cleared or filled in is a change; the empty side says so (#107) -->
                      <xsl:variable name="empty" select="@status = 'changed' and (string(@baseline) = '' or string(@model) = '')"/>
                      <xsl:choose>
                        <xsl:when test="$empty and string(@baseline) = ''"><td class="nil">(empty)</td></xsl:when>
                        <xsl:otherwise><td><xsl:call-template name="r:cell-text"><xsl:with-param name="s" select="string(@baseline)"/><xsl:with-param name="wbr" select="$wbr"/></xsl:call-template></td></xsl:otherwise>
                      </xsl:choose>
                      <xsl:choose>
                        <xsl:when test="$empty and string(@model) = ''"><td class="nil">(empty)</td></xsl:when>
                        <xsl:when test="$empty"><td><xsl:value-of select="@model"/></td></xsl:when>
                        <xsl:otherwise><td><xsl:choose>
                          <xsl:when test="@status = 'changed' and Redline"><xsl:copy-of select="Redline/node()"/></xsl:when>
                          <xsl:otherwise><xsl:call-template name="r:cell-text"><xsl:with-param name="s" select="string(@model)"/><xsl:with-param name="wbr" select="$wbr"/></xsl:call-template></xsl:otherwise>
                        </xsl:choose><xsl:if test="@tag"><span class="notation"><xsl:value-of select="@tag"/></span></xsl:if></td></xsl:otherwise>
                      </xsl:choose>
                    </xsl:otherwise>
                  </xsl:choose>
                </tr>
              </xsl:for-each>
              <xsl:if test="not($rows)"><tr><td colspan="3" class="none-cell"><i><xsl:value-of select="if ($only-changed) then 'no changes' else 'none'"/></i></td></tr></xsl:if>
            </tbody>
          </table>
        </div>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- #169: an element's user-defined tagged values: tag, baseline value, destination value.
       A tag only on one side shows "(no tag)" on the other; an empty value shows "(empty)".
       #199: always and values as for r:prop-table. -->
  <xsl:template name="r:tag-table">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="only-changed" as="xs:boolean" select="true()"/>
    <xsl:param name="always" as="xs:boolean" select="false()"/>
    <xsl:param name="values" as="xs:boolean" select="false()"/>
    <xsl:variable name="rows" select="$e/Tags/Tag[not($only-changed) or @status != 'identical']"/>
    <xsl:choose>
      <xsl:when test="$values">
        <div class="tw">
          <table class="t tags one-col">
            <thead><tr><th></th><th>Value</th></tr></thead>
            <tbody>
              <xsl:for-each select="$rows"><tr data-status="identical"><th class="k"><xsl:value-of select="@name"/></th><td><xsl:value-of select="(@model, @baseline)[1]"/></td></tr></xsl:for-each>
              <xsl:if test="not($rows)"><tr><td colspan="2" class="none-cell"><i>none</i></td></tr></xsl:if>
            </tbody>
          </table>
        </div>
      </xsl:when>
      <xsl:when test="$rows or $always">
        <div class="tw">
          <table class="t tags">
            <thead><tr><th></th><th>Baseline</th><th>Destination</th></tr></thead>
            <tbody>
              <xsl:for-each select="$rows">
                <tr data-status="{@status}">
                  <th class="k"><xsl:value-of select="@name"/></th>
                  <xsl:choose>
                    <xsl:when test="@status = 'added'"><td class="nil">(no tag)</td></xsl:when>
                    <xsl:when test="string(@baseline) = ''"><td class="nil">(empty)</td></xsl:when>
                    <xsl:otherwise><td><xsl:value-of select="@baseline"/></td></xsl:otherwise>
                  </xsl:choose>
                  <xsl:choose>
                    <xsl:when test="@status = 'deleted'"><td class="nil">(no tag)</td></xsl:when>
                    <xsl:when test="string(@model) = ''"><td class="nil">(empty)</td></xsl:when>
                    <xsl:otherwise><td><xsl:value-of select="@model"/></td></xsl:otherwise>
                  </xsl:choose>
                </tr>
              </xsl:for-each>
              <xsl:if test="not($rows)"><tr><td colspan="3" class="none-cell"><i><xsl:value-of select="if ($only-changed) then 'no changes' else 'none'"/></i></td></tr></xsl:if>
            </tbody>
          </table>
        </div>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- #169: a "Tagged values" section (packages and classes); #199 hidden in hover mode, where the ⓘ shows it -->
  <xsl:template name="r:tag-sec">
    <xsl:param name="e" as="element()"/>
    <xsl:variable name="t"><xsl:call-template name="r:tag-table"><xsl:with-param name="e" select="$e"/><xsl:with-param name="only-changed" select="not($full) and not($e/@status = ('added', 'deleted'))"/><xsl:with-param name="values" select="r:unchanged($e) and exists($e/Tags/Tag)"/></xsl:call-template></xsl:variable>
    <xsl:if test="$t/*"><div class="sec hov-hide"><div class="sec-h"><span class="ico"><xsl:copy-of select="$tag-ico"/></span>Tagged values</div><xsl:copy-of select="$t"/></div></xsl:if>
  </xsl:template>

  <!-- #199 the kind of element, as headings name it: Package, Class, Diagram, Attribute -->
  <xsl:function name="r:kind" as="xs:string">
    <xsl:param name="e" as="element()"/>
    <xsl:sequence select="local-name($e)"/>
  </xsl:function>

  <!-- #199 one notes box, the same on the page and in the ⓘ pop-up, named after the
       comparison XML's Notes field: "‹Kind› notes", "(destination, redlined)" only when the
       notes changed, "(baseline)" for a removed element. -->
  <xsl:template name="r:notes-box">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="cls" as="xs:string"/>
    <xsl:variable name="n" select="$e/Notes"/>
    <xsl:variable name="st" select="string($e/@status)"/>
    <div class="desc one {$cls}"><div>
      <div class="lbl"><xsl:value-of select="r:kind($e)"/> notes<xsl:value-of select="if ($n/@status = 'changed') then ' (destination, redlined)' else if ($st = 'deleted') then ' (baseline)' else ''"/></div>
      <xsl:choose>
        <xsl:when test="$n/@status = 'changed'"><xsl:copy-of select="$n/Redline/node()"/></xsl:when>
        <xsl:when test="$st = 'deleted' and string($n/Baseline) != ''"><xsl:value-of select="$n/Baseline"/></xsl:when>
        <xsl:when test="string($n/Destination) != ''"><xsl:value-of select="$n/Destination"/></xsl:when>
        <xsl:when test="string($n/Baseline) != ''"><xsl:value-of select="$n/Baseline"/></xsl:when>
        <xsl:otherwise><i class="nodesc">No notes defined in the model.</i></xsl:otherwise>
      </xsl:choose>
    </div></div>
  </xsl:template>

  <!-- the notes on the page: shown in inline mode and in print, hidden in hover mode -->
  <xsl:template name="r:notes-block">
    <xsl:param name="e" as="element()"/>
    <xsl:param name="cls" as="xs:string" select="'sec'"/>
    <div class="{$cls}"><xsl:call-template name="r:notes-box"><xsl:with-param name="e" select="$e"/><xsl:with-param name="cls" select="'plain'"/></xsl:call-template></div>
  </xsl:template>

  <!-- hidden copy of the ⓘ pop-up: notes, "‹Kind› metadata — changes" and (not for
       diagrams) "Tagged values — changes"; an unchanged element in a full report shows
       "‹Kind› metadata" and "Tagged values" with its values instead (#199) -->
  <xsl:template name="r:notes-src">
    <xsl:param name="e" as="element()"/>
    <xsl:variable name="st" select="string($e/@status)"/>
    <xsl:variable name="one" select="if ($st = ('added', 'deleted')) then $st else ()"/>
    <xsl:variable name="vals" select="r:unchanged($e)"/>
    <div class="notes-src">
      <xsl:call-template name="r:notes-box"><xsl:with-param name="e" select="$e"/><xsl:with-param name="cls" select="'pdesc'"/></xsl:call-template>
      <div class="lbl sp"><xsl:value-of select="r:kind($e)"/> metadata<xsl:if test="not($full)"> — changes</xsl:if></div>
      <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="$e"/><xsl:with-param name="only-changed" select="not($full) and not($one)"/><xsl:with-param name="skip" select="'Notes'"/><xsl:with-param name="one-sided" select="$one"/><xsl:with-param name="always" select="true()"/><xsl:with-param name="values" select="$vals"/></xsl:call-template>
      <xsl:if test="not($e/self::Diagram)">
        <div class="lbl sp">Tagged values<xsl:if test="not($full)"> — changes</xsl:if></div>
        <xsl:call-template name="r:tag-table"><xsl:with-param name="e" select="$e"/><xsl:with-param name="only-changed" select="not($full) and not($one)"/><xsl:with-param name="always" select="true()"/><xsl:with-param name="values" select="$vals"/></xsl:call-template>
      </xsl:if>
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
      <xsl:choose>
        <xsl:when test="$ico = '📁'"><span class="ico"><span class="fold" aria-hidden="true"></span></span></xsl:when>
        <xsl:otherwise><span class="ico"><xsl:value-of select="$ico"/></span></xsl:otherwise>
      </xsl:choose>
      <span class="name"><xsl:call-template name="r:display-name"><xsl:with-param name="e" select="$e"/></xsl:call-template></span>
      <!-- all stereotypes in one pair of guillemets, as UML 2.5.1 shows them (#118); «deprecated» is shown with the name -->
      <xsl:variable name="stereos" select="tokenize(string($e/@stereotype), ',\s*')[normalize-space() != '' and . != 'deprecated']"/>
      <xsl:if test="exists($stereos)"><span class="stereo">«<xsl:value-of select="$stereos" separator=", "/>»</span></xsl:if>
      <xsl:if test="$e/Properties/Property[@name = 'Notes']">
        <!-- #123/#199: the ⓘ holds a change the page does not show in hover mode (notes, metadata, tagged values) -->
        <xsl:variable name="chg" select="($st = 'changed' or $st = 'moved') and (r:pstatus($e, 'Notes') = 'changed'
            or exists($e/Properties/Property[@status != 'identical' and @name != 'Notes']) or exists($e/Tags/Tag[@status != 'identical']))"/>
        <span class="info{if ($chg) then ' info-chg' else ''}" data-id="{$e/@id}">i</span>
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
          <xsl:if test="$logo != ''"><a class="brand" href="https://cim-compare.ucaiug.io" target="_blank" rel="noopener" title="cim-compare website" aria-label="cim-compare website"><xsl:value-of select="$logo" disable-output-escaping="yes"/></a></xsl:if>
          <div>
            <div class="title">CIM Model Comparison <span class="arrow">·</span>
              <span class="ver"><xsl:value-of select="@baselineVersion"/></span><xsl:text> </xsl:text><span class="lbl">baseline</span>
              <xsl:text> </xsl:text><span class="arrow">→</span><xsl:text> </xsl:text>
              <span class="ver"><xsl:value-of select="@destinationVersion"/></span><xsl:text> </xsl:text><span class="lbl">destination</span></div>
            <div class="sub">Scope: <xsl:value-of select="@scope"/> · compared <xsl:value-of select="@comparedOn"/> · <a class="gen" href="{@releaseUrl}" target="_blank" rel="noopener" title="{@generator} release"><xsl:value-of select="@generator"/></a><xsl:if test="$image-note != ''"> · <span class="imgnote"><xsl:value-of select="$image-note"/></span></xsl:if><xsl:if test="$full"> · full (identical items included)</xsl:if></div>
          </div>
          <div class="tools">
            <div class="search"><input id="q" type="search" placeholder="Find class, package, diagram or attribute…  (/)" autocomplete="off"/><div id="q-results" class="results"></div></div>
            <button type="button" onclick="expandAll()">Expand all</button>
            <button type="button" onclick="collapseAll()">Collapse all</button>
            <label class="chk chk-rail"><input id="chk-rail" type="checkbox" checked="checked"/> Outline <kbd>o</kbd></label>
            <label class="chk"><input id="chk-guids" type="checkbox" checked="checked"/> GUIDs <kbd>g</kbd></label>
            <label class="chk"><input id="chk-clean" type="checkbox"/> Hide redline</label>
            <xsl:if test="$diagrams"><label class="chk" title="Highlight added, removed, changed, moved and restyled elements on the diagram images"><input id="chk-hl" type="checkbox" checked="checked"/> Diagram highlights</label></xsl:if>
            <span class="seg" id="seg-meta" title="How ⓘ metadata is shown"><button type="button" class="on" data-mode="hover"><svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true"><rect x="1.5" y="1.5" width="13" height="9" rx="2" fill="none" stroke="currentColor" stroke-width="1.4"/><path d="M5 10.5 4 14l4-3.5" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/><path d="M4.5 5h7M4.5 7.5h4.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>hover</button><button type="button" data-mode="inline"><svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true"><path d="M1.5 3h13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><rect x="3.5" y="5.5" width="11" height="5" rx="1" fill="none" stroke="currentColor" stroke-width="1.3"/><path d="M1.5 13h13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>inline</button></span>
            <!-- #131 print or save as PDF, with the browser settings that give a good result -->
            <span class="pbwrap"><button type="button" id="btn-print" aria-describedby="pb-help"><svg width="15" height="15" viewBox="0 0 16 16" aria-hidden="true"><path d="M4 6V1.5h8V6" fill="none" stroke="currentColor" stroke-width="1.3"/><rect x="1.5" y="6" width="13" height="6" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.3"/><rect x="4" y="9.5" width="8" height="5" fill="none" stroke="currentColor" stroke-width="1.3"/><circle cx="12" cy="8.3" r=".8" fill="currentColor"/></svg>Print / PDF</button><span class="pbpop" id="pb-help" role="tooltip"><b>Print or save as PDF</b><span>Opens your browser’s print dialog (or press Ctrl+P, ⌘P on a Mac).</span><span>• To save a PDF, choose <i>Save as PDF</i> (or <i>Microsoft Print to PDF</i>) as the printer.</span><span>• Choose <i>Landscape</i> if the browser doesn’t pick it.</span><span>• Turn off <i>Headers and footers</i> for a cleaner page.</span><span>• Leave <i>Background graphics</i> on so the change colours print.</span><span class="pbn">Wait for diagram images to load in large reports.</span></span></span>
          </div>
        </header>
        <div class="wrap">
          <div class="summary">
            <xsl:call-template name="r:summary-group"><xsl:with-param name="kind" select="'package'"/><xsl:with-param name="label" select="'Packages'"/></xsl:call-template>
            <xsl:call-template name="r:summary-group"><xsl:with-param name="kind" select="'class'"/><xsl:with-param name="label" select="'Classes'"/></xsl:call-template>
            <xsl:if test="$diagrams">
              <xsl:call-template name="r:summary-group"><xsl:with-param name="kind" select="'diagram'"/><xsl:with-param name="label" select="'Diagrams'"/></xsl:call-template>
            </xsl:if>
            <span id="flt-status" class="clear">Click a count to show only those items</span>
            <!-- #175: keep the counts, or the counts and the legend, in view while scrolling -->
            <span class="pinseg" role="group" aria-label="Keep in view while scrolling" title="Keep in view while scrolling (p)">
              <button type="button" data-pin="off"><svg width="13" height="13" viewBox="0 0 16 16" aria-hidden="true"><path d="M6 1.5h4l-.6 4.2 2.6 2.3v1.2H4V8l2.6-2.3z" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/><path d="M8 9.2V14.5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>Off</button><button type="button" data-pin="counts" class="on">Counts</button><button type="button" data-pin="all">Counts + legend</button>
            </span><kbd class="pin-kbd">p</kbd>
          </div>
          <div class="legend">
            <span><i class="sw s-added"></i><b>Added</b> — only in destination</span>
            <span><i class="sw s-deleted"></i><b>Deleted</b> — only in baseline</span>
            <span><i class="sw s-moved"></i><b>Moved</b> — same GUID, different package</span>
            <span><i class="sw s-changed"></i><b>Changed</b> — same GUID and package; properties or members differ</span>
            <span><i class="sw s-identical"></i><b>Identical</b></span>
            <span><i class="sw s-pkg"></i><b>Package</b> (unchanged itself)</span>
            <span><del>struck</del><ins>inserted</ins> — destination text with baseline wording struck (notes, role notes, role names, multiplicities); the <xsl:copy-of select="$ii"/>shows the notes and every changed property</span>
            <span><b class="chg">Blue</b> — a changed attribute type (the new type)</span>
          </div>
          <!-- #126: what was left out, as the last line under the legend -->
          <xsl:if test="@emptyValuesNotShown">
            <div class="empty-note"><xsl:copy-of select="$ii"/><xsl:value-of select="@emptyValuesNotShown"/><xsl:value-of select="if (@emptyValuesNotShown = '1') then ' difference is' else ' differences are'"/> not shown where one side is empty in <span class="tipwrap" tabindex="0">a metadata field EA fills in automatically<span class="tip"><b>Metadata fields affected</b><span class="tl">Complexity, Phase, Version, GenType: any value</span><span class="tl">Containment: “Not Specified” / “Unspecified”</span><span class="tl">IsLiteral, Derived, Const, Static: false</span><span class="tip-n">Any other difference is always shown.</span></span></span>; they do not change the model’s definitions. Run with <code>--show-empty-values</code> to include them.</div>
          </xsl:if>
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
        <div id="lightbox" class="lightbox"></div>
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
            <span class="cnt s-{$s}" data-kind="{$kind}" data-status="{$s}" title="show only {$s} {if ($kind = 'class') then 'classes' else concat($kind, 's')}"><xsl:value-of select="$c/@n"/><xsl:text> </xsl:text><xsl:value-of select="$s"/></span>
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
        <xsl:call-template name="r:notes-block"><xsl:with-param name="e" select="."/><xsl:with-param name="cls" select="'sec pkg-desc'"/></xsl:call-template>
        <xsl:call-template name="r:tag-sec"><xsl:with-param name="e" select="."/></xsl:call-template>
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
        <xsl:call-template name="r:class-detail"/>
      </div>
    </div>
  </xsl:template>

  <!-- #199 hover mode hides the notes, Metadata and Tagged values sections (the ⓘ shows
       them). An element with nothing else to show says so instead of opening blank. -->
  <xsl:template name="r:hover-note">
    <xsl:param name="e" as="element()"/>
    <div class="sec hov-only"><div class="hov-note"><xsl:value-of select="r:kind($e)"/> '<xsl:value-of select="$e/@name"/>' <xsl:value-of select="if (r:unchanged($e)) then 'is unchanged' else 'has no other changes'"/>. The <xsl:copy-of select="$ii"/> shows its notes and <xsl:value-of select="if (r:unchanged($e)) then 'metadata' else 'every changed property'"/>.</div></div>
  </xsl:template>

  <!-- detail of a class: Notes → Metadata → Tagged values → Attributes → Links.
       #199 an added or removed class is laid out like a changed one. -->
  <xsl:template name="r:class-detail">
    <xsl:variable name="st" select="string(@status)"/>
    <xsl:variable name="one" select="if ($st = ('added', 'deleted')) then $st else ()"/>
    <xsl:variable name="meta">
      <xsl:call-template name="r:prop-table">
        <xsl:with-param name="e" select="."/>
        <xsl:with-param name="only-changed" select="not($full) and not($one)"/>
        <xsl:with-param name="skip" select="'Notes'"/>
        <xsl:with-param name="one-sided" select="$one"/>
        <xsl:with-param name="values" select="r:unchanged(.)"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="tags" select="Tags/Tag[$full or $one or @status != 'identical']"/>
    <xsl:call-template name="r:notes-block"><xsl:with-param name="e" select="."/></xsl:call-template>
    <xsl:if test="$meta/*"><div class="sec hov-hide"><div class="sec-h"><span class="ico">▤</span>Metadata</div><xsl:copy-of select="$meta"/></div></xsl:if>
    <xsl:call-template name="r:tag-sec"><xsl:with-param name="e" select="."/></xsl:call-template>
    <xsl:variable name="attrs" select="Attribute[$full or $one or @status != 'identical']"/>
    <xsl:if test="$attrs">
      <div class="sec"><div class="sec-h"><span class="ico">≡</span>Attributes</div>
        <div class="tw"><table class="t attrsB attrs">
          <thead><tr><th>Attribute</th><th>Baseline</th><th>Destination</th><th><xsl:value-of select="if ($st = 'deleted') then 'Notes (baseline)' else 'Notes (destination, redlined)'"/></th></tr></thead>
          <tbody>
            <xsl:for-each select="$attrs">
              <xsl:sort select="if ($one) then lower-case(@name) else ''"/>
              <xsl:apply-templates select="." mode="row"/>
            </xsl:for-each>
          </tbody>
        </table></div>
      </div>
    </xsl:if>
    <xsl:variable name="links" select="Link[$full or $one or @status != 'identical']"/>
    <xsl:if test="$links">
      <div class="sec"><div class="sec-h"><span class="ico">⇄</span>Links</div>
        <xsl:for-each select="$links">
          <xsl:sort select="index-of($link-order, string(@kind))[1]" data-type="number"/>
          <xsl:apply-templates select="." mode="card"/>
        </xsl:for-each>
      </div>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="$attrs or $links"/>
      <xsl:when test="$meta/* or $tags or Notes/@status = 'changed'">
        <xsl:call-template name="r:hover-note"><xsl:with-param name="e" select="."/></xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <div class="empty">Class '<xsl:value-of select="@name"/>' has no changes to its properties or links.</div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====================================================== attributes (variant B rows) -->

  <!-- attribute properties an Attributes row shows itself (name, type, multiplicity, notes) -->
  <xsl:variable name="row-shown" as="xs:string*" select="('Name', 'Type', 'LowerBound', 'UpperBound', 'Notes')"/>

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
        <!-- #123: a change the row does not show (e.g. Default, Const, Stereotype) marks the ⓘ and adds a note -->
        <xsl:variable name="hidden" select="$st = 'changed' and (exists(Properties/Property[@status != 'identical' and not(@name = $row-shown)]) or exists(Tags/Tag[@status != 'identical']))"/>
        <span class="info{if ($hidden) then ' info-chg' else ''}" data-id="{@guid}">i</span>
        <xsl:if test="$hidden"><span class="mnote"><span class="m-hover">metadata changed - hover<xsl:copy-of select="$ii"/></span><span class="m-inline">metadata changed - see below</span></span></xsl:if>
        <xsl:if test="@renamedFrom"><span class="renamed-from">renamed from <xsl:value-of select="@renamedFrom"/></span></xsl:if>
        <span class="guid"><xsl:value-of select="@guid"/></span>
      </td>
      <xsl:choose>
        <xsl:when test="$st = 'added'"><td class="nx">did not exist</td></xsl:when>
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
      <xsl:choose>
        <!-- #199 the Notes column holds the destination's notes: a removed attribute's cell is
             hatched, unless the whole class was removed (then the column is the baseline's) -->
        <xsl:when test="$st = 'deleted' and ../@status != 'deleted'"><td class="nt hatch"></td></xsl:when>
        <xsl:otherwise>
          <td class="nt">
            <xsl:choose>
              <xsl:when test="$st = 'deleted'"><xsl:value-of select="r:prop(., 'Notes', 'baseline')"/></xsl:when>
              <xsl:when test="$st = 'added' and r:prop(., 'Notes', 'model') != ''"><ins><xsl:value-of select="r:prop(., 'Notes', 'model')"/></ins></xsl:when>
              <xsl:when test="r:pstatus(., 'Notes') = 'changed'"><xsl:call-template name="r:text-cell"><xsl:with-param name="e" select="."/><xsl:with-param name="name" select="'Notes'"/><xsl:with-param name="side" select="'model'"/></xsl:call-template></xsl:when>
              <xsl:otherwise><xsl:value-of select="if (r:prop(., 'Notes', 'model') != '') then r:prop(., 'Notes', 'model') else r:prop(., 'Notes', 'baseline')"/></xsl:otherwise>
            </xsl:choose>
          </td>
        </xsl:otherwise>
      </xsl:choose>
    </tr>
    <!-- the ⓘ pop-up (and, in inline mode, the row below): notes, then "Attribute metadata —
         changes" and "Tagged values — changes"; an unchanged attribute in a full report shows
         its values (#199) -->
    <xsl:variable name="one" select="if ($st = ('added', 'deleted')) then $st else ()"/>
    <xsl:variable name="vals" select="r:unchanged(.)"/>
    <tr class="meta"><td colspan="4">
      <xsl:call-template name="r:notes-box"><xsl:with-param name="e" select="."/><xsl:with-param name="cls" select="'pdesc'"/></xsl:call-template>
      <div class="lbl sp">Attribute metadata<xsl:if test="not($full)"> — changes</xsl:if></div>
      <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full) and not($one)"/><xsl:with-param name="skip" select="'Notes'"/><xsl:with-param name="one-sided" select="$one"/><xsl:with-param name="always" select="true()"/><xsl:with-param name="values" select="$vals"/></xsl:call-template>
      <div class="lbl sp">Tagged values<xsl:if test="not($full)"> — changes</xsl:if></div>
      <xsl:call-template name="r:tag-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full) and not($one)"/><xsl:with-param name="always" select="true()"/><xsl:with-param name="values" select="$vals"/></xsl:call-template>
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
      <xsl:text>&#160;</xsl:text><span class="cardw"><span class="card">[<xsl:choose>
        <xsl:when test="$rl"><xsl:call-template name="r:token"><xsl:with-param name="e" select="$end"/><xsl:with-param name="name" select="'Cardinality'"/><xsl:with-param name="side" select="'model'"/></xsl:call-template></xsl:when>
        <xsl:otherwise><xsl:value-of select="r:prop($end, 'Cardinality', $side)"/></xsl:otherwise>
      </xsl:choose>]</span><xsl:if test="$rl and $end/Properties/Property[@name = 'Cardinality']/@tag"><span class="notation"><xsl:value-of select="$end/Properties/Property[@name = 'Cardinality']/@tag"/></span></xsl:if></span><xsl:if test="position() = 1"><xsl:text>&#160;→</xsl:text></xsl:if></span></span>
      <xsl:if test="position() = 1"><xsl:text> </xsl:text></xsl:if>
    </xsl:for-each>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="Link" mode="card">
    <xsl:variable name="st" select="string(@status)"/>
    <xsl:variable name="src" select="End[@side = 'source']"/>
    <xsl:variable name="dst" select="End[@side = 'target']"/>
    <div class="linkB" data-status="{$st}" data-link="{@kind}" data-guid="{@guid}" data-ends="{r:link-ends(., if ($st = 'deleted') then 'baseline' else 'model')}">
      <div class="link-h"><span class="badge" data-status="{$st}"><xsl:value-of select="$badge($st)"/></span><xsl:value-of select="@kind"/><span class="guid"><xsl:value-of select="@guid"/></span></div>
      <div class="sigline">
        <div><span class="lbl">Baseline</span>
          <xsl:choose>
            <xsl:when test="$st = 'added'"><span class="nx">did not exist</span></xsl:when>
            <xsl:otherwise><xsl:call-template name="r:sig-compact"><xsl:with-param name="link" select="."/><xsl:with-param name="side" select="'baseline'"/></xsl:call-template></xsl:otherwise>
          </xsl:choose>
        </div>
        <div class="mid">→</div>
        <div><span class="lbl">Destination</span>
          <xsl:choose>
            <xsl:when test="$st = 'deleted'"><span class="gone">removed from model</span></xsl:when>
            <xsl:otherwise><xsl:call-template name="r:sig-compact"><xsl:with-param name="link" select="."/><xsl:with-param name="side" select="'model'"/><xsl:with-param name="redline" select="$st = 'changed'"/></xsl:call-template></xsl:otherwise>
          </xsl:choose>
        </div>
      </div>
      <xsl:variable name="one" select="if ($st = ('added', 'deleted')) then $st else ()"/>
      <xsl:variable name="meta"><xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full) and not($one)"/><xsl:with-param name="one-sided" select="$one"/><xsl:with-param name="values" select="r:unchanged(.)"/><xsl:with-param name="wbr" select="true()"/></xsl:call-template></xsl:variable>
      <xsl:variable name="ltags"><xsl:call-template name="r:tag-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full) and not($one)"/></xsl:call-template></xsl:variable>
      <xsl:if test="$meta/* or End or $ltags/*">
        <div class="rows">
          <xsl:if test="$meta/*"><div class="sub">Link metadata</div><xsl:copy-of select="$meta"/></xsl:if>
          <xsl:if test="$ltags/*"><div class="sub">Tagged values</div><xsl:copy-of select="$ltags"/></xsl:if>
          <xsl:if test="$one and End">
            <div class="endsB">
              <xsl:for-each select="End">
                <div><div class="sub"><xsl:value-of select="if (@side = 'source') then 'Source' else 'Target'"/> end</div>
                  <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="one-sided" select="$one"/><xsl:with-param name="wbr" select="true()"/></xsl:call-template>
                  <xsl:if test="Tags/Tag"><div class="sub">Tagged values</div><xsl:call-template name="r:tag-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/></xsl:call-template></xsl:if>
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
                    <xsl:when test="Properties/Property[@status != 'identical'] or Tags/Tag[@status != 'identical']">
                      <div class="sub"><xsl:value-of select="$which"/> end — <span class="nm-case"><xsl:value-of select="(r:prop(., $rolep, 'model'), r:prop(., 'End', 'model'))[. != ''][1]"/></span></div>
                      <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full)"/><xsl:with-param name="wbr" select="true()"/></xsl:call-template>
                      <xsl:variable name="etags"><xsl:call-template name="r:tag-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="not($full)"/></xsl:call-template></xsl:variable>
                      <xsl:if test="$etags/*"><div class="sub">Tagged values</div><xsl:copy-of select="$etags"/></xsl:if>
                    </xsl:when>
                    <!-- #199 an unchanged link in a full report shows its ends' values, like its metadata -->
                    <xsl:when test="r:unchanged(..)">
                      <div class="sub"><xsl:value-of select="$which"/> end</div>
                      <xsl:call-template name="r:prop-table"><xsl:with-param name="e" select="."/><xsl:with-param name="only-changed" select="false()"/><xsl:with-param name="values" select="true()"/><xsl:with-param name="wbr" select="true()"/></xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                      <div class="sub"><xsl:value-of select="$which"/> end</div><div class="none">no changes</div>
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
    <xsl:variable name="one" select="if ($st = ('added', 'deleted')) then $st else ()"/>
    <xsl:variable name="meta">
      <xsl:call-template name="r:prop-table">
        <xsl:with-param name="e" select="."/>
        <xsl:with-param name="only-changed" select="not($full) and not($one)"/>
        <xsl:with-param name="skip" select="'Notes'"/>
        <xsl:with-param name="one-sided" select="$one"/>
        <xsl:with-param name="values" select="r:unchanged(.)"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="images" select="$st = ('added', 'deleted') or Highlight or Connector"/>
    <div class="node dia closed" id="{@id}" data-depth="{$depth}">
      <xsl:call-template name="r:row"><xsl:with-param name="e" select="."/><xsl:with-param name="ico" select="'▦'"/></xsl:call-template>
      <div class="detail">
        <xsl:call-template name="r:notes-block"><xsl:with-param name="e" select="."/></xsl:call-template>
        <xsl:if test="$meta/*"><div class="sec hov-hide"><div class="sec-h"><span class="ico">▤</span>Metadata</div><xsl:copy-of select="$meta"/></div></xsl:if>
        <xsl:choose>
          <xsl:when test="$images"/>
          <xsl:when test="$st = 'identical' and not($meta/*)"><div class="empty">Diagram '<xsl:value-of select="@name"/>' has no changes.</div></xsl:when>
          <xsl:otherwise><xsl:call-template name="r:hover-note"><xsl:with-param name="e" select="."/></xsl:call-template></xsl:otherwise>
        </xsl:choose>
        <!-- #37: the images are shown only when there is something on them to point
             at (a highlight or a changed connector), or when the diagram was added or
             removed. A diagram whose only changes are properties (name, notes,
             modified date) shows its metadata alone. -->
        <xsl:if test="$images">
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
                  <xsl:when test="$st = 'added'"><div class="nx">Diagram did not exist in the baseline model.</div></xsl:when>
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
                <div class="cx-h">Connectors <span class="cx-n"><xsl:value-of select="string-join(for $k in ('added', 'removed', 'rerouted', 'labels moved', 'labels changed', 'restyled') return (if ($cx[@kind = $k]) then concat(count($cx[@kind = $k]), ' ', $k) else ()), ' · ')"/></span>
                  <span class="cx-hint">hover a connector to outline the elements it joins</span></div>
                <xsl:for-each select="$cx">
                  <xsl:sort select="index-of(('added', 'removed', 'rerouted', 'labels moved', 'labels changed', 'restyled'), @kind)[1]" data-type="number"/>
                  <xsl:sort select="@name"/>
                  <div class="cx-row" tabindex="0" data-side="{@side}" data-ends="{@ends}"><span class="cx-k cx-{translate(@kind, ' ', '-')}"><xsl:value-of select="@kind"/></span><span class="cx-name"><xsl:value-of select="@name"/></span><xsl:if test="@what"><span class="cx-what"><xsl:value-of select="@what"/></span></xsl:if></div>
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
      <!-- #139 a diagram EA would not draw as SVG is a PNG, shown without highlights;
           #139/#141 one EA could not draw at all has no image -->
      <xsl:variable name="ext" select="(@*[local-name() = concat($side, '-ext')], $image-type)[1]"/>
      <xsl:choose>
        <xsl:when test="$ext = 'none'"><div class="dnone">No image: EA could not draw this diagram<xsl:if test="lower-case($image-type) = 'svg'"> as SVG or PNG</xsl:if>.</div></xsl:when>
        <xsl:otherwise>
          <img loading="lazy" src="Images-{$side}/{@eaid}.{$ext}" alt="{@name} ({$side})"/>
          <xsl:if test="$ext != $image-type"><div class="dnote">EA could not draw this diagram as SVG; it is shown as a PNG, without highlights.</div></xsl:if>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:for-each select="Highlight[@side = $side]">
        <span class="hl hl-{@kind}" data-box="{@box}" title="{if (@name != '') then concat(@name, ': ') else ''}{if (@kind = 'moved') then 'moved or resized' else @kind}"></span>
      </xsl:for-each>
    </div>
  </xsl:template>

</xsl:stylesheet>
