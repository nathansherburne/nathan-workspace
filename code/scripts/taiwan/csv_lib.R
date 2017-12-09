library(Hmisc)
library(data.table)

getDataFrameFromTaiwanCSV <- function(filename, path) {
  # Creates a dataframe out of a District/Township-level CSV file from Taiwan's CDC website.
  #
  # Args:
  #   filename: a string the is formatted in such a way that this function can parse
  #             through it and retrieve, Region, City, and District names.
  #   path: a string that is the directory containing the file specified by filename.
  #
  # Returns:
  #   A dataframe with Year, Region, City(or County), District(or Township), Week, and
  #   Cases columns.
  #
  lis = read.csv(paste0(path, filename), stringsAsFactors = FALSE)
  # First row is getting the column names... remove
  lis = lis[-1,]
  kNumRows = nrow(lis)
  # Dataframe to return
  df = data.frame(Year=rep(0, kNumRows), Region=rep(0, kNumRows), City=rep(0, kNumRows), District=rep(0, kNumRows), Week=rep(0, kNumRows), Cases=rep(0, kNumRows))
  # Separate year and week into columns
  year.and.week = str_split_fixed(gsub("(^\\d{4})", "\\1~", lis[[1]]), "~", 2)
  cases = lis[[2]]
  # Parse filename for region, city/county, and district/township
  kDistInd = 2
  kCityInd = 3
  kRegInd = 4
  filename_spl = unlist(strsplit(filename, ","))
  dist.city.reg = filename_spl[c(kDistInd, kCityInd, kRegInd)]
  
  dist.city.reg = as.vector(sapply(dist.city.reg, function(messy.string) { 
    words = unlist(strsplit(messy.string, "-"))
    words = words[!words==""]  # Remove blank words
    name = paste(sapply(words, capitalize), collapse=' ')
    name = gsub("Dist.", "District", name)
    return(name)
  }))
  
  # Add them each as columns in the dataframe
  df$Year = as.numeric(year.and.week[,1])
  df$Region = rep(dist.city.reg[3], kNumRows)
  df$City = rep(dist.city.reg[2], kNumRows)
  df$District = rep(dist.city.reg[1], kNumRows)
  df$Week = as.numeric(year.and.week[,2])
  df$Cases = cases
  
  return(df)
}

removeLastWord <- function(str) {
  words = strsplit(str, split=' ')[[1]]
  words = words[-length(words)]
  return(paste(words, collapse=' '))
}

getDistrictCoordinates <- function(pair) {
  district = pair[2]
  city = pair[1]
  coords = geocode(paste(district, city, collapse=' '))
  print(coords)
  df = as.data.frame(coords)
  df$City = city
  df$District = district
  return(df)
}

getThisCityDistrictCoordinates <- function(district.names, city.name) {
  all.coords.wide = sapply(district.names, city.name=city.name, getDistrictCoordinates)
  all.coords.long = t(all.coords.wide)
  all.coords.long.df = as.data.frame(all.coords.long)
  setDT(all.coords.long.df, keep.rownames = TRUE)[] # Make rownames into first column
  colnames(all.coords.long.df)[1] <- "District"
  all.coords.long.df$City = rep(city.name, nrow(all.coords.long.df))
  all.coords.long.df = all.coords.long.df[ ,c('City', 'District', 'lon', 'lat')]
  return(all.coords.long.df)
}

getAllTWCoords <- function(city.dist.pairs, coord.file.path="") {
  # Retrieves the coordinates for every district/township in Taiwan from
  # either Google Maps (ggmap) or a previously created coordinate file.
  #
  # Args:
  #   city.dist.pairs: a dataframe with two columns, 'City' and 'District',
  #                    containing all city/district pairs in Taiwan.
  #   coord.file.path (optional): a string that specifies a coordinate file 
  #                    to use instead of querying Google.
  #
  # Returns: 
  #   A long dataframe containing City, District, lon, and lat.
  #
  # Side-Effects: 
  #   A CSV file is created so that future calls to this function do not 
  #   need to query Google.
  if(file.exists(coord.file.path)) {
    df.ALL = read.csv(coord.file.path, stringsAsFactors = FALSE)
  } else {
    coords.dfs = apply(city.dist.pairs, 1, getDistrictCoordinates)  ## Max 2500 requests per day. 
    df.ALL = do.call(rbind, coords.dfs)
    df.ALL[which(df.ALL$District=='Other'), c('lat', 'lon')] = c(NA,NA)
    df.ALL = df.ALL[ , c('City', 'District', 'lon', 'lat')]
    write.csv(df.ALL, coord.file.path, row.names=FALSE)
  }
  return(df.ALL)
}

specifyGenericDists <- function(dt) {
  # Augments district names if they share their name with the district of another city.
  # City names are used to augment.
  # 
  # Args:
  #   dt: The data table to augment
  #
  # Return:
  #   dt: The augmented data table.
  ucd = unique(dt[ , c("City", "District")])
  dup.names = unique(ucd$District[duplicated(ucd$District)])
  dt[District %in% dup.names, District := paste(District, City, sep = ", "), by = .(City, District)]
  return(dt)
}
