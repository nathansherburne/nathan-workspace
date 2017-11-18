#!/bin/bash/python
# get_neighborhood_names.py
#
# This script goes to Wikipedia and finds the names of
# Rio de Janiero's Neighborhoods. Wikipedia has other
# info as well (e.g. Administrative Regions). But as of
# now, this script simply gets the names of neighborhoods.
#
# This script fills in the spatial gap between our data 
# from Rio's website and GADM's spatial hierarchy. GADM
# does not have a level (as far as I know) for Rio's
# Neighborhoods, Admin Regions, or Planning Areas. So this
# script should ideally fill that role of providing a 
# naming/spelling convention that the automatic conversion
# and formatting scripts can use in their processing.

import urllib2
from bs4 import BeautifulSoup
import subprocess
import csv
import sys

# Save HTML from Wikipedia that has table with Neighborhood info.
url = "https://en.wikipedia.org/wiki/List_of_Administrative_Regions_in_Rio_de_Janeiro"
html = urllib2.urlopen(url).read()
f = open("tmp.html", "w")
f.write(html)
f.close()

# Use custom HTML to CSV converter to make the table more accessible.
bashCommand = "~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py tmp.html -o ./"
subprocess.call(bashCommand, shell=True)

# Store the rows of the table.
rows = list()
with open('tmp.csv', 'rb') as csvfile:
    csvReader = csv.reader(csvfile, delimiter=',')
    for row in csvReader:
        rows.append(row)

# Find column of interest (i.e. Neighborhood column)
headers = rows.pop(0)
neighColIndex = None
for i in range(0, len(headers)):
    if "Neigh" in headers[i]:
        neighColIndex = i
        break

if neighColIndex is None:
    print "Could not locate Neighborhood column"
    sys.exit(1)

# Neighborhood names are separated by commas, get individual.
neighborhoodNames = list()
for row in rows:
    thisRowNeighs = row[neighColIndex]
    neighborhoodNames.extend(thisRowNeighs.split(','))

# Just print out the names to STDOUT, let user decide what they want to do with it.
for name in neighborhoodNames:
    print name.strip()  # Remove leading/trialing whitespace

