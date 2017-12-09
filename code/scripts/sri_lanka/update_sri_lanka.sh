#!/bin/bash

# Download
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d LK -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/sri_lanka/download -u

# Convert
#IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/paho/get_unconverted_PDFs.py))'
#if [ ${#errors[@]} -ne 0 ]; then
#    PDF2CSV.py -b "${UNCONVERTED[@]}" -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/convert/
#else
#    echo "No new PDF files to convert"
#fi
python ~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py ~/Dropbox/LEPR03/nathan-workspace/data/dengue/sri_lanka/download/*.html -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/sri_lanka/convert/

# Merge
~/Dropbox/LEPR03/nathan-workspace/data/dengue/sri_lanka/scripts/combine_CSVs.R
