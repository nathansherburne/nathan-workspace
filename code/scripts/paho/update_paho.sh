#!/bin/bash

# Download
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d PAHO -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/download/ -u

# Convert
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/paho/get_unconverted_PDFs.py))'
if [ ${#errors[@]} -ne 0 ]; then
    PDF2CSV.py -b "${UNCONVERTED[@]}" -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/convert/
else
    echo "No new PDF files to convert"
fi

# Format
~/Dropbox/LEPR03/nathan-workspace/code/scripts/paho/clean_CSVs.R

# Cumulative subtraction
# Take pairs of weekly cumulatives, subtract the corresponding values, and make a
# new CSV that is the difference of the two.
~/Dropbox/LEPR03/nathan-workspace/code/scripts/paho/get_noncumulative.R

# Merge Non-cumulative CSVs into one
~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/merge.R ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/diffs/*.csv -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/merge/

# Sort the merged CSV
~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/sort.R ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/merge/merged.csv --columns Country.or.Subregion Year Week > ~/Dropbox/LEPR03/nathan-workspace/data/dengue/paho/merge/merged_sorted.csv
