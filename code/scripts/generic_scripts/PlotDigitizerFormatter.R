#!/usr/bin/env Rscript
library("argparse")

formatPlotDigitizerCSV <- function(df) {
  
  # Remove first 2 rows that Plot Digitizer stores creation information in.
  rowsToRemove = c(1,2)
  df = df[-rowsToRemove, ]
  
  # First row contains the column names.
  colnames(df) = df[1, ]
  df = df[-1, ]
  
  return(df)
}

parser <- ArgumentParser()
parser$add_argument("input", metavar="file", nargs="+", help="CSV file(s) to format.")
parser$add_argument("-o", "--output", metavar="directory", default=getwd(), help="the output directory")

args <- parser$parse_args()

unformattedCSVs = lapply(args$input, function(filename) read.csv(filename, stringsAsFactors = FALSE, row.names = NULL))
formattedCSVs = lapply(unformattedCSVs, function(df) formatPlotDigitizerCSV(df))

outputNames = lapply(args$input, function(filename) paste0(args$output, "/", basename(filename)))
mapply(function(df, filename) write.csv(df, filename, row.names = FALSE), formattedCSVs, outputNames)
