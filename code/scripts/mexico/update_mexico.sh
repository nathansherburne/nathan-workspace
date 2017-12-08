#! /bin/bash

## Download ##
python ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d MX -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/mexico/download/ -u

## Convert ##
# Use OpenCV (cv) virtualenvironment that has OpenCV packages used for table coordinate locator 
source ~/.bash_profile
source $(which virtualenvwrapper.sh)
workon cv

# PDF -> CSV
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/mexico/get_unconverted_PDFs.py))'
for filename in "${UNCONVERTED[@]}"
do
    # The locate_table_coordinates script should output the coordinates in top,left,bottom,right format, which is the same format that tabula uses.
    IFS=$'\r\n' GLOBIGNORE='*' command eval 'BBOX_COORDINATES=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/mexico/locate_table_coordinates.py -i $filename))'
    IFS=$'\r\n' GLOBIGNORE='*' command eval 'COL_COORDINATES=($(python ~/Dropbox/LEPR03/nathan-workspace/code/apps/locate_column_coordinates.py -i $filename))'
    outputFilename=$filename
    outputFilename=${outputFilename/download/convert}
    outputFilename=${outputFilename/pdf/csv}
    java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/tabula-1.0.1-jar-with-dependencies.jar -i $filename -p1 --area "${BBOX_COORDINATES}" --columns "${COL_COORDINATES}" --stream -o $outputFilename
    echo "$outputFilename created."
done
# Done with OpenCV
deactivate cv

