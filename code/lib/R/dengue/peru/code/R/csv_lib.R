formatPerucsv <- function(dt) {
  COL_NAMES = c("Department", "Province", "District", "Confirmed.PreW", "Probable.PreW", "Discarded.PreW", 
                "Confirmed", "Probable", "Discarded", "Grand.Total", "Total.Prob.Conf", "Accumulated.Incidence", "Population.at.Risk")
  names(dt) = COL_NAMES
  dt = dt[!(Department %in% c("Total Departamento", "Total General"))]
  
  prev.weeks.cum = dt[, c("Department", "Province", "District", "Confirmed.PreW", "Probable.PreW", "Discarded.PreW", "Population.at.Risk"), with = FALSE]
  cur.week = dt[, c("Department", "Province", "District", "Confirmed", "Probable", "Discarded", "Population.at.Risk"), with = FALSE]
  return(cur.week)
}