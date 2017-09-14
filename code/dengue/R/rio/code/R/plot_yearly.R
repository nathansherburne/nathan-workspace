# Plot Yearly Dengue Rio Data

dBoxDir = "~/Dropbox/LEPR03/nathan/"
plotOutDir = paste0(dBoxDir, "Dengue/Rio/Data/Dengue_Classic/Plots/")
DataDir = paste0(dBoxDir, "Dengue/Rio/Data/Dengue_Classic/CSVs/master/")

RioData = read.csv(paste0(DataDir, "master_2000-2010_weekly.csv"))

## Initialize some useful variables
state_names = unique(RioData$State)
nstates = length(state_names)
nmonths = 12
year = RioData$Year
unqYear = unique(year)
unqPArea = unique(RioData$Planning_Area)
unqAReg = unique(RioData$Administrative_Region)
unqNeigh = unique(RioData$Neighboorhood)
nyears = length(unqYear)
nPAreas = length(unqPArea)
nARegs = length(unqAReg)
nNeighs = length(unqNeigh)
first_year = unqYear[1]

global_yearly_totals = c()
pArea_yearly_totals = c()
aReg_yearly_totals = c()
for(i in 1:nyears) {
  year_total = sum(RioData$Cases[which(RioData$Year == unqYear[i])])
  global_yearly_totals = c(global_yearly_totals, year_total)
  
  for(j in 1:nPAreas) {
    pArea = subset(RioData, Planning_Area == unqPArea[j] & Year == unqYear[i])
    pArea_total = sum(pArea$Cases)
    pArea_yearly_totals= c(pArea_yearly_totals, pArea_total)
  }
  
  for(j in 1:nARegs) {
    aReg = subset(RioData, Administrative_Region == unqAReg[j] & Year == unqYear[i])
    aReg_total = sum(aReg$Cases)
    aReg_yearly_totals= c(aReg_yearly_totals, aReg_total)
  }
}
pArea_yearly_totals = matrix(pArea_yearly_totals, ncol=nyears, nrow=nPAreas)
aReg_yearly_totals = matrix(aReg_yearly_totals, ncol=nyears, nrow=nARegs)
##############################
# Create plots
##############################
year_ticks = unqYear

############
# Global Yearly Totals
############
plotname = "Rio-Global-Yearly-Totals"
pdf(file = paste0(plotOutDir, plotname, ".pdf"), width = 15, height = 6)
par(cex.axis=0.9)
plot(global_yearly_totals,type="l",col="red",xlab="year",ylab="cases",xaxt='n')
axis(side = 1, at = 1:nyears, lab=year_ticks)
legend("topleft",legend="Global", col="red", lty=1, lwd=2, cex=0.6)
title(plotname) 
dev.off()

############
# Each Planning Area Yearly Totals
############
color = rainbow(nPAreas)
plotname = "Planning-Area-Yearly-Totals"
pdf(file = paste0(plotOutDir, plotname, ".pdf"), width = 15, height = 6)
plot(0,type="l",col="black",xlab="year",ylab="cases",xaxt='n', ylim=range(pArea_yearly_totals), xlim=c(0,nyears))
for(i in 1:nPAreas) {
  lines(pArea_yearly_totals[i,], col=color[i])
}
axis(side = 1, at = 1:nyears, lab=year_ticks)
legend("topleft",legend=unqPArea, col=color, lty="solid", lwd=2, cex=0.6, ncol=2)
title(plotname)
dev.off()

############
# Each Administrative Region Yearly Totals
############
color = rainbow(nARegs)
plotname = "Administrative-Region-Yearly-Totals"
pdf(file = paste0(plotOutDir, plotname, ".pdf"), width = 15, height = 6)
plot(0,type="l",col="black",xlab="year",ylab="cases",xaxt='n', ylim=range(aReg_yearly_totals), xlim=c(0,nyears))
for(i in 1:nARegs) {
  lines(aReg_yearly_totals[i,], col=color[i])
}
axis(side = 1, at = 1:nyears, lab=year_ticks)
legend("topleft",legend=unqAReg, col=color, lty="solid", lwd=2, cex=0.6)
title(plotname)
dev.off()


###########
# Incidence per 100,000
###########
