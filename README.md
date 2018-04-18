# Web Scraping

### Directory descriptions:
#### data:
- contains PDF, HTML, CSV files for each data set.
- data sets are generally organized into 4 directories:
  1. download
    - where initially downloaded files go (often PDF or HTML).
  2. convert
    - the initial downloaded files are usually not in CSV format, so if they need to be converted, the post-conversion CSV files are placed here.
  3. format
    - often the post-conversion CSV files are not perfect. There might be some spelling adjustments, row deletions, re-structuring, etc. that needs to be done. After the CSVs are formatted, they should be placed here.
  4. merge
    - After downloading, converting, and then formatting individual files, it is nice to have all of the data in one place. This stage contains the 'merged' CSV file, often called the 'master' file which contains the exact same data that is in the format directory, just all in one big CSV file.
  - These stages are reflected in each data set's update script.

#### scraping_scripts:
- contains all of the source code for the project.
- As of now it is a Git submodule of nathan-workspace, so it has it's own commits/pushes/etc...

#### docs:
- some PDFs

#### tmp:
- an important directory that the project's code uses to store temporary files in. 
- see scraping_scripts/lib/python/helpers/os_helper.Tempfile class
