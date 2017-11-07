#!/bin/bash


## Tennessee ##
# Download
python2.7 ../apps/download_data.py -o ../../data/flu/tennessee/download/ -d TN -u

# Convert from PDF to CSV
#java -Djava.library.path=/Users/ndsherb/opencv/opencv/build/lib -jar ~/nathan/code/apps/PDF3CSV_TN.jar -o /Users/ndsherb/nathan/data/dengue/tennessee/convert -g

# Format 
# -o ->

# Merge 
# -> master.csv

## Rio ##
#Download
#python2.7 ../apps/download_data.py -o ../../data/dengue/rio/download/ -d Rio -u


## Peru ##
#Download
#python2.7 ../apps/download_data.py -o ../../data/dengue/peru/download/ -d PE -u


## etc...
