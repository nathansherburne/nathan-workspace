# Rio - Combine individual CSVs from web scraping into one CSV
rm(list=ls())

library(data.table)
CSVOutDir = "~/Dropbox/LEPR03/nathan/Dengue/Rio/Data/Dengue_Classic/CSVs/master/"
CSV.DIR = paste0("~/Dropbox/LEPR03/nathan/Dengue/Rio/Data/Dengue_Classic/CSVs/FINAL_individual/weekly/")
BRAZ_MON_ABBR = c("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
MY_AREA_NAMES = c('Area 1.0','Area 2.1','Area 2.2','Area 3.1','Area 3.2','Area 3.3','Area 4.0','Area 5.1','Area 5.2','Area 5.3')
MY_WEEK_NAMES = seq(1:52)

years = c(seq(2000,2010),seq(2016,2017))
first.year = 2000
nyears = length(years)
nWeeks = 52

### Read in CSVs
CSV.filenames = list.files(CSV.DIR, pattern="*.csv")
CSV.data.frames = list()
for(i in 1:length(CSV.filenames)) {
  CSV.data.frames[[i]] = read.csv(paste0(CSV.DIR, CSV.filenames[i]), head=TRUE, encoding="UTF-8",stringsAsFactors=FALSE)
  CSV.data.frames[[i]] = data.frame(CSV.data.frames[[i]][,-1], row.names=CSV.data.frames[[i]][,1]) # Remove index column, replace with 1st column
}

### Create master CSV
names(CSV.data.frames) = years
CSV.df = c() # Append one year after another onto this
for(i in 1:length(CSV.data.frames)) {
  df = CSV.data.frames[[i]]
  tot_col_i = grep("total", colnames(df), ignore.case=TRUE)
  pop_col_i = grep("pop", colnames(df), ignore.case=TRUE)
  unk_row_i = grep("ignorado", rownames(df), ignore.case=TRUE)
  tot_row_i = grep("total", rownames(df), ignore.case=TRUE)
  
  start_pArea = grep(pattern="[[:digit:]]", rownames(df)) # pArea rows
  start_aRegion = grep(pattern="(\\b[IVX]+\\b)", rownames(df)) # aRegion rows
  aRegion_names = rownames(df)[start_aRegion]
  pArea_names = rownames(df)[start_pArea]
  s = seq(1,nrow(df))
  neigh_rows = s[-c(start_pArea,start_aRegion,tot_row_i,unk_row_i)] # neighboorhood rows
  neigh_names = rownames(df)[neigh_rows]
  
  fmt_df = df[neigh_rows,]
  
  # Remove all but the week number from column names
  names(fmt_df) = gsub("[a-zA-z.]", "", names(fmt_df)) 
  
  # Find number of Neighboorhoods in each Admin Region
  end_aRegion = neigh_rows[which(diff(neigh_rows) >= 2)] # Last neighboorhood of each aRegion
  end_aRegion =c(end_aRegion, neigh_rows[length(neigh_rows)]) # Include the last neighboorhood
  neigh_per_aRegion = end_aRegion - start_aRegion
  
  # Find number of Admin Regions in each Planning Area
  aRegion_per_pArea = c()
  for(j in 2:length(start_pArea)) {
    aReg_in_pArea = which(start_aRegion < start_pArea[j] & start_aRegion > start_pArea[j-1])
    aRegion_per_pArea = c(aRegion_per_pArea, length(aReg_in_pArea))
  }
  aReg_in_pArea = which(start_aRegion > start_pArea[j])
  aRegion_per_pArea = c(aRegion_per_pArea, length(aReg_in_pArea))
  
  # Add Admin Region column
  aRegion_col = rep(aRegion_names,neigh_per_aRegion)
  fmt_df = cbind(fmt_df,Administrative_Region = aRegion_col)
  
  # Add Planning Area column
  neigh_per_pArea = as.numeric(unlist(lapply(split(neigh_per_aRegion, rep(1:10, aRegion_per_pArea)), function(x) sum(x))))
  pArea_col = rep(MY_AREA_NAMES, neigh_per_pArea)
  fmt_df = cbind(fmt_df,Planning_Area = pArea_col)
  
  # Add Year column
  fmt_df = fmt_df[-c(tot_col_i,pop_col_i)]
  year_col = rep(years[i], nrow(fmt_df))
  fmt_df = cbind(fmt_df,Year = year_col)
  
  # Make rownames into an actual column (Neighboorhood column)
  setDT(fmt_df, keep.rownames = TRUE)[]
  setnames(fmt_df, 1, "Neighboorhood")

  # Make Population column
  pop_col = df[pop_col_i]
  pop_col = pop_col[-c(start_aRegion,start_pArea,tot_row_i,unk_row_i),]
  #pop_col = rep(pop_col, each=nWeeks)
  fmt_df = cbind(fmt_df,Population = pop_col)
  
  # Make Unknown row (unk)
  unknown_row = as.numeric(df[unk_row_i,])
  unk_pop = unknown_row[pop_col_i]
  unknown_row = unknown_row[-c(pop_col_i,tot_col_i)]
  unknown_row = c('UNKNOWN', unknown_row, 'UNKNOWN', 'UNKNOWN', years[i], NA)
  unknown_row = as.data.frame(matrix(unknown_row, nrow=1), stringsAsFactors=FALSE)
  colnames(unknown_row) = colnames(fmt_df)
  fmt_df = rbind(fmt_df, unknown_row)
  
  # Change from wide to long format
  fmt_df = melt(fmt_df, id.vars=c('Year','Planning_Area','Administrative_Region','Neighboorhood','Population'), variable.name="Week", value.name="Cases")
  
  # Define how (the way the CSV orders them) the rows are to be sorted (because alphabetical is default)
  fmt_df$Neighboorhood = factor(fmt_df$Neighboorhood, levels=c(neigh_names,'UNKNOWN'))
  fmt_df$Planning_Area = factor(fmt_df$Planning_Area, levels=c(MY_AREA_NAMES, 'UNKNOWN'))
  fmt_df$Administrative_Region = factor(fmt_df$Administrative_Region, levels=c(aRegion_names,'UNKNOWN'))
  
  # Sort
  fmt_df = fmt_df[with(fmt_df, order(Year,Planning_Area,Administrative_Region,Neighboorhood)), ]
  
  CSV.df = rbind(CSV.df,fmt_df)
}

# Create Incidence per 100,000 column
Incidence_per_100000 = as.numeric(CSV.df$Cases) * (100000 / as.numeric(CSV.df$Population))
CSV.df = cbind(CSV.df,round(Incidence_per_100000, digits=2))
colnames(CSV.df)[length(CSV.df)] = "Incidence_Per_100000"

# No weekly data for 2011-2015. So create two separate CSVs for consecutive years.
CSV_2000_to_2010_weekly = CSV.df[which(CSV.df$Year <= 2010),]
CSV_2016_to_2017_weekly = CSV.df[which(CSV.df$Year >= 2016),]

write.csv(CSV.df, paste0(CSVOutDir, "master_all_years_weekly.csv"), row.names = FALSE)
write.csv(CSV_2000_to_2010_weekly, paste0(CSVOutDir, "master_2000-2010_weekly.csv"), row.names = FALSE)
write.csv(CSV_2016_to_2017_weekly, paste0(CSVOutDir, "master_2016-2017_weekly.csv"), row.names = FALSE)
