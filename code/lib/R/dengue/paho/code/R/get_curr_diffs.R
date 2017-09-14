#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/nathan/Dengue/Americas_by_Country/"
CSV_IN_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/final_individual/")
CSV_OUT_DIR = paste0(ROOT_DIR, "data/CSVs/weekly/noncumulative")
LIB_DIR = paste0(ROOT_DIR, "Code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))

CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
this.week = read.csv(paste0(CSV_IN_DIR, CSV.filenames[1]), stringsAsFactors = FALSE)
last.week = read.csv(paste0(CSV_IN_DIR, CSV.filenames[2]), stringsAsFactors = FALSE)


## Get differences (Assumes countries have the same row numbers for both weeks)
#mapply(getDifference, this.week, last.week) ## mapply can't go row-wise??

diff.df = data.frame()
for(i in 1:nrow(this.week)) {
  diff.row = getDifference(last.week[i, ], this.week[i, ])
  diff.df = rbind(diff.df, diff.row)
}

# Get current week number
s = unlist(strsplit(CSV.filenames[1], "_"))
week.number = as.numeric(gsub("([0-9]+).*$", "\\1", s[length(s)]))
filename = paste0("noncumulative_week_", week.number)
write.csv(df, paste0(CSV_OUT_DIR, filename), row.names = FALSE)