<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2023. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- correct WTF name param value should be passed in -->
<xsl:param name="pWTF" select="'WCM Regression'"/>
<xsl:param name="pAverageThresholdValue" select="'.168 seconds'"/>
<xsl:param name="pThroughputThresholdValue" select="'26 per seconds'"/>

<xsl:param name="pUrlPath" select="'https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/'"/>
<xsl:template match="/">
<html>
<head>
  <link href="wtf.css" rel="stylesheet" type="text/css" />
  <style>
    #filterInput {
      width: 1150px;
      font-size: 18px;
      padding: 12px 20px 12px 12px;
      border: 2px solid grey;
      margin-bottom: 20px;
    }

    #resultsTable tr.header, #resultsTable tr:hover {
      background-color: #f1f1f1;
    }
  </style>
</head>
<body>
  <div>
    <span class="logo logo-right"></span>
    <h2 class="top-h2"> <xsl:value-of select="$pWTF"/> Dashboard Test Results </h2>
  </div>
  <table id="thresholdTable" border="5">
  <tr>
  <td class="threshold" colspan="1">Threshold values</td>
    <td class="indication" colspan="6">
       <div> Values &lt; Threshold Values are highlighted in <span class="fontGreen">green</span></div> 
       <div> Values &gt;= Threshold Values and Values &lt;= 1%-4.99% of threshold value are highlighted in <span class="fontYellow">yellow</span></div>
       <div> Values &gt;= Threshold Values and Values &gt;= 5% of threshold value and Failures are highlighted in <span class="fontRed">red and bold</span></div>
    </td>
    </tr>
    <tr class="headers">
      <th class="thresholdWidth">Build</th>
      <th class="thresholdWidth">Response Time</th>
      <th class="thresholdWidth">Throughput</th>
    </tr>
    <tr>
      <td class="center-text">CF210(release)</td>
      <td class="center-text"><xsl:value-of select="$pAverageThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pThroughputThresholdValue"/></td>
    </tr>
  </table>
    <!-- Table filter -->
  <input type="text" id="filterInput" autocomplete="off" onkeyup="filterFunction()" placeholder="Search by build name..." title="Type in a build name"></input>
  <!-- individual results are shown in this table -->
  <table id="resultsTable" border="5">
    <tr class="headers">
      <th>Build</th>
    </tr>
    <xsl:for-each select="results/wtf_run">
      <tr>
        <td>
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="$pUrlPath"/>
              <xsl:value-of select="@buildName"/>
              <xsl:value-of select="'/wcm_test_report_njdc/index.html'"/>
            </xsl:attribute>
            <xsl:value-of select="@buildName"/>
          </a>
        </td>
      </tr>
    </xsl:for-each>
  </table>
  <script><![CDATA[
    function filterFunction() {
      var input, filter, table, tr, td, i, txtValue;
      input = document.getElementById("filterInput");
      filter = input.value.toUpperCase();
      table = document.getElementById("resultsTable");
      tr = table.getElementsByTagName("tr");
      for (i = 0; i < tr.length; i++) {
        td = tr[i].getElementsByTagName("td")[0];
        if (td) {
          txtValue = td.textContent || td.innerText;
          if (txtValue.toUpperCase().indexOf(filter) > -1) {
            tr[i].style.display = "";
          } else {
            tr[i].style.display = "none";
          }
        }       
      }
    }]]>
  </script>
  <br/>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
