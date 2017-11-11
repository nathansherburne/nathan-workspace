#!/usr/bin/env Rscript
library("argparse")

parser <- ArgumentParser()
parser$add_argument("input", metavar="file", nargs="+", help="CSV file(s) to merge (must have identical column structure and names")
parser$add_argument("-o", "--output", metavar="directory", default=getwd(), help="the output directory")

args <- parser$parse_args()

all.dfs = lapply(args$input, function(filename) read.csv(filename, stringsAsFactors = FALSE))
merged = do.call(rbind, all.dfs)
write.csv(merged, paste0(args$output, "/", "merged.csv"), row.names = FALSE)