rm(list=ls())
library(DICE)
source("~/GitReps/FluFore_code/codes/ScoreCsv.R")

score <- function(mydata) {
  PSI.scoring = data.frame()
  for(i in 1:length(forecast.week)) {
    week = forecast.week[i]
    nweeksFit = which(mydata$weeks==week)
    filename = list.files(path=paste0(RepDir,"CDC",start.year,"-",start.year+1,"/weekly_submission/"),pattern=paste0("EW",week,"-",".*\\.csv"))
    est = read.csv(file=paste0(RepDir,"CDC",start.year,"-",start.year+1,"/weekly_submission/",filename))
    res = ScoreCsv(mydata,est,nweeksFit,peak = TRUE)
    res = cbind(res, as.integer(week))
    PSI.scoring = rbind(PSI.scoring, res)
  }
  PSI.scoring$Actual.Value = NULL
  PSI.scoring$Point.Est = NULL
  PSI.scoring$Absolute.error = NULL
  names(PSI.scoring) = c('location', 'target', 'score', 'forecast_week')
  PSI.scoring$team = "PSI"
  PSI.scoring$skill = exp(PSI.scoring$score)
  
  # Read CDC scoring results
  filename = "PSI_all_scores_revised.csv"
  CDC.scoring = read.csv(file=paste0(RepDir,"CDC",start.year,"-",start.year+1,'/', filename), stringsAsFactors = FALSE)
  
  CDC.scoring = CDC.scoring[with(CDC.scoring, order(forecast_week, location, target)), ]
  PSI.scoring = PSI.scoring[with(PSI.scoring, order(forecast_week, location, target)), ]
  rownames(CDC.scoring) = NULL
  rownames(PSI.scoring) = NULL
  return (PSI.scoring)
}

displayDifference <- function(PSI.scoring, CDC.scoring) {
  difference = CDC.scoring$score - PSI.scoring$score
  
  nScores = length(difference)
  nExactMatch = length(which(difference == 0))
  exactMatchPer = nExactMatch / nScores
  nApproxMatch = length(which(abs(difference) <= APPROX_MATCH & difference != 0))
  approxMatchPer = nApproxMatch / nScores
  nApproxMismatch = length(which(abs(difference) > APPROX_MATCH & abs(difference) != 10))
  approxMismatchPer = nApproxMismatch / nScores
  nCompleteMismatch = length(which(abs(difference) == 10))
  completeMismatchPer = nCompleteMismatch / nScores
  print("")
  print(paste0("For ", nScores, " scores: "))
  print(paste0(round(exactMatchPer * 100, 2), "% are exact matches. (diff == 0)"))
  print(paste0(round(approxMatchPer * 100, 2), "% are aproximate matches. (diff < 0.5 & diff != 0)"))
  print(paste0(round(approxMismatchPer * 100, 2), "% are aproximate mismatches. (diff > 0.5 & diff != 10)"))
  print(paste0(round(completeMismatchPer * 100, 2), "% have opposite values. (diff == 10)"))
}
  
start.year = 2016
forecast.week = c(seq(43,52,1),seq(1,18,1))
mydata = get.subset(start.year=start.year,end.year=start.year+1, mod_level = 2, fit_level=3)
mydata.rounded = mydata
mydata.rounded$model$raw = round(mydata.rounded$model$raw, digits=1)
mydata.rounded$fit$raw = round(mydata.rounded$fit$raw, digits=1)
APPROX_MATCH = 0.5

dataDir = paste0("~/GitReps/FluFore_data/CDC2016-2017/weekly_submission/")
RepDir = paste0("~/GitReps/FluFore_data/")

# Read CDC scoring results
filename = "PSI_all_scores_revised.csv"
CDC.scoring = read.csv(file=paste0(RepDir,"CDC",start.year,"-",start.year+1,'/', filename), stringsAsFactors = FALSE)
CDC.scoring = CDC.scoring[with(CDC.scoring, order(forecast_week, location, target)), ]
rownames(CDC.scoring) = NULL

PSI.scoring = score(mydata)
PSI.scoring.rounded = score(mydata.rounded)

displayDifference(PSI.scoring, CDC.scoring)
print("")
displayDifference(PSI.scoring.rounded, CDC.scoring)
print("")
displayDifference(PSI.scoring, PSI.scoring.rounded)
write.csv(PSI.scoring, "~/Dropbox/LEPR03/nathan/data/CDC_flu_challenge/PSI_all_scores_PSI-generated.csv", row.names=FALSE)
write.csv(PSI.scoring.rounded, "~/Dropbox/LEPR03/nathan/data/CDC_flu_challenge/PSI_all_scores_PSI-generated_rounded.csv", row.names=FALSE)
write.csv(CDC.scoring, "~/Dropbox/LEPR03/nathan/data/CDC_flu_challenge/PSI_all_scores_CDC-generated.csv", row.names=FALSE)
