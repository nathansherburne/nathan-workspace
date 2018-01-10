#!/usr/bin/env Rscript
library("argparse")
library(readr)

parser <- ArgumentParser()
parser$add_argument("input", metavar="file", help="CSV file to sort")
parser$add_argument("-c", "--columns", nargs="+", help="the column names, in order, by which the rows will be sorted.")
args <- parser$parse_args()

df = read.csv(args$input, stringsAsFactors = FALSE)
df = df[do.call("order", df[args$columns]),]
cat(format_csv(df))
