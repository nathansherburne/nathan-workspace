ROOT_DIR = "~/nathan/Dengue/sri_lanka/"
DATA_DIR = paste0(ROOT_DIR, "data/")
CSV_DIR = paste0(DATA_DIR, "CSVs/")
OUT_DIR = paste0(CSV_DIR, "master/")
WEEKLY_DIR = paste0(CSV_DIR, "weekly/")
MONTHLY_DIR = paste0(CSV_DIR, "monthly/")

CSV.filenames.w = list.files(WEEKLY_DIR, pattern="*.csv")
CSV.filenames.m = list.files(MONTHLY_DIR, pattern="*.csv")

dfs.w.all = lapply(CSV.filenames.w, function(filename) {
  df = read.csv(paste0(WEEKLY_DIR, filename), stringsAsFactors = FALSE, row.names = NULL)
  colnames(df) <- c(colnames(df)[-1],"x")
  df$x <- NULL
  return(df)
  })
dfs.m.all = lapply(CSV.filenames.m, function(filename) read.csv(paste0(MONTHLY_DIR, filename), stringsAsFactors = FALSE))

years.w = as.vector(sapply(CSV.filenames.w, function(filename) substr(filename, 1, 4)))
years.m = as.vector(sapply(CSV.filenames.m, function(filename) substr(filename, 1, 4)))

dfs.w.all = mapply(function(df, year) {
  df$year=year
  return(df)
  }, dfs.w.all, years.w, SIMPLIFY = FALSE)
df.w.all = do.call(rbind, dfs.w.all)
df.w.all = df.w.all[c("year", "Month", "Week.Ending", "Week", "Cases")]


dfs.m.all = mapply(function(df, year) {
  df$year=year
  return(df)
}, dfs.m.all, years.m, SIMPLIFY = FALSE)
df.m.all = do.call(rbind, dfs.m.all)
df.m.all = df.m.all[,c(length(df.m.all), 1:(length(df.m.all)-1))]

write.csv(df.w.all, paste0(OUT_DIR, "master_weekly.csv"), row.names=FALSE)
write.csv(df.m.all, paste0(OUT_DIR, "master_monthly.csv"), row.names=FALSE)