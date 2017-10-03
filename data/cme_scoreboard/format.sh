#!/bin/bash

extra_column_names=( CME Observed\ Geomagnetic\ Storm\ Parameters Actual\ Shock\ Arrival\ Time Max\ Kp Dst\ min.\ in\ nT Dst\ min.\ time CME\ Note )
inputDir="convert"
outputDir="format"

header_files=$(find convert -regex '.*/\([0-9]\{4\}\)--0-[0-9]*\.csv')
table_files=$(find convert -regex '.*/\([0-9]\{4\}\)--0-[0-9]*-[0-9]*\.csv')
num_pairs=$(echo "${header_files}" | wc -l)

for i in $(seq 1 $num_pairs)
do
    hFilename=$(awk -v d1=$i 'NR==d1' <<< "${header_files}")
    tFilename=$(awk -v d1=$i 'NR==d1' <<< "${table_files}")
    outputFilename="${hFilename/$inputDir/$outputDir}"

    header_data="$(sed "s/^\([\"']\)\(.*\)\1\$/\2/g" $hFilename)"
    replace_str=$'\n'
    for column_name in "${extra_column_names[@]}"
    do 
        header_data="${header_data/$column_name/$replace_str$column_name=}"
    done
    header_data="$(echo "${header_data}" | tr -d ',')" # Remove all commas since this data will be in a CSV.

    included_parameters="$(echo "${header_data}" | grep -o ".*=:" | tr -d '=:')"
    values="$(echo "${header_data}" | grep -o "=:.*" | tr -d '=:' | tr -d ' ')"
    
    csv_params="$(echo "${included_parameters}" | tr '\n' ', ' | sed 's/\(.*\),/\1''/')"
    csv_vals="$(echo "${values}" | tr '\n' ', ' | sed 's/\(.*\),/\1''/')"

    # Add extra column names to first row only. Add values for those columns to the end of every row.
    awk -F, -v d1="${csv_params}" -v d2="${csv_vals}" 'NR==1{print $0 FS FS d1}NR>1{print $0 FS d2}' $tFilename > $outputFilename
done
