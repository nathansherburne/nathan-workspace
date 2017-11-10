#!/bin/bash

# Download
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/get_dengue_data.py -d PAHO -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/download/ -u

# Convert
PDF2CSV.py -b ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/download/*.pdf -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/convert/

# Format
~/Dropbox/LEPR03/nathan-workspace/code/scripts/paho/clean_CSVs.R
~/Dropbox/LEPR03/nathan-workspace/code/scripts/paho/get_noncumulative.R

