#! /usr/bin/env Rscript
library(data.table)
library(DICE)
library(stringr)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
IN_DIR = paste0(ROOT_DIR, "data/flu/tennessee/convert/")
MASTER.FILEPATH = "~/Dropbox/LEPR03/nathan-workspace/data/flu/tennessee/merge/TN-ILI-2009-todate.csv"

master.df = read.csv(MASTER.FILEPATH, row.names = NULL, stringsAsFactors = FALSE)
# Find out what the last week in master is
last.row.master = master.df[nrow(master.df),]
last.week.num = last.row.master[1]

# Go from format: "4/4/17" to "2017-04-04"
last.row.end.date = as.character(last.row.master[2])
last.row.month = unlist(strsplit(last.row.end.date, '/'))[1]
last.row.day = unlist(strsplit(last.row.end.date, '/'))[2]
last.row.year = paste0("20", unlist(strsplit(last.row.end.date, '/'))[3]) # i.e. changes "17" -> "2017"
last.row.end.date = as.Date(paste0(last.row.year, '-', last.row.month, '-', last.row.day))


# Get all of the files that are not included in the master.
# (i.e. the files with a date greater than the date of the 
# last row of the master file)
CSV.filenames = list.files(IN_DIR, pattern="*.csv", full.names = TRUE)
master.changed = FALSE
for(i in 1:length(CSV.filenames)) {
  CSV.filename = CSV.filenames[i]
  new.file.week.num = str_extract(str_extract(CSV.filename, "week[0-9]*"), "\\-*\\d+\\d*")
  new.file.year.num = str_extract(str_extract(CSV.filename, "spnreport_[0-9]*"), "\\-*\\d+\\d*")
  new.file.start.date = CDCweek2date(new.file.week.num, new.file.year.num)
  new.file.end.date = new.file.start.date + 6
  
  if(new.file.end.date > last.row.end.date) {
    ## Format the date from "YYYY-MM-DD" format -> "MM/DD/YY" format (with leading zeros removed).
    new.file.year = substr(unlist(strsplit(as.character(new.file.end.date), '-'))[1], 3, 5)  # changes "2017" -> "17"
    new.file.month = unlist(strsplit(as.character(new.file.end.date), '-'))[2]
    new.file.day = unlist(strsplit(as.character(new.file.end.date), '-'))[3]
    new.file.month = gsub("0(\\d)", "\\1", new.file.month)  # Remove leading zeros
    new.file.day = gsub("0(\\d)", "\\1", new.file.day)  # Remove leading zeros
    new.file.end.date = paste0(new.file.month, '/', new.file.day, '/', new.file.year);
    df = read.csv(CSV.filename, row.names = NULL, skip = 2, stringsAsFactors = FALSE)
   
    # Get ILI values for each row (2nd to last column). If the last column has "n/a" in it, 
    # use that instead of the ILI value.
    ILI.list = list()
    for(i in 1:nrow(df)) {
      ILI = df[i,length(df[i,]) -1]
      NA.val = df[i,length(df[i,])]
      ILI = str_extract(ILI,"\\-*\\d+\\.*\\d*" ) # Remove % sign
      if(NA.val == "n/a") {
        ILI = NA
      }
      ILI.list[i] = ILI
    }
    ILI.list = unlist(ILI.list)
    
    # Put all values for the new row into one list.
    NEW.ROW = c(new.file.week.num, as.character(new.file.end.date), ILI.list)
    
    # Append the row to the master
    master.df = rbind(master.df, NEW.ROW)
    master.changed = TRUE
  }
}

if(master.changed) {
  # Write
  write.csv(master.df, MASTER.FILEPATH, row.names = FALSE)
  print("Master CSV updated!")
} else {
  print("Master CSV already up to date.")
}

