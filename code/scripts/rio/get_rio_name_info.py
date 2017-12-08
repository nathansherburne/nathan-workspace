#!/bin/bash/python
# get_rio_name_info.py
#
# This script goes to Wikipedia and downloads a table
# that contains the names of Rio's administrative 
# regions and their respective neighborhoods. Population 
# data is also included in the table for admin regions.
#
# This script fills in the spatial gap between our data 
# from Rio's website and GADM's spatial hierarchy. GADM
# does not have a level (as far as I know) for Rio's
# Neighborhoods, Admin Regions, or Planning Areas. So this
# script should ideally fill that role of providing a 
# naming/spelling convention that the automatic conversion
# and formatting scripts can use in their processing.
import urllib2
import subprocess
import os

# Save HTML from Wikipedia that has table with Neighborhood info.
url = "https://en.wikipedia.org/wiki/List_of_Administrative_Regions_in_Rio_de_Janeiro"
html = urllib2.urlopen(url).read()
homeDir = os.path.expanduser("~")
f = open(os.path.join(homeDir, "Dropbox/LEPR03/nathan-workspace/data/dengue/rio/download/wiki_table_name_info.html"), "w")
f.write(html)
f.close()

# Use custom HTML to CSV converter to make the table more accessible.
bashCommand = "~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/download/wiki_table_name_info.html -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"
subprocess.call(bashCommand, shell=True)

# Format it
bashCommand = "~/Dropbox/LEPR03/nathan-workspace/code/scripts/rio/format_rio_wiki_table.R"
subprocess.call(bashCommand, shell=True)
