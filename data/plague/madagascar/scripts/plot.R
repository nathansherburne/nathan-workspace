library(ggplot2)
library(reshape2)

MD_Plague = read.csv("~/Dropbox/LEPR03/nathan-workspace/data/plague/madagascar/merge/MD_2017_08-01_to_11-12.csv", stringsAsFactors = FALSE)
long.format = melt(MD_Plague, id.vars = "Date")
# Sort them so they get stacked correctly in barplot
# For barplot, the first values of dataframe get stacked on top, last go on bottom.
long.format$variable = factor(long.format$variable, levels = c("Septicemic", "Not.Specified", "Bubonic", "Pneumonic"))
long.format = long.format[order(long.format$variable), ]

every.xth.label = 8
select.indexes = seq(1, length(long.format$Date), every.xth.label)
select.x.labels = long.format$Date[select.indexes]
x.axis.labels = rep("", length(long.format$Date))
x.axis.labels[select.indexes] = select.x.labels

ggplot(long.format, aes(x = Date, y = value, fill=variable)) +
  geom_bar(stat='identity') +
  xlab("\nDate") +
  ylab("Cases\n") +
  scale_x_discrete(labels = x.axis.labels) +
  scale_fill_manual(values=c("blue","gray", "black", "red"))

ggsave("~/Dropbox/LEPR03/nathan-workspace/data/plague/madagascar/plots/MD_2017_08-01_to_11-12.png", plot = last_plot())