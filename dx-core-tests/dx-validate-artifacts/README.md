# dx-validate-artifacts pipeline

Validates dx core artifacts in a given server using rest api and UI tests.
Note: Server needs to be setup with 9.5 and Site Management enabled

Validates following artifacts:
1. Validates VP(Import-ArtifactsVP4)
2. Validates WCM Libraries
	a. Validates ImportArtifactsLibrary
	b. Validates ImportArtifactsContentTemplate
	c. Validates ImportArtifactsContent
	d. Validates ImportArtifactsStylesheetsName(v2)
	e. Validates ImportArtifactsContentSites(v2)
	f. Validates ImportArtifactsContentSitesPage(v2)
3. Validates Script application(ImportArtifactsScriptApp) and page(ImportArtifactsPage1)
4. Validates WAB and page(ImportArtifactsPage2)
