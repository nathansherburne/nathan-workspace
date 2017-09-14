# Plot All Dengue Rio Data
library(data.table)
library(ggplot2)

ROOT_DIR = "~/nathan/Dengue/"
PLOT_DIR = paste0(ROOT_DIR, "Rio/Data/Dengue_Classic/Plots/")
DataDir = paste0(ROOT_DIR, "Rio/Data/Dengue_Classic/CSVs/master/")

Rio.df = read.csv(paste0(DataDir, "master_2000-2010_weekly.csv"))
Rio.dt = data.table(Rio.df)

## Initialize some useful variables
stateRio.dtnames = unique(Rio.dt$State)
nstates = length(stateRio.dtnames)
nmonths = 12
kYearSet = unique(Rio.dt$Year)
kWeekSet = unique(Rio.dt$Week)
kPAreaSet = unique(Rio.dt$Planning_Area)
kARegSet = unique(Rio.dt$Administrative_Region)
kNeighSet = unique(Rio.dt$Neighboorhood)
kNumYears = length(kYearSet)
kNumWeeks = length(kWeekSet)
kNumPAreas = length(kPAreaSet)
kNumARegs = length(kARegSet)
kNumNeighs = length(kNeighSet)
firstRio.dtyear = kYearSet[1]

## Data sets
global.yearly.totals = Rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=Year]
pArea.yearly.totals = Rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Planning_Area, Year)]
aReg.yearly.totals = Rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Administrative_Region, Year)]
neigh.yearly.totals = Rio.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Neighboorhood, Year)]

# Peaks
neigh.yearly.peaks.all = Rio.dt[Rio.dt[ , .(peak_idx = .I[Cases==max(Cases)]), by=.(Neighboorhood, Year)]$peak_idx]
neigh.yearly.peaks.unq = Rio.dt[Rio.dt[ , .(peak_idx = .I[which.max(Cases)]), by=.(Neighboorhood, Year)]$peak_idx]
neigh.yearly.peaks.no.multi = neigh.yearly.peaks.all[!(duplicated(neigh.yearly.peaks.all[,-c("Week")]) | duplicated(neigh.yearly.peaks.all[,-c("Week")], fromLast = TRUE)), ]

##############################
# Create plots
##############################
yearRio.dtticks = kYearSet

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
pArea.tot.plot = ggplot(pArea.yearly.totals, aes(x=Year, y=Cases, color=Planning_Area)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Rio Yearly Totals - Planning Area")
ggsave("RIO_Yearly_Totals_pArea.pdf", plot = pArea.tot.plot, path = PLOT_DIR, width = 11, height = 4)


############
# Each Administrative Region Yearly Totals
############
aReg.tot.plot = ggplot(aReg.yearly.totals, aes(x=Year, y=Cases, color=Administrative_Region)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Rio Yearly Totals - Administrative Region")
ggsave("RIO_Yearly_Totals_aReg.pdf", plot = aReg.tot.plot, path = PLOT_DIR, width = 11, height = 4)

############
# Each Neighboorhood Yearly Totals
############
neigh.tot.plot = ggplot(neigh.yearly.totals, aes(x=Year, y=Cases, color=Neighboorhood)) +
  geom_line() + 
  scale_x_continuous(breaks = kYearSet, labels = kYearSet) +
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Rio Yearly Totals - Neighboorhood")
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

dypc.split = neigh.peak.week.hist.no.multi + facet_wrap( ~ Neighboorhood, ncol=6)