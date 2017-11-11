#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
IN_DIR = paste0(ROOT_DIR, "data/dengue/paho/diffs/")
OUT_DIR = paste0(ROOT_DIR, "data/dengue/paho/merge/")
LIB_DIR = paste0(ROOT_DIR, "code/scripts/paho/")
source(paste0(LIB_DIR, "csv_lib.R"))

CSV.filenames = list.files(IN_DIR, pattern="*.csv")
this.week = read.csv(paste0(IN_DIR, CSV.filenames[1]), stringsAsFactors = FALSE)
last.week = read.csv(paste0(IN_DIR, CSV.filenames[2]), stringsAsFactors = FALSE)


## Get differences (Assumes countries have the same row numbers for both weeks)
#mapply(getDifference, this.week, last.week) ## mapply can't go row-wise??

diff.df = data.frame()
for(i in 1:nrow(this.week)) {
  diff.row = getDifference(last.week[i, ], this.week[i, ])
  diff.df = rbind(diff.df, diff.row)
}