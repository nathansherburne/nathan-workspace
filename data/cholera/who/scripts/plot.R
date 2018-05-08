library(ggplot2)
library(data.table)

dt = as.data.table(read.csv("format/epi_monitor_2017_10_52.csv"))
dt$id = seq(1, nrow(dt))

every.xth.label = 1
select.indexes = seq(1, nrow(dt), every.xth.label)
select.x.labels = dt[select.indexes, epi_week]
x.axis.labels = rep("", nrow(dt))
x.axis.labels[select.indexes] = select.x.labels

ggplot(dt, aes(x = id, y = total_cases, width = 0.9)) +
  geom_bar(stat='identity') +
  xlab("\nEpi Week") +
  ylab("Total Cases\n") +
  scale_x_continuous(breaks = seq(1, nrow(dt)), labels = x.axis.labels)

ggsave("plot/epi_monitor_2017_10_52.png", plot = last_plot(), width=14, height=8)
