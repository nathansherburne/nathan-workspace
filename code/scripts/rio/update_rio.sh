#!/bin/bash

# Download
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d Rio -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/download/ -u


## Convert ##

# Some files are downloaded as HTML, but some are downloaded as PDF. Start by making all the PDFs into HTMLs.
# PDF -> HTML
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED_PDFS=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/rio/get_unconverted.py | grep ".pdf"))'
UNCONVERTED_PDFS_WEEKLY=($(printf '%s\n' ${UNCONVERTED_PDFS[@]} | grep "weekly"))
if [ "${#UNCONVERTED_PDFS_WEEKLY[@]}" -gt 0 ]
then
    java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/PDF2HTML.jar -i "${UNCONVERTED_PDFS_WEEKLY[@]}" -n4.0 -m10 -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/
fi
UNCONVERTED_PDFS_MONTHLY=($(printf '%s\n' ${UNCONVERTED_PDFS[@]} | grep "monthly"))
if [ "${#UNCONVERTED_PDFS_MONTHLY[@]}" -gt 0 ]
then
	# Notice added '-s' option for monthly. This is necessary as the monthly PDFs have slightly different proportions
	# (The values were found experimentallyy using '-d' (debug) option and looking at the output PDFs).
    java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/PDF2HTML.jar -i "${UNCONVERTED_PDFS_MONTHLY[@]}" -n4.0 -s2.0 -m10 -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/
fi
# For all of the files that started as HTML and didn't need the above conversion, just copy them over to the 'convert' folder so that all HTMLs are in one place.
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED_HTMLs=($(python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/rio/get_unconverted.py | grep ".htm"))'
cp "${UNCONVERTED_HTMLs[@]}" ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/

# Now that all of our data is in HTML format, convert them all to CSV.
# Only convert HTMLs that have been modified in the last hour. That is, since we are usually just going to be running an update,
# we should only have to deal with newly modified HTML files.
# HTML -> CSV
UNCONVERTED_HTMLS=($(find ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*.htm* -mmin -60))
python ~/Dropbox/LEPR03/nathan-workspace/code/apps/HTML2CSV.py "${UNCONVERTED_HTMLS[@]}" -o ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/

## Format ##
# Only format CSVs that have been modified in the last hour
UNFORMATTED_CSVS=($(find ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*.csv -mmin -60))
# Blank columns and rows often appear as a side effect of conversion. Remove them.
for csvFile in ${UNFORMATTED_CSVS[@]}
do
    # Remove completely blank columns
    ~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/removeBlankColumns.R $csvFile > tmp.csv
    rm $csvFile
    mv tmp.csv $csvFile
    echo "1: Removed blank column from $csvFile"
    # Remove mostly blank (or NA) rows
    python ~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/removeMostlyNARows.py $csvFile > tmp.csv
    rm $csvFile
    mv tmp.csv $csvFile
    echo "2: Removed garbage rows (mostly blank) from $csvFile"
    # Now, since some rows might have just been removed, there could be more completely blank columns.
    # Remove those, too.
    ~/Dropbox/LEPR03/nathan-workspace/code/scripts/generic_scripts/removeBlankColumns.R $csvFile > tmp.csv
    rm $csvFile
    mv tmp.csv $csvFile
    echo "3: Removed blank column from $csvFile"
done

### PDF to HTML converter saves each page as its own HTML file.
### So now we combine the pages into one CSV
# Weekly
YEARS_WITH_MULTIPLE_PAGES_W=$(ls ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*_weekly_[0-9].csv | grep -o "[0-9]\{4\}" | uniq)
for year in $YEARS_WITH_MULTIPLE_PAGES_W
do
    cat ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_weekly_*.csv > ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_weekly.csv
    echo "Merged individual pages of ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_weekly.csv"
done
# Monthly
YEARS_WITH_MULTIPLE_PAGES_M=$(ls ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/*_monthly_[0-9].csv | grep -o "[0-9]\{4\}" | uniq)
for year in $YEARS_WITH_MULTIPLE_PAGES_M
do
    cat ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_monthly_*.csv > ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_monthly.csv
    echo "Merged individual pages of ~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/"$year"_monthly.csv"
done

## Merge ##
# Currently this R script combines ALL the CSVs and creates a new master every time.
# TODO: Update it so that it looks for an already existing master file, and
# finds out if there is any new data in the individual CSVs to append to it.
~/Dropbox/LEPR03/nathan-workspace/code/scripts/rio/combine_weekly_csvs.R
~/Dropbox/LEPR03/nathan-workspace/code/scripts/rio/combine_monthly_csvs.R
