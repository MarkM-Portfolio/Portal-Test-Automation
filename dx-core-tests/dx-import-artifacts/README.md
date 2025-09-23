# dx-import-artifacts pipeline

Imports dx core artifacts to a given server using rest api and dxclient.
Note: Server needs to be setup with 9.5 and Site Management enabled

Imports following artifacts:
1. Imports VP(Import-ArtifactsVP4)
2. Imports WCM Libraries
	a. Creates ImportArtifactsLibrary
	b. Creates ImportArtifactsContentTemplate
	c. Creates ImportArtifactsContent
	d. Creates ImportArtifactsStylesheetsName(v2)
	e. Creates ImportArtifactsContentSites(v2)
	f. Creates ImportArtifactsContentSitesPage(v2)
3. Imports Script application(ImportArtifactsScriptApp) and page(ImportArtifactsPage1)
4. Imports WAB and page(ImportArtifactsPage2)
