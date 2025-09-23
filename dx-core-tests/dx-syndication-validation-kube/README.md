# DX Syndication Kube Validation pipeline

This pipeline executes DX Syndication validation tests in Kube environment.

# Steps

1. Creates two native kube deployment
2. Deploys cross-reference libraries on the Syndicator kube instance
3. Creates a new library on the Syndicator kube instance
4. Enables syndication relationship between the two kube instances
5. Populate the newly created library with content and new JCR types
6. Verifies that the contents of cross-reference library and new JCR types are successfully syndicated.

# Usage

There are two ways to test syndication using this pipeline:

![FinalDiagram](https://media.git.cwp.pnp-hcl.com/user/3347/files/322fcd00-b992-11ec-8d3a-f62f7e99bfaa)


1. Deploy existing wcm library to the syndicator instance

In this approach, you can add wcm library zip file in helpers folder similar to how it is done for SyndicationLibraries.zip and in the setup_syndicator.sh script file, we can deploy the that using the import-wcm-library ConfigEngine task.

Once the libraries are deployed and syndication relation is established(in setup_subscriber), you can use validate_syndication.sh to validate contents of your library that was deployed to the syndicator instance is now successfully syndicated to the subscriber instance. In the validate_syndication.sh, we are using WCM REST APIs to validate component, contents and types exists in the subscriber instance, for example:

```
echo "Verifying if Menu components is succesfully syndicated from library SyndicationLibraryA"
curl -b subscriberCookie.txt -s  -o MenuA.txt -w "%{http_code}" --location --request GET "http://${SUBSCRIBER_IP}:10039/wps/mycontenthandler/wcmrest-v2/component/menus?libraryID=${SyndicationLibraryA_ID}" \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--header 'Cookie: JSESSIONID=000045dibNHXRabRpPXK09BMLJN:-1; LtpaToken2=wjWfRG0zynIzFQiEqPwnaq36NOq8tiDi2qVIUgcoHUTrsgbcq2lw+2VuV+OOOHUGmyVEGNl2L0uRw3MKZIOsGPOcWeiXxxX+4CNXZ58pdJh07aUG4/SYAOymBMM97CJZXJR6SaIBAkgh9q4w8fD2sk7inPyYZzoNvH8o+4/HYdWJOaWYZTc41+4iiq0evxbWNJwH0hc84y4yX4bcWuTIpiB9QugwpQaQ/JSiz1Y4xf/mrOxwwpKzQ9pawLUzuRrmlQzHjn7dstkn6oc8UY6eiow32EbVXp9z3t6CveKVcVZYJSzHEMJq6fzYOqFNwnTcn+J3gFFwPCsYs9O37VZV3+6dhRm4tlK6bcqMdbiWAKREr6xdOpIjRAf5NQzEh5osBTw4NOTZVyPxFvMGaNEL8a2BoTRjq0P5/GUFujFcNW1ytzAFU7i6kEHkbq577ecqwbltF1UkQTZtWC/wTstjFigR8ndbn6vKP9DXelknfTVyi6+VkzjGGSHb5CRPmTiDrlU9TbVgBSU35x6+wrq4rCV+BjDT427Foechnrc7hpFBzZEImEEM+CI4x6oLNG8No4USJajkmv4PvWe81I3sYi5H9r4plHljRe5j0r6ihWUhV8Lu1kAZXNwJ7lal0m77cP/5O5qZsO2gyPp2wowTRxjOWRBEltYCi0i+PljwE90=' \
--data '{}'
```

2. Another approach, to create a new library using REST API during the Prepare Syndicator stage - setup_syndicator.sh:

```
echo "Create Syndication Library"
curl -b cookie.txt -s  -o SyndicationLibrary.txt -w "%{http_code}" --location --request POST "http://${SYNDICATOR_IP}:10039/wps/mycontenthandler/wcmrest/Library?mime-type=application/json" \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=0000lmG0NZCvpX7miPAqmF6jSW4:-1; LtpaToken2=cD4ZjDInifWJHEu9X0WuLspqRb4xbgryuIyBMGTN8/Gp5c5gHa4MFUihyrcwloQ2FblUEpnYqFmiqYi0Agvsrj5teS7z7GSoeiTN7b8oAVRlDCm5j2l3noTJuFoU0+mes34sYJmzINMcgNYoaJIEJQ43Gk5XspBpvhk9q4vokq2hDiPQbVDRxWp2QhR5JAXyCid9QalmYPgaVkH94bfXZih/tB1caB5xZA3hZkCEnpxKTR6nr/jlQJvk/MyRZ5quK1UymB8fBMTTvLjNuHzuQ8XSSdWrhY72v2KoCn/H4YFbWVf9aZ5xvMlKiqFWhCt8NhpgqigijfZDcBNIvbTFDAvojfXpW5n8NlpUkiOqXVd7R7D7LXVIq7gMfsLAXXNz+aiOzcj3aSDVw/LbWvhCX+KP409m0CKaA0gg2vkbJD9jl7vjH718q1MvV8XZowQEgN1VeMWDFAMOmRlpYmIHUVLah9/mylkhRNaJobK7K0OiA8JtfcB+D9JJRqjOqPpyd9N/D1/E1Qgr4qQt89Pnz+i9Od0+eR16zQRTpsfLUzlssrukRtE4TbSEySK6rG4P6p4KJVr42lcnTXz3iRXUG4blprW2fes8sFTjYO+3W8aoTUlbjyUZfDqg2jI8F0BAVvKqfELUbRMsAZjaStoaYwn0mZAu1Nuzi6OuwY7IE90=' \
--data '<?xml version="1.0" encoding="UTF-8"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:wcm="http://www.ibm.com/xmlns/wcm/8.0">
    <wcm:name>SyndicationLibrary</wcm:name>
    <content type="application/vnd.ibm.wcm+xml">
        <wcm:library xmlns="http://www.ibm.com/xmlns/wcm/8.0">
            <allowDeletion>false</allowDeletion>
            <enabled>true</enabled>
            <language>en</language>
            <includeDefaultItems>true</includeDefaultItems>
        </wcm:library>
    </content>
</entry>'
```

Since this a new library, you can populate this library with various artifacts like template, content, components by using REST APIs. Use create_artifacts.sh file to populate various artifacts.

Once the library and new artifacts are created and syndication relation is established(in setup_subscriber), you can use validate_syndication.sh to validate contents of your library that was deployed to the syndicator instance is now successfully syndicated to the subscriber instance. In the validate_syndication.sh, we are using WCM REST APIs to validate component, contents and types exists in the subscriber instance.

### Syndication between 3 instances

If the THREE_WAY_SYNDICATION parameter in the Jenkins pipeline is enabled then the pipeline would create 3 new EC2 instances, System A, B and C, using the latest 9.5 CF. First, it will establish syndication between System A and System B where A is syndicator and B is subscriber. Similarly, syndication is setup between System B and System C, where B is syndicator here and C is subscriber. So libraries that were imported and new artifacts that were created in System A are now validated that it first exists in System B and then in System C.

<img width="530" alt="image" src="https://media.git.cwp.pnp-hcl.com/user/3347/files/9fe4b180-c167-11ec-9cbd-ec3113999715">

### Define which libraries to syndicate

Library names to Syndicate for eg. Web Content
To Syndicate multiple libraries use ',' For eg. Web Content, LibraryA, LibraryB

### Define which items in the library to syndicate

Select which items from the library to syndicate. Here -Dpublished-items will syndicate only published and expired items. -Dall-items is used to gradually syndicate projects to a staging or delivery server, rather than waiting until all items in a project achieve a published state. Using this option it will syndicate: Published, Expired, Draft items and Projects that contain draft items saved in the configured library. For -Dall-items-and-versions, in addition to items syndicated by all items, it will also sydicate versions and deleted items.

### How to import and syndicate library

If you have a library that you would like to add to the Syndication validation framework then it can be automated in the following way:
1. You can commit the zip/tar version of the library to be imported either in the Portal-Test-Automation/dx-core-tests/dx-syndication-validation/helpers (for eg. SyndicationLibraries.zip) or could be in a remote repo (for eg. design-studio-sample-libraries -> surf-city). In the case of remote repo, in the "Prepare Syndicator instance" stage, ensure to add a step to clone that library to be imported.
2. Copy the library to be imported to Syndicator instance. For eg. /opt/zips
3. Create a new directory and extract zip or tar file. For eg. /opt/zips/SyndicationLibraries, /opt/zips/SurfCityLibrary
4. In the setup_syndicator.sh script file, update the -Dimport.directory arg with the path of the extracted directory that you would like to be imported. For eg. -Dimport.directory="/opt/zips/SyndicationCrossReferenceLibraries;/opt/zips/SurfCityLibrary"
5. In the Jenkins pipeline, SYNDICATE_LIBRARIES parameter needs to be updated to include the name of the library that was imported, so that it gets syndicated to the subrsciber instance. For eg. Web Content,SyndicationLibrary,Syndication_LibraryA,Syndication_LibraryB,surfcity-surfshop
6. In the validate_syndication.sh script file, using REST APIs call validate the content of the imported library in the Syndicator instance is now syndicated in the Subscriber instance.

Currently, Syndication validation framework has automated importing and syncation of SyndicationLibraries and SurfCityLibrary. See "Prepare Syndicator instance" stage in the Jenkinsfile for more information.
