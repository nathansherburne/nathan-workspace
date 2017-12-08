require(data.table)

detrend <- function(matrix) {
  matrix[is.na(matrix)] = 0
  logMatrix = matrix
  for (i in 1:ncol(matrix)) {
    logMatrix[ ,i] = log(matrix[ ,i]+1)  # add 1 to state data to avoid zeros!
    logMatrix[ ,i] = logMatrix[ ,i] - mean(logMatrix[ ,i])
    logMatrix[ ,i] = logMatrix[ ,i] / sd(logMatrix[ ,i])
  }
  return (logMatrix)
}