<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2022. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- correct WTF name param value should be passed in -->
<xsl:param name="pWTF" select="'DamNjdc'"/>
<xsl:param name="pUploadThresholdValue" select="'8 minutes 20 seconds'"/>
<xsl:param name="pOperationThresholdValue" select="'25 minutes 40 seconds'"/>
<xsl:param name="pFetchBinaryImageThresholdValue" select="'0.197 seconds'"/>
<xsl:param name="pFetchBinaryDocumentThresholdValue" select="'0.205 seconds'"/>
<xsl:param name="pFetchBinaryVideoThresholdValue" select="'0.221 seconds'"/>
<xsl:param name="pGetApiThresholdValue" select="'0.203 seconds'"/>
<xsl:param name="pAnonymousFetchBinaryImageThresholdValue" select="'0.134 seconds'"/>
<xsl:param name="pAnonymousFetchBinaryDocumentThresholdValue" select="'0.133 seconds'"/>
<xsl:param name="pAnonymousFetchBinaryVideoThresholdValue" select="'0.201 seconds'"/>

<xsl:param name="pUrlPath" select="'https://dxcore-test-results-secure.team-q-dev.com/DX-95-TestReports/'"/>                                           
<xsl:template match="/">
<html>
<head>
  <link href="wtf_njdc.css" rel="stylesheet" type="text/css" />
  <style>
    #filterInput {
      width: 1150px;
      font-size: 18px;
      padding: 12px 20px 12px 12px;
      border: 2px solid grey;
      margin-bottom: 20px;
      margin-top: 20px;
    }

    #resultsTable tr.header, #resultsTable tr:hover {
      background-color: #f1f1f1;
    }
    .thresholdWidth {
      width:187px;
    }
    .threshold {
      font-weight: bold;
    }
    .indication {
      font-weight: bold;
      font-size: 10px;
    }
    .fontGreen {
      color: green ;
      font-weight:normal;
    }
    .fontYellow {
        color: rgb(255, 223, 16) ;
        font-weight:normal;
    }
    .fontRed {
        color: red ;
    }
    .logo-right-top {
      float: right;
    }
  </style>
</head>
<body>
  <div>
    <span class="logo logo-right-top"></span>
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
      <th class="thresholdWidth">Upload Time</th>
      <th class="thresholdWidth">Operation Time</th>
      <th class="thresholdWidth">Fetch binary time - Image (in sec)</th>
      <th class="thresholdWidth">Fetch binary time - Video (in sec)</th>
      <th class="thresholdWidth">Fetch binary time - Documents(in sec)</th>
      <th class="thresholdWidth">Get Api Time (in sec)</th>
      <th class="thresholdWidth">Anonymous Fetch binary time - Image (in sec)</th>
      <th class="thresholdWidth">Anonymous Fetch binary time - Documents(in sec)</th>
      <th class="thresholdWidth">Anonymous Fetch binary time - Video (in sec)</th>
    </tr>
    <tr>
      <td class="center-text">CF207(release)</td>
      <td class="center-text"><xsl:value-of select="$pUploadThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pOperationThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pFetchBinaryImageThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pFetchBinaryDocumentThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pFetchBinaryVideoThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pGetApiThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymousFetchBinaryImageThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymousFetchBinaryDocumentThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymousFetchBinaryVideoThresholdValue"/></td>
    </tr>
  </table>

    <!-- Table filter -->
  <input type="text" id="filterInput" autocomplete="off" onkeyup="filterFunction()" placeholder="Search by build name..." title="Type in a build name"></input>
  <!-- individual results are shown in this table -->
  <table id="resultsTable" border="5">
    <tr class="headers">
      <th>Build</th>
      <th>Upload Time</th>
      <th>Operation Time</th>
      <th>Fetch binary time - Image (in sec)</th>
      <th>Fetch binary time - Documents(in sec)</th>
      <th>Fetch binary time - Video (in sec)</th>
      <th>Get Api time - assetID(in sec)</th>
      <th>Get Api time - assetName(in sec)</th>
      <th>Get Api time - AssetCustomURL(in sec)</th>
      <th>Anonymous Fetch binary time - Image (in sec)</th>
      <th>Anonymous Fetch binary time - Documents(in sec)</th>
      <th>Anonymous Fetch binary time - Video (in sec)</th>
    </tr>
    <xsl:for-each select="results/dam_performance_run">
      <tr>
        <td>
             <a>
            <xsl:attribute name="href">
              <xsl:value-of select="$pUrlPath"/>
              <xsl:value-of select="@buildName"/>
              <xsl:value-of select="'/dam-jtl-Report-njdc.html'"/>
            </xsl:attribute>
            <xsl:value-of select="@buildName"/>
          </a>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="(number(substring(@uploadTime,1,2))*60)+(number(substring(@uploadTime,11,2))) &gt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:when test="( (number(substring(@uploadTime,1,2))*60)+(number(substring(@uploadTime,11,2))) &gt; (number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))) ) and ( (number(substring(@uploadTime,1,2))*60)+(number(substring(@uploadTime,11,2))) &lt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100))">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@uploadTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@uploadTime"/>
        </td>
         <td>
          <xsl:attribute name="class">
          <xsl:choose>
              <xsl:when test="((number(substring(@operationsTime,1,2))*60)+(number(substring(@operationsTime,11,2)))) &gt; ((((number(substring($pOperationThresholdValue,1,2))*60)+(number(substring($pOperationThresholdValue,11,2))))*105) div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( (number(substring(@operationsTime,1,2))*60)+(number(substring(@operationsTime,11,2))) &gt; (number(substring($pOperationThresholdValue,1,2))*60)+(number(substring($pOperationThresholdValue,11,2))) ) and ( (number(substring(@operationsTime,1,2))*60)+(number(substring(@operationsTime,11,2))) &lt; ((((number(substring($pOperationThresholdValue,1,2))*60)+(number(substring($pOperationThresholdValue,11,2))))*105) div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
               <xsl:when test="contains(@operationsTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@operationsTime"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
             <xsl:when test="number(substring(@imageFetchBinaryTime,1,5)) &gt; (((number(substring($pFetchBinaryImageThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( number(substring(@imageFetchBinaryTime,1,5)) &gt; number(substring($pFetchBinaryImageThresholdValue,1,5)) ) and ( number(substring(@imageFetchBinaryTime,1,5)) &lt; (((number(substring($pFetchBinaryImageThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@imageFetchBinaryTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@imageFetchBinaryTime"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
               <xsl:when test="number(substring(@documentFetchBinaryTime,1,5)) &gt; (((number(substring($pFetchBinaryDocumentThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( number(substring(@documentFetchBinaryTime,1,5)) &gt; number(substring($pFetchBinaryDocumentThresholdValue,1,5)) )and( number(substring(@documentFetchBinaryTime,1,5)) &lt; (((number(substring($pFetchBinaryDocumentThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@documentFetchBinaryTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@documentFetchBinaryTime"/>
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
             <xsl:when test="number(substring(@videoFetchBinaryTime,1,5)) &gt; (((number(substring($pFetchBinaryVideoThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@videoFetchBinaryTime,1,5)) &gt; number(substring($pFetchBinaryVideoThresholdValue,1,5)) ) and ( number(substring(@videoFetchBinaryTime,1,5)) &lt; (((number(substring($pFetchBinaryVideoThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@videoFetchBinaryTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@videoFetchBinaryTime"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="number(substring(@getApiTimeByAssetId,1,5)) &gt; (((number(substring($pGetApiThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:when test="( number(substring(@getApiTimeByAssetId,1,5)) &gt; number(substring($pGetApiThresholdValue,1,5)) )and( number(substring(@getApiTimeByAssetId,1,5)) &lt; (((number(substring($pGetApiThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@getApiTimeByAssetId,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@getApiTimeByAssetId"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
               <xsl:when test="number(substring(@getApiTimeByAssetName,1,5)) &gt; (((number(substring($pGetApiThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( number(substring(@getApiTimeByAssetName,1,5)) &gt; number(substring($pGetApiThresholdValue,1,5)) )and( number(substring(@getApiTimeByAssetName,1,5)) &lt; (((number(substring($pGetApiThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@getApiTimeByAssetName,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@getApiTimeByAssetName"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <xsl:when test="number(substring(@getApiTimeByCustomURL,1,5)) &gt; (((number(substring($pGetApiThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( number(substring(@getApiTimeByCustomURL,1,5)) &gt; number(substring($pGetApiThresholdValue,1,5)) ) and ( number(substring(@getApiTimeByCustomURL,1,5)) &lt; (((number(substring($pGetApiThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@getApiTimeByCustomURL,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@getApiTimeByCustomURL"/>
        </td>
             <td>
          <xsl:attribute name="class">
            <xsl:choose>
             <xsl:when test="number(substring(@anonymousImageFetchBinaryTime,1,5)) &gt; (((number(substring($pAnonymousFetchBinaryImageThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( number(substring(@anonymousImageFetchBinaryTime,1,5)) &gt; number(substring($pAnonymousFetchBinaryImageThresholdValue,1,5)) ) and ( number(substring(@anonymousImageFetchBinaryTime,1,5)) &lt; (((number(substring($pAnonymousFetchBinaryImageThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@anonymousImageFetchBinaryTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymousImageFetchBinaryTime"/>
        </td>         
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
               <xsl:when test="number(substring(@anonymousDocumentFetchBinaryTime,1,5)) &gt; (((number(substring($pAnonymousFetchBinaryDocumentThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="( number(substring(@anonymousDocumentFetchBinaryTime,1,5)) &gt; number(substring($pAnonymousFetchBinaryDocumentThresholdValue,1,5)) )and( number(substring(@anonymousDocumentFetchBinaryTime,1,5)) &lt; (((number(substring($pAnonymousFetchBinaryDocumentThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@anonymousDocumentFetchBinaryTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymousDocumentFetchBinaryTime"/>
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
             <xsl:when test="number(substring(@anonymousVideoFetchBinaryTime,1,5)) &gt; (((number(substring($pAnonymousFetchBinaryVideoThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousVideoFetchBinaryTime,1,5)) &gt; number(substring($pAnonymousFetchBinaryVideoThresholdValue,1,5)) ) and ( number(substring(@anonymousVideoFetchBinaryTime,1,5)) &lt; (((number(substring($pAnonymousFetchBinaryVideoThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when>
              <xsl:when test="contains(@anonymousVideoFetchBinaryTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymousVideoFetchBinaryTime"/>
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