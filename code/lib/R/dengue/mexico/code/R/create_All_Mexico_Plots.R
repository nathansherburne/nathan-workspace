rm(list = ls())

library("WaveletComp")
library("Hmisc")
library("lubridate")
library("zoo")
library("RColorBrewer")
library("sp")
library("raster")
library("dplyr")
library("GADMTools")
library("rgeos")
library("fields")
library(gridExtra)
library(cowplot)
library(rsoi)
library(ggplot2)
library(data.table)

## Define directory locations
kHomeDir = "~/nathan/"
kPlotDir = paste0(kHomeDir, "Dengue/Mexico/Data/Dengue_Classic/Plots/")
kDataDir = paste0(kHomeDir, "Dengue/Mexico/Data/Dengue_Classic/CSVs/masterCSV/")
kRDir = paste0(kHomeDir, "Dengue/Mexico/Code/R/")

source(paste0(kRDir, "Plot_Months_Hist.R"))

## Get Latitudinal and Longitudinal data
MEX = gadm.loadCountries("MEX", level = 1, basefile = "./")
lonlat = coordinates(MEX$spdf)
colnames(lonlat) = c('lon', 'lat')
rownames(lonlat) = MEX$spdf$NAME_1
lonlat.dt = data.table(State=rownames(lonlat), lon=lonlat[,'lon'], lat=lonlat[,'lat'])

## Read the data 
kMexData = read.csv(paste0(kDataDir, "master.csv"), stringsAsFactors=FALSE)

## Initialize some useful variables
kNumMonths = 12
kYearSet = unique(kMexData$Year)
kNumYears = length(kYearSet)
kSelectStates = c('Campeche', 'Colima', 'Chiapas', 'Guerrero', 'Jalisco', 'Michoacán', 'Morelos', 'Nayarit', 'Nuevo León', 
                  'Oaxaca', 'Puebla', 'Quintana Roo', 'Sinaloa', 'Tabasco', 'Tamaulipas', 'Veracruz', 'Yucatán')
kNumSelectStates = length(kSelectStates)
kStateNamesAlpOrd = unique(kMexData$State)
kNumStates = length(kStateNamesAlpOrd)

state.dt = as.data.table(kMexData)
state.dt[ , Month := month.abb[match(Month, month.name)]]  # Convert full month names into abbreviations.
state.dt$Month = factor(state.dt$Month, levels=month.abb)

## Organize dataframe based on latitude
state.dt = merge(state.dt, lonlat.dt, by='State')

# Reorder state.dt by latitude
state.dt.select = state.dt[State %in% kSelectStates]
state.dt.select = state.dt.select[order(lat)]
lonlat.select = unique(state.dt.select[ , .(State, lon, lat)])

## Make matrix for Incidence heatmap. Use log values instead of raw incidence number (detrending)
log.inc.dt = state.dt.select[ , .(log_incidence = detrend(Cases), Year=Year, lon=lon, lat=lat, Month=Month), by=State]
log.inc.mat = matrix(log.inc.dt$log_incidence, ncol=kNumSelectStates, nrow=kNumMonths * kNumYears)

## Make matrix for Rate heatmap. Use log values instead of raw rate number (detrending)
log.rate.dt = state.dt.select[ , .(log_rate = detrend(Incidence_Per_100000), Year=Year, lon=lon, lat=lat), by=State]
log.rate.mat = matrix(log.rate.dt$log_rate, ncol=kNumSelectStates, nrow=kNumMonths * kNumYears)

## Get peak months (Timing and Value)
state.yearly.peaks.all = state.dt.select[state.dt.select[ , .(peak_idx = .I[Cases==max(Cases)]), by=.(State, Year, lat, lon)]$peak_idx]
state.yearly.peaks.unq = state.dt.select[state.dt.select[ , .(peak_idx = .I[which.max(Cases)]), by=.(State, Year, lat, lon)]$peak_idx]
state.freq.peak.mon = state.yearly.peaks.all[ , .(Mode_Peak_Month = Mode(Month), lon=lon, lat=lat, Cases=Cases), by=State]
state.freq.peak.mon = unique(state.freq.peak.mon[, .(Total_Cases = sum(Cases), Mode_Peak_Month=Mode_Peak_Month, lat=lat, lon=lon), by=State])

## Get National Yearly Totals
national.yearly.totals = state.dt.select[ , .(Cases = sum(Cases, na.rm = TRUE)), by=Year]

## Get State Yearly Totals
state.yearly.totals = state.dt.select[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Year, State)]

## Get State Yearly Rates
state.yearly.rates = state.dt.select[ , .(Incidence_Per_100000 = unique(Incidence_Per_100000)), by=.(Year, State)]

## Get National Monthly Totals
national.monthly.totals = state.dt.select[ , .(Cases = sum(Cases, na.rm = TRUE)), by=.(Year, Month)]

## Get ENSO --> ONI data
ONI.dt = as.data.table(download_enso(climate_idx = "oni", create_csv = FALSE))
# "El Nino" and "La Nina" are labelled incorrectly (i.e. "Cool phase/El Nino", "Warm Phase/La Nina").
# Rename them.
ONI.dt[phase %like% "Cool|Warm", phase := ifelse(phase %like% "Cool", "Cool Phase/La Nina", "Warm Phase/El Nino")]
# Remove years not in Mexico data set
ONI.dt = ONI.dt[Year %in% kYearSet]
ONI.dt = ONI.dt[!is.na(ONI)]


#########
#Plotting
#########

rf <- colorRampPalette(rev(brewer.pal(11, 'Spectral')))  # make colors
heatmap.colors <- rf(64)
kSelectStateColors = rainbow(kNumSelectStates)
options(scipen=5)  # The scipen option determines how likely R is to switch to scientific notation. The higher the value the less likely it is to switch.

nat.tot.plot = ggplot(national.yearly.totals, aes(x=Year, y=Cases, lty="National")) + 
  geom_line() + 
  scale_x_discrete(limits=kYearSet) + xlab("") +
  scale_y_continuous(breaks=scales::pretty_breaks(n = 5)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  theme(legend.title=element_blank(), 
        legend.justification=c(0,1), 
        legend.position=c(0,1)) +
  labs(title="Mexico National Yearly Totals")
state.tot.plot = ggplot(state.yearly.totals, aes(x=Year, y=Cases, color=State)) + 
  geom_line() + 
  scale_x_discrete(limits=kYearSet) + xlab("") +
  scale_y_continuous(breaks = scales::pretty_breaks(n = 5)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  guides(col = guide_legend(nrow = 8)) + 
  theme(legend.title=element_blank(), 
        legend.justification=c(0,1), 
        legend.position=c(0,1),
        legend.text=element_text(size=8),
        legend.key.size = unit(0.6, 'lines')) +
  labs(title="Mexico State Yearly Totals")
state.rate.plot = ggplot(state.yearly.rates, aes(x=Year, y=Incidence_Per_100000, color=State)) + 
  geom_line() + 
  scale_x_discrete(limits=kYearSet) + xlab("") +
  scale_y_continuous(breaks = scales::pretty_breaks(n = 5)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  guides(col = guide_legend(nrow = 8)) + 
  theme(legend.title=element_blank(), 
        legend.justification=c(0,1), 
        legend.position=c(0,1), 
        legend.text=element_text(size=8),
        legend.key.size = unit(0.6, 'lines')) +
  labs(title="Mexico State Yearly Rates")

state.peak.vals.plot = ggplot(state.yearly.peaks.unq, aes(x=Year, y=Cases, color=State)) + 
  geom_line() + 
  scale_x_discrete(limits=kYearSet) + xlab("") +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  guides(col = guide_legend(nrow = 8)) + 
  theme(legend.title=element_blank(), 
        legend.justification=c(0,1), 
        legend.position=c(0,1), 
        legend.text=element_text(size=8),
        legend.key.size = unit(0.6, 'lines')) +
  labs(title="Mexico State Yearly Peaks")

nat.peak.months.hist = ggplot(state.yearly.peaks.all, aes(Month)) + 
  geom_bar() + 
  theme(legend.title=element_blank()) +
  labs(title="Mexico National Peak Months")
state.peak.months.hists = nat.peak.months.hist + facet_wrap( ~ State, ncol=4) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  labs(title="Mexico State Peak Months")

# Peak Month Frequency
freq.month.by.lat.plot = ggplot(state.freq.peak.mon, aes(x=lat, y=Mode_Peak_Month, size=Total_Cases)) +
  geom_point() + 
  labs(y = "") +
  scale_x_continuous(breaks=scales::pretty_breaks(n = 10))
legend = get_legend(freq.month.by.lat.plot)
freq.month.by.lat.plot = freq.month.by.lat.plot + 
  theme(legend.position="None")
freq.month.by.lon.plot = ggplot(state.freq.peak.mon[order(lon)], aes(x=lon, y=Mode_Peak_Month, size=Total_Cases)) +
  geom_point() + 
  labs(y = "") +
  theme(legend.position="None") +
  scale_x_continuous(breaks=scales::pretty_breaks(n = 10))
freq.months = grid.arrange(freq.month.by.lat.plot, freq.month.by.lon.plot, legend, 
                           ncol = 3, top = "Most Frequent Peak Months", widths=c(2.3, 2.3, 0.8))
peak.graphs = plot_grid(nat.peak.months.hist, state.peak.months.hists, freq.months, nrow=3, rel_heights = c(1/4, 1/2, 1/4))

ggsave("MEX_Yearly_Totals_Nat.pdf", plot = nat.tot.plot, path = kPlotDir, width = 11, height = 4)
ggsave("MEX_Yearly_Totals_State.pdf", plot = state.tot.plot, path = kPlotDir, width = 11, height = 4)
ggsave("MEX_Yearly_Rates_State.pdf", plot = state.rate.plot, path = kPlotDir, width = 11, height = 4)
ggsave("MEX_Yearly_Peak_Values_State.pdf", plot = state.peak.vals.plot, path = kPlotDir, width = 11, height = 4)
ggsave("MEX_Yearly_Peak_Months_Nat.pdf", plot = nat.peak.months.hist, path = kPlotDir, width = 11, height = 6)
ggsave("MEX_Yearly_Peak_Months_State.pdf", plot = state.peak.months.hists, path = kPlotDir)
ggsave("MEX_Peak_Graphs.pdf", plot = peak.graphs, path = kPlotDir)

############
# Heat Maps
############

### Plot Incidence heat map ###
plotname = 'Mexico Incidence'
filename = 'MEX_Heatmap_Incidence'
pdf(file = paste0(kPlotDir, filename, ".pdf"), width=10, height=8)
par(mar=c(4, 5, 2, 6))
lrange = range(log.inc.mat, na.rm=TRUE)
image(x=1:nrow(log.inc.mat), y=1:ncol(log.inc.mat), log.inc.mat, zlim=lrange, 
      ylab='State Latitude', xlab = 'Year', axes=FALSE, xaxt='n', col=heatmap.colors, main=plotname)
box()
# Construct y-axis
lat.label = round(lonlat.select$lat, digits=1)
lat.axis = seq(from=1, to=kNumSelectStates)
axis(2, at=lat.axis, label=lat.label, las=1)
# Construct x-axis
kMonthAxis = seq(1, kNumMonths*kNumYears, by=kNumMonths)
axis(1, at=kMonthAxis, label=kYearSet)
# Add Color Key
image.plot(log.inc.mat, zlim=round(lrange), horizontal=FALSE, legend.only=TRUE, 
           legend.lab='log(Incidence)*', legend.line=2, col=heatmap.colors)
dev.off()

### Plot Rate heat map ###
plotname = 'Mexico Incidence'
filename = 'MEX_Heatmap_Rate'
pdf(file = paste0(kPlotDir, filename, ".pdf"), width=10, height=8)
par(mar=c(4, 5, 2, 6))
lrange = range(log.rate.mat, na.rm=TRUE)
image(x=1:nrow(log.rate.mat), y=1:ncol(log.rate.mat), log.rate.mat, zlim=lrange, 
      ylab='State Latitude', xlab = 'Year', axes=FALSE, xaxt='n', col=heatmap.colors, main=plotname)
box()
# Construct y-axis
lat.label = round(lonlat.select$lat, digits=1)
lat.axis = seq(from=1, to=kNumSelectStates)
axis(2, at=lat.axis, label=lat.label, las=1)
# Construct x-axis
kMonthAxis = seq(1, kNumMonths*kNumYears, by=kNumMonths)
axis(1, at=kMonthAxis, label=kYearSet)
# Add Color Key
image.plot(log.rate.mat, zlim=round(lrange), horizontal=FALSE, legend.only=TRUE, 
           legend.lab='log(Rate)*', legend.line=2, col=heatmap.colors)
dev.off()

#############
# ENSO
#############

## On same graph
# Make a Date column that is in the correct range
ONI.dt$scaled.Date = year(ONI.dt$Date) + (month(ONI.dt$Date)-1)/12
# Make an ONI value column that can be plotted in the same range as national cases
ONI.dt$nat.scaled.ONI = rescale(ONI.dt$ONI, c(-max(national.yearly.totals), max(national.yearly.totals)))

y.range = c(-max(national.yearly.totals), max(national.yearly.totals))
ONI.nat.plot = ggplot() +
  geom_bar(data = national.yearly.totals, aes(x=Year, y=Cases, lty="National"), stat="identity", position = position_nudge(x = 0.5), width = 1) + 
  geom_line(data=ONI.dt, aes(x=scaled.Date, y=nat.scaled.ONI, color=phase, group=1), size=1) +
  scale_x_continuous(breaks=seq(1985,2015,by=2), expand=c(.01,0), labels=seq(1985,2015,by=2)) + xlab("") +
  scale_y_continuous(breaks=pretty.default(y.range, n=10), limits = y.range, labels = c(rep("", 6), seq(0,120000,by=20000))) +
  scale_color_manual(values=c('grey','blue', 'red')) +
  theme(legend.title=element_blank(), 
        legend.position="bottom") +
  labs(title="Yearly National Cases of Dengue in Mexico with the Oceanic Nino Index")

ggsave("MEX_ONI_Nat.pdf", plot = ONI.nat.plot, path = kPlotDir)


## On different graphs, same page
nat.tot.plot.2 = ggplot() + 
  geom_bar(data = national.yearly.totals, aes(x=Year, y=Cases, lty="National"), stat="identity", position = position_nudge(x = 0.5), width = 1) + 
  scale_x_discrete(limits=kYearSet, expand=c(.01,0)) + xlab("") +
  scale_y_continuous(breaks=pretty.default(y.range, n=10)) +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  theme(legend.title=element_blank(), 
        legend.justification=c(0,1), 
        legend.position=c(0,1)) +
  labs(title="Mexico National Yearly Totals")

ONI.plot = ggplot(ONI.dt, aes(x=Date, y=ONI, color=phase)) + 
  geom_line(aes(group=1)) +
  scale_color_manual(values=c('grey','blue', 'red')) +
  scale_x_date(breaks = seq(as.Date(min(ONI.dt$Date)), as.Date(max(ONI.dt$Date)), by="1 year"), 
               labels=date_format("%Y"), expand=c(.01,0)) + xlab("") +
  theme(axis.text.x = element_text(angle = 90, hjust = 1)) +
  theme(legend.title=element_blank(), 
        legend.position="bottom") +
  labs(title="Oceanic Nino Index")

gA <- ggplotGrob(nat.tot.plot.2)
gB <- ggplotGrob(ONI.plot)
pdf(paste0(kPlotDir, "ENSO_ONI_1985-2015.pdf"), height = 11, width = 18)
grid::grid.newpage()
grid::grid.draw(rbind(gA, gB))
dev.off()
