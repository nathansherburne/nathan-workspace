removeTotals <- function(df) {
  # Removes any row or column of a data frame containing the string 'total' in it.
  df = df[-grep("Total", df[[1]], ignore.case=TRUE), ] # Remove 'Total' Row
  df = df[ ,-grep("Total", colnames(df), ignore.case=TRUE)] # Remove 'Total' Column
  return(df)
}

nameOrAddRateCol <- function(df, pop.data.2010) {
  # Names the rate column if it is present. Calculates the rate column if it is not present.
  #
  # Args:
  #   df: a wide format data frame containing monthly data
  #   pop.data.2010: population data corresponding to the rows of 'df'
  #
  # Return:
  #   df: a wide format data frame containing monthly data as well as incidence per 100,000
  rate_col = grep("rate|inc", colnames(df), ignore.case=TRUE)
  if(length(rate_col) == 0) {
    df.with.pop = merge(df, pop.data.2010)
    df = df[with(df, order(df$State)), ]  # Sort so that Rate values line up. (merge sorts as side effect??)
    df$Incidence_Per_100000 = apply(df.with.pop, 1, getYearlyRates)
  }
  else {
    colnames(df)[rate_col] = 'Incidence_Per_100000'
  }
  return(df)
}

addYearCol <- function(df, year) {
  # Adds a column with one repeating value to a data frame.
  #
  # Args:
  #   df: a data frame to add a column to
  #   year: a value to make a column out of
  #
  # Return:
  #   df: the data frame with new column filled with 'year'.
  df$Year = rep(year, nrow(df))
  return(df)
}

getYearlyRates <- function(state.row) {
  # Calculates the total incidence per 100,000 over 12 months.
  #
  # Args:
  #   state.row: a row of a data frame with monthly incidence and population.
  #
  # Return:
  #   -- : a double, the year's total incidence per 100,000
  total.cases = sum(as.numeric(state.row[month.name]), na.rm=TRUE)
  state.pop = as.numeric(state.row['Population'])
  return(round(100000 * total.cases / state.pop, digits=2))
}


### Unused ###

calcAndAddRateCol <- function(df, pop.data.year) {
  # Remove incidence_per / rate column if it exists
  rate_col = grep("rate|inc", colnames(df), ignore.case=TRUE)
  if(length(rate_col) > 0) {
    df = df[-rate_col]
  }
  # Calculate a monthly rate column based on population data
  browser()
  df.with.pop = merge(df, pop.data.year[ , c('State', 'Population')])
  df = df[with(df, order(State)), ]  # Sort so that Rate values line up. (merge sorts as side effect??)
  rates = apply(df.with.pop, 1, getMonthlyRates)
  rates = do.call(rbind, rates)
  return(rates)
}

getMonthlyRates <- function(state.row) {
  # Calculates the incidence per 100,000 for 12 months individually.
  #
  # Args:
  #   state.row: a row of a data frame with monthly incidence and population.
  #
  # Return:
  #   rates.row : a single row of a data frame, each month's incidence per 100,000
  monthly.cases = as.numeric(state.row[month.name])
  state.pop = as.numeric(state.row['Population'])
  monthly.rates = round(100000 * monthly.cases / state.pop, digits=2)
  rates.row = state.row
  rates.row[month.name] = monthly.rates
  return(rates.row)
}

formatRownames <- function(df) {
  st.names = df$State
  st.names[which(st.names == "Ciudad de México")] = "Distrito Federal"
  df$State = st.names
  return(df)
}

removeNumericWhitespace <- function(df) {
  df[month.name] = as.data.frame(apply(df[month.name],2,function(x)as.numeric(gsub('\\s+', '',x))))
  return(df)
}