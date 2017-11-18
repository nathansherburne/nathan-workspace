#!/usr/bin/env Rscript
library("argparse")
library(readr)

parser <- ArgumentParser()
parser$add_argument("input", metavar="file", help="CSV file to remove blank columns")
args <- parser$parse_args()

df = read.csv(args$input, stringsAsFactors = FALSE, header = FALSE)
df = df[,colSums(is.na(df))<nrow(df)]
cat(format_csv(df, col_names = FALSE))
