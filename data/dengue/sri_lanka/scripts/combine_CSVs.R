ROOT_DIR = "~/nathan/data/dengue/sri_lanka/"
DATA_DIR = paste0(ROOT_DIR, "convert/")
OUT_DIR = paste0(ROOT_DIR, "merge/")

CSV.filenames = list.files(DATA_DIR, pattern="*.csv")
CSV.filenames.w = CSV.filenames[grep("week", CSV.filenames)]
CSV.filenames.m = CSV.filenames[grep("month", CSV.filenames)]

dfs.w.all = lapply(CSV.filenames.w, function(filename) {
  df = read.csv(paste0(DATA_DIR, filename), stringsAsFactors = FALSE, row.names = NULL)
  colnames(df) <- c(colnames(df)[-1],"x")
  df$x <- NULL
  return(df)
  })
dfs.m.all = lapply(CSV.filenames.m, function(filename) read.csv(paste0(DATA_DIR, filename), stringsAsFactors = FALSE))

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