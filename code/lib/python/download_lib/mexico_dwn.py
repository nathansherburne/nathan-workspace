import os
import urllib2
from datetime import datetime

# initialize some constants
now = datetime.now()
CURRENT_YEAR = str(now.year)
CURRENT_MONTH = str(now.month)
CURRENT_DAY = str(now.day)

def getMexicoData(out_dir, update_only=False):
    end_year = int(CURRENT_YEAR)
    if update_only:
        start_year = end_year - 2  # Mexico is always at least a year or two behind.
    else:
        start_year = 1985
    for year in range(start_year, end_year + 1):
        year_str = str(year)
        filename = year_str + ".pdf"
        write_path = out_dir + '/' + filename
        if not os.path.isfile(write_path):
            download_url = "http://www.epidemiologia.salud.gob.mx/anuario/" + year_str +  "/casos/mes/027.pdf"
            try:
                response = urllib2.urlopen(download_url)
            except urllib2.HTTPError:
                exit(0)
            file = open(write_path, 'w')
            file.write(response.read())
            print("File downloaded: " + filename)
            file.close()
