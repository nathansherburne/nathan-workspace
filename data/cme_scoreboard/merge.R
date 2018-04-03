#!/usr/bin/env Rscript
library(plyr)

INPUT_DIR = "format/"
OUTPUT_DIR = "merge/"

CSV.filenames = list.files(INPUT_DIR)
all.dfs = lapply(CSV.filenames, function(filename) read.csv(paste0(INPUT_DIR, filename), stringsAsFactors=FALSE))

merged = data.frame()
for(i in 1:length(all.dfs)) {
  merged = rbind.fill(merged, all.dfs[[i]])
}
merged = merged[order(merged[1]),]

write.csv(merged, paste0(OUTPUT_DIR, "master.csv"), row.names = FALSE)
