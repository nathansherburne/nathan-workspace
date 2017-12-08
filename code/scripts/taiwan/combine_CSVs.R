# Taiwan Dengue
# TODO: code runs slowly because using dataframe instead of data table.

library(plyr)
library(stringr)
library("GADMTools")
library("rgeos")
library("fields")
library(ggmap)
library(DICE)
library(data.table)

# Nathan's old directories
#ROOT_DIR = "~/nathan/Dengue/taiwan/"                           
#CSV_OUT_DIR = paste0(DATA_DIR, "CSVs/masterCSVs/") 
#CSV_IN_DIR = paste0(DATA_DIR, "CSVs/download_dump/")   
#LIB_DIR = paste0(ROOT_DIR, "Dengue/taiwan/code/R/")

# New directory in Dropbox
ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
DATA_DIR = paste0(ROOT_DIR, "data/")            
CSV_OUT_DIR = paste0(DATA_DIR, "dengue/taiwan/merge/")        
CSV_IN_DIR = paste0(DATA_DIR, "dengue/taiwan/download/")
LIB_DIR = paste0(ROOT_DIR, "code/scripts/taiwan/")
COORD_FILE_NAME = paste0(CSV_OUT_DIR, "TW_dist_coords.csv")
source(paste0(LIB_DIR, "csv_lib.R"))

# Read in CSVs
CSV.filenames = list.files(CSV_IN_DIR, pattern="*.csv")
# Use only CSVs at the district level (district level goes by "dist" or "township" or "other")
pattern = "dist|town|other"
CSV.filenames.dist = CSV.filenames[grep(pattern, CSV.filenames)]
  ALL_CSV = lapply(CSV.filenames.dist, path=CSV_IN_DIR, getDataFrameFromTaiwanCSV )

# Combine into one dataframe
CSV.df = do.call(rbind, ALL_CSV)

# Cases column was read in as factor becaue of <NA>s. Change to numeric.
CSV.df$Cases = as.numeric(as.character(CSV.df$Cases))

# Add date columns (Begin_Week and End_Week) that correspond to week numbers
Dates.df =write.csv(paste0(CSV_IN_DIR, "Dates/", "Taiwan_weeks_2_dates.csv"))
CSV.df = merge(CSV.df, Dates.df)
# Get the corresponding CDC weeks/years
weeks.years = lapply(CSV.df$Start_Date, function(start_date) Date2CDCweek(as.Date(start_date)))
CSV.df$CDC_Week = sapply(weeks.years, '[[', 1)
CSV.df$CDC_Year = sapply(weeks.years, '[[', 2)

# Sort back after merge
CSV.df = CSV.df[with(CSV.df, order(Year, Region, City, District, Week)), ]
rownames(CSV.df) = NULL # Reset row index

## Add GADM City/County data
TWN = gadm.loadCountries("TWN", level = 2, basefile = "./")

# Change City/County names to GADM names
# NOTE: some GADM City/County names are misspelt, but I'm using them anyway for consistency.
GADM_city_names_ord = TWN$spdf$NAME_2[order(TWN$spdf$NAME_2)]
CSV_city_names_ord = unique(CSV.df$City)[order(unique(CSV.df$City))]
CSV.df$City = mapvalues(CSV.df$City, from=CSV_city_names_ord, to=GADM_city_names_ord)

# Add HASC names
city_HASC = TWN$spdf$HASC_2
CSV.df$HASC = mapvalues(CSV.df$City, from=TWN$spdf$NAME_2, to=TWN$spdf$HASC_2)

# Add Latitudinal and Longitudinal data
lonlat = coordinates(TWN$spdf)
colnames(lonlat) = c('lon', 'lat')
rownames(lonlat) = TWN$spdf$NAME_2
#CSV.df$lat = mapvalues(CSV.df$City, from=rownames(lonlat), to=lonlat[ , 'lat'])
#CSV.df$lon = mapvalues(CSV.df$City, from=rownames(lonlat), to=lonlat[ , 'lon'])

# Re-organize columns
CSV.df = CSV.df[ ,c('Year', 'Region', 'City', 'District', 'HASC', 'Week', 'Cases', 'Start_Date', 'End_Date', 'CDC_Year', 'CDC_Week')]

# Use city and district names to search Google Maps (ggmap) for coordinates
city.dist.pairs = unique(CSV.df[ , c('City', 'District')])
city.dist.pairs$City = mapvalues(city.dist.pairs$City, from=GADM_city_names_ord, to=CSV_city_names_ord) # Map back to correct spellings for geocode search
coords.df = getAllTWCoords(city.dist.pairs, COORD_FILE_NAME)
coords.df$City = mapvalues(coords.df$City, from=CSV_city_names_ord, to=GADM_city_names_ord) # Map city names back to GADM format

# Add lat and lon columns
CSV.df = merge(CSV.df, coords.df, by=c("City", "District"))

# Re-organize columns
CSV.df = CSV.df[ ,c('Year', 'Region', 'City', 'District', 'HASC', 'lat', 'lon', 'Week', 'Cases', 'Start_Date', 'End_Date', 'CDC_Year', 'CDC_Week')]

# Sort back after merge
CSV.df = CSV.df[with(CSV.df, order(Year, Region, City, District, Week)), ]
rownames(CSV.df) = NULL # Reset row index

# Write CSV
write.csv(CSV.df, paste0(CSV_OUT_DIR, "master_TWN_weeks.csv"), row.names=FALSE)

## Create CDC-week dataframe
CSV.dt = data.table(CSV.df)
# # Sum duplicate rows (CDC weeks that straddle the New Year are split in to two rows)
CDC.dt = CSV.dt[CDC_Year != 1997, .(Cases = sum(Cases, na.rm = TRUE)), by=.(CDC_Year, Region, City, District, CDC_Week, lat, lon)]
CDC.dt = CDC.dt[order(CDC_Year, Region, City, District, CDC_Week)]
setnames(CDC.dt, c("CDC_Year", "CDC_Week"), c("Year", "Week"))
CDC.dt = specifyGenericDists(CDC.dt)
# Write CSV
write.csv(CDC.dt, paste0(CSV_OUT_DIR, "master_CDC_weeks.csv"), row.names=FALSE)