Tennessee ILI Data
=======================

TN-ILI-2009-todate.csv
  - generated from scraping, which starts week 32, 2009
    - see: https://www.tn.gov/health/cedep/immunization-program/ip/flu-in-tennessee.html
  - week 1-31, 2009 are copied from TN-ILI-2009-todate.csv.orig
  - data from scraping PDFs rounded to 1 decimal place 
  - if "n/a" in "Compared to State" column, set %ILI value to NA.

TN-ILI-2009-todate.csv.orig
  - original file, starts week 1 of 2009
  - data before week 22, 2014 is rounded to 2 decimal places
  - if "n/a" in "Compared to State" column, sometimes %ILI value is 0 instead of NA (which I think is wrong).

Questions:
  - should week 1-31, 2009 be copied from orig?
    - I already did this in case anything that is using these columns can still find them.
  - should data prior to week 22, 2014 from orig that has 2 decimal places instead of 1 be copied from orig?
  - is orig incorrect in assigning %ILI to 0 instead of NA when "n/a" is in "Compared to State" column? 
