<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" />

    <!-- Receives an IEC package if one is specified on the command line.  Will be used
    to  -->
    <xsl:param name="iecPackage" />

    <!-- THE following variable declaration handles pre-processing of what would be costly (and repetitive) XSLT calls to obtain class  
         information such as GUID and color.  By building out a simple formatted string the XSLT processing speed is greatly improved. 
         The format of the string is:      |name:guid:color|...|name:guid:color|   
         with the pipe (|) serving as the delimiter between classes.   -->
    <xsl:variable name="all-classes">
        <xsl:for-each select="/.//CompareItem[@type='Class']">
        	<xsl:value-of select="concat('|', @name, ':', @guid, ':')" />
            <xsl:choose>
                <xsl:when test="@status='Baseline only'">
                    <xsl:text>#F5B7B1</xsl:text>
                </xsl:when>
                <xsl:when test="@status='Model only'">
                    <xsl:text>#ABEBC6</xsl:text>
                </xsl:when>
                <xsl:when test="@status='Moved'">
                    <xsl:text>#F9E79F</xsl:text>
                </xsl:when>
                <xsl:when test="@status='Changed'">
                    <xsl:text>#D6EAF8</xsl:text>
                </xsl:when>
                <xsl:when test="@status='Identical' and ((count(Properties/Property[not(@status='Identical')]) > 0) or ((count(CompareItem[@type='Attribute']/Properties/Property[not(@status='Identical')]) + count(CompareItem[@type=''and not(./CompareItem)]/Properties/Property[not(@status='Identical')])) > 0) or (CompareItem[@type='Links' and ./CompareItem[(@name='Association' or @name='Generalization' or @name='Aggregation') and (./Properties/Property[not(@status='Identical')] or ./CompareItem[(@type='Src' or @type='Dst') and ./Properties/Property[not(@status='Identical')]])]]))">
                    <xsl:text>#D6EAF8</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <!--  Color for 'Identical' for those classes that truly have no changes... -->
                    <xsl:text>#EBDEF0</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
        <xsl:text>|</xsl:text>
        <xsl:for-each select="/.//CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Class']]">
           <xsl:value-of select="concat('|', @name, ':', @guid, ':', '#F5B7B1')" />
        </xsl:for-each>
        <xsl:text>|</xsl:text>
    </xsl:variable>
    
    <xsl:variable name="base-version">	   
        <xsl:choose>
            <xsl:when test="//CompareItem[@type='Class' and @name='IEC61970CIMVersion']/CompareItem[@name='version']/Properties/Property[@name='Default']/@baseline">
                <xsl:value-of select="//CompareItem[@type='Class' and @name='IEC61970CIMVersion']/CompareItem[@name='version']/Properties/Property[@name='Default']/@baseline"/>
            </xsl:when>
            <xsl:otherwise><xsl:text>Unspecified</xsl:text></xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="latest-version">     
        <xsl:choose>
            <xsl:when test="//CompareItem[@type='Class' and @name='IEC61970CIMVersion']/CompareItem[@name='version']/Properties/Property[@name='Default']/@model">
                <xsl:value-of select="//CompareItem[@type='Class' and @name='IEC61970CIMVersion']/CompareItem[@name='version']/Properties/Property[@name='Default']/@model"/>
            </xsl:when>
            <xsl:otherwise><xsl:text>Unspecified</xsl:text></xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="/">
        <html>
            <head>
                <title>CIM Model Version Comparison</title>

                <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.7.0/css/all.css" integrity="sha384-lZN37f5QGtY3VHgisS14W3ExzMWZxybE1SJSEsQp9S+oqd12jhcu+A56Ebc1zFSJ" crossorigin="anonymous" />

                <style type="text/css">
                 	/* typography: fonts */
                    body { 
                        font-size: 12px; 
                        font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
                        /* font-family: arial, sans-serif; */
                    }
                    
                    /* ================================================ */
                    /* START::SEARCH NAVBAR */
                    /* ================================================ */
                    
                    * {
                      box-sizing: border-box;
                    }
                    
                    .topnav {
                      overflow: hidden;
                      /*background-color: #e9e9e9;*/
                      background-color: #AED6F1;
                    }
                    
                    .topnav .title-container {
                      float: left;
                      margin-top: 14px;
                      margin-left: 14px;
                    }
                    
                    .topnav .button-container {
                      float: right;
                    }
                    
                    .topnav .search-container {
                      float: right;
                    }
                    
                    .topnav input[type="text"] {
                      padding: 3px;
                      margin-top: 10px;
                      font-size: 14px;
                      /* border: darkgray 1px solid; */
                    }
                    
                    .topnav .search-container a {
                      float: right;
                      margin-top: 10px;
                      margin-bottom: 10px;
                      margin-right: 14px;
                      background: #ccc;
                      font-size: 16px;
                      border: none;
                      cursor: pointer;
                      padding: 4px 4px;
                      text-align: center;
                      border: darkgray 1px solid;
                    }
                    
                    /* unvisited link */
                    .topnav .search-container a:link {
                      color: black;
                    }
                    
                    /* visited link */
                    .topnav .search-container a:visited {
                      color: black;
                    }
                    
                    /* mouse over link */
                    .topnav .search-container a:hover {
                      background: darkgray;
                      color: black;
                    }
                    
                    /* selected link */
                    .topnav .search-container a:active {
                      color: black;
                    }
                    
                    /* Expand / Collapse buttons */
                    .topnav .button-container button {
                      float: right;
                      margin-top: 10px;
                      margin-bottom: 10px;
                      margin-right: 14px;
                      background: #ccc;
                      font-size: 16px;
                      border: none;
                      cursor: pointer;
                      padding: 4px 4px;
                      text-align: center;
                      border: darkgray 1px solid;
                    }   
                    
                    .topnav .search-container button:hover {
                      background: darkgray;
                    }                 
                    @media screen and (max-width: 600px) {
                      .topnav .search-container {
                        float: none;
                      }
                    
                      .topnav a,
                      .topnav input[type="text"],
                      .topnav .search-container button {
                        float: none;
                        display: block;
                        text-align: left;
                        width: 100%;
                        margin: 0;
                        padding: 14px;
                      }
                    
                      .topnav input[type="text"] {
                        border: 1px solid darkgray;
                      }
                    }
                    
                    /* ================================================ */
                    /* END::SEARCH NAVBAR */
                    /* ================================================ */
                      
                    /* ================================================ */
                    /* START::TABLES */
                    /* ================================================ */

                    #properties {
                        /* font-family: "Trebuchet MS", Arial, Helvetica, sans-serif; */
                        font-size: 12px;
                        border-collapse:
                        collapse;
                        width: 100%;
                    }

                    #properties td, #properties th {
                        border: 1px solid #ddd;
                        padding: 2px;
                    }

                    #properties tr:nth-child(even){
                        background-color: #f2f2f2;
                    }

                    #properties tr:hover {
                        background-color: #ddd;
                    }

                    #properties th {
                        padding-top: 2px;
                        padding-bottom: 2px;
                        text-align: left;
                        background-color: white;
                        /* color: white; */
                    }
                    
                    #attributes {
                        /* font-family: "Trebuchet MS", Arial, Helvetica, sans-serif; */
                        font-size: 12px;
                        border-collapse:
                        collapse;
                        width: 100%;
                    }

                    #attributes td, #attributes th {
                        valign: top;
                        border: 1px solid #ddd;
                        padding: 2px;
                    }
                    
                    #attributes td {
                        valign: top;
                    }
                    
                    #attributes tr {
                        valign: top;
                    }

                    #attributes tr:hover {
                        background-color: #ddd;
                    }

                    #attributes th {
                        padding-top: 2px;
                        padding-bottom: 2px;
                        text-align: left;
                        background-color:lightgray;
                    }

                    /* ================================================ */
                    /* END::TABLES */
                    /* ================================================ */

                    /* ================================================ */
                    /* START::COLLAPSIBLE */
                    /* ================================================ */
                    
                    .wrap-collapsible {
                        margin-bottom: 1.2rem 0;
                    }

                    input[type='checkbox'] {
                        display: none;
                    }

                    .lbl-toggle {
                        display: block;

                        font-family: monospace;
                        font-size: 1.1rem;
                        text-align: left;
                        padding: .3rem;
                        color: black;
                        cursor: pointer;
                        border-radius: 7px;
                        transition: all 0.25s
                        ease-out;
                    }

                    .lbl-toggle:hover {
                        font-weight: bold;
                    }

                    .lbl-toggle::before {
                        content: ' ';
                        display: inline-block;
    
                        border-top: 5px solid transparent;
                        border-bottom: 5px solid transparent;
                        border-left: 5px solid currentColor;
                        vertical-align: middle;
                        margin-right: .7rem;
                        transform: translateY(-2px);
    
                        transition: transform .2s ease-out;
                    }

                    .toggle:checked + .lbl-toggle::before {
                        transform: rotate(90deg) translateX(-3px);
                    }

                    .collapsible-content {
                        max-height: 0px;
                        overflow: hidden;
                        transition: max-height .25s ease-in-out;
                    }

                    .toggle:checked + .lbl-toggle + .collapsible-content {
                        max-height: 5000000px;
                    }

                    .toggle:checked + .lbl-toggle {
                        border-bottom-right-radius: 0;
                        border-bottom-left-radius: 0;
                    }
                    
                    .lbl-home-icon {
                      float: right;
                      margin-right: 5px;
                    } 

                    .collapsible-content .content-inner {
                        /* background: white; */
                        border-bottom: 1px solid lightgray;
                        border-bottom-left-radius: 7px;
                        border-bottom-right-radius: 7px;
                        padding: .5rem 1.25rem;
                        padding-bottom: 1.5rem;
                    }

                    /* ================================================ */
                    /*  END::COLLAPSIBLE */
                    /* ================================================ */
                    
                    /* ================================================ */
                    /* START::TOOLTIPS */
                    /* ================================================ */
                    
                    /* Tooltip container */
                    .tooltip {
                      position: relative;
                      display: inline-block;
                      font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
                    }
                    
                    /* Tooltip text */
                    .tooltip .tooltiptext {
                      top: 10px;
                      left: 125%;
                      visibility: hidden;
                      width: 350px;
                      max-width: 650px;
                      font-size: 12px;
                      font-weight: normal;
                      background-color: lightyellow;
                      color: black;
                      text-align: left;
                      border: 1px;
                      border-radius: 4px;
                      border-style: dashed;
                      border-color: darkgray;
                      padding: 10px 10px;
                    
                      /* Position the tooltip */
                      position: absolute;
                      z-index: 1;
                    }
                    
                    /* Show the tooltip text when you mouse over the tooltip container */
                    .tooltip:hover .tooltiptext {
                      visibility: visible;
                      font-weight: normal;
                    }
                    
                    /* ATTRIBUTE Tooltip container */
                    .attribute-tooltip {
                      position: relative;
                      display: inline-block;
                      font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
                    }
                    
                    /* ATTRIBUTE Tooltip text */
                    .attribute-tooltip .attribute-tooltiptext {
                      bottom: -30px;
                      left: 125%;
                      visibility: hidden;
                      width: 350px;
                      max-width: 650px;
                      font-size: 12px;
                      font-weight: normal;
                      background-color: white;
                      color: black;
                      text-align: left;
                      border: 1px;
                      border-radius: 4px;
                      border-style: dashed;
                      border-color: darkgray;
                      padding: 5px 5px;
                    
                      /* Position the tooltip */
                      position: absolute;
                      z-index: 1;
                    }
                    
                    /* Show the tooltip ATTRIBUTE table when you mouse over the tooltip container */
                    .attribute-tooltip:hover .attribute-tooltiptext {
                      visibility: visible;
                      font-weight: normal;
                    }
                    /* ================================================ */
                    /*  END::TOOLTIPS */
                    /* ================================================ */
                    
                    /* ================================================ */
                    /* START::MISC */
                    /* ================================================ */

                    .name, .type, .cardinality, .namespace { 
                        font-family: courier, monospace; 
                    }
                    
                    /* unvisited link */
                    .class-link:link {
                      color: black;
                      text-decoration: none;
                    }
                    
                    /* visited link */
                    .class-link:visited {
                      color: black;
                      text-decoration: none;
                    }
                    
                    /* mouse over link */
                    .class-link:hover {
                      font-weight: bold;
                      color: black;
                      text-decoration: none;
                    }
                    
                    /* selected link */
                    .class-link:active {
                      color: black;
                      text-decoration: none;
                    }  
                    
	                /* borders */
                    div.group {
                        border-style: solid;
                        border-color: gray;
                        border-width: 1px; 
                        padding: 0.5rem 0.5rem;
                    }
                    
                    /* model-only is GREEN */
                    p.model-only { 
                        background-color: #ABEBC6; color: black;
                    }
                    
                    /* baseline-only is RED */
                    p.baseline-only { 
                        background-color: #F5B7B1; color: black; 
                    }
                    
                    /* moved is YELLOW */
                    p.moved { 
                        background-color: #F9E79F; color: black; 
                    }
                    
                    /* changed is BLUE */
                    p.changed { 
                        background-color: #D6EAF8; color: black; 
                    }
                    
                    /* model-only is GREEN */
                    tr.model-only { 
                        background-color: #ABEBC6;
                    }
                    
                    /* baseline-only is RED */
                    tr.baseline-only { 
                        background-color: #F5B7B1; 
                    }
                    
                    /* moved is YELLOW */
                    tr.moved { 
                        background-color: #F9E79F; 
                    }
                    
                    /* changed is BLUE */
                    tr.changed { 
                        background-color: #D6EAF8; 
                    }
                    
                    /* ================================================ */
                    /* END::MISC */
                    /* ================================================ */
                </style>

            </head>
            <body>
    	    	<script><xsl:text disable-output-escaping="yes"><![CDATA[
                    function executeSearch() {
                        var searchLink = document.getElementById('search-link');
                        var searchClassField = document.getElementById('search-class-field');
                        
                        var className = searchClassField.value;
                        if (className != null && className != '') {
                            var classHeader = document.getElementsByName(className)[0];
                            if (classHeader != null) {
                                var classNameId = classHeader.getAttribute('classNameId');
                                searchLink.href = '#' + classNameId; 
                                autoExpand(classNameId);
                                
                                setTimeout(
                                    function() { 
                                        classHeader.scrollIntoView(); 
                                    }, 3000);
                                    
                                return true;
                            } else {
                                alert("No class named '" + className + "' exists in this comparison report.\nNote that class searches are case sensitive. Please try again.");
                            } 
                        }
                        return false;
                    }
    
                    function toggleHeaders(expand) {
                      var currentInputField;
                      var inputFields = document.getElementsByTagName("INPUT");
                      for(var i = 0; i < inputFields.length; i++) {
                          currentInputField = inputFields[i];
                          if (currentInputField && currentInputField.type.toLowerCase() == 'checkbox' && currentInputField.id.startsWith("collapsible-")) {
                            currentInputField.checked = expand;
                            var packageNameId = currentInputField.getAttribute('packageNameId');
                            if (packageNameId != null && packageNameId != '') {
                                var packageIcon = document.getElementById("package-icon-" + packageNameId);
                                if (packageIcon != null) {
                                    if (expand) {
                                        packageIcon.className = "fas fa-folder-open";
                                    } else {
                                        packageIcon.className = "fas fa-folder";
                                    }
                                }
                            }
                          }
                      }
                      return true;
                    }
                    
                    function togglePackageHeaderIcon(currentPackageHeader) {
                        var packageNameId = currentPackageHeader.getAttribute('packageNameId');
                        if (packageNameId != null && packageNameId != '') {
                            var packageIcon = document.getElementById("package-icon-" + packageNameId);
                            if (packageIcon != null) {
                                if (currentPackageHeader.checked) {
                                    packageIcon.className = "fas fa-folder-open";
                                } else {
                                    packageIcon.className = "fas fa-folder";
                                }
                            }
                        }
                        return true;
                    }
                                
                    //
                    // Original JavaScript code by Chirp Internet: www.chirp.com.au
                    // Please acknowledge use of this code by including this header.
                    //
                    window.addEventListener("DOMContentLoaded", function(e) {
                    
                      var links = document.getElementsByTagName("A");
                      for(var i=0; i < links.length; i++) {
                        if(!links[i].hash) continue;
                        if(links[i].origin + links[i].pathname != self.location.href) continue;
                        (function(anchorPoint) {
                          links[i].addEventListener("click", function(e) {
                            autoExpand(links[i].hash.replace(/#/, ""));
                            anchorPoint.scrollIntoView(true);
                            e.preventDefault();
                          }, false);
                        })(document.getElementById(links[i].hash.replace(/#/, "")));
                      }
                    }, false);
                                   
                    function autoExpand(classId) {
                      var radioButton = document.getElementById("collapsible-" + classId);
                      if (radioButton != null) {
                        radioButton.checked = true;
                        autoExpand(radioButton.getAttribute('parentCollapsibleId'));
                      }
                    }]]></xsl:text>
                </script>
                <p>
                  <div class="topnav">
                    <div class="title-container">
                        <span style="font-size:1rem;font-weight:bold">Comparison Report of Changes Between CIM model <xsl:value-of select="$base-version"/> (baseline) and <xsl:value-of select="$latest-version"/></span>
                    </div>
                    <div class="button-container"><button type="button" onClick="return toggleHeaders(false);"><i class="fas fa-compress-arrows-alt"></i></button></div>
                    <div class="button-container"><button type="button" onClick="return toggleHeaders(true);"><i class="fas fa-expand-arrows-alt"></i></button></div>
                    <div class="search-container">
                        <input id="search-class-field" name="search-class-field" type="text" placeholder="Enter class name to search for..." size="35"/>
                        <a id="search-link" href="#" onClick="return executeSearch();"><i class="fa fa-search"></i></a>
                    </div>
                  </div>
                </p>
                <xsl:apply-templates />
            </body>
        </html>
    </xsl:template>

    <xsl:template match="CompareResults[@hasChanges='true']">
        <xsl:choose>
            <xsl:when test="$iecPackage">
                <xsl:apply-templates select="//CompareItem[@type='Package' and @name=$iecPackage]" />
            </xsl:when>
            <xsl:otherwise>
                <!-- Default is to process from the top-most 'Model' package on down... -->
                <xsl:apply-templates select="//CompareItem[@type='Package' and @name='Model']" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="CompareItem[@type='Package']|CompareItem[@type='' and @status='Baseline only' and ./Properties/Property[@name='Type' and @baseline='Package']]">
       <xsl:variable name="color">
            <xsl:choose>
                <xsl:when test="@status='Baseline only'">
                    <xsl:text>#F5B7B1</xsl:text>
                </xsl:when>
                <xsl:when test="@status='Model only'">
                    <xsl:text>#ABEBC6</xsl:text>
                </xsl:when>
                <xsl:when test="@status='Moved'">
                    <xsl:text>#F9E79F</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <!--  Color for all packages that are not new, remove or moved...  -->
                <xsl:text>lightgray</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
       </xsl:variable>
       <xsl:variable name="notes">
            <xsl:choose>
                <xsl:when test="Properties/Property[@name='Notes' and @status='Baseline only']">
                    <xsl:choose>
                        <xsl:when test="string-length(Properties/Property[@name='Notes']/@baseline) > 0">
                            <xsl:value-of select="Properties/Property[@name='Notes']/@baseline"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>No notes available.</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="string-length(Properties/Property[@name='Notes']/@model) > 0">
                            <xsl:value-of select="Properties/Property[@name='Notes']/@model"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>No notes available.</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <p>
        <div class="wrap-collapsible">
            <input id="collapsible-{@guid}" packageNameId="{@guid}" parentCollapsibleId="{ancestor::CompareItem[1]/@guid}" onClick="return togglePackageHeaderIcon(this);" class="toggle" type="checkbox" />
            <label for="collapsible-{@guid}" class="lbl-toggle" style="background-color:{$color}">
                <i id="package-icon-{@guid}" class="fas fa-folder" style="font-size:20px;color:white;text-shadow:2px 2px 4px #000000;"/>&#160;<xsl:value-of select='@name' />
                <div class="tooltip"><i class="fas fa-info-circle" style="text-align:left;font-size:14px;color:white;text-shadow:1px 1px 2px #000000;"/><span class="tooltiptext"><xsl:value-of select="$notes"/></span></div>
                <div class="lbl-home-icon"><a href="#"><i class="fas fa-home" style="font-size:20px;color:white;text-shadow:2px 2px 4px #000000;"/></a></div>
            </label>
            <div class="collapsible-content">
            	<div class="content-inner">
                    <xsl:if test="count(CompareItem[@type='Package']|CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Package']]) > 0">
                    	<xsl:apply-templates select="CompareItem[@type='Package']|CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Package']]" />
                    </xsl:if>
                    <p>
                    <xsl:choose>
                        <xsl:when test="(count(CompareItem[@type='Class' and @status='Identical']) > 0) or (count(CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Class']]) > 0) or (count(CompareItem[@type='Class' and @status='Model only']) > 0) or (count(CompareItem[@type='Class' and @status='Moved']) > 0) or (count(CompareItem[@type='Class' and @status='Changed']) > 0)">
                            <xsl:if test="count(CompareItem[@type='Class' and @status='Identical' and not(.//Properties/Property[not(@status='Identical')]) and not(.//CompareItem[not(@status='Identical')])]) > 0">
                                <h2>Identical Classes:</h2>
                                <!-- The XPATH expression for this select for "Identical Classes" ensures that ONLY classes with absolutely no changes are included.  
                                This was necessary as the results produced by Enterprise Architect may have the @status of a Class declared as 'Identical' and yet 
                                have changes in child properties or elements.  The conclusion is that the @status attribute is intended to ONLY indicate if direct 
                                properties have changed on the Class. -->
                                <xsl:apply-templates select="CompareItem[@type='Class' and @status='Identical' and not(.//Properties/Property[not(@status='Identical')]) and not(.//CompareItem[not(@status='Identical')])]" />
                            </xsl:if>
                            <xsl:if test="count(CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Class']]) > 0">
                                <h2>Deleted Classes:</h2>
                                <xsl:apply-templates select="CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Class']]" />
                            </xsl:if>
                            <xsl:if test="count(CompareItem[@type='Class' and @status='Model only']) > 0">
                                <h2>Added Classes:</h2>
                                <xsl:apply-templates select="CompareItem[@type='Class' and @status='Model only']" />
                            </xsl:if>
                            <xsl:if test="count(CompareItem[@type='Class' and @status='Moved']) > 0">
                                <h2>Moved Classes:</h2>
                                <xsl:apply-templates select="CompareItem[@type='Class' and @status='Moved']" />
                            </xsl:if>
                            <xsl:if test="(count(CompareItem[@type='Class' and @status='Changed']) > 0) or (count(CompareItem[@type='Class' and @status='Identical' and ((count(./Properties/Property[not(@status='Identical')]) > 0) or ((count(./CompareItem[@type='Attribute']/Properties/Property[not(@status='Identical')]) + count(./CompareItem[@type=''and not(./CompareItem)]/Properties/Property[not(@status='Identical')])) > 0) or (./CompareItem[@type='Links' and ./CompareItem[(@name='Association' or @name='Generalization' or @name='Aggregation') and (./Properties/Property[not(@status='Identical')] or ./CompareItem[(@type='Src' or @type='Dst') and ./Properties/Property[not(@status='Identical')]])]]))]) > 0)">
                                <h2>Changed Classes:</h2>
                                <!-- The XPATH expression for this select for "Changed Classes" ensures that we are including classes  
                                declared with a status of 'Identical' but which do have changes in child properties or elements. -->
                                <xsl:apply-templates select="CompareItem[@type='Class' and @status='Changed']|CompareItem[@type='Class' and @status='Identical' and .//Properties/Property[not(@status='Identical')] and .//CompareItem[not(@status='Identical')]]" />
                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <h3><p style="font-size: 1.1rem">Package '<xsl:value-of select='@name'/>' has no changes to the classes it contains.</p></h3>
                        </xsl:otherwise>
                    </xsl:choose>
                    </p>
                    <!-- </xsl:if>  --> 
        	   </div>
            </div>
	    </div>
        </p>
    </xsl:template>

    <xsl:template match="CompareItem[@type='Class']|CompareItem[@type='' and ./Properties/Property[@name='Type' and @baseline='Class']]">
       <xsl:variable name="notes">
            <xsl:choose>
                <xsl:when test="Properties/Property[@name='Notes']/@status='Baseline only'">
                    <xsl:choose>
                        <xsl:when test="string-length(Properties/Property[@name='Notes']/@baseline) > 0">
                        	<xsl:value-of select="Properties/Property[@name='Notes']/@baseline"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>No notes available.</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:choose>
                        <xsl:when test="string-length(Properties/Property[@name='Notes']/@model) > 0">
                            <xsl:value-of select="Properties/Property[@name='Notes']/@model"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>No notes available.</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="color">
            <xsl:call-template name="class-color">
            	<xsl:with-param name="name" select="@name"/>
            </xsl:call-template>
        </xsl:variable>
        <p>
        <div id="{@guid}" class="wrap-collapsible">
            <input id="collapsible-{@guid}" name="{@name}" classNameId="{@guid}" parentCollapsibleId="{ancestor::CompareItem[1]/@guid}" class="toggle" type="checkbox" />
            <label for="collapsible-{@guid}" class="lbl-toggle"  style="background-color:{$color}">
                <i class="fas fa-cog" style="font-size:18px;color:white;text-shadow:2px 2px 4px #000000;"/>&#160;<a class="class-link" href="#{@guid}"><xsl:value-of select="@name"/></a>
                <div class="tooltip" style="text-align:right;"><i class="fas fa-info-circle" style="text-align:left;font-size:14px;color:white;text-shadow:1px 1px 2px #000000;"/><span class="tooltiptext"><xsl:value-of select="$notes"/></span></div>
                <div class="lbl-home-icon"><a href="#"><i class="fas fa-home" style="font-size:20px;color:white;text-shadow:2px 2px 4px #000000;"/></a></div>
            </label>
            <div class="collapsible-content">
            <xsl:choose>
                <xsl:when test="(count(Properties/Property[not(@status='Identical')]) > 0) or ((count(CompareItem[@type='Attribute']/Properties/Property[not(@status='Identical')]) + count(CompareItem[@type=''and not(./CompareItem)]/Properties/Property[not(@status='Identical')])) > 0) or (CompareItem[@type='Links' and ./CompareItem[(@name='Association' or @name='Generalization' or @name='Aggregation') and (./Properties/Property[not(@status='Identical')] or ./CompareItem[(@type='Src' or @type='Dst') and ./Properties/Property[not(@status='Identical')]])]])">
                    <div class="content-inner">
                        <xsl:if test="count(Properties/Property[not(@status='Identical')]) > 0">
                            <p>
                                <h3><p style="font-size: 1.1rem"><i class="fas fa-cubes" />&#160;Metadata:</p></h3>
                                <xsl:apply-templates select="Properties" />
                            </p>
                        </xsl:if>
                        <xsl:if test="(count(CompareItem[@type='Attribute']/Properties/Property[not(@status='Identical')]) + count(CompareItem[@type='' and @status='Baseline only' and not(./CompareItem)]/Properties/Property[not(@status='Identical')])) > 0">
                            <p>
                               <h3><p style="font-size:1.1rem"><i class="fas fa-bars" />&#160;Attributes:</p></h3>
                               <div>
                                    <p>
                                        <table id="attributes">
                                            <tr>
                                                <th style="width:50%" colspan="5"><p><xsl:value-of select="$base-version"/></p></th>
                                                <th style="width:50%" colspan="5"><p><xsl:value-of select="$latest-version"/></p></th>
                                            </tr>
                                            <xsl:apply-templates select="CompareItem[@type='Attribute']"/>
                                            <xsl:apply-templates select="CompareItem[@type='' and @status='Baseline only']"/>
                                       </table>
                                   </p>
                               </div>
                           </p>
                        </xsl:if>
                        <xsl:if test="CompareItem[@type='Links' and ./CompareItem[(@name='Association' or @name='Generalization' or @name='Aggregation') and (./Properties/Property[not(@status='Identical')] or ./CompareItem[(@type='Src' or @type='Dst') and ./Properties/Property[not(@status='Identical')]])]]">
                            <p><xsl:apply-templates select="CompareItem[@type='Links']"/></p>
                        </xsl:if>
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div class="content-inner">
                        <p>
                            <h3><p style="font-size: 1.1rem">No changes occurred to the properties or links for this class.</p></h3>

                        </p>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        	</div>
        </div>
        </p>
    </xsl:template>

    <xsl:template match="CompareItem[@type='Links']">
        <xsl:if test="CompareItem[@name='Generalization' or @name='Aggregation' or @name='Association']/CompareItem[@type='Src' or @type='Dst']">
            <p>
            <h3><p style="font-size: 1.1rem"><i class="fas fa-exchange-alt"/>&#160;Links:</p></h3>
            <div class="group"> 
                <xsl:apply-templates select="CompareItem[@name='Generalization']"/>
                <xsl:apply-templates select="CompareItem[@name='Association']"/>
                <xsl:apply-templates select="CompareItem[@name='Aggregation']"/>
            </div>
            </p>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="CompareItem[(@name='Association' or @name='Generalization' or @name='Aggregation') and (./Properties/Property[not(@status='Identical')] or ./CompareItem[(@type='Src' or @type='Dst') and ./Properties/Property[not(@status='Identical')]])]"> 
        <xsl:if test="CompareItem[@type='Src'] or CompareItem[@type='Dst']">
        <p>
            <div class="group">
            	<xsl:variable name="src-name-and-role">
                    <xsl:choose>
                        <xsl:when test="CompareItem[@type='Src']/@name='Source'">
                            <xsl:text><xsl:value-of select="CompareItem[@type='Src']/@name"/> (Role Name Unspecified)</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="CompareItem[@type='Src']/@name"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="dst-name-and-role">
                    <xsl:choose>
                        <xsl:when test="CompareItem[@type='Dst']/@name='Source'">
                            <xsl:text><xsl:value-of select="CompareItem[@type='Dst']/@name"/> (Role Name Unspecified)</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="CompareItem[@type='Dst']/@name"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="src-name">
                	<xsl:choose>
                        <xsl:when test="CompareItem[@type='Src']/Properties/Property[@name='End']/@status='Baseline only'">
                            <xsl:value-of select="CompareItem[@type='Src']/Properties/Property[@name='End']/@baseline"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="CompareItem[@type='Src']/Properties/Property[@name='End']/@model"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="dst-name">
                	<xsl:choose>
                        <xsl:when test="CompareItem[@type='Dst']/Properties/Property[@name='End']/@status='Baseline only'">
                            <xsl:value-of select="CompareItem[@type='Dst']/Properties/Property[@name='End']/@baseline"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="CompareItem[@type='Dst']/Properties/Property[@name='End']/@model"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="src-guid">
                	<xsl:choose>
                        <xsl:when test="CompareItem[@type='Src']">
                        	<xsl:call-template name="link-class-guid">
                               <xsl:with-param name="name" select="$src-name"/>
                           </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                        	<xsl:text></xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="dst-guid">
                	<xsl:choose>
                        <xsl:when test="CompareItem[@type='Dst']">
                        	<xsl:call-template name="link-class-guid">
                               <xsl:with-param name="name" select="$dst-name"/>
                            </xsl:call-template>
                        </xsl:when>
                        <xsl:otherwise>
                        	<xsl:text></xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="src-cardinality">
                    <xsl:choose>
                        <xsl:when test="CompareItem[@type='Src']/Properties/Property[@name='Cardinality']/@status='Baseline only'">
                            <xsl:value-of select="CompareItem[@type='Src']/Properties/Property[@name='Cardinality']/@baseline"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="CompareItem[@type='Src']/Properties/Property[@name='Cardinality']/@model"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="dst-cardinality">
                    <xsl:choose>
                        <xsl:when test="CompareItem[@type='Dst']/Properties/Property[@name='Cardinality']/@status='Baseline only'">
                            <xsl:value-of select="CompareItem[@type='Dst']/Properties/Property[@name='Cardinality']/@baseline"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="CompareItem[@type='Dst']/Properties/Property[@name='Cardinality']/@model"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <h3><xsl:value-of select="@name"/>:</h3>
                <xsl:choose>
                    <xsl:when test="not(@status='Baseline only')">
                        <p>
                        <table>
                           <tr>
                               <td style="font-size:.9rem;font-weight:bold"><p><xsl:value-of select="$src-name-and-role"/>&#160;&#160;[<xsl:value-of select="$src-cardinality"/>]</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p>&#160;&#160;&#160;<i class="fas fa-arrow-right" style="text-align:center"/>&#160;&#160;&#160;</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p><xsl:value-of select="$dst-name-and-role"/>&#160;&#160;[<xsl:value-of select="$dst-cardinality"/>]</p></td>
                           </tr>
                           <tr>
                               <td style="font-size:.9rem;font-weight:bold"><p><i class="fas fa-link" style="font-size:.8rem"/>&#160;<a href="#{$src-guid}" onClick="autoExpand('{$src-guid}');"><xsl:value-of select="$src-name"/></a></p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p>&#160;</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p><i class="fas fa-link" style="font-size:.8rem"/>&#160;<a href="#{$dst-guid}" onClick="autoExpand('{$dst-guid}');"><xsl:value-of select="$dst-name"/></a></p></td>
                           </tr>
                       </table>
                       </p>
    	               <xsl:if test="count(Properties/Property[not(@status='Identical')]) > 0">
                           <p>
                           <br></br>
    	                   <xsl:apply-templates select="Properties"/>
                           </p>
    	               </xsl:if> 
                       <p>
                       <br></br>
                       <table style="width:100%">
                           <tr>
    	                       <th style="width:49%;white-space:nowrap"><p style="font-size:1.1rem;font-weight:bold;text-align:left"><xsl:value-of select="$src-name-and-role"/></p></th>
                               <th style="width:2%;white-space:nowrap"><p>&#160;</p></th>
                               <th style="width:49%;white-space:nowrap"><p style="font-size:1.1rem;font-weight:bold;text-align:left"><xsl:value-of select="$dst-name-and-role"/></p></th>
                           </tr>
                           <tr>
                               <td><p><xsl:apply-templates select="child::CompareItem[@type='Src']"/></p></td>
    	                       <td><p><i class="fas fa-arrow-right" style="text-align:center;font-size:2.5rem"/></p></td>
    	                       <td><p><xsl:apply-templates select="child::CompareItem[@type='Dst']"/></p></td>
    	                   </tr>
                       </table>
    	               </p>
                    </xsl:when>
                    <xsl:otherwise>
                        <p>
                    	<table>
                           <tr>
                               <td style="font-size:.9rem;font-weight:bold"><p><xsl:value-of select="$src-name-and-role"/>&#160;&#160;[<xsl:value-of select="$src-cardinality"/>]</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p>&#160;&#160;&#160;<i class="fas fa-arrow-right" style="text-align:center"/>&#160;&#160;&#160;</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p><xsl:value-of select="$dst-name-and-role"/>&#160;&#160;[<xsl:value-of select="$dst-cardinality"/>]</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p>&#160;&#160;&#160;&#160;&#160;&#160;<span style="font-weight:bold;font-size:1rem;color:red">REMOVED FROM MODEL</span></p></td>
                           </tr>
                           <tr>
                               <td style="font-size:.9rem;font-weight:bold"><p><i class="fas fa-link" style="font-size:.8rem"/>&#160;<a href="#{$src-guid}" onClick="autoExpand('{$src-guid}');"><xsl:value-of select="$src-name"/></a></p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p>&#160;</p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p><i class="fas fa-link" style="font-size:.8rem"/>&#160;<a href="#{$dst-guid}" onClick="autoExpand('{$dst-guid}');"><xsl:value-of select="$dst-name"/></a></p></td>
                               <td style="font-size:.9rem;font-weight:bold"><p>&#160;</p></td>
                           </tr>
                       </table>
                       </p>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
    	</p>
        </xsl:if>
    </xsl:template>

    <xsl:template match="CompareItem[(@type='Src' or @type='Dst') and ./Properties/Property[not(@status='Identical')]]"> 
        <div>
            <xsl:if test="count(Properties/Property[not(@status='Identical')]) > 0">
                <xsl:apply-templates select="Properties"/>
            </xsl:if> 
        </div>
    </xsl:template>

     <xsl:template match="Properties[./Property[not(@status='Identical')]]">
        <!-- generates the properties of a Package if any have changed -->
        <table id="properties">
            <tr>
                <th style="background-color:lightgray;width:10%"><p>&#160;</p></th>
                <th style="text-align:left;font-weight:bold;background-color:lightgray;width:45%"><p><xsl:value-of select="$base-version"/></p></th>
                <th style="text-align:left;font-weight:bold;background-color:lightgray;width:45%"><p><xsl:value-of select="$latest-version"/></p></th>
            </tr>
        	<xsl:for-each select="Property[not(@status='Identical')]">
                <xsl:variable name="color"><xsl:call-template name="color"/></xsl:variable>   
                <xsl:choose>
                    <xsl:when test="@status='Baseline only'">
                        <tr valign="top">
                            <th class="baseline-only" style="background-color:{$color}"><p class="baseline-only" style="font-weight:bold"><xsl:value-of select="@name"/></p></th>
                            <td><p><xsl:value-of select="@baseline"/></p></td>
                            <td><p>&#160;</p></td>
                        </tr>
                    </xsl:when>
                    <xsl:when test="@status='Model only'">
                   	    <tr valign="top">
                            <th class="model-only" style="background-color:{$color}"><p class="model-only" style="font-weight:bold"><xsl:value-of select="@name"/></p></th>
                            <td><p>&#160;</p></td>
                            <td><p><xsl:value-of select="@model"/></p></td>
                        </tr>
                    </xsl:when>
                    <xsl:when test="@status='Moved'">
                   	    <tr valign="top">
                            <th class="move" style="background-color:{$color}"><p class="moved" style="font-weight:bold"><xsl:value-of select="@name"/></p></th>
                            <td><p><xsl:value-of select="@baseline"/></p></td>
                            <td><p><xsl:value-of select="@model"/></p></td>
                        </tr>
                    </xsl:when>
                    <xsl:when test="@status='Changed'">
                   	    <tr valign="top">
                            <th class="changed" style="background-color:{$color}"><p class="changed" style="font-weight:bold"><xsl:value-of select="@name"/></p></th>
                            <td><p><xsl:value-of select="@baseline"/></p></td>
                            <td><p><xsl:value-of select="@model"/></p></td>
                        </tr>
                    </xsl:when>
                </xsl:choose>
       	    </xsl:for-each>
        </table>
    </xsl:template>
 
    <xsl:template match="CompareItem[@type='Attribute']|CompareItem[@type='' and not(./Properties/Property['Type']/@baseline='Class' or ./Properties/Property['Type']/@baseline='Package') and not(./CompareItem)]"> 
        <xsl:variable name="color"><xsl:call-template name="color"/></xsl:variable>   
        <xsl:choose>    
            <xsl:when test="count(Properties/Property[not(@status='Identical')]) > 0">
                <tr valign="top">
                   <xsl:choose>
                       <xsl:when test="@status='Baseline only'">
                           <xsl:variable name="guid">
                                <xsl:choose>
                                    <xsl:when test="Properties/Property[@name='Type']/@baseline">
                                        <xsl:call-template name="link-class-guid">
                                            <xsl:with-param name="name" select="Properties/Property[@name='Type']/@baseline" />
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text></xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                           </xsl:variable>
                           <td style="background-color:{$color}"><p id="{Properties/Property[@name='Name']/@baseline}"><xsl:value-of select="Properties/Property[@name='Name']/@baseline"/></p></td>
                           <td style="background-color: {$color}"><p>&#160;</p></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='LowerBound']/@baseline"/>..<xsl:value-of select="Properties/Property[@name='UpperBound']/@baseline"/></p></td>
                           <td><p><a href="#{$guid}" onClick="autoExpand('{$guid}');"><xsl:value-of select="Properties/Property[@name='Type']/@baseline"/></a></p></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='Notes']/@baseline"/></p></td>
                           
                           <td colspan="5"><p style="color:red;weight:bold;">ATTRIBUTE REMOVED FROM MODEL</p></td>
                       </xsl:when>
                       <xsl:when test="@status='Model only'">
                           <td colspan="5"><p style="color:red;weight:bold;">ATTRIBUTE DID NOT PREVIOUSLY EXIST</p></td>
                          
                       	   <xsl:variable name="guid">
                                <xsl:choose>
                                    <xsl:when test="Properties/Property[@name='Type']/@model">
                                        <xsl:call-template name="link-class-guid">
                                            <xsl:with-param name="name" select="Properties/Property[@name='Type']/@model" />
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text></xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                           </xsl:variable>
                           <td style="background-color:{$color}"><p id="{Properties/Property[@name='Name']/@model}"><xsl:value-of select="Properties/Property[@name='Name']/@model"/></p></td>
                           <td style="background-color: {$color}"><div class="attribute-tooltip"><i class="fas fa-info-circle" style="text-align:left;font-size:12px;color:white;text-shadow:1px 1px 2px #000000;"/><span class="attribute-tooltiptext" style="font-size:16px;font-weight:bold;"><i class="fas fa-cubes" />&#160;Attribute '<xsl:value-of select="Properties/Property[@name='Name']/@model"/>' Metadata:<br></br><xsl:apply-templates select="Properties"/></span></div></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='LowerBound']/@model"/>..<xsl:value-of select="Properties/Property[@name='UpperBound']/@model"/></p></td>
                           <td><p><a href="#{$guid}" onClick="autoExpand('{$guid}');"><xsl:value-of select="Properties/Property[@name='Type']/@model"/></a></p></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='Notes']/@model"/></p></td>
                       </xsl:when>
                       <xsl:when test="@status='Moved' or @status='Changed'">
                           <xsl:variable name="baseline-guid">
                                <xsl:choose>
                                    <xsl:when test="Properties/Property[@name='Type']/@baseline">
                                        <xsl:call-template name="link-class-guid">
                                            <xsl:with-param name="name" select="Properties/Property[@name='Type']/@baseline" />
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text></xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                           </xsl:variable>
                           <td style="background-color:{$color}"><p id="{Properties/Property[@name='Name']/@baseline}"><xsl:value-of select="Properties/Property[@name='Name']/@baseline"/></p></td>
                           <td style="background-color: {$color}"><div class="attribute-tooltip"><i class="fas fa-info-circle" style="text-align:left;font-size:12px;color:white;text-shadow:1px 1px 2px #000000;"/><span class="attribute-tooltiptext" style="font-size:16px;font-weight:bold;"><i class="fas fa-cubes" />&#160;Attribute '<xsl:value-of select="Properties/Property[@name='Name']/@baseline"/>' Metadata:<br></br><xsl:apply-templates select="Properties"/></span></div></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='LowerBound']/@baseline"/>..<xsl:value-of select="Properties/Property[@name='UpperBound']/@baseline"/></p></td>
                           <td><p><a href="#{$baseline-guid}" onClick="autoExpand('{$baseline-guid}');"><xsl:value-of select="Properties/Property[@name='Type']/@baseline"/></a></p></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='Notes']/@baseline"/></p></td>
                           
                           <xsl:variable name="model-guid">
                                <xsl:choose>
                                    <xsl:when test="Properties/Property[@name='Type']/@model">
                                        <xsl:call-template name="link-class-guid">
                                            <xsl:with-param name="name" select="Properties/Property[@name='Type']/@model" />
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:text></xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                           </xsl:variable>
                           <td style="background-color: {$color}"><p id="{Properties/Property[@name='Name']/@model}"><xsl:value-of select="Properties/Property[@name='Name']/@model"/></p></td>
                           <td style="background-color: {$color}"><p>&#160;</p></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='LowerBound']/@model"/>..<xsl:value-of select="Properties/Property[@name='UpperBound']/@model"/></p></td>
                           <td><p><a href="#{$model-guid}" onClick="autoExpand('{$model-guid}');"><xsl:value-of select="Properties/Property[@name='Type']/@model"/></a></p></td>
                           <td><p><xsl:value-of select="Properties/Property[@name='Notes']/@model"/></p></td>                     
	                   </xsl:when>
                   </xsl:choose>
                </tr>
           </xsl:when>
	   </xsl:choose>   
    </xsl:template>

    <xsl:template name="color">
        <xsl:choose>
            <xsl:when test="@status='Baseline only'">
                <item>#F5B7B1</item>
            </xsl:when>
            <xsl:when test="@status='Model only'">
                <item>#ABEBC6</item>
            </xsl:when>
            <xsl:when test="@status='Moved'">
                <xsl:text>#F9E79F</xsl:text>
            </xsl:when>
            <xsl:when test="@status='Changed'">
                <xsl:text>#D6EAF8</xsl:text>
            </xsl:when>
            <xsl:when test="@status='Identical'">
                <xsl:text>#EBDEF0</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text></xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
 
	<xsl:template name="class-color">
	   <!-- The 'name' param corresponds to the 'name' attribute in a CompareItem element and indicates the class name to look for in the formatted string... -->
	   <xsl:param name="name"/>
	   <xsl:variable name="string-element" select="concat('|', $name, ':')"/>
	   <xsl:choose>
	       <xsl:when test="string-length(substring-after($all-classes, $string-element)) > 0">
	           <xsl:value-of select="substring-after(substring-before(substring-after($all-classes, $string-element), '|'), ':')"/>
	       </xsl:when>
	       <xsl:otherwise>
	           <xsl:text></xsl:text>
	       </xsl:otherwise>
	   </xsl:choose>
    </xsl:template>
    
	<xsl:template name="link-class-guid">
	   <!-- The 'name' param corresponds to the 'name' attribute in a CompareItem element and indicates the class name to look for in the formatted string... -->
	   <xsl:param name="name"/>
	   <xsl:variable name="string-element" select="concat('|', $name, ':')"/>
	   <xsl:choose>
	       <xsl:when test="string-length(substring-after($all-classes, $string-element)) > 0">
	           <xsl:value-of select="substring-before(substring-after($all-classes, $string-element), ':')"/>
	       </xsl:when>
	       <xsl:otherwise>
	           <xsl:text></xsl:text>
	       </xsl:otherwise>
	   </xsl:choose>
    </xsl:template>
    
</xsl:stylesheet>