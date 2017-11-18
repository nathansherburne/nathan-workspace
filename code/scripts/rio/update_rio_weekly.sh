#!/bin/bash

# Download
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d Rio -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/download/ -u

## Convert ##
# PDF -> HTML
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED_PDFs=($(python get_unconverted.py | grep ".pdf"))'
java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/PDF2HTML.jar -i "${UNCONVERTED_PDFs[@]}" -p1,2,3 -n4.0 -m10 -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED_HTMLs=($(python get_unconverted.py | grep ".htm"))'
cp "${UNCONVERTED_HTMLs[@]}" ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/

# HTML -> CSV
python ~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*.htm* -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/

## Format ##
ALL_CSV_FILES=~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*.csv
for csvFile in $ALL_CSV_FILES
do
    python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/removeMostlyBlankRows.py $csvFile > tmp.csv
    rm $csvFile
    mv tmp.csv $csvFile
done

# For some reason the first page has a blank column in it -- remove it.
# Can't redirect output to file the same command is reading from, so this takes three steps.
FIRST_PAGE_CSV_FILES=~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*_1.csv
for csvFile in $FIRST_PAGE_CSV_FILES
do
    ~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/removeBlankColumns.R $csvFile > tmp.csv
    rm $csvFile
    mv tmp.csv $csvFile
done

## PDF to HTML converter saves each page as its own HTML file.
## So now we combine the pages into one CSV
YEARS_WITH_MULTIPLE_PAGES=$(ls ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*_weekly_*.csv | grep -o "[0-9]\{4\}" | uniq)
for year in $YEARS_WITH_MULTIPLE_PAGES
do
    cat ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_weekly_*.csv >> ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_weekly.csv
done

## Merge
#
