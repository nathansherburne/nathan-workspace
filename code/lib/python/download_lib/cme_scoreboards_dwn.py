import os
import urllib2
from datetime import datetime

now = datetime.now()
CURRENT_YEAR = str(now.year)
CURRENT_MONTH = str(now.month)
CURRENT_DAY = str(now.day)

def getCMEScoreboards(out_dir, update_only=False):
    # Get current year
    main_url = "https://kauai.ccmc.gsfc.nasa.gov/CMEscoreboard/"
    filename = CURRENT_YEAR + ".html"
    scoreboard = open(os.path.join(out_dir, filename), 'w')
    response = urllib2.urlopen(main_url)
    scoreboard.write(response.read())
    print "File downloaded: " + filename

    if update_only:
        return

    # Get previous years
    first_year = 2013
    for year in range(first_year, int(CURRENT_YEAR)):
        prev_url = "https://kauai.ccmc.gsfc.nasa.gov/CMEscoreboard/PreviousPredictions/" + str(year)
        filename = str(year) + ".html"
        scoreboard = open(os.path.join(out_dir, filename), 'w')
        response = urllib2.urlopen(prev_url)
        scoreboard.write(response.read())
        print "File downloaded: " + filename

