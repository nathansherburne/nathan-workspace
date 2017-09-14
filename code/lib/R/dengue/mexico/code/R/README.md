Dengue -- Mexico
===================

After running the python script for Dengue Classic, each year of data is in its own CSV file. This file
is located in the directory defined by the 'constants.py' file associated with the data set.

CombineCSV.R takes each CSV and combines them into one long data frame.

Notes
---------

- Some CSVs(the earlier years) have a 'rate per 100,000 habitants' column. More recent years exclude this information.
  - In the CSVs that have this column, some label it 'incidencia', while others label it 'tasa'.
  - This rate is provided per state as a *yearly* rate, not monthly. In order to fill each row, this R script makes a
    generalization, assigning the yearly rate to each month.
  - Population data is taken from the Mexico 2010 Census: http://www.beta.inegi.org.mx/proyectos/ccpv/2010/ to obtain 2009-15 rates.

Files
----------
- MEX_amd1.rds - GADM file for latitude nd longitude data.
- combine_CSVs.R - creates the master CSV with all mexico data (poorly written, but works fine)
- GetDataFrameForCSV.R - a function used by *combine_CSV* (not necessary if *combine_CSV* is written better)
- Create_All_Mexico_Plots.R -
- Plot_Months_Hist - Plotting function to avoid loops
