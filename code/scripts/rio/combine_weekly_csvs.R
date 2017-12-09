#! /usr/bin/env Rscript
# Formats and Merges individual Rio De Janiero CSVs.
# Depends also on a CSV with naming convention for neighborhoods and administritive regions.
library(data.table)
library(stringr)
ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
OUT_DIR = paste0(ROOT_DIR, "data/dengue/rio/merge/")
IN_DIR = paste0(ROOT_DIR, "data/dengue/rio/convert/")
IN_DIR2 = paste0(ROOT_DIR, "data/dengue/rio/format/")
OUTPUT_FILENAME = "master_weekly.csv"

ADMIN_COL_NAME = "Administrative.Region"
PAREA_COL_NAME = "Planning.Area"
NEIGH_COL_NAME = "Neighborhood"
YEAR_COL_NAME = "Year"
POP_COL_NAME = "Population"
WEEK_COL_NAME = "Week"
CASES_COL_NAME = "Cases"
RATE_COL_NAME = "Incidence.Per.100000"
starting.col.names = c("Population", seq(1,52), "Total.Cases")

# Get Admin Region names and Nieghborhood names
name.info.df = read.csv(paste0(IN_DIR2, "wiki_table_name_info.csv"), stringsAsFactors = FALSE)
admin.neigh.name.map = as.data.frame(cbind(paste(name.info.df$Number, name.info.df$Administrative.region, sep = ' '), name.info.df$Neighbourhoods))
colnames(admin.neigh.name.map) = c(ADMIN_COL_NAME, NEIGH_COL_NAME)
ADMIN_NAMES = unique(admin.neigh.name.map$Administrative.Region) # Concatenate number and name strings
NEIGH_NAMES = admin.neigh.name.map$Neighborhood

### Read in CSVs
CSV.filenames = list.files(IN_DIR, pattern="*weekly.csv", full.names = TRUE)
# colClasses = "character" so that R doesn't mess with the types of variables. For example
# read.csv was converting many columns to Double (16 -> 16.00). Then when the decimals were
# removed, it become 1600, when it should be 16.
CSV.data.frames = lapply(CSV.filenames, function(filename) read.csv(filename, header=FALSE, 
        encoding="UTF-8",stringsAsFactors=FALSE, row.names = 1, strip.white = TRUE, colClasses = "character"))

### Create master CSV
CSV.df = c() # Append one after another onto this data frame.
for(i in 1:length(CSV.data.frames)) {
  filename = CSV.filenames[i]
  year = substr(basename(filename), 1, 4)
  df = CSV.data.frames[[i]]
  colnames(df) = starting.col.names  # Make my column names for consitency
  na.columns = which(is.na(colnames(df)))
  if(length(na.columns) > 0) {
    df = df[,-na.columns]  # Remove any extra NA columns
  }
  if(length(grep("total", rownames(df)[1], ignore.case = TRUE)) == 0) {
    df = df[-1,]  # If the first row is not "total" row, it is the old header. Remove it.
  }
  rownames(df) = gsub("\\[[0-9]*\\]", "", rownames(df))  # Some rownames have superscript note references (e.g. "rowname[1]"). Remove these.
  # Thousands place is separated by space (e.g. "1 250" instead of "1250").
  # Thousands place is also separated by '.'. (1.676.454 instead of 1676454).
  numeric = as.data.frame(apply(df,2,function(x)as.numeric(gsub("\\s+|\\.", '',x))))  # Remove spaces and decimal points
  rownames(numeric) = rownames(df)
  df = numeric
  
  tot_col_i = grep("total", colnames(df), ignore.case=TRUE)
  pop_col_i = grep("pop", colnames(df), ignore.case=TRUE)
  unk_row_i = grep("ignorado", rownames(df), ignore.case=TRUE)

  # Use NEIGH_NAMES to do a fuzzy string compare with the rownames of the df
  # in order to find which rows are neighborhood rows.
  dist.name = adist(NEIGH_NAMES, row.names(df), ignore.case = TRUE)
  min.name<-apply(dist.name, 1, min)
  match.s1.s2<-NULL  
  for(j in 1:nrow(dist.name))
  {
    s2.i<-match(min.name[j],dist.name[j,])
    s1.i<-j
    match.s1.s2<-rbind(data.frame(s2.i=s2.i,s1.i=s1.i,s2name=rownames(df)[s2.i], s1name=NEIGH_NAMES[s1.i], adist=min.name[j]),match.s1.s2)
  }
  match.s1.s2 = match.s1.s2[with(match.s1.s2, order(s2.i)), ]
  neigh.indexes = match.s1.s2$s2.i
  neigh.name.map = match.s1.s2[,c("s1name", "s2name")]
  
  # Use ADMIN_NAMES to do a fuzzy string compare with the rownames of the df
  # in order to find which rows are admin rows.
  dist.name = adist(ADMIN_NAMES, row.names(df), ignore.case = TRUE)
  min.name<-apply(dist.name, 1, min)
  match.s1.s2<-NULL  
  for(j in 1:nrow(dist.name))
  {
    s2.i<-match(min.name[j],dist.name[j,])
    s1.i<-j
    match.s1.s2<-rbind(data.frame(s2.i=s2.i,s1.i=s1.i,s2name=rownames(df)[s2.i], s1name=ADMIN_NAMES[s1.i], adist=min.name[j]),match.s1.s2)
  }
  # Order the matched admin names based on Rio PDF's ordering
  match.s1.s2 = match.s1.s2[with(match.s1.s2, order(s2.i)), ]
  admin.indexes = match.s1.s2$s2.i
  admin.names = match.s1.s2$s1name
  admin.index.name.map = match.s1.s2[which(colnames(match.s1.s2) %in% c("s2.i", "s1name"))]
  admin.index.col.name = "Admin.Index"
  colnames(admin.index.name.map) = c(admin.index.col.name, ADMIN_COL_NAME)
  
  # Group neighborhood indexes into lists based on which admin region they are a part of.
  # Neighborhood indexes are in between admin region indexes. For example, if the admin region
  # indexes were 1, 5, 7, then their corresponding neighborhoods would be the indexes in 
  # between. That is, the admin region at index 1 would have the neighborhoods at indexes 2, 3,
  # 4. The admin region at index 5 would just have the neighborhood at index 6. And the admin
  # region at index 7 would have the rest of the neighborhoods after it.
  
  # Since neighborhood indexes are being grouped into ranges, and the ranges are defined the admin indexes,
  # we need to add an end cap index that will create an upper bound for the last admin region.
  end.cap = max(neigh.indexes) + 1
  neigh.indexes.in.bins = split(neigh.indexes, cut(neigh.indexes, breaks = c(admin.indexes, end.cap)))
  names(neigh.indexes.in.bins) = admin.names
  
  # Now to find Planning area rows indexes. They are the indexes that are fill in the gaps of missing
  # indexes from the nieghborhood and admin region indexes. (i.e. if we have admin region indexes as 
  # (2, 5, 10) and neighborhood indexes as (3, 4, 6, 7, 8, 11, 12) then the planning area indexes must
  # be (1, 9), since those are the missing indexes from the union of the admin region and nieghborhood
  # indexes. The 1 is there because it is assumed that the first planning area comes immediately before
  # the first admin region.
  all.indexes = seq(min(admin.indexes) - 1, max(neigh.indexes))
  planning.indexes = setdiff(all.indexes, union(admin.indexes, neigh.indexes))
  planning.numbers = str_extract(rownames(df)[planning.indexes], "\\-*\\d+\\.*\\d*")  # Extract floating point numbers from rownames
  planning.numbers = as.character(format(as.numeric(planning.numbers), nsmall=1))  # Make sure all numbers have one decimal place (i.e. 1 -> 1.0) for formatting consistency
  PLANNING_AREA_NAMES = paste("Planning Area", planning.numbers, sep=' ')
  end.cap = max(admin.indexes) + 1
  admin.indexes.in.bins = split(admin.indexes, cut(admin.indexes, breaks = c(planning.indexes, end.cap)))
  names(admin.indexes.in.bins) = PLANNING_AREA_NAMES

  # Get planning area -> admin region mapping df
  planning.admin.names.map = melt(admin.indexes.in.bins)
  colnames(planning.admin.names.map) = c(admin.index.col.name, PAREA_COL_NAME)  # Merge maps the columns based on column names, so name them appropriately.
  planning.admin.names.map = merge(planning.admin.names.map, admin.index.name.map)  # Map admin indexes to their names
  planning.admin.names.map = planning.admin.names.map[-which(colnames(planning.admin.names.map) == admin.index.col.name)] # Remove admin index column

  # Get rid of all rows except neighborhood rows because we don't care about admin region
  # or planning area totals. Those can be calculated from the neighborhoods later if desired.
  fmt_df = df[neigh.indexes,]
  
  # Rename neighborhoods with correct, consistent spelling.
  row.names(fmt_df) = neigh.name.map$s1name[match(neigh.name.map$s2name, rownames(fmt_df))]
  
  # Remove Total column
  fmt_df = fmt_df[-c(tot_col_i)]
  
  # Add Year column
  year_col = rep(year, nrow(fmt_df))
  fmt_df = cbind(fmt_df,Year = year_col)
  
  # Make rownames into an actual column (Neighboorhood column)
  setDT(fmt_df, keep.rownames = TRUE)[]
  setnames(fmt_df, 1, NEIGH_COL_NAME)

  # Make a long data frame defining the relationship between planning areas, admin regions
  # and neighborhoods.
  patch.name.map = merge(admin.neigh.name.map, planning.admin.names.map)  
  fmt_df = merge(fmt_df, patch.name.map)
  # Make Unknown row (unk)
  unknown_row = as.numeric(df[unk_row_i,])
  unk_pop = NA
  unknown_row = unknown_row[-c(pop_col_i,tot_col_i)]
  unknown_row = c('UNKNOWN', unk_pop, unknown_row, year, "UNKNOWN", "UNKNOWN")
  unknown_row = as.data.frame(matrix(unknown_row, nrow=1), stringsAsFactors=FALSE)
  colnames(unknown_row) = colnames(fmt_df)
  fmt_df = rbind(fmt_df, unknown_row)
  
  # Change from wide to long format
  fmt_df = melt(fmt_df, id.vars=c(YEAR_COL_NAME,PAREA_COL_NAME,ADMIN_COL_NAME,NEIGH_COL_NAME,POP_COL_NAME), variable.name=WEEK_COL_NAME, value.name=CASES_COL_NAME)
  # Sort
  fmt_df = fmt_df[with(fmt_df, order(Year,Planning.Area,Administrative.Region,Neighborhood)), ]
  CSV.df = rbind(CSV.df,fmt_df)
}
# Create Incidence per 100,000 column
Incidence.Per.100000 = as.numeric(CSV.df$Cases) * (100000 / as.numeric(CSV.df$Population))
CSV.df = cbind(CSV.df,round(Incidence.Per.100000, digits=2))
colnames(CSV.df)[length(CSV.df)] = RATE_COL_NAME

write.csv(CSV.df, paste0(OUT_DIR, OUTPUT_FILENAME), row.names = FALSE)
