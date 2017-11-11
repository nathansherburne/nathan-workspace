#! /bin/bash

# Download update
python2.7 ~/Dropbox/LEPR03/nathan-workspace/code/apps/download_data.py -d SD -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/san_diego/download/

# Convert
# PDF -> HTML
java -jar ~/Dropbox/LEPR03/nathan-workspace/code/apps/PDF2CSV_PARAMS.jar -i ~/Dropbox/LEPR03/nathan-workspace/data/flu/san_diego/download/InfluenzaWatch_2017-11-10.pdf -p2 -n2.0 -m6 -o ~/Dropbox/LEPR03/nathan-workspace/data/flu/san_diego/convert/

# HTML -> CSV 

