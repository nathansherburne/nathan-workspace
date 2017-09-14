library(data.table)

ROOT_DIR = "~/nathan/Dengue/tennessee/"
DATA_DIR = paste0(ROOT_DIR, "data/")
CSV_OUT_DIR = paste0(DATA_DIR, "CSVs/masterCSVs/")
CSV_IN_DIR = paste0(DATA_DIR, "CSVs/")
LIB_DIR = paste0(ROOT_DIR, "Dengue/tennessee/code/R/")

column.names = c("Summary.for", "Total.Regional.Patients", "%ILI", "Comparison.to.State")

CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
CSV.dts = lapply(CSV.filenames, function(filename) {
  dt = read.csv(paste0(CSV_IN_DIR, filename), stringsAsFactors = FALSE, header = FALSE)
  if(length(dt) != 4) {
    browser()
  }
  names(dt) = column.names
})
