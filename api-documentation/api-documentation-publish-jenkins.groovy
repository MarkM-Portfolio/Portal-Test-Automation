// Import the shared library with the name configured in Jenkins
@Library('dx-shared-library') _

// Create object to store parameters with values
def pipelineParameters = [:]

// some miscelaneous variables that are globally used
def repoCheckoutDirectory = 'hclDxGitRepo'
def credentialsId = 'jenkins-git'

pipeline {
    agent {
        label 'build_docu'
    }

    stages {
        stage('Load parameters') {
            steps {
                dxParametersLoadFromFile(pipelineParameters, "${env.WORKSPACE}/api-documentation/parameters.yaml")
            }
        }

        stage('Build settings and checkout target repository') {
            steps {
                script {
                    // Build GP pages URL using github.io
                    if (pipelineParameters.HCL_DX_GITHUB_HOST.contains('github')) {
                        // Split the github host at the : sign, leaving us with the fitting name for the github.io pages viewer
                        def pagesUser = pipelineParameters.HCL_DX_GITHUB_HOST.split(':')[1]
                        pipelineParameters.GITHUB_PAGES_URL = "https://${pagesUser}.github.io/${pipelineParameters.REPOSITORY_NAME}"
                    } else {
                        // Build a real pages URL manually
                        // We create the full pseudo URL and remove the git@ prefix
                        def pagesUrl = "${pipelineParameters.HCL_DX_GITHUB_HOST}/${pipelineParameters.REPOSITORY_NAME}"
                        // Remove the git@ prefix, to make it compatible with pages.
                        pagesUrl = pagesUrl.split['@'][1]
                        // replace any : with a / to create a full URL
                        pagesUrl = pagesUrl.replaceAll(':','/')
                        pipelineParameters.GITHUB_PAGES_URL = pagesUrl
                    }
                    println "Created Github pages URL > ${pipelineParameters.GITHUB_PAGES_URL} <"
                    
                    sh "mkdir -p ${repoCheckoutDirectory}"
                    // Move into a fixed directory to perform the checkout
                    dir(repoCheckoutDirectory) {
                        // Checkout URL consists of host and target repository name
                        // and doesn't need credentials when using https
                        def checkoutUrl = "https://" + ("${pipelineParameters.HCL_DX_GITHUB_HOST}/${pipelineParameters.REPOSITORY_NAME}".split('@')[1]).replaceAll(':','/')
                        sh "git clone ${checkoutUrl}"
                    }
                }
            }
        }

        stage('Handle application repositories') {
            steps {
                script {
                    // Split the list of application repositories by comma
                    def appRepositories = pipelineParameters.APP_GIT_REPOS.replace(" ", "").split(',')
                    for (app in appRepositories) {
                        // Since we are using a comma separated list, it's important to filter out tailing and leading spaces
                        def currentApp = app.trim()
                        println "Handling application repository > ${currentApp} < "
                        // Create a directory where we can check out the code
                        sh "mkdir -p ${currentApp}"
                        dir(currentApp) {
                            // Create a placeholder for the tag
                            def appTag = ''
                            switch(currentApp) {
                                case "RingAPI":
                                    appTag = pipelineParameters.ENV_TAG_RING_API
                                    break;
                                case "media-library":
                                    appTag = pipelineParameters.ENV_TAG_DAM_API
                                    break;
                                case "image-processor":
                                    appTag = pipelineParameters.ENV_TAG_IP_API
                                    break;
                                default:
                                    error("The list of apps contains a unknown app.")
                            }
                            println "Tag used for app > ${currentApp} < is > ${appTag} <"
                            // Perform a checkout of the source app repo
                            checkout([$class: 'GitSCM', 
                                branches: [[
                                    name: "refs/tags/${appTag}"
                                ]], 
                                userRemoteConfigs: [[
                                    credentialsId: "${credentialsId}", 
                                    url: "git@git.cwp.pnp-hcl.com:websphere-portal-incubator/${currentApp}.git"
                                ]]
                            ])
                            // Use NVM wrapper to run NodeJS
                            nvm("${env.G_NVM_NODE_VERSION}") {
                                withNPM(npmrcConfig: 'npmrc') {
                                    // Move to server directory of app
                                    dir("packages/server-v1/") {
                                        def appFolder = ''
                                        def appTitle = ''
                                        // Generate title and folder name
                                        // Run the build command to create the openapi.json
                                        switch(currentApp) {
                                            case "RingAPI":
                                                appFolder = 'ring-api'
                                                appTitle = 'Ring API'
                                                sh "make install && make build"
                                                break;
                                            case "media-library":
                                                appFolder = 'dam-api'
                                                appTitle = 'DAM API'
                                                sh "make install && make build"
                                                break;
                                            case "image-processor":
                                                appFolder = 'image-processor-api'
                                                appTitle = 'Image Processor API'
                                                sh "make install && make build"
                                                break;
                                            default:
                                                error("The list of apps contains a unknown app.")
                                        }
                                        println "Specs generated for > ${appTitle} < in directory > ${appFolder} <"

                                        def openapiUpdate='''"x-logo":{"url":"../DX_logo.svg","backgroundColor":"#F2F4F5","altText":"HCL_Digital_Experience"},"description":'''
                                        def openapitoBeUpdated= '"description":'

                                        // Add the spec to the target repository
                                        sh """
                                            mkdir -p "${WORKSPACE}/${repoCheckoutDirectory}/${repoCheckoutDirectory}/${appFolder}"
                                            cp "openapi/specs/openapi_en.json" "${WORKSPACE}/${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}/openapi.json"
                                            cd "${WORKSPACE}"
                                            cp "api-documentation/baseFiles/index.html"  "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}/index.html"
                                            sed -i "s/APINAME/${appTitle}/g" "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}/index.html"
                                            cp "api-documentation/experience-apis/${appFolder}/README.md"  "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}/README.md"
                                            sed -i "s|GITHUB_PAGES_URL|${pipelineParameters.GITHUB_PAGES_URL}|g" "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}/README.md"
                                            sed -i 's|${openapitoBeUpdated}|${openapiUpdate}|1' "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}/openapi.json"
                                            cd "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${appFolder}" 
                                            (git status) && (git add openapi.json index.html README.md) && (git status)
                                        """
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Handle DX Core components') {
            steps {
                script {
                    // Split the list of application repositories by comma
                    def coreComponents = pipelineParameters.CORE_COMPONENTS.replace(" ", "").split(',')

                    for (component in coreComponents) {
                        // Since we are using a comma separated list, it's important to filter out tailing and leading spaces
                        def currentComponent = component.trim()
                        println "Handling core application > ${currentComponent} <"

                        // Create placeholder variables for tag, folder name, title and internal folder
                        def compontentFolder = ''
                        def internalComponentFolder = ''
                        def componentTag = ''
                        def componentTitle = ''

                        switch(currentComponent) {
                            case "pzn":
                                componentFolder = 'pzn-api'
                                internalComponentFolder = 'pzn/v1'
                                componentTag = pipelineParameters.ENV_TAG_PZN_API
                                componentTitle = 'Personalization API'
                                break;
                            case "wcm-v2":
                                componentFolder = 'wcm-api'
                                internalComponentFolder = 'wcm/v2'
                                componentTag = pipelineParameters.ENV_TAG_WCMV2_API
                                componentTitle = 'WCM API'
                                break;
                            case "access":
                                componentFolder = 'access-api'
                                internalComponentFolder = 'access/v1'
                                componentTag = pipelineParameters.ENV_TAG_ACCESS_API
                                componentTitle = 'Access Control API'
                                break;
                            case "remotemodel":
                                componentFolder = 'remotemodel-api'
                                internalComponentFolder = 'remotemodel/v1'
                                componentTag = pipelineParameters.ENV_TAG_REMOTEMODEL_API
                                componentTitle = 'Remote Model API'
                                break;
                            case "search":
                                componentFolder = 'search-api'
                                internalComponentFolder = 'search/v1'
                                componentTag = pipelineParameters.ENV_TAG_SEARCH_API
                                componentTitle = 'Search API'
                                break;
                            case "users":
                                componentFolder = 'users-api'
                                internalComponentFolder = 'users/v1'
                                componentTag = pipelineParameters.ENV_TAG_USERS_API
                                componentTitle = 'Users and Groups API'
                                break;
                            default:
                                error("Component not found in the list of components")
                        }

                        def openapiUpdate='''"x-logo":{"url":"../DX_logo.svg","backgroundColor":"#F2F4F5","altText":"HCL_Digital_Experience"},"description":'''
                        def openapitoBeUpdated= '"description":'

                        // Create dir
                        sh "mkdir -p ${currentComponent}"

                        dir(currentComponent) {
                            // Check out the DX Core Theme repository
                            checkout([$class: 'GitSCM', 
                                branches: [[
                                    name: "refs/tags/${componentTag}"
                                ]], 
                                userRemoteConfigs: [[
                                    credentialsId: "${credentialsId}", 
                                    url: "git@git.cwp.pnp-hcl.com:websphere-portal/theme.git"
                                ]]
                            ])

                            // Add the changed code to the commit
                            sh """
                                mkdir -p "${WORKSPACE}/${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}"
                                cp wp/code/wp.api.explorer/src/Web-Content/specs/${internalComponentFolder}/openapi.json "${WORKSPACE}/${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}/openapi.json"
                                cd "${WORKSPACE}"
                                cp "api-documentation/baseFiles/index.html"  "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}/index.html"
                                sed -i "s/APINAME/${componentTitle }/g" "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}/index.html"
                                cp "api-documentation/experience-apis/${componentFolder}/README.md"  "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}/README.md"
                                sed -i "s|GITHUB_PAGES_URL|${pipelineParameters.GITHUB_PAGES_URL}|g" "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}/README.md"
                                sed -i 's|${openapitoBeUpdated}|${openapiUpdate}|1' "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}/openapi.json"
                                cd "${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}/${componentFolder}" 
                                (git status) && (git add openapi.json index.html README.md) && (git status)
                            """
                        }
                    }
                }
            }
        }

        stage ('Readme, Logo add to HCL-DX Repository') {
            steps {
                println "Adding README, logo and lib to the target repository."
                sh """ 
                    cp api-documentation/experience-apis/README.md ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                    cp api-documentation/experience-apis/CODE_OF_CONDUCT.md ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                    cp api-documentation/experience-apis/LICENSE ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                    cp -r api-documentation/experience-apis/lib ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                    cp -p api-documentation/experience-apis/DX_logo.svg ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                    cd ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                    (git rm ./lib/LICENSE.txt) || (echo "LICENSE already removed from lib")
                    sed -i "s|GITHUB_PAGES_URL|${pipelineParameters.GITHUB_PAGES_URL}|g" README.md
                    (git status) && (git add .) && (git status) 
                """
            }
        }

        stage ('Publishing specs to HCL-DX Repository') {
            steps {
                script {
                    println "Publishing specs to HCL-DX Repository"
                    sh """ cd ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME} && (git status) && (git commit -m "updating API Documentation release (version ${pipelineParameters.ENV_TAG_RING_API})" )  || (echo "nothing to commit, working tree is upto date") """
                    if (env.HCL_DX_GITHUB_ACCESS_TOKEN) {
                        println "Upload using HCL_DX_GITHUB_ACCESS_TOKEN"
                        def checkinUrl = "https://${env.HCL_DX_GITHUB_ACCESS_TOKEN}@" + ("${pipelineParameters.HCL_DX_GITHUB_HOST}/${pipelineParameters.REPOSITORY_NAME}".split('@')[1]).replaceAll(':','/')
                        wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[var: 'HCL_DX_GITHUB_ACCESS_TOKEN', password: HCL_DX_GITHUB_ACCESS_TOKEN]], varMaskRegexes: []]) {
                            sh """
                                cd ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME}
                                git remote set-url origin ${checkinUrl}.git
                                git push origin HEAD
                            """
                        }
                    } else {
                        println "Upload using HCL_DX_GITHUB_CREDENTIAL_ID"
                        sshagent(credentials: ["${pipelineParameters.HCL_DX_GITHUB_CREDENTIAL_ID}"]) {
                            sh "cd ${repoCheckoutDirectory}/${pipelineParameters.REPOSITORY_NAME} && (git push origin HEAD)"
                        }
                    }
                }
            }
        }
    }

    post {
        cleanup {
            /* Cleanup workspace */
            dxWorkspaceDirectoriesCleanup()
        }
    }
}
