#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/nathan/Dengue/paho/"
CSV_IN_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/converted_from_PDF/")
CSV_OUT_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/cumulative/")
LIB_DIR = paste0(ROOT_DIR, "code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))


CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
# Order by date modified
details = file.info(list.files(CSV_IN_DIR, pattern="*.csv", full.names = TRUE))
CSV.filenames = CSV.filenames[with(details, order(as.POSIXct(mtime)))]

ALL_CSV_df_ugly = lapply(CSV.filenames, function(filename) read.csv(paste0(CSV_IN_DIR, filename), stringsAsFactors = FALSE, na.strings=c(""," ", "NA")))
ALL_CSV_df_pretty = lapply(ALL_CSV_df_ugly, formatPAHOcsv) # Format (remove 'total' rows, fix column names, etc...)
print("Formatting CSV...")

# Save formatted data frame
mapply(function(df, filename) write.csv(df, paste0(CSV_OUT_DIR, filename), row.names = FALSE), ALL_CSV_df_pretty, CSV.filenames)
print("cumulative CSV formatted")
