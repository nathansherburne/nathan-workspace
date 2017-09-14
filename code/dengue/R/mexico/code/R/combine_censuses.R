library(Hmisc)

dBoxDir = "~/Dropbox/LEPR03/nathan/"
DataDir = paste0(dBoxDir, "Dengue/Mexico/Data/")
CSVOutDir = paste0(DataDir, "Dengue_Classic/CSVs/masterCSV/")
CSV.DIR = paste0(DataDir, "Dengue_Classic/CSVs/")
censusDir = paste0(DataDir, "Census_All/")
LibDir = paste0(dBoxDir, "Dengue/Mexico/Code/R/")
source(paste0(LibDir, "csv_lib.R"))


CSV.filenames = list.files(censusDir, pattern="*.csv")
CSV.dfs = lapply(CSV.filenames, function(file) read.csv(paste0(censusDir, file), stringsAsFactors = FALSE))
years.set = unlist(lapply(CSV.filenames, first.word))
CSV.dfs = mapply(addYearCol, CSV.dfs, years.set, SIMPLIFY = FALSE)
df.ALL = do.call(rbind, CSV.dfs)
df.ALL = df.ALL[ , c('Year', 'State', 'Population')]

write.csv(df.ALL, paste0(censusDir, 'master/', 'Mexico_Census_1980-2010.csv'), row.names = FALSE)