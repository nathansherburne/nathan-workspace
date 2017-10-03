#!/usr/bin/env Rscript
library(data.table)

ROOT_DIR = "~/nathan/"
IN_DIR = paste0(ROOT_DIR, "data/dengue/paho/diffs/")
OUT_DIR = paste0(ROOT_DIR, "data/dengue/paho/merge/")
LIB_DIR = paste0(ROOT_DIR, "code/lib/R/dengue/paho/code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))

CSV.filenames = list.files(IN_DIR, pattern="*.csv")
all.dfs = lapply(CSV.filenames, function(filename) read.csv(paste0(IN_DIR, filename), stringsAsFactors = FALSE))
merged = do.call("rbind", all.dfs)

merged = merged[with(merged, order(Country.or.Subregion, Week)), ]  # Sort
merged = unique(merged)  # Rows are duplicated when a week does not update any info.

write.csv(merged, paste0(OUT_DIR, "merged.csv"), row.names = FALSE)