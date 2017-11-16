# For San Diego's CSV date format
convertDateFormat <- function(inputDate) {
  ## Format the date from "YYYY-MM-DD" format -> "MM/DD/YY" format (with leading zeros removed).
  year = substr(unlist(strsplit(as.character(inputDate), '-'))[1], 3, 5)  # changes "2017" -> "17"
  month = unlist(strsplit(as.character(inputDate), '-'))[2]
  day = unlist(strsplit(as.character(inputDate), '-'))[3]
  month = gsub("0(\\d)", "\\1", month)  # Remove leading zeros
  day = gsub("0(\\d)", "\\1", day)  # Remove leading zeros
  date = paste0(month, '/', day, '/', year);
  return(date)
}
