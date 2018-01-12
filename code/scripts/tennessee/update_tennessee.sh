#! /bin/bash

# Download
echo "Downloading... (code/apps/download_data.py)"
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d TN -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/tennessee/download/ -u

# Convert all unconverted PDFs
# PDF -> HTML
# The left, right, top, and bottom parameters work for the current 2017 PDFs 
# (i.e. they define a bounding box around the first table in the PDF).
echo "Finding unconverted PDFs... (scripts/tennessee/get_unconverted_PDFs.py)"
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/tennessee/get_unconverted_PDFs.py))'
if [ ${#UNCONVERTED[@]} -gt 0 ]
then
    echo "Converting PDF->HTML... (code/apps/PDF2HTML.jar)"
    java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/PDF2HTML.jar -i "${UNCONVERTED[@]}" -p1 -n4.0 -l0.04 -r0.73 -t0.09 -b0.3 -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/tennessee/convert/
fi

# HTML -> CSV
echo "Converting HTML->CSV... (code/apps/HTML2CSV.py)"
python ~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py ~/Dropbox/LEPR03/nathan-workspace/data/flu/tennessee/convert/*.html -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/tennessee/convert/

# Update Master file
echo "Appending to master file..."
~/Dropbox/LEPR03/nathan-workspace/code/scripts/tennessee/append_master.R
