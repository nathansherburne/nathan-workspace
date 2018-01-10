import os
import urllib2
import re
from datetime import datetime

now = datetime.now()
CURRENT_YEAR = str(now.year)
CURRENT_MONTH = str(now.month)
CURRENT_DAY = str(now.day)

def getRioData(out_dir, update_only=False):
    url = "http://www.rio.rj.gov.br/web/sms/exibeconteudo?id=2815389"
    #connect to a URL
    website = urllib2.urlopen(url)

    #read html code
    html = website.read()

    #use re.findall to get all the links
    all_links = re.findall('"(https?://.*?)"', html)
    links = []
    # Get only the relevant links
    for link in all_links:
        if link.endswith('.pdf') or link.endswith('.htm'):
            links.append(link)

    # Only download current year for updates
    if update_only:
        current_year_links = []
        previous_year_links = []  # If updating the previous year just after the New Year
        previous_year = str(int(CURRENT_YEAR) - 1)
        for link in links:
            if CURRENT_YEAR in link:
                current_year_links.append(link)
            elif previous_year in link:
                previous_year_links.append(link)
        if len(current_year_links) == 0:
            # Print errror message to log file
            print "No matching data for current year: " + CURRENT_YEAR
            print "Perhaps it is around the New Year right now and Rio has no data for " + CURRENT_YEAR
            print "Trying previous year..."
            if len(previous_year_links) == 0:
                print "Warning: Could not find PDF for previous year either."
            else:
                links = previous_year_links
        else:
            links = current_year_links

    # Download PDF and HTM files
    for link in links:
        filename = os.path.basename(link)
        name, ext = os.path.splitext(filename)
        name = name.replace('.','')
        year = re.findall('\d+', name)[0]
        if 'mes' in name.lower():
            new_filename = year + '_monthly' + ext
        else:
            new_filename = year + '_weekly' + ext
        write_path = os.path.join(out_dir, new_filename)
        response = urllib2.urlopen(link)
        file = open(write_path, 'w')
        file.write(response.read())
        print("File downloaded: " + filename)
        file.close()

