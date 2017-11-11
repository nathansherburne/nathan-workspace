#!/usr/bin/env Rscript

##
# This script takes the formatted PAHO CSVs (cumulative)
# and takes the difference of each sequential pair,
# creating a new csv for each weekly difference.
##

library(data.table)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
CSV_IN_DIR = paste0(ROOT_DIR, "data/dengue/paho/format/")
CSV_OUT_DIR = paste0(ROOT_DIR, "data/dengue/paho/diffs/")
LIB_DIR = paste0(ROOT_DIR, "code/scripts/paho/")
source(paste0(LIB_DIR, "csv_lib.R"))

CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
# Order by date modified
CSV.filenames = rev(CSV.filenames[order(CSV.filenames)])

ALL_CSV_dfs = lapply(CSV.filenames, function(filename) read.csv(paste0(CSV_IN_DIR, filename), stringsAsFactors = FALSE))

NON_CUMU_df = mapply(getNoncumulativeDf, ALL_CSV_dfs[-length(ALL_CSV_dfs)], ALL_CSV_dfs[-1], SIMPLIFY = FALSE)
names(NON_CUMU_df) = mapply(function(name1, name2) {
   temp = unlist(strsplit(name1, '[_.]'))
   year = temp[1]
   cur_week = temp[3]
   prev_week = unlist(strsplit(name2, '[_.]'))[3]
   return(paste0(year, '_EW_', prev_week, '-', cur_week, '.csv'))
}, CSV.filenames[-length(CSV.filenames)], CSV.filenames[-1])

respective.years = unlist(lapply(names(NON_CUMU_df), function(name) substr(name, 1, 4)))  # Gets year from each name
NON_CUMU_df = mapply(function(df, year) addYearCol(df, year), NON_CUMU_df, respective.years, SIMPLIFY = FALSE)
NON_CUMU_df = lapply(NON_CUMU_df, function(df) setcolorder(df, 
   c("Year", "Week", "Country.or.Subregion", "Probable", "Lab.Confirm", "Severe.Dengue", "Deaths", "New.Serotype", "Cumul.Serotype", "Population.x.1000")))

mapply(function(df, filename) write.csv(df, paste0(CSV_OUT_DIR, filename), row.names = FALSE), NON_CUMU_df, names(NON_CUMU_df))

