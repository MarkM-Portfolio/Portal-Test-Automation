<!-- Licensed Materials - Property of HCL 
Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved.
Note to US Government Users Restricted Rights:
Use, duplication or disclosure restricted by GSA ADP Schedule 
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- correct build name param value should be passed in -->
  <xsl:param name="pBuildName" select="'DX_Core_20210228-055521_rohan_develop_linux_db2_cf17_wtf2'"/>
  <xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>
  <xsl:template match="/">
    <wtf_run>
      <xsl:attribute name="buildName">
        <xsl:value-of select="$pBuildName"/>
      </xsl:attribute>
      <xsl:attribute name="tests">
        <xsl:value-of select="sum(results/testsuite/@tests)" />
      </xsl:attribute>
      <xsl:attribute name="failures">
        <xsl:value-of select="sum(results/testsuite/@failures)" />
      </xsl:attribute>
      <xsl:attribute name="errors">
        <xsl:value-of select="sum(results/testsuite/@errors)" />
      </xsl:attribute>
      <xsl:attribute name="skipped">
        <xsl:value-of select="sum(results/testsuite/@skipped)" />
      </xsl:attribute>
    </wtf_run>
  </xsl:template>
</xsl:stylesheet>
