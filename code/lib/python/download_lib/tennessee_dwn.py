import os
import urllib2
import re
from datetime import datetime
from bs4 import BeautifulSoup

now = datetime.now()
CURRENT_YEAR = str(now.year)
CURRENT_MONTH = str(now.month)
CURRENT_DAY = str(now.day)

def getTennesseeData(out_dir, update_only = False):
    # Select desired week from dropdown lists that are separated by year.
    END_YEAR = int(CURRENT_YEAR)
    MAIN_URL = "https://www.tn.gov/health/cedep/immunization-program/ip/flu-in-tennessee.html"
    BASE_URL = "https://www.tn.gov"
    
    respose = urllib2.urlopen(MAIN_URL)
    html = respose.read()
    soup = BeautifulSoup(html, "lxml")

    if not update_only:
        # Default start with earliest TN record.
        last_file_year = 2009
        last_file_week = 0

    elif update_only:
        # Find most recently (max year and max week) downloaded PDF 
        # file so that we know what week/year to start downloading.
        max_year = 0
        max_week = 0
        for filename in os.listdir(out_dir):
            if filename.endswith(".pdf"):
                # Find week and year of PDF.
                numbers = map(int, re.findall(r'\d+', filename))
                week = None
                year = None
                for number in numbers:
                    if number > 2000 and number < 2050: # 2050 is an arbitrary max year
                        year = number
                    elif number > 0 and number <= 53:
                        week = number
                if week is None or year is None:
                    # Print warning to log file
                    print "Warning: could not find week or year number in filename: " + filename
                    continue
                # Look for most recent (i.e., max) date.
                if year > max_year:  
                    max_year = year
                    max_week = week
                elif year == max_year and week > max_week:
                    max_week = week
        last_file_year = max_year    
        last_file_week = max_week

    # Download desired PDFs
    a_tags = soup.findAll('a', href = True)
    new_pdf_hrefs = []
    for a_tag in a_tags:
        href = a_tag['href']
        if href.endswith(".pdf"):
            # Find the week and year of this PDF.
            numbers = map(int, re.findall(r'\d+', href))
            week = None
            year = None
            for number in numbers:
                if number > 2000 and number < 2050:
                    year = number
                elif number > 0 and number < 54:
                    week = number
            if week is None or year is None:
                # Print warning to log file
                print "Warning: could not find week or year number in href: " + href
                continue
            if (year == last_file_year and week > last_file_week) or year > last_file_year:
                # The date of this PDF is after the most recent PDF we have on file.
                # Download new PDF.
                url = BASE_URL + href
                filename = "week" + str(week) + "ILI_spnreport_" + str(year) + ".pdf"  
                try:
                    response = urllib2.urlopen(url)
                except:
                    print("Error: Could not find: " + filename)
                    print("--> Invalid address: " + url)
                    continue
                write_path = os.path.join(out_dir, filename)
                file = open(write_path, 'w')
                file.write(response.read())
                print("File Downloaded: " + filename)
                file.close()
