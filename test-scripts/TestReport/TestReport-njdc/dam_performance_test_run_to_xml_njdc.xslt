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
      <xsl:attribute name="imageFetchBinaryTime">
        <xsl:value-of select="results/dam_performance_run/@imageFetchBinaryTime" />
      </xsl:attribute>
      <xsl:attribute name="documentFetchBinaryTime">
        <xsl:value-of select="results/dam_performance_run/@documentFetchBinaryTime" />
      </xsl:attribute>
      <xsl:attribute name="videoFetchBinaryTime">
        <xsl:value-of select="results/dam_performance_run/@videoFetchBinaryTime" />
      </xsl:attribute>
      <xsl:attribute name="getApiTimeByAssetId">
        <xsl:value-of select="results/dam_performance_run/@getApiTimeByAssetId" />
      </xsl:attribute>
      <xsl:attribute name="getApiTimeByAssetName">
        <xsl:value-of select="results/dam_performance_run/@getApiTimeByAssetName" />
      </xsl:attribute>
      <xsl:attribute name="getApiTimeByCustomURL">
        <xsl:value-of select="results/dam_performance_run/@getApiTimeByCustomURL" />
      </xsl:attribute>
      <xsl:attribute name="anonymousImageFetchBinaryTime">
        <xsl:value-of select="results/dam_performance_run/@anonymousImageFetchBinaryTime" />
      </xsl:attribute>
      <xsl:attribute name="anonymousDocumentFetchBinaryTime">
        <xsl:value-of select="results/dam_performance_run/@anonymousDocumentFetchBinaryTime" />
      </xsl:attribute>
      <xsl:attribute name="anonymousVideoFetchBinaryTime">
        <xsl:value-of select="results/dam_performance_run/@anonymousVideoFetchBinaryTime" />
      </xsl:attribute>
    </dam_performance_run>
  </xsl:template>
</xsl:stylesheet>