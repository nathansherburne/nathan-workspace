## Taiwan Plots ##
library(lubridate)
library(data.table)
library(ggplot2)
library(gridExtra)
library(RColorBrewer)
library(plotly)
library(fields)
library(gridExtra)


ROOT_DIR = "~/nathan/Dengue/"
TWN_DIR = paste0(ROOT_DIR, "Taiwan/")
DataDir = paste0(TWN_DIR, "Data/")
kPlotDir = paste0(DataDir, "Plots/")
CSVOutDir = paste0(DataDir, "CSVs/masterCSVs/")
CSV.DIR = paste0(DataDir, "CSVs/download_dump/")
LIB_DIR = paste0(ROOT_DIR, "denguelib/R/")
source(paste0(LIB_DIR, "my_lib.R"))

Taiwan.df = read.csv(paste0(CSVOutDir, "master_CDC_weeks.csv"), stringsAsFactors=FALSE)
Taiwan.dt = data.table(Taiwan.df)

# 'sum(Taiwan.dt[District=="Other"]$Cases)' equals '19'. So they are negligible.
Taiwan.dt = Taiwan.dt[District != "Other"]
Taiwan.dt = removeInactiveSubdivisions(Taiwan.dt, quote(District), quote(Cases), yearly = TRUE)

### Constants ###
kNumMonths = 12
kYearSet = unique(Taiwan.dt$Year)
kTotalDays = (length(kYearSet) * 365) + length(which(leap_year(kYearSet)))
kRegSet = unique(Taiwan.dt$Region)
kCitySet = unique(Taiwan.dt$City)
kDistSet = unique(Taiwan.dt[ , .(City, District)])$District  # Some districts from different cities have the same name
kNumYears = length(kYearSet)
kNumReg = length(kRegSet)
kNumCity = length(kCitySet)
kNumDist = length(kDistSet)

## Data sets
national.yearly.totals = Taiwan.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=Year]
regional.yearly.totals = Taiwan.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Region, Year)]
city.yearly.totals = Taiwan.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(City, Year)]
district.yearly.totals = Taiwan.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(District, Year, lat, lon)]

# Peak Weeks
district.yearly.peaks.all = Taiwan.dt[Taiwan.dt[ , .(peak_idx = .I[Cases==max(Cases)]), by=.(District, Year, lat, lon)]$peak_idx]
district.yearly.peaks.unq = Taiwan.dt[Taiwan.dt[ , .(peak_idx = .I[which.max(Cases)]), by=.(District, Year, lat, lon)]$peak_idx]
# For plotting peak week frequency, remove years with multiple peaks (i.e. weeks that 'tied' for max cases)
district.yearly.peaks.no.multi = district.yearly.peaks.all[!(duplicated(district.yearly.peaks.all[,-c("Week")]) | duplicated(district.yearly.peaks.all[,-c("Week")], fromLast = TRUE)), ]
# Frequency of peak weeks
dist.freq.peak.week = district.yearly.peaks.all[ , .(Mode_Peak_Week = Mode(Week), lon=lon, lat=lat, Cases=Cases, City=City), by=District]
dist.freq.peak.week = unique(dist.freq.peak.week[, .(Total_Cases = sum(Cases), Mode_Peak_Week=Mode_Peak_Week, lat=lat, lon=lon, City=City), by=District])

# log(incidence)
dyt.lat = district.yearly.totals[order(lat)]
dist.lats.ord = unique(dyt.lat[ , .(District, lat)])
dyt.lat[ , log_incidence := detrend(Cases), by=District]
dyt.mat.log = matrix(dyt.lat$log_incidence, nrow=kNumYears, ncol=kNumDist)


############
# Plotting
############

## Plotting variables
kRegColors = rainbow(kNumReg)
rf <- colorRampPalette(rev(brewer.pal(11, 'Spectral')))  # make colors
heatmap.colors <- rf(64)

### Plot National and Regional ###
nat.tot.plot = ggplot(national.yearly.totals, aes(x=Year, y=Cases, lty="National")) + 
  geom_line() + 
  theme(legend.title=element_blank()) +
  theme(legend.position="bottom") +
  labs(title="Taiwan National Yearly Totals")
reg.tot.plot = ggplot(regional.yearly.totals, aes(x=Year, y=Cases, color=Region)) + 
  geom_line() + 
  theme(legend.title=element_blank()) +
  theme(legend.position="bottom") +
  labs(title="Taiwan Regional Yearly Totals")
city.tot.plot = ggplot(city.yearly.totals, aes(x=Year, y=Cases, color=City)) + 
  geom_line() + 
  theme(legend.title=element_blank()) +
  theme(legend.position="bottom") +
  labs(title="Taiwan City Yearly Totals")
dist.tot.plot = ggplot(district.yearly.totals, aes(x=Year, y=Cases, color=District)) + 
  geom_line() + 
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Taiwan District Yearly Totals")
all.yearly.totals = grid.arrange(nat.tot.plot, reg.tot.plot, city.tot.plot, dist.tot.plot, nrow=4)
ggsave("TWN_Yearly_Totals.png", plot = all.yearly.totals, path = kPlotDir, width = 11, height = 16)


###########
# Heat maps
###########

### Plot log(Incidence) Heat Map ###
filename = "TWN_Heatmap_Incidence"
plotname = "Taiwan Incidence"
pdf(file = paste0(kPlotDir, filename, ".pdf"), width=11, height=8)
par(mar=c(4, 5, 2, 6))
lrange = range(dyt.mat.log)

image(x=1:kNumYears, y=1:kNumDist, dyt.mat.log, zlim=lrange, 
      ylab='District Latitude', xlab = 'Year', axes=FALSE, col=heatmap.colors, main=plotname, xaxt='n')
box()
# Construct y-axis
lat.spacing = 4
lat.label = round(dist.lats.ord$lat, digits=2)[seq(1, length(dist.lats.ord$lat), lat.spacing)]
lat.axis  = seq(from=1, to=kNumDist, lat.spacing)
axis(2, at=lat.axis, label=lat.label, las=1)
#Construct x-axis
axis(1, at=1:kNumYears, label=kYearSet)
# Add Color Key
image.plot(dyt.mat.log, zlim=round(lrange), horizontal=FALSE, legend.only=TRUE, 
           legend.lab='log(Incidence)', legend.line=2, col=heatmap.colors)
dev.off()

### Plot log(Rate) Heat Map ###
# Need rate data (Tycho has city rates, but not districts)
# Population data on a district level? - haven't found yet.

############
# Peak Weeks
############

# Plot Peak Values
dist.peak.val.plot = ggplot(district.yearly.peaks.unq, aes(x=Year, y=Cases, color=District)) +
  geom_line() + 
  theme(legend.title=element_blank()) +
  theme(legend.position="none") +
  labs(title="Taiwan District Yearly Peaks")

# Plot Peak Weeks (bad because multi-peak week years mask/clutter up the graph)
dist.peak.week.hist.bad = ggplot(district.yearly.peaks.all, aes(Week)) +
  geom_histogram() +
  labs(title="Peak Weeks (include all ties)")

# Plot peak weeks without years that have multiple peak weeks
dist.peak.week.hist.no.multi = ggplot(district.yearly.peaks.no.multi, aes(Week)) +
  geom_histogram() +
  labs(title="Peak Weeks (ties removed)")

# Plot peak weeks above the 80th percentile for # of Cases
percentile.80.val = quantile(district.yearly.peaks.no.multi$Cases, seq(0,1,length=21), type=4)["80%"][[1]]
dist.tot.dens = ggplot(district.yearly.peaks.no.multi, aes(Cases)) +
  geom_histogram(breaks=seq(0,750,5), fill=c("red", rep("black", 750/5 - 1))) +
  scale_x_continuous(breaks = c(0,percentile.80.val,250,500,750), labels = c("0","5","250","500","750")) +
  theme(axis.text.x = element_text(color = c("black", "red", "black", "black", "black")),
        axis.ticks.x = element_line(color = c("black", "red", "black", "black", "black"),
                                    size = c(.5,1,.5,.5,.5))) +
  labs(title="Taiwan District Yearly Peak Incidence Frequency (80% in red)")

ggsave("TWN_Yearly_Peaks_Inc.png", plot = dist.peak.val.plot, path = kPlotDir, width = 11, height = 4)

# Plot peak weeks without years that have multiple peak weeks and only the top 20% (by # of cases)
district.yearly.peaks.clean = district.yearly.peaks.no.multi[Cases>percentile.80.val]
bins = max(district.yearly.peaks.clean$Week) - min(district.yearly.peaks.clean$Week) + 1
dist.peak.week.hist.clean = ggplot(district.yearly.peaks.clean, aes(Week)) +
  geom_histogram(bins=bins) +
  labs(title="Peak Week Distribution (top 20% + ties removed)")
#ggsave("TWN_Yearly_Peak_Week_Clean_Combined.png", plot = dist.peak.week.hist.clean, path = kPlotDir, width=11, height = 6)

dypc.split = dist.peak.week.hist.clean + facet_wrap( ~ District, ncol=6)
#ggsave("TWN_Yearly_Peak_Week_Clean_Split.png", plot = dypc.split, path = kPlotDir, width=11, height = 6)

# Peak Month Frequency
dist.freq.peak.week[Mode_Peak_Week<40, Mode_Peak_Week := Mode_Peak_Week + 52, ]

freq.week.by.lat.plot = ggplot(dist.freq.peak.week, aes(x=lat, y=Mode_Peak_Week, size=Total_Cases, color=City)) +
  geom_point() + 
  scale_y_continuous("Week", breaks = c(42,48,53,62,72,82), labels = c(42, 48, 1, 10, 20, 30)) +
  scale_x_continuous(breaks=scales::pretty_breaks(n = 10))
legend = get_legend(freq.week.by.lat.plot)
freq.week.by.lat.plot = freq.week.by.lat.plot +
  theme(legend.position="None")
freq.week.by.lon.plot = ggplot(dist.freq.peak.week[order(lon)], aes(x=lon, y=Mode_Peak_Week, size=Total_Cases, color=City)) +
  geom_point() + 
  scale_y_continuous("Week", breaks = c(42,48,53,62,72,82), labels = c(42, 48, 1, 10, 20, 30)) +
  theme(legend.position="None") +
  scale_x_continuous(breaks=scales::pretty_breaks(n = 10))

freq.weeks = grid.arrange(freq.week.by.lat.plot, freq.week.by.lon.plot,
                              nrow = 2, top = "Most Frequent Peak Weeks")
freq.weeks = grid.arrange(freq.weeks, legend, ncol=2, widths=c(9,1))
peak.graphs = grid.arrange(dist.peak.week.hist.clean, freq.weeks, nrow = 2, heights=c(6,10))

ggsave("TWN_Peak_Weeks.png", plot = peak.graphs, path = kPlotDir, width = 11, height = 10)


##################################
# Two Cities: Kaohsiung and Tainan
##################################

two.cities.dt = Taiwan.dt[City %in% c("Tainan","Kaohsiung")]
two.city.yearly.totals = two.cities.dt[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(City, Year)]
ggplot(two.city.yearly.totals, aes(x=Year, y=Cases, color=City)) + geom_line()
kTwoNumDist = length(unique(two.cities.dt$District))
kNumWeeks = 52 * (kNumYears - 1)
two.lat = two.cities.dt[order(lat)]
two.lat = two.lat[Week != 53]
two.lat = two.lat[Year != 2017]
two.lat[ , log_incidence := detrend(Cases), by=District]
two.mat.log = matrix(two.lat$log_incidence, nrow=kNumWeeks, ncol=kTwoNumDist)
two.lats.ord = unique(two.lat[ , .(District, lat)])

two.district.yearly.peaks.all = two.cities.dt[two.cities.dt[ , .(peak_idx = .I[Cases==max(Cases)]), by=.(District, Year, lat, lon)]$peak_idx]
two.district.yearly.peaks.no.multi = two.district.yearly.peaks.all[!(duplicated(two.district.yearly.peaks.all[,-c("Week")]) | 
                                                                       duplicated(two.district.yearly.peaks.all[,-c("Week")], fromLast = TRUE)), ]
# Frequency of peak weeks
two.freq.peak.week = two.district.yearly.peaks.all[ , .(Mode_Peak_Week = Mode(Week), lon=lon, lat=lat, Cases=Cases, City=City), by=District]
two.freq.peak.week = unique(two.freq.peak.week[, .(Total_Cases = sum(Cases), Mode_Peak_Week=Mode_Peak_Week, lat=lat, lon=lon, City=City), by=District])

###########
# Plot Heatmap
###########
filename = "TWN_Two_City_Heatmap_Incidence"
plotname = "Tainan and Kaohsiung log(Incidence)"
pdf(file = paste0(kPlotDir, filename, ".pdf"), width=11, height=8)
par(mar=c(4, 5, 2, 6))
lrange = range(two.mat.log)

image(x=1:kNumWeeks, y=1:kTwoNumDist, two.mat.log, zlim=lrange, 
      ylab='District Latitude', xlab = 'Year', axes=FALSE, col=heatmap.colors, main=plotname, xaxt='n')
box()
# Construct y-axis
lat.spacing = 4
lat.label = round(two.lats.ord$lat, digits=2)[seq(1, length(two.lats.ord$lat), lat.spacing)]
lat.axis  = seq(from=1, to=kTwoNumDist, lat.spacing)
axis(2, at=lat.axis, label=lat.label, las=1)
#Construct x-axis
all.weeks.lab = rep(unique(two.lat$Year), each=52)
axis(1, at=1:kNumWeeks, label=all.weeks.lab)
# Add Color Key
image.plot(two.mat.log, zlim=round(lrange), horizontal=FALSE, legend.only=TRUE, 
           legend.lab='log(Incidence)', legend.line=2, col=heatmap.colors)
dev.off()

##########
bins = max(two.district.yearly.peaks.no.multi$Week) - min(two.district.yearly.peaks.no.multi$Week) + 1
two.peak.week.hist = ggplot(two.district.yearly.peaks.no.multi, aes(Week)) +
  geom_histogram(bins=bins) +
  labs(title="Peak Week Distribution")


###########
# For visual purpose, make the graph group week 52 and 1 together by wrapping lower weeks above upper weeks.
# Basically, this makes the y-range 30:81 instead of 1:52.
two.freq.peak.week[Mode_Peak_Week<40, Mode_Peak_Week := Mode_Peak_Week + 52, ]
y.range = range(two.freq.peak.week$Mode_Peak_Week)

two.freq.week.by.lat.plot = ggplot(two.freq.peak.week, aes(x=lat, y=Mode_Peak_Week, size=Total_Cases, color=City)) +
  geom_point() + 
  labs(y = "Week") +
  scale_y_continuous(breaks = c(42,48,53,62,72,82), labels = c(42, 48, 1, 10, 20, 30)) +
  scale_x_continuous(breaks=scales::pretty_breaks(n = 10))
legend = get_legend(two.freq.week.by.lat.plot)
two.freq.week.by.lat.plot = two.freq.week.by.lat.plot +
  theme(legend.position="None")
two.freq.week.by.lon.plot = ggplot(two.freq.peak.week[order(lon)], aes(x=lon, y=Mode_Peak_Week, size=Total_Cases, color=City)) +
  geom_point() + 
  scale_y_continuous("Week", breaks = c(42,48,53,62,72,82), labels = c(42, 48, 1, 10, 20, 30)) +
  theme(legend.position="None") +
  scale_x_continuous(breaks=scales::pretty_breaks(n = 10))

two.freq.weeks = grid.arrange(two.freq.week.by.lat.plot, two.freq.week.by.lon.plot,
                              nrow = 2, top = "Most Frequent Peak Weeks")
two.freq.weeks = grid.arrange(two.freq.weeks, legend, ncol=2, widths=c(9,1))
two.peak.graphs = grid.arrange(two.peak.week.hist, two.freq.weeks, nrow = 2, heights=c(6,10))

ggsave("TWN_Two_Peak_Weeks.png", plot = two.peak.graphs, path = kPlotDir, width = 11, height = 10)

