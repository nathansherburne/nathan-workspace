#!/usr/bin/env Rscript
library("argparse")
library(readr)

parser <- ArgumentParser()
parser$add_argument("input", metavar="file", help="CSV file to round")
parser$add_argument("-d", "--digits", help="number of decimal places to round to.")
parser$add_argument("-c", "--columns", help="which columns to round, comma separated list of integers.")
args <- parser$parse_args()

columns = as.numeric(unlist(strsplit(gsub("\\s+", "", args$columns), ',')))
df = read.csv(args$input, stringsAsFactors = FALSE)
df = df[do.call("order", df[args$columns]),]
cat(format_csv(df))