#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/nathan/Dengue/peru/"
CSV_IN_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/")
CSV_OUT_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/")
LIB_DIR = paste0(ROOT_DIR, "code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))


CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
# Order by date modified
details = file.info(list.files(CSV_IN_DIR, pattern="*.csv", full.names = TRUE))
CSV.filenames = CSV.filenames[with(details, order(as.POSIXct(mtime)))]

ALL_CSV_dt_ugly = lapply(CSV.filenames, function(filename) as.data.table(read.csv(paste0(CSV_IN_DIR, filename), stringsAsFactors = FALSE, na.strings=c(""," ", "NA"), header = FALSE, dec=",")))
ALL_CSV_dt_pretty = lapply(ALL_CSV_dt_ugly, formatPerucsv) # Format (remove 'total' rows, fix column names, etc...)

mapply(function(df, filename) write.csv(df, paste0(CSV_OUT_DIR, filename), row.names = FALSE), ALL_CSV_dt_pretty, CSV.filenames)
