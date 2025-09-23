<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2022. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- correct WTF name param value should be passed in -->
<xsl:param name="pWTF" select="'Dam'"/>
<xsl:param name="pUploadThresholdValue" select="'278'"/>
<xsl:param name="pOperationThresholdValue" select="'2313'"/>
<xsl:param name="pTotal_ResponseTime" select="'0.501'"/>
<xsl:param name="pTotal_Throughput" select="'8.4'"/>
<xsl:param name="pAnonymous_renderTime_jpg_2mb_id_smartphone" select="'0.481'"/>
<xsl:param name="pAnonymous_renderTime_png_500kb_furl_tablet" select="'0.432'"/>
<xsl:param name="pAnonymous_renderTime_mp4_15mb_id" select="'0.735'"/>
<xsl:param name="pAnonymous_renderTime_webm_2mb_furl" select="'0.584'"/>
<xsl:param name="pAnonymous_renderTime_pdf_5mb_furl" select="'0.638'"/>
<xsl:param name="pAnonymous_renderTime_docx_199kb_id" select="'0.501'"/>


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
      <th class="thresholdWidth">Upload Time (in sec)</th>
      <th class="thresholdWidth">Operation Time (in sec)</th>
      <th class="thresholdWidth">Total Average Response(in sec)</th>
      <th class="thresholdWidth">Throughput(/sec)</th>
      <th class="thresholdWidth">Anonymous renderTime jpg 2mb id smartphone (in sec)</th>
      <th class="thresholdWidth">Anonymous renderTime png 500kb furl tablet (in sec)</th>
      <th class="thresholdWidth">Anonymous renderTime mp4 15mb id (in sec)</th>
      <th class="thresholdWidth">Anonymous renderTime webm 2mb furl (in sec)</th>
      <th class="thresholdWidth">Anonymous renderTime pdf 5mb furl (in sec)</th>
      <th class="thresholdWidth">Anonymous renderTime docx 199kb id (in sec)</th>
    </tr>
    <tr>
      <td class="center-text">CF212(release)</td>
      <td class="center-text"><xsl:value-of select="$pUploadThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pOperationThresholdValue"/></td>
      <td class="center-text"><xsl:value-of select="$pTotal_ResponseTime"/></td>
      <td class="center-text"><xsl:value-of select="$pTotal_Throughput"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymous_renderTime_jpg_2mb_id_smartphone"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymous_renderTime_png_500kb_furl_tablet"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymous_renderTime_mp4_15mb_id"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymous_renderTime_webm_2mb_furl"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymous_renderTime_pdf_5mb_furl"/></td>
      <td class="center-text"><xsl:value-of select="$pAnonymous_renderTime_docx_199kb_id"/></td>
    </tr>
  </table>

    <!-- Table filter -->
  <input type="text" id="filterInput" autocomplete="off" onkeyup="filterFunction()" placeholder="Search by build name..." title="Type in a build name"></input>
  <!-- individual results are shown in this table -->
  <table id="resultsTable" border="5">
    <tr class="headers">
      <th>Build</th>
      <th>Upload Time (in sec)</th>
      <th>Operation Time (in sec)</th>
      <th>Total Average Response(in sec)</th>
      <th>Throughput(/sec)</th>
      <th>Anonymous renderTime jpg 2mb id_smartphone (in sec)</th>
      <th>Anonymous renderTime png 500kb furl tablet (in sec)</th>
      <th>Anonymous renderTime mp4 15mb id (in sec)</th>
      <th>Anonymous renderTime webm 2mb url (in sec)</th>
      <th>Anonymous renderTime pdf 5mb furl (in sec)</th>
      <th>Anonymous renderTime docx 199kb id (in sec)</th>
    </tr>
    <xsl:for-each select="results/dam_performance_run">
      <tr>
        <td>
             <a>
            <xsl:attribute name="href">
              <xsl:value-of select="$pUrlPath"/>
              <xsl:value-of select="@buildName"/>
              <xsl:value-of select="'/dam-jtl-Report.html'"/>
            </xsl:attribute>
            <xsl:value-of select="@buildName"/>
          </a>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="(number(substring(@uploadTime,1,2))*60)+(number(substring(@uploadTime,11,2))) &gt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="((number(substring(@uploadTime,1,2))*60)+(number(substring(@uploadTime,11,2))) &gt; (number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))) ) and ( (number(substring(@uploadTime,1,2))*60)+(number(substring(@uploadTime,11,2))) &lt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100))">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
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
              <!-- <xsl:when test="((number(substring(@operationsTime,1,2))*60)+(number(substring(@operationsTime,11,2)))) &gt; ((((number(substring($pOperationThresholdValue,1,2))*60)+(number(substring($pOperationThresholdValue,11,2))))*105) div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:when test="((number(substring(@operationsTime,1,2))*60)+(number(substring(@operationsTime,11,2))) &gt; (number(substring($pOperationThresholdValue,1,2))*60)+(number(substring($pOperationThresholdValue,11,2))) ) and ( (number(substring(@operationsTime,1,2))*60)+(number(substring(@operationsTime,11,2))) &lt; ((((number(substring($pOperationThresholdValue,1,2))*60)+(number(substring($pOperationThresholdValue,11,2))))*105) div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
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
              <!-- <xsl:when test="(number(substring(@Total_ResponseTime,1,2))*60)+(number(substring(@Total_ResponseTime,11,2))) &gt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="((number(substring(@Total_ResponseTime,1,2))*60)+(number(substring(@Total_ResponseTime,11,2))) &gt; (number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))) ) and ( (number(substring(@Total_ResponseTime,1,2))*60)+(number(substring(@Total_ResponseTime,11,2))) &lt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100))">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@Total_ResponseTime,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@Total_ResponseTime"/>
        </td>
        <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="(number(substring(@Total_Throughput,1,2))*60)+(number(substring(@Total_Throughput,11,2))) &gt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="((number(substring(@Total_Throughput,1,2))*60)+(number(substring(@Total_Throughput,11,2))) &gt; (number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))) ) and ( (number(substring(@Total_Throughput,1,2))*60)+(number(substring(@Total_Throughput,11,2))) &lt; ((((number(substring($pUploadThresholdValue,1,2))*60)+(number(substring($pUploadThresholdValue,11,2))))*105) div 100))">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@Total_Throughput,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@Total_Throughput"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="number(substring(@anonymousImageRenderTime,1,5)) &gt; (((number(substring($pimageRenderThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousImageRenderTime,1,5)) &gt; number(substring($pimageRenderThresholdValue,1,5)) ) and ( number(substring(@anonymousImageRenderTime,1,5)) &lt; (((number(substring($pimageRenderThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@anonymous_renderTime_jpg_2mb_id_smartphone,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymous_renderTime_jpg_2mb_id_smartphone"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="number(substring(@anonymousVideoRenderTime,1,5)) &gt; (((number(substring($pvideoRenderThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousVideoRenderTime,1,5)) &gt; number(substring($pvideoRenderThresholdValue,1,5)) ) and ( number(substring(@anonymousVideoRenderTime,1,5)) &lt; (((number(substring($pvideoRenderThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@anonymous_renderTime_png_500kb_furl_tablet,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymous_renderTime_png_500kb_furl_tablet"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="number(substring(@anonymousDocumentRenderTime,1,5)) &gt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousDocumentRenderTime,1,5)) &gt; number(substring($pdocumentRenderThresholdValue,1,5)) )and( number(substring(@anonymousDocumentRenderTime,1,5)) &lt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@anonymous_renderTime_mp4_15mb_id,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymous_renderTime_mp4_15mb_id"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="number(substring(@anonymousDocumentRenderTime,1,5)) &gt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousDocumentRenderTime,1,5)) &gt; number(substring($pdocumentRenderThresholdValue,1,5)) )and( number(substring(@anonymousDocumentRenderTime,1,5)) &lt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@anonymous_renderTime_webm_2mb_furl,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymous_renderTime_webm_2mb_furl"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="number(substring(@anonymousDocumentRenderTime,1,5)) &gt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousDocumentRenderTime,1,5)) &gt; number(substring($pdocumentRenderThresholdValue,1,5)) )and( number(substring(@anonymousDocumentRenderTime,1,5)) &lt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@anonymous_renderTime_pdf_5mb_furl,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymous_renderTime_pdf_5mb_furl"/>
        </td>
         <td>
          <xsl:attribute name="class">
            <xsl:choose>
              <!-- <xsl:when test="number(substring(@anonymousDocumentRenderTime,1,5)) &gt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100)">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
               <xsl:when test="(number(substring(@anonymousDocumentRenderTime,1,5)) &gt; number(substring($pdocumentRenderThresholdValue,1,5)) )and( number(substring(@anonymousDocumentRenderTime,1,5)) &lt; (((number(substring($pdocumentRenderThresholdValue,1,5)))*105)div 100) )">
                <xsl:value-of select="'center-text moderate'" />
              </xsl:when> -->
              <xsl:when test="contains(@anonymous_renderTime_docx_199kb_id,'Failed')">
                <xsl:value-of select="'center-text errors'" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'good'" />                
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="@anonymous_renderTime_docx_199kb_id"/>
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