Mode <- function(x) {
  # Returns the most common occurring item in a structure. If there's a tie, it returns the first mode.
  ux <- unique(x)
  ux[which.max(tabulate(match(x, ux)))]
}

detrend <- function(vec) {
  # Detrends a vector of numbers.
  #
  # Args:
  #   vec: a vector of numbers
  #
  # Return:
  #   vec: a vector of detrended numbers
  vec = log(vec + 1)  # Add 1 to avoid taking the log of zeros.
  vec = vec - mean(vec)
  vec = vec / sd(vec)
}

removeInactiveSubdivisions <- function(dt, subdivision, variable, yearly=FALSE) {
  # Remove subdivisions (city/district/region/etc...) that report 0 cases over 50% of the time.
  #
  # Args:
  #   dt: a data table to clean
  #   subdivision: a quote() variable specifying how to break up the data
  #   variable: a string specifying the column by which to judge by
  #   yearly: judge based on yearly total reports
  #
  # Return:
  #   dt: a data table identical to the input, but containing only 'active' subdivisions.
  ACTIVITY_THRESHOLD = 0.5
  dt.tmp = dt
  if(yearly) {
    dt.tmp = dt.tmp[ , .(var = sum(eval(variable), na.rm = TRUE)), by=.(eval(subdivision), Year)]
    names(dt.tmp)[1] = as.character(subdivision)
    variable = quote(var)
  }
  dt.tmp = dt.tmp[ , .(result=numNonZeros(eval(variable))/length(eval(variable))), by=eval(subdivision)]
  names(dt.tmp)[1] = as.character(subdivision)
  active.dt = dt.tmp[result>=ACTIVITY_THRESHOLD]
  active_subdivisions = active.dt[ , eval(subdivision)]
  return(dt[eval(subdivision) %in% active_subdivisions])
}

numNonZeros <- function(vec) {
  return(length(which(vec != 0)))
}

fuzzyStringCompare <- function(strings1, strings2) {
  # Compare two lists of strings for similarities. All pairs of strings between the two
  # lists are compared, their "distances" computed, and the minumum-distance pair for each
  # string is returned in a data frame.
  #
  # Args:
  #   strings1: a list of strings
  #   strings2: a list of strings
  #
  # Return:
  #   match.s1.s2: a data frame specifying string pairs, one from each of the input lists.
  #                 The data frame contains the index of each string in their corresponding
  #                 input lists, the strings themselves, and the "distance" between the two
  #                 strings assigned by the fuzzy comparison.
  dist.name = adist(strings1, strings2, ignore.case = TRUE)
  min.name<-apply(dist.name, 1, min)
  match.s1.s2<-NULL  
  for(j in 1:nrow(dist.name))
  {
    s2.i<-match(min.name[j],dist.name[j,])
    s1.i<-j
    match.s1.s2<-rbind(data.frame(s2.i=s2.i,s1.i=s1.i,s2name=strings2[s2.i], s1name=strings1[s1.i], adist=min.name[j]),match.s1.s2)
  }
  match.s1.s2 = match.s1.s2[with(match.s1.s2, order(s2.i)), ]
  return(match.s1.s2)
}




##################
# From 'cowplot' package
###################
#' Retrieve the legend of a plot
#'
#' This function extracts just the legend from a ggplot
#'
#' @param plot A ggplot or gtable from which to retrieve the legend
#' @return A gtable object holding just the lengend
#' @examples
#' p1 <- ggplot(mtcars, aes(mpg, disp)) + geom_line()
#' plot.mpg <- ggplot(mpg, aes(x = cty, y = hwy, colour = factor(cyl))) + geom_point(size=2.5)
#' # Note that these cannot be aligned vertically due to the legend in the plot.mpg
#' ggdraw(plot_grid(p1, plot.mpg, ncol=1, align='v'))
#'
#' legend <- get_legend(plot.mpg)
#' plot.mpg <- plot.mpg + theme(legend.position='none')
#' # Now plots are aligned vertically with the legend to the right
#' ggdraw(plot_grid(plot_grid(p1, plot.mpg, ncol=1, align='v'),
#'                  plot_grid(NULL, legend, ncol=1),
#'                  rel_widths=c(1, 0.2)))
#' @export
get_legend <- function(plot)
{
  grobs <- ggplot_to_gtable(plot)$grobs
  legendIndex <- which(sapply(grobs, function(x) x$name) == "guide-box")
  ## make sure this plot has a legend to be extracted
  ## note that multiple legends show up as one grob so this still works for multiple legend plots (i.e. color and shape)
  ## not sure if it is possible to create a plot with multiple legend grobs, but also not sure how this function should handle those situations so this seems to be the best check to provide a useful message
  if (length(legendIndex) == 1){
    legend <- grobs[[legendIndex]]
  } else {
    stop('Plot must contain a legend')
  }
}

# ****** Internal functions used by drawing code ******
ggplot_to_gtable <- function(plot)
{
  if (methods::is(plot, "ggplot")){
    # ggplotGrob must open a device and when a multiple page capable device (e.g. PDF) is open this will save a blank page
    # in order to avoid saving this blank page to the final target device a NULL device is opened and closed here to *absorb* the blank plot
    
    # commenting this out to see if it was the cause of
    grDevices::pdf(NULL)
    plot <- ggplot2::ggplotGrob(plot)
    grDevices::dev.off()
    plot
  }
  else if (methods::is(plot, "gtable")){
    plot
  }
  else{
    stop('Argument needs to be of class "ggplot" or "gtable"' )
  }
}
########
# End 'cowplot'
########