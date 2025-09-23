#!/usr/bin/env python

# Licensed Materials - Property of HCL                             #
#                                                                  #
# Copyright HCL Technologies Ltd. 2001, 2021. All Rights Reserved. #
#                                                                  #
# Note to US Government Users Restricted Rights:                   #
#                                                                  #
# Use, duplication or disclosure restricted by GSA ADP Schedule    #

import sys
from xml.etree import ElementTree

def run(files):
    first = None
    for filename in files:
        data = ElementTree.parse(filename).getroot()
        if first is None:
            first = data
        else:
            first.extend(data)
    if first is not None:
        print (ElementTree.tostring(first))

if __name__ == "__main__":
    run(sys.argv[1:])