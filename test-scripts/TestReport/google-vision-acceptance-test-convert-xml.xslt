<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2023. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="snapshotDir" select="resolve-uri('results')"/>
<xsl:template match="/">
<html>
<head>
  <link href="wtf.css" rel="stylesheet" type="text/css" />
</head>
<body>
  <div class="logo-header">
    <span class="logo"></span>
    <h2 class="top-h2">Google Vision Acceptance Test Results</h2>
  </div>
  <!-- overall suite results are shown in this table -->
  <table class="totals" border="5">
      <tr class="headers">
        <th>Total Tests</th>
        <th>Total Failures</th>
        <th>Total Errors</th>
        <th>Total Skipped</th>
        <th>Total Time (secs)</th>
      </tr>
      <tr>
        <td class="center-text">
          <xsl:value-of select="sum(results/testsuites/@tests)" />
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="sum(results/testsuites/@failures) &gt; 0">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text pass'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="sum(results/testsuites/@failures)" />
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="sum(results/testsuites/@errors) &gt; 0">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text pass'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="sum(results/testsuites/@errors)" />
        </td>
        <td class="center-text">
          <xsl:value-of select="sum(results/testsuites/@skipped)" />
        </td>
        <td class="center-text">
          <xsl:value-of select="sum(results/testsuites/@time)" />
        </td>
      </tr>
    </table>
  <br/>
  <!-- individual test results are shown in this table -->
  <table border="7">
    <tr class="headers">
      <th>Testcase Name</th>
      <th>Tests</th>
      <th>Failures</th>
      <th>Errors</th>
      <th>Time (secs)</th>  
    </tr>
    <xsl:for-each select="results/testsuites">
      <tr>
          <td class="text">
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="$snapshotDir"/>
                <xsl:value-of select="'/'"/>
                <xsl:value-of select="@name"/>
                <xsl:value-of select="'.html'"/>
              </xsl:attribute>
              <xsl:value-of select="@name"/>
            </a>
          </td>      
          <td class="center-text"><xsl:value-of select="sum(testsuite/@tests)" /></td>
          <td>
            <xsl:attribute name="class">
              <xsl:choose>
                <xsl:when test="sum(testsuite/@failures) &gt; 0">
                  <xsl:value-of select="'center-text errors'" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="'center-text pass'" />                
                </xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
            <xsl:value-of select="sum(testsuite/@failures)" />
         </td> 
         <td>
            <xsl:attribute name="class">
              <xsl:choose>
                <xsl:when test="sum(testsuite/@errors) &gt; 0">
                  <xsl:value-of select="'center-text errors'" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="'center-text pass'" />                
                </xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
            <xsl:value-of select="sum(testsuite/@errors)" />
         </td>  
          <td class="right-text"><xsl:value-of select="@time"/></td>
      </tr>
    </xsl:for-each>
  </table>
  <br/>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
