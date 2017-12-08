#! /usr/bin/env Rscript
# Format the info from the Wikipedia table https://en.wikipedia.org/wiki/List_of_Administrative_Regions_in_Rio_de_Janeiro
# that was converted into CSV.
# Basically, just split Neighborhoods (which are in comma separated lists, into individual rows)

df = read.csv("~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/convert/wiki_table_name_info.csv", stringsAsFactors = FALSE)

neigh.col = grep("neigh", colnames(df), ignore.case = TRUE)
admin.dfs = list()
for(i in seq(1, nrow(df))) {
  row = df[i, ]
  neighborhoods = unlist(lapply(strsplit(as.character(row[neigh.col]), ','), trimws))  # Separate by commas and trim whitespace
  neigh.rows = list()
  for(j in seq(1, length(neighborhoods))) {
    neigh.row = row
    neigh.row[neigh.col] = neighborhoods[j]
    neigh.rows[[j]] = neigh.row
  }
  neigh.rows = do.call(rbind, neigh.rows)
  admin.dfs[[i]] = neigh.rows
}

final.df = do.call(rbind, admin.dfs)
write.csv(final.df, "~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/format/wiki_table_name_info.csv", row.names = FALSE)
write.csv(final.df, "~/Dropbox/LEPR03/nathan-workspace/data/dengue/rio/merge/wiki_table_name_info.csv", row.names = FALSE)
