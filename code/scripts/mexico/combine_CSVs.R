library(reshape2)
library(GADMTools)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
DATA_DIR = paste0(ROOT_DIR, "data/dengue/mexico/convert/")
CSV_OUT_DIR = paste0(ROOT_DIR, "data/dengue/mexico/merge/cases/")
CENSUS_DIR = paste0(ROOT_DIR, "data/dengue/mexico/merge/auxilliary/")
LIB_DIR = paste0(ROOT_DIR, "code/lib/R/")
source(paste0(LIB_DIR, "my_lib.R"))

MEX = gadm.loadCountries("MEX", level = 1, basefile = "./")
STATE_NAMES = MEX$spdf$NAME_1

MasterCensus = read.csv(paste0(CENSUS_DIR, "Mexico_Census_1980-2010.csv"), stringsAsFactors = FALSE)

CSV.filenames = list.files(DATA_DIR, pattern="*.csv")
year.set = unlist(lapply(CSV.filenames, function(filename) substr(filename, 1, 4)))
for(i in 1:length(CSV.filenames)) {
  df = read.csv(paste0(DATA_DIR, CSV.filenames[i]), stringsAsFactors=FALSE, header = FALSE)
  year = as.numeric(year.set[i])
  RATE_COL_NAME = 'Incidence.Per.100000'
  
  # Get just state rows
  state.i = 1
  df[,state.i][which(df[,state.i] == "Ciudad de México")] = "Distrito Federal"  # In 2016 they changed Distrito Federal's name to Ciudad de México
  match.s1.s2 = fuzzyStringCompare(df[,state.i], STATE_NAMES)
  header.row.i = min(match.s1.s2$s1.i) - 1  # Assume the header is the row above the first state row
  header = as.character(df[header.row.i, ])
  df = df[match.s1.s2$s1.i, ]
  df[, state.i] = STATE_NAMES[match.s1.s2$s2.i]
 
  # Get the column names into English, correct spelling, etc..
  colnames(df) = header
  colnames(df)[1] = "State"
  rate.i = grep('tas|rat|inc', colnames(df), ignore.case = TRUE)  # Find Rate column
  jan.i = grep('ene|jan', colnames(df), ignore.case = TRUE)  # Find January column
  months.i = seq(jan.i,jan.i+11)
  select.columns = c(state.i, months.i)  # The columns that we want to keep
  colnames(df)[months.i] = month.name  # Set good month names
  if(length(rate.i) != 0) {  # Starting in 2009, there is no Rate column. 
    colnames(df)[rate.i] = RATE_COL_NAME  # Only try to rename it if it exists.
    select.columns = c(select.columns, rate.i)
  }
  df = df[, select.columns]
  
  # Remove whitespace (which is often used as a thousands separator) from all but first column
  df[2:length(df)] = apply(df[2:length(df)],2,function(x) as.numeric(gsub('\\s+', '',x)))
 
  
  # Code works up to here. Three more steps need to be done.
  browser()
  
  ## TODO: 
  # Calculate rate if it is not included. (i.e. 2009 -> present)
  if(length(rate.i) == 0) {
    rounded.year = floor(year / 5) * 5  # Each year gets the previous census's data. (e.g. 2005-2009 get 2005's census)
    year.census = MasterCensus[which(MasterCensus$Year == rounded.year),]
    census.state.names = unique(year.census$State)  # Different spellings, need to map the names.
    census.match.s1.s2 = fuzzyStringCompare(census.state.names, STATE_NAMES)
    
    state.yearly.rates = as.numeric(unlist(apply(df, 1, function(row) {
      state.name = row["State"]
      year.total.cases = sum(as.numeric(row[month.name]))
      state.pop = year.census[which(year.census$State == state.name), "Population"]
      yearly.rate = round(100000 * year.total.cases / state.pop, digits=2)
      return(yearly.rate)
    })))
    browser()
    df[RATE_COL_NAME] = state.yearly.rates
  }

  # TODO:
  # Melt into long format look up melt() R function
  # Then just rbind each year's long format data frame together into CSV.df
}

# Order the columns, sort the rows, and then save.
#CSV.df = CSV.df[ , c('Year', 'State', 'Month', 'Cases', 'Incidence_Per_100000')]
#CSV.df = CSV.df[with(CSV.df, order(Year, State, Month)), ]
browser()
#write.csv(CSV.df, paste0(CSV_OUT_DIR, "master.csv"), row.names = FALSE)