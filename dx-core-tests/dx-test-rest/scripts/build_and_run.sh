#!/bin/sh
# ********************************************************************
# * Licensed Materials - Property of HCL                             *
# *                                                                  *
# * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
# *                                                                  *
# * Note to US Government Users Restricted Rights:                   *
# *                                                                  *
# * Use, duplication or disclosure restricted by GSA ADP Schedule    *
# ********************************************************************

source ~/.bash_profile 

# Build FEs
cd /opt/git/git-dx-clone/prereqs.infra/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/prereq/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/lwp04.infra/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/prereqs.was/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/buildcontrib/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/base/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/spellchecker/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/cfgfw/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/jcr/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/ext/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/people.api/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/ui/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.base/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/wcm.common/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/caf/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/wcm.api/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/dcs/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/portal9.packaging/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/pzn/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/solutionInstaller/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/people.impl/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/wcm.impl/wp && wsbld buildSelf

cd /opt/git/git-dx-clone/wcm.dam/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/wcm.ui/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/bp/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/search/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/pzn.ext/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/ap/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/theme/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/scriptingportlet/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/pzn.ui/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.scriptingportlet/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/sitebuilder/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/dch.integ/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.ext/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/toolbar/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/portalsamples/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/installer/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.search/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/contents/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/cf.diff/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/jcr.tools/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/wcm.ext/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.pzn.ext/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/cfgfw.tools/wp && wsbld buildSelf

cd /opt/git/git-dx-clone/test.theme/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.toolbar/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.dcs/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.jcr/wp && wsbld buildSelf
cd /opt/git/git-dx-clone/test.contents/wp && wsbld buildSelf

# Run unit test
cd /opt/git/git-dx-clone/wcm.impl/wp/code/test.rest && wsbld test


# Get coverage report
cd /opt/git/git-dx-clone/wcm.impl/wp/code/test.rest && wsbld coverage-report