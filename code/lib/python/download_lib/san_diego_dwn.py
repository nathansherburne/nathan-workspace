import os
import urllib2
from datetime import datetime

now = datetime.now()
CURRENT_YEAR = str(now.year)
CURRENT_MONTH = str(now.month)
CURRENT_DAY = str(now.day)

def getSanDiegoData(out_dir, update_only=True):
    # Update is always true because this URL is the address to the most recent PDF.
    updateUrl = "http://www.sandiegocounty.gov/content/dam/sdc/hhsa/programs/phs/documents/InfluenzaWatch.pdf"
    filename = os.path.basename(updateUrl)
    name, ext = filename.split('.')
    name = name + '_' + CURRENT_YEAR + '-' + CURRENT_MONTH + '-' + CURRENT_DAY
    filename = name + '.' + ext
    writePath = os.path.join(out_dir, filename)
    if os.path.isfile(writePath):
        print "No Update:"
        print "--> already downloaded: " + filename
        return
    try:
        response = urllib2.urlopen(updateUrl)
    except urllib2.HTTPError:
        exit(0)
    file = open(writePath, 'w')
    file.write(response.read())
    print("File downloaded: " + filename)
    file.close()
