<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2024. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- correct WTF name param value should be passed in -->
<xsl:param name="pWTF" select="'WTF1'"/>
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
    <!-- Table filter -->
  <input type="text" id="filterInput" autocomplete="off" onkeyup="filterFunction()" placeholder="Search by build name..." title="Type in a build name"></input>
  <!-- individual results are shown in this table -->
  <table id="resultsTable" border="5">
    <tr class="headers">
      <th>Build</th>
      <th>Tests</th>
      <th>Failures</th>
      <th>Errors</th>
      <th>Skipped</th>
    </tr>
    <xsl:for-each select="results/gv_acceptance_test_run">
      <tr>
        <td>
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="@buildUrl"/>
            </xsl:attribute>
            <xsl:value-of select="@buildName"/>
          </a>
        </td>
        <td class="center-text">
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="@tests &lt; 1">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'center-text'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@tests"/></td>
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
