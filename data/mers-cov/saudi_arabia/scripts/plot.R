library(ggplot2)
library(data.table)

SB.dt = as.data.table(read.csv("~/dev/repos/nathan-workspace/data/mers-cov/saudi_arabia/convert/epi-17-november-2017.csv"))
SB.dt$id = seq(1, nrow(SB.dt))

every.xth.label = 6
select.indexes = seq(1, nrow(SB.dt), every.xth.label)
select.x.labels = SB.dt[select.indexes, week]
x.axis.labels = rep("", nrow(SB.dt))
x.axis.labels[select.indexes] = select.x.labels

ggplot(SB.dt, aes(x = id, y = cases, width = 0.5)) +
  geom_bar(stat='identity') +
  xlab("\nWeek") +
  ylab("Cases\n") +
  scale_x_continuous(breaks = seq(1, nrow(SB.dt)), labels = x.axis.labels)

ggsave("~/dev/repos/nathan-workspace/data/mers-cov/saudi_arabia/plots/epi-17-november-2017.png", plot = last_plot(), width=14, height=8)