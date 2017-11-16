


convertDateFormat <- function(input, inputFormat, outputFormat) {
  
  
  
  last.row.end.date = as.character(last.row.master[2])
  last.row.month = unlist(strsplit(last.row.end.date, '/'))[1]
  last.row.day = unlist(strsplit(last.row.end.date, '/'))[2]
  last.row.year = paste0("20", unlist(strsplit(last.row.end.date, '/'))[3]) # i.e. changes "17" -> "2017"
  last.row.end.date = as.Date(paste0(last.row.year, '-', last.row.month, '-', last.row.day))
}
