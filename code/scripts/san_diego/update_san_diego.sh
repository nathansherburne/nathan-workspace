#! /bin/bash

## Download update ## 
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d SD -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/san_diego/download/ -u

## Convert ##
# Use OpenCV (cv) virtualenvironment that has OpenCV packages used for table coordinate locator 
source $(which virtualenvwrapper.sh)
workon cv

# PDF -> CSV
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/san_diego/get_unconverted_PDFs.py))'
for filename in "${UNCONVERTED[@]}"
do
    # The locate_table_coordinates script should output the coordinates in top,left,bottom,right format, which is the same format that tabula uses.
    IFS=$'\r\n' GLOBIGNORE='*' command eval 'COORDINATES=($(python ~/Dropbox/LEPR03/nathan-workspace/code/apps/locate_table_coordinates.py -i $filename -p2))'
    outputFilename=$filename
    outputFilename=${outputFilename/download/convert}
    outputFilename=${outputFilename/pdf/csv}
    java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/tabula-1.0.1-jar-with-dependencies.jar -i $filename -p2 --area "${COORDINATES}" --stream -o $outputFilename
    echo "$outputFilename created."
done
# Done with OpenCV
deactivate cv

# Format + Merge
~/Dropbox/LEPR03/nathan-workspace/code/scripts/san_diego/append_san_diego.R
