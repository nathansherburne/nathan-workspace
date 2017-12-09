#! /usr/bin/env Rscript
library(data.table)
library(DICE)
library(stringr)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
IN_DIR = paste0(ROOT_DIR, "data/flu/san_diego/convert/")
MASTER.FILEPATH.1 = paste0(ROOT_DIR, "data/flu/san_diego/merge/sd_county_by_cdc_week_2008-todate.csv")
MASTER.FILEPATH.2 = paste0(ROOT_DIR, "data/flu/san_diego/merge/sd_county_influenza_17-18.csv")
source(paste0(ROOT_DIR, "code/scripts/generic_scripts/utils.R"))

master.df.1 = read.csv(MASTER.FILEPATH.1, row.names = NULL, stringsAsFactors = FALSE)
master.df.2 = read.csv(MASTER.FILEPATH.2, row.names = NULL, stringsAsFactors = FALSE)

# Find last added row of master 2 CSV so we know which week to start appending
last.row.master.2 = master.df.2[nrow(master.df.2),]
last.row.week.num = last.row.master.2$Week
last.row.year.num = last.row.master.2$Year

CSV.filenames = list.files(IN_DIR, pattern="*.csv", full.names = TRUE)
dfs = lapply(CSV.filenames, function(filename) read.csv(filename, stringsAsFactors = FALSE, skip = 1, row.names = 1))

for(i in 1:length(CSV.filenames)) {
  filename = CSV.filenames[i]
  df = dfs[[i]]
  week.num = str_extract(colnames(df)[1], "\\-*\\d+\\d*")
  year.num = str_extract(str_extract(filename, "InfluenzaWatch_[0-9]*"), "\\-*\\d+\\d*")
  if(year.num == last.row.year.num && week.num > last.row.week.num
     || year.num > last.row.year.num) {  # Only update if this is a new date
    
    if(week.num >= 27 && week.num <= 53) {
      year.range = paste0(year.num, '.', as.numeric(year.num) + 1)
    } else {
      year.range = paste0(as.numeric(year.num) - 1, '.', year.num)
    }
    M1.select.column.name = paste0('X', year.range)  # R puts 'X' in front of numeric column names when reading CSVs (i.e. '2017-2018' -> 'X2017-2018')
    
    # Dynamically find out which row is which (in case spelling or order changes from week to week)
    Total.row = grep("Total", rownames(df), ignore.case = TRUE)
    A.H3.row = grep("H3", rownames(df), ignore.case = TRUE)
    B.Victoria.row = grep("Vic", rownames(df), ignore.case = TRUE)
    B.Yamagata.row = grep("Yam", rownames(df), ignore.case = TRUE)
    AB.row = grep("A/B", rownames(df), ignore.case = TRUE)
    
    H1.row.nums = grep("H1", rownames(df), ignore.case = TRUE)
    H1N1.row = grep("H1N1", rownames(df), ignore.case = TRUE)
    A.H1.row = H1.row.nums[-which(H1.row.nums == H1N1.row)]
    
    # "Influena A" and "Influenza B" rows are harder to grep since their names are not very unique among the others.
    # For "Influenza B", just grep for "B" and remove rows that we've already found. Similar idea for "Influenza A".
    other.row.nums = c(H1N1.row, A.H3.row, B.Victoria.row, B.Yamagata.row, AB.row, Total.row, A.H1.row)
    rows.with.letter.b = grep("B", rownames(df), ignore.case = TRUE)
    B.row = rows.with.letter.b[-which(rows.with.letter.b %in% other.row.nums)]
    other.row.nums = c(other.row.nums, B.row)
    A.row = which(rownames(df) == rownames(df)[-other.row.nums])
    
    # Update each master df
    master.df.1[which(master.df.1$week == week.num), which(colnames(master.df.1) == M1.select.column.name)] = df[Total.row, 1]
    week.end.date = convertDateFormat(CDCweek2date(week.num, year.num) + 6)
    
    if(length(Total.row) == 0) {
      Total = NA
    } else {
      Total = df[Total.row, 1]
    }
    if(length(A.H3.row) == 0) {
      A.H3 = NA
    } else {
      A.H3 = df[A.H3.row, 1]
    }
    if(length(B.Victoria.row) == 0) {
      B.Victoria = NA
    } else {
      B.Victoria = df[B.Victoria.row, 1]
    }
    if(length(B.Yamagata.row) == 0) {
      B.Yamagata = NA
    } else {
      B.Yamagata = df[B.Yamagata.row, 1]
    }
    if(length(AB.row) == 0) {
      AB = NA
    } else {
      AB = df[AB.row, 1]
    }
    if(length(H1N1.row) == 0) {
      H1N1 = NA
    } else {
      H1N1 = df[H1N1.row, 1]
    }
    if(length(A.H1.row) == 0) {
      A.H1 = NA
    } else {
      A.H1 = df[A.H1.row, 1]
    }
    if(length(B.row) == 0) {
      B = NA
    } else {
      B = df[B.row, 1]
    }
    if(length(A.row) == 0) {
      A = NA
    } else {
      A = df[A.row, 1]
    }
    
    NEW.ROW = c(year.num, week.num, A, A.H1, A.H3, B.Yamagata, B.Victoria, B, H1N1, AB, Total, week.end.date)
    master.df.2 = rbind(master.df.2, NEW.ROW)
  }
}
master.df.2 = master.df.2[with(master.df.2, order(Year, Week)), ]
write.csv(master.df.1, MASTER.FILEPATH.1, row.names = FALSE)
write.csv(master.df.2, MASTER.FILEPATH.2, row.names = FALSE)

print(paste0(MASTER.FILEPATH.1, " updated"))
print(paste0(MASTER.FILEPATH.2, " updated"))









