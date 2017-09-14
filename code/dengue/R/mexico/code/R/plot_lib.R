require("RColorBrewer")

PlotMonthsHist <- function(values, state_name) {
  nmonths = 12
  ret = hist(values,main=state_name, xlab="Month", xaxt='n', breaks=seq(0, nmonths))
  axis(side = 1, at = ret$mids, lab=month.abb, cex.axis=.6)
}

Mode <- function(x) {
  # Returns the most common occurring item in a structure. If there's a tie, it returns the first mode.
  ux <- unique(x)
  ux[which.max(tabulate(match(x, ux)))]
}

detrend <- function(vec) {
  # Detrends a vector of numbers.
  #
  # Args:
  #   vec: a vector of numbers
  #
  # Return:
  #   vec: a vector of detrended numbers
  vec = log(vec + 1)  # Add 1 to avoid taking the log of zeros.
  vec = vec - mean(vec)
  vec = vec / sd(vec)
}