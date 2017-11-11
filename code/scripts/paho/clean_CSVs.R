#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
IN_DIR = paste0(ROOT_DIR, "data/dengue/paho/convert/")
OUT_DIR = paste0(ROOT_DIR, "data/dengue/paho/format/")
LIB_DIR = paste0(ROOT_DIR, "code/scripts/paho/")
source(paste0(LIB_DIR, "csv_lib.R"))

CSV.filenames = list.files(IN_DIR, pattern="2017_EW.*csv")
# Order by date 
CSV.filenames = CSV.filenames[order(CSV.filenames)]

ALL_CSV_df_ugly = lapply(CSV.filenames, function(filename) read.csv(paste0(IN_DIR, filename), stringsAsFactors = FALSE, na.strings=c(""," ", "NA"), header = FALSE))
ALL_CSV_df_pretty = lapply(ALL_CSV_df_ugly, formatPAHOcsv) # Format (remove 'total' rows, fix column names, etc...)
print("Formatting CSV...")

# Save formatted data frame
mapply(function(df, filename) write.csv(df, paste0(OUT_DIR, filename), row.names = FALSE), ALL_CSV_df_pretty, CSV.filenames)
print("cumulative CSV formatted")
