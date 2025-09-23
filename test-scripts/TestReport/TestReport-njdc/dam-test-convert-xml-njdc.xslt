<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2022. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="snapshotDir" select="resolve-uri('results')"/>
<xsl:template match="/">
<html>
<head>
  <link href="wtf_njdc.css" rel="stylesheet" type="text/css" />
</head>
<body>
  <div class="logo-header">
    <span class="logo"></span>
    <h2 class="top-h2">Dam Performance Test Report NJDC</h2>
  </div>
  <!-- overall suite results are shown in this table -->
  
  <!-- individual test results are shown in this table -->
  <table border="7">
    <tr class="headers">
      <th>TestReport Name</th> 
    </tr>
    <xsl:for-each select="testreport/dam_performance_report_njdc">
      <tr>
          <td class="text">
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="$snapshotDir"/>
                <xsl:value-of select="'/'"/>
                 <xsl:value-of select="@name"/>
                <xsl:value-of select="'/index.html'"/>
                <!-- <xsl:value-of select="'.html'"/> -->
              </xsl:attribute>
              <xsl:value-of select="@name"/>
            </a>
          </td>             
      </tr>
    </xsl:for-each>
  </table>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
