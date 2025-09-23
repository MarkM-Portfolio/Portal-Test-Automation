<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="pDirectoryPath" select="'./surefire-reports/HTMLReports/TEST-'"/>
<xsl:template match="/">
<html>
<head>
  <link href="wtf.css" rel="stylesheet" type="text/css" />
</head>
<body>
  <div>
    <span class="logo logo-right"></span>
    <h2 class="top-h2"> Master WTF Test Results </h2>
    <!-- overall results are shown in this table -->
    <table class="totals" border="5">
      <tr class="headers">
        <th>Total Tests</th>
        <th>Total Failures</th>
        <th>Total Errors</th>
        <th>Total Skipped</th>
      </tr>
      <tr>
        <td class="center-text">
          <xsl:value-of select="sum(results/testsuite/@tests)" />
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="sum(results/testsuite/@failures) &gt; 0">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text pass'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="sum(results/testsuite/@failures)" />
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="sum(results/testsuite/@errors) &gt; 0">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text pass'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="sum(results/testsuite/@errors)" />
        </td>
        <td class="center-text">
          <xsl:value-of select="sum(results/testsuite/@skipped)" />
        </td>
      </tr>
    </table>
  </div>
  <!-- individual results are shown in this table -->
  <table border="5">
    <tr class="headers">
      <th>Name</th>
      <th>Tests</th>
      <th>Failures</th>
      <th>Errors</th>
      <th>Skipped</th>
      <th>Time (secs)</th>
    </tr>
    <xsl:for-each select="results/testsuite">
      <tr>
        <td>
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="$pDirectoryPath"/>
              <xsl:value-of select="@name"/>
              <xsl:value-of select="'.html'"/>
            </xsl:attribute>
            <xsl:value-of select="@name"/>
          </a>
        </td>
        <td class="center-text"><xsl:value-of select="@tests"/></td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="@failures &gt; 0">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text pass'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@failures"/>
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="@errors &gt; 0">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text pass'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@errors"/>
        </td>
        <td class="center-text"><xsl:value-of select="@skipped"/></td>
        <td class="right-text"><xsl:value-of select="@time"/></td>
      </tr>
    </xsl:for-each>
  </table>
  <br/>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
