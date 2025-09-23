<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<html>
<head>
  <link href="wtf.css" rel="stylesheet" type="text/css" />
</head>
<body>
  <div class="logo-header">
    <span class="logo"></span>
    <h2 class="top-h2">IUT Test Results</h2>
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
        <td class="center-text">
          <xsl:value-of select="sum(results/testsuite/@time)" />
        </td>
      </tr>
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
    <xsl:for-each select="results/testsuite/testcase">
      <tr>
        <xsl:choose>
          <xsl:when test="not(error) and not(failure)">
            <td class="pass"><xsl:value-of select="concat(@classname,'/',@name)"/></td>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:choose>
              <xsl:when test="@name=@classname">
                <td>
                  <OPTION VALUE="@name=@classname">
                      <xsl:value-of select="@message"/>
                  </OPTION>    
                </td>                            
              </xsl:when>
              <xsl:otherwise>
                <td class="errors">
                  <xsl:value-of select="concat(@classname,'/',@name)"/>
                  <br/>
                  <xsl:value-of select="@message" />
                    <xsl:if test="@type != @message">
                      <xsl:text> </xsl:text><xsl:value-of select="@type" />
                    </xsl:if>
                  <xsl:value-of select="." />
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
</body>
</html>
</xsl:template>
</xsl:stylesheet>
