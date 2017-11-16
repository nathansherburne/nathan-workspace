#! /bin/bash

# Download update
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d SD -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/san_diego/download/

# Convert
# PDF -> CSV
IFS=$'\r\n' GLOBIGNORE='*' command eval  'UNCONVERTED=($(python get_unconverted_PDFs.py))'
for filename in "${UNCONVERTED[@]}"
do
    outputFilename=$filename
    outputFilename=${outputFilename/download/convert}
    outputFilename=${outputFilename/pdf/csv}
    java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/tabula-1.0.1-jar-with-dependencies.jar -i $filename -p2 -g --stream -o $outputFilename
done

# Format + Merge
~/Dropbox/LEPR03/nathan-workspace/code/scripts/san_diego/append_san_diego.R
