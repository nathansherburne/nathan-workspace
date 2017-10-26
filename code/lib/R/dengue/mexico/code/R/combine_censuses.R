library(Hmisc)

ROOT_DIR = "~/nathan/data/dengue/mexico/"
DATA_DIR = paste0(ROOT_DIR, "format/")
OUT_DIR = paste0(ROOT_DIR, "merge/auxilliary/")
LIB_DIR = paste0("~/nathan/code/lib/R/dengue/mexico/code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))


CSV.filenames = list.files(DATA_DIR, pattern="INEGI_Exporta_.*.csv", full.names=TRUE)
CSV.dfs = lapply(CSV.filenames, function(file) read.csv(file, stringsAsFactors = FALSE))
years.set = unlist(lapply(CSV.filenames, function(filename) {
  name = unlist(strsplit(basename(filename), '[.]'))[1]  # Basename no ext
  year = unlist(strsplit(name, '[_]'))[3]
  return(year)
}))
CSV.dfs = mapply(addYearCol, CSV.dfs, years.set, SIMPLIFY = FALSE)
df.ALL = do.call(rbind, CSV.dfs)
df.ALL = df.ALL[ , c('Year', 'State', 'Population')]
write.csv(df.ALL, paste0(OUT_DIR, 'Mexico_Census_1980-2010.csv'), row.names = FALSE)