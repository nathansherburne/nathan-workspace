library(reshape2)

ROOT_DIR = "~/nathan/data/dengue/mexico/"
DATA_DIR = paste0(ROOT_DIR, "convert/")
CSV_OUT_DIR = paste0(ROOT_DIR, "merge/cases/")
CENSUS_DIR = paste0(ROOT_DIR, "merge/auxilliary/")
LIB_DIR = paste0("~/nathan/code/lib/R/dengue/mexico/code/R/")
source(paste0(LIB_DIR, "csv_lib.R"))

PopData2010 = read.csv(paste0(CENSUS_DIR, "2010_CENSUS_STATE_POP.csv"), stringsAsFactors = FALSE)
MasterCensus = read.csv(paste0(CENSUS_DIR, "Mexico_Census_1980-2010.csv"), stringsAsFactors = FALSE)
pop.data.all = split(MasterCensus, MasterCensus$Year)

CSV.filenames = list.files(DATA_DIR, pattern="*.csv")
year.set = unlist(lapply(CSV.filenames, function(filename) substr(filename, 1, 4)))
CSV.data.frames = list()
for(i in 1:length(CSV.filenames)) {
  CSV.data.frames[[i]] = read.csv(paste0(DATA_DIR, CSV.filenames[i]), head=TRUE, encoding="UTF-8", stringsAsFactors=FALSE)
  colnames(CSV.data.frames[[i]])[1] = "State"
}
CSV.data.frames = lapply(CSV.data.frames, removeTotals)
CSV.data.frames = lapply(CSV.data.frames, formatRownames)
CSV.data.frames = lapply(CSV.data.frames, removeNumericWhitespace)
CSV.data.frames = lapply(CSV.data.frames, pop.data.2010=PopData2010, nameOrAddRateCol)  # Keeps original rates, calculates 2009- rates. Yearly.
#CSV.data.frames = mapply(calcAndAddRateCol, CSV.data.frames, pop.data.all, SIMPLIFY = FALSE)  # Calculates and replaces 1985- rates. Monthly.
CSV.data.frames = mapply(addYearCol, CSV.data.frames, year.set, SIMPLIFY=FALSE)
CSV.long.dfs = lapply(CSV.data.frames, function(df) melt(df, id.vars=c("Year", "State", "Incidence_Per_100000"), variable.name="Month", value.name="Cases"))
CSV.df = do.call(rbind, CSV.long.dfs)
CSV.df = CSV.df[ , c('Year', 'State', 'Month', 'Cases', 'Incidence_Per_100000')]
CSV.df = CSV.df[with(CSV.df, order(Year, State, Month)), ]
rownames(CSV.df) = NULL # Reset row index

write.csv(CSV.df, paste0(CSV_OUT_DIR, "master.csv"), row.names = FALSE)