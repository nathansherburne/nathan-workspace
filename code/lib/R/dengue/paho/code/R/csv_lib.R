require(data.table)

formatPAHOcsv <- function(df) {
  # Reads a PAHO CSV, removes non-country rows (i.e. "total", "subtotal", ...), and optionally gives new column names.
  # 
  # Args:
  #   dt: the data table to format
  #
  # Returns:
  #   A datatable with countries as rows
  #
  COL_NAMES = c("Country.or.Subregion", "Week", "Probable", "Probable.Incidence.Rate", 
                "Lab.Confirm", "Lab.Confirm.Incidence.Rate", "Serotype", "Severe.Dengue", 
                "Deaths", "Population.x.1000", "(SD/D).x.100", "CFR") 
  
  names(df) = COL_NAMES
  # Remove unwanted rows (total, subtotal, etc...)
  df = df[-grep("total|country", df$Country.or.Subregion, ignore.case=TRUE), ]
  # Remove unneeded columns (incidence rate, SD/D ratio, CFR,... can all be calculated)
  drops = c("Probable.Incidence.Rate", "Lab.Confirm.Incidence.Rate", "(SD/D).x.100", "CFR")
  df = df[ , names(df)[!(names(df) %in% drops)]]
  # Clean up country names
  df$Country.or.Subregion = sapply(df$Country.or.Subregion, function(str) { 
    str = remFirstIfLower(str) 
    trimws(str)})
  # Remove commas for thousands separator, convert Strings to numeric
  new.cols = as.data.frame(apply(df[ , c("Probable", "Lab.Confirm", "Severe.Dengue", "Deaths", "Population.x.1000")], 2, function(col) as.numeric(gsub(",", "", col))))
  df[, match(names(new.cols), names(df))] = new.cols
  # If the country has not reported yet (i.e. 'Week' is still 00), assume that NA is the same as 0 for Week 00.
  #df[which(df$Week=="Week 00"), ][is.na(df[which(df$Week=="Week 00"), ])] = 0
  return(df)
}

addYearCol <- function(dt, year) {
  dt = as.data.table(dt)
  yearCol = rep(year, nrow(dt))
  dt[, Year := yearCol, ]
  return(as.data.frame(dt))
}

remFirstIfLower <- function(str) {
  # Removes the first character in a string if it is lowercase.
  #
  if(substring(str, 1, 1) %in% letters) {  # Check if first letter is lowercase
    return(substring(str, 2))  # If so, return without the first letter.
  }
  return(str)
}

getDifference <- function(wk.prev,wk.curr) {
  # Finds an individual week's (or range of weeks) data by taking the difference of two weeks.
  # Some countries do not report weekly data. So the range of weeks is included instead of a single week number.
  #
  # Args:
  #   wk.prev: a row of a data table containing the previous week's cumulative data
  #   wk.curr: a row of a data table containing the current week's cumulative data
  #
  # Return:
  #   diff: a row of a data table containing the difference of wk.curr and wk.prev
  #if(wk.prev$Week == wk.curr$Week) {  # This country did not report this week
    #return(NULL)
  #}
  diff = wk.curr
  diff$Week = paste(wk.prev$Week,'-',wk.curr$Week)
  diff$Probable = wk.curr$Probable - wk.prev$Probable
  diff$Lab.Confirm = wk.curr$Lab.Confirm - wk.prev$Lab.Confirm
  diff$Severe.Dengue = wk.curr$Severe.Dengue - wk.prev$Severe.Dengue
  diff$Deaths = wk.curr$Deaths - wk.prev$Deaths
  ser.pres.wk.prev =  as.numeric(na.omit(as.numeric(unlist(strsplit(wk.prev$Serotype, "[^0-9]+")))))
  ser.pres.wk.curr =  as.numeric(na.omit(as.numeric(unlist(strsplit(wk.curr$Serotype, "[^0-9]+")))))
  new.ser = ser.pres.wk.curr[!ser.pres.wk.curr %in% ser.pres.wk.prev]
  if(length(new.ser)==0) {
    diff$New.Serotype = NA
  } else {
    diff$New.Serotype = paste("DEN", paste0(new.ser, collapse = ","))  # Use the same format it is already in.
  }
  names(diff)[which(names(diff) == "Serotype")] = "Cumul.Serotype"
  return(diff)
}

getNoncumulativeDf <- function(cur.week, prev.week) {
  diff.df = data.frame()
  for(i in 1:nrow(cur.week)) {
    diff.row = getDifference(prev.week[i, ], cur.week[i, ])
    diff.df = rbind(diff.df, diff.row)
  }
  return(diff.df)
}








