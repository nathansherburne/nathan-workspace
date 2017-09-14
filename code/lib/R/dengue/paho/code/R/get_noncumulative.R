#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/nathan/Dengue/paho/"
CSV_IN_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/cumulative/")
CSV_OUT_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/noncumulative/")
LIB_DIR = paste0(ROOT_DIR, "code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))

CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
# Order by date modified
details = file.info(list.files(CSV_IN_DIR, pattern="*.csv", full.names = TRUE))
CSV.filenames = rev(CSV.filenames[with(details, order(as.POSIXct(mtime)))])

ALL_CSV_dfs = lapply(CSV.filenames, function(filename) read.csv(paste0(CSV_IN_DIR, filename), stringsAsFactors = FALSE))

NON_CUMU_df = mapply(getNoncumulativeDf, ALL_CSV_dfs[-length(ALL_CSV_dfs)], ALL_CSV_dfs[-1], SIMPLIFY = FALSE)
names(NON_CUMU_df) = mapply(function(name1, name2) {
   temp = unlist(strsplit(name1, '[_.]'))
   year = temp[1]
   cur_week = temp[3]
   prev_week = unlist(strsplit(name2, '[_.]'))[3]
   return(paste0(year, '_EW_', prev_week, '-', cur_week, '.csv'))
}, CSV.filenames[-length(CSV.filenames)], CSV.filenames[-1])

mapply(function(df, filename) write.csv(df, paste0(CSV_OUT_DIR, filename), row.names = FALSE), NON_CUMU_df, names(NON_CUMU_df))

