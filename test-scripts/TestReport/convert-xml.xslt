<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="pSnapshotDirPath" select="'../../snapshots/'"/> 
<!-- correct snapshot dir param value should be passed in -->
<xsl:param name="snapshotDir" select="'2021.02.04_14.42.41'"/> 
<xsl:template match="/">
<html>
<head>
  <link href="wtf.css" rel="stylesheet" type="text/css" />
</head>
<body>
  <div class="logo-header">
    <span class="logo"></span>
    <h2 class="top-h2">WTF Test Results</h2>
  </div>
  <!-- overall suite results are shown in this table -->
  <table border="5">
    <tr class="headers">
      <th>Name</th>
      <th>Tests</th>
      <th>Failures</th>
      <th>Errors</th>
      <th>Skipped</th>
      <th>Time (secs)</th>
    </tr>
    <xsl:for-each select="testsuite">
      <tr>
        <td><xsl:value-of select="@name"/></td>
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
        <td><xsl:value-of select="@time"/></td>
      </tr>
    </xsl:for-each>
  </table>
  <br/>
  <!-- individual test results are shown in this table -->
  <table border="7">
    <tr class="headers">
      <th>Testcase Name</th>
      <th>Failures</th>
      <th>Errors</th>
      <th>Time (secs)</th>  
    </tr>
    <xsl:for-each select="testsuite/testcase">
      <tr>
        <xsl:choose>
          <xsl:when test="not(error) and not(failure)">
            <td><xsl:value-of select="@name"/></td>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:choose>
              <xsl:when test="@name=@classname">
                <td>
                  <a>
                    <xsl:attribute name="href">
                      <xsl:value-of select="$pSnapshotDirPath"/>
                      <xsl:value-of select="$snapshotDir"/>
                      <xsl:value-of select="'/'"/>
                      <xsl:value-of select="@classname"/>
                      <xsl:value-of select="'.'"/>
                      <xsl:value-of select="'class'"/>
                      <xsl:value-of select="'/'"/>
                      <xsl:value-of select="'index.html'"/>
                    </xsl:attribute>
                    <xsl:value-of select="'Setup/Teardown error'"/>
                  </a>
                </td>                            
              </xsl:when>
              <xsl:otherwise>
                <td>
                  <a>
                    <xsl:attribute name="href">
                      <xsl:value-of select="$pSnapshotDirPath"/>
                      <xsl:value-of select="$snapshotDir"/>
                      <xsl:value-of select="'/'"/>
                      <xsl:value-of select="@classname"/>
                      <xsl:value-of select="'.'"/>
                      <xsl:value-of select="@name"/>
                      <xsl:value-of select="'/'"/>
                      <xsl:value-of select="'index.html'"/>
                    </xsl:attribute>
                    <xsl:value-of select="@name"/>
                  </a>
                </td>                            
              </xsl:otherwise>
            </xsl:choose>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
          <xsl:when test="failure">
            <td class="center-text errors"><xsl:value-of select="count(failure)" /></td>            
          </xsl:when>
          <xsl:otherwise>
            <td class="center-text pass"><xsl:text>0</xsl:text></td>            
          </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
          <xsl:when test="error">
            <td class="center-text errors"><xsl:value-of select="count(error)" /></td>            
          </xsl:when>
          <xsl:otherwise>
            <td class="center-text pass"><xsl:text>0</xsl:text></td>            
          </xsl:otherwise>
        </xsl:choose>
        <td class="right-text"><xsl:value-of select="@time"/></td>
      </tr>
    </xsl:for-each>
  </table>
  <br/>
  <div>
    <a class="all-snapshots">
      <xsl:attribute name="href">
        <xsl:value-of select="$pSnapshotDirPath"/>
        <xsl:value-of select="$snapshotDir"/>
        <xsl:value-of select="'/'"/>
      </xsl:attribute>
      <xsl:value-of select="'All Snapshots'"/>
    </a>
  </div>

</body>
</html>
</xsl:template>
</xsl:stylesheet>
