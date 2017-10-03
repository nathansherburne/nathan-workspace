#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/nathan/"
IN_DIR = paste0(ROOT_DIR, "data/dengue/paho/convert/")
OUT_DIR = paste0(ROOT_DIR, "data/dengue/paho/format/")
LIB_DIR = paste0(ROOT_DIR, "code/lib/R/dengue/paho/code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))
YEAR="2017"

CSV.filenames = list.files(IN_DIR, pattern=paste0(YEAR,"_EW.*csv"))
# Order by date 
CSV.filenames = CSV.filenames[order(CSV.filenames)]

ALL_CSV_df_ugly = lapply(CSV.filenames, function(filename) read.csv(paste0(IN_DIR, filename), stringsAsFactors = FALSE, na.strings=c(""," ", "NA")))
ALL_CSV_df_pretty = lapply(ALL_CSV_df_ugly, formatPAHOcsv) # Format (remove 'total' rows, fix column names, etc...)
print("Formatting CSV...")
ALL_CSV_df_pretty = lapply(ALL_CSV_df_pretty, function(df) addYearCol(df, YEAR))
ALL_CSV_df_pretty = lapply(ALL_CSV_df_pretty, function(df) setcolorder(df, 
          c("Year", "Week", "Country.or.Subregion", "Probable", "Lab.Confirm", "Severe.Dengue", "Deaths", "Serotype", "Population.x.1000")))
# Save formatted data frame
mapply(function(df, filename) write.csv(df, paste0(OUT_DIR, filename), row.names = FALSE), ALL_CSV_df_pretty, CSV.filenames)
print("cumulative CSV formatted")
