# Plot All Dengue Rio Data
library(data.table)
library(ggplot2)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
PLOT_DIR = paste0(ROOT_DIR, "data/dengue/rio/plots/")
IN_DIR = paste0(ROOT_DIR, "data/dengue/rio/merge/")

rio.df = read.csv(paste0(IN_DIR, "master_weekly.csv"))
rio.dt = data.table(rio.df)

## Initialize some useful variables
nmonths = 12
kYearSet = unique(rio.dt$Year)
kWeekSet = unique(rio.dt$Week)
kPAreaSet = unique(rio.dt$Planning.Area)
kARegSet = unique(rio.dt$Administrative.Region)
kNeighSet = unique(rio.dt$Neighborhood)
kNumYears = length(kYearSet)
kNumWeeks = length(kWeekSet)
kNumPAreas = length(kPAreaSet)
kNumARegs = length(kARegSet)
kNumNeighs = length(kNeighSet)
firstrio.dtyear = kYearSet[1]

## Data sets
global.yearly.totals = rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=Year]
pArea.yearly.totals = rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Planning.Area, Year)]
aReg.yearly.totals = rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Administrative.Region, Year)]
neigh.yearly.totals = rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Neighborhood, Year)]

# Peaks
neigh.yearly.peaks.all = rio.dt[rio.dt[ , .(peak_idx = .I[Cases==max(Cases)]), by=.(Neighborhood, Year)]$peak_idx]
neigh.yearly.peaks.unq = rio.dt[rio.dt[ , .(peak_idx = .I[which.max(Cases)]), by=.(Neighborhood, Year)]$peak_idx]
neigh.yearly.peaks.no.multi = neigh.yearly.peaks.all[!(duplicated(neigh.yearly.peaks.all[,-c("Week")]) | duplicated(neigh.yearly.peaks.all[,-c("Week")], fromLast = TRUE)), ]

##############################
# Create plots
##############################

############
# Global Yearly Totals
############
glob.tot.plot = ggplot(global.yearly.totals, aes(x=Year, y=Cases)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  labs(title="Rio Yearly Totals")
ggsave("RIO_Yearly_Totals_Global.pdf", plot = glob.tot.plot, path = PLOT_DIR, width = 11, height = 4)

############
# Each Planning Area Yearly Totals
############
pArea.tot.plot = ggplot(pArea.yearly.totals, aes(x=Year, y=Cases, color=Planning.Area)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Rio Yearly Totals - Planning Area")
ggsave("RIO_Yearly_Totals_pArea.pdf", plot = pArea.tot.plot, path = PLOT_DIR, width = 11, height = 4)


############
# Each Administrative Region Yearly Totals
############
aReg.tot.plot = ggplot(aReg.yearly.totals, aes(x=Year, y=Cases, color=Administrative.Region)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Rio Yearly Totals - Administrative Region")
ggsave("RIO_Yearly_Totals_aReg.pdf", plot = aReg.tot.plot, path = PLOT_DIR, width = 11, height = 4)

############
# Each Neighborhood Yearly Totals
############
neigh.tot.plot = ggplot(neigh.yearly.totals, aes(x=Year, y=Cases, color=Neighborhood)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Rio Yearly Totals - Neighborhood")
ggsave("RIO_Yearly_Totals_neigh.pdf", plot = neigh.tot.plot, path = PLOT_DIR, width = 11, height = 4)

###########
# Incidence per 100,000
###########

###########
# Peak Weeks (Timing and Value)
###########
# Plot Peak Weeks (bad because multi-peak week years make the graph uniform)
neigh.peak.week.hist.bad = ggplot(neigh.yearly.peaks.all, aes(Week)) +
  geom_histogram() +
  scale_x_continuous(breaks = c(10,20,30,40,50), labels = c(10,20,30,40,50)) +
  labs(title="Peak Weeks (with ties)")
ggsave("RIO_Yearly_Peak_Week_With_Ties.pdf", plot = neigh.peak.week.hist.bad, path = PLOT_DIR, width=11, height = 6)

# Plot peak weeks without years that have multiple peak weeks
neigh.peak.week.hist.no.multi = ggplot(neigh.yearly.peaks.no.multi, aes(Week)) +
  geom_histogram() +
  scale_x_continuous(breaks = c(10,20,30,40,50), labels = c(10,20,30,40,50)) +
  labs(title="Peak Weeks (ties removed)")
ggsave("RIO_Yearly_Peak_Week_Without_Ties.pdf", plot = neigh.peak.week.hist.no.multi, path = PLOT_DIR, width=11, height = 6)

dypc.split = neigh.peak.week.hist.no.multi + facet_wrap( ~ Neighborhood, ncol=6)