<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2022. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- correct build name param value should be passed in -->
  <xsl:param name="pBuildName" select="'DX_Dam_Performance_test_default'"/>
  <xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>
  <xsl:template match="/">
    <dam_performance_run>
      <xsl:attribute name="buildName">
        <xsl:value-of select="$pBuildName"/>
      </xsl:attribute>
      <xsl:attribute name="uploadTime">
        <xsl:value-of select="results/dam_performance_run/@uploadTime" />
      </xsl:attribute>
      <xsl:attribute name="operationsTime">
        <xsl:value-of select="results/dam_performance_run/@operationsTime" />
      </xsl:attribute>
      <xsl:attribute name="Total_ResponseTime">
        <xsl:value-of select="results/dam_performance_run/@Total_ResponseTime" />
      </xsl:attribute>
      <xsl:attribute name="Total_Throughput">
        <xsl:value-of select="results/dam_performance_run/@Total_Throughput" />
      </xsl:attribute>
      <xsl:attribute name="anonymous_renderTime_jpg_2mb_id_smartphone">
        <xsl:value-of select="results/dam_performance_run/@anonymous_renderTime_jpg_2mb_id_smartphone" />
      </xsl:attribute>
      <xsl:attribute name="anonymous_renderTime_png_500kb_furl_tablet">
        <xsl:value-of select="results/dam_performance_run/@anonymous_renderTime_png_500kb_furl_tablet" />
      </xsl:attribute>
      <xsl:attribute name="anonymous_renderTime_mp4_15mb_id">
        <xsl:value-of select="results/dam_performance_run/@anonymous_renderTime_mp4_15mb_id" />
      </xsl:attribute>
      <xsl:attribute name="anonymous_renderTime_webm_2mb_furl">
        <xsl:value-of select="results/dam_performance_run/@anonymous_renderTime_webm_2mb_furl" />
      </xsl:attribute>
      <xsl:attribute name="anonymous_renderTime_pdf_5mb_furl">
        <xsl:value-of select="results/dam_performance_run/@anonymous_renderTime_pdf_5mb_furl" />
      </xsl:attribute>
      <xsl:attribute name="anonymous_renderTime_docx_199kb_id">
        <xsl:value-of select="results/dam_performance_run/@anonymous_renderTime_docx_199kb_id" />
      </xsl:attribute>
    </dam_performance_run>
  </xsl:template>
</xsl:stylesheet>
