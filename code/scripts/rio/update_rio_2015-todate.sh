#!/bin/bash

# Download
#python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d Rio -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/download/ -u

# Convert
# PDF -> HTML
java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/PDF2HTML.jar -i ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/download/2016_weekly.pdf -p1,2,3 -n4.0 -m10 -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/

# HTML -> CSV
python ~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*.html -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/

# Format
# For some reason the first page has a blank column in it -- remove it.
# Can't redirect output to file the same command is reading from, so this takes three steps.
~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/removeBlankColumns.R ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly_1.csv > ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly_1tmp.csv
rm ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly_1.csv
mv ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly_1tmp.csv ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly_1.csv

cat ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly_*.csv >> ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/2016_weekly.csv


# Merge

