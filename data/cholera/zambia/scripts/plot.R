library(ggplot2)
library(data.table)

dt = as.data.table(read.csv("format/10_2017-05_2018.csv"))
dt$id = seq(1, nrow(dt))

every.xth.label = 1
select.indexes = seq(1, nrow(dt), every.xth.label)
select.x.labels = dt[select.indexes, date]
x.axis.labels = rep("", nrow(dt))
x.axis.labels[select.indexes] = select.x.labels

ggplot(dt, aes(x = id, y = cases, width = 0.9)) +
  geom_bar(stat='identity') +
  xlab("\nDate") +
  ylab("Cases\n") +
  scale_x_continuous(breaks = seq(1, nrow(dt)), labels = x.axis.labels)

ggsave("plot/10_2017-05_2018.png", plot = last_plot(), width=14, height=8)
