import os
import urllib2
import re
from bs4 import BeautifulSoup
from datetime import datetime, date, timedelta

now = datetime.now()
CURRENT_YEAR = str(now.year)
CURRENT_MONTH = str(now.month)
CURRENT_DAY = str(now.day)

def getPAHOData(out_dir, update_only=False):
    url = "http://www.paho.org/hq/index.php?option=com_topics&view=rdmore&cid=6290&Itemid=40734"
    print("Downloading PDFs from " + url)
    request = urllib2.Request(url)
    request.add_header('User-agent', 'Mozilla/5.0 (Linux i686)')
    html_page = urllib2.urlopen(request)
    soup = BeautifulSoup(html_page, "lxml")

    a_tags = soup.findAll('a', href=True)
    pdf_a_tags = []
    # Get only the relevant hrefs
    for a_tag in a_tags:
        if a_tag['href'].startswith("/hq/index.php?option=com_docman&task=doc_download&Itemid=270&gid=") and a_tag.getText != "":
            pdf_a_tags.append(a_tag)

    max_year = 0
    max_week = 0
    for filename in os.listdir(out_dir):
        if filename.endswith(".pdf"):
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

    # Download PDF files
    prefix = "http://www.paho.org"
    for a_tag in pdf_a_tags:
        full_link = prefix + a_tag['href']
        title = a_tag.getText()  # The title contains the week and year numbers.
        numbers = map(int, re.findall(r'\d+', title))
        if(len(numbers) == 0): continue
        year = numbers[0]  # Year is always the first number.
        week = numbers[len(numbers) - 1]  # If EW is present, it is the last number.
        if week == year: # (i.e., year is the only number present (week number not specified).
            # This means that the year is over.
            week = "FINAL"  # Not sure which year ends on EW 52 or 53.

        if update_only:
            if not (year == last_file_year and week > last_file_week) or year > last_file_year:
                # The date of this file is not after the date of our most recent report.
                # Not a new file. Don't download it.
                continue
        filename = str(year) + '_EW_' + str(week) + '.pdf'
        write_path = os.path.join(out_dir, filename)
        response = urllib2.urlopen(full_link)
        file = open(write_path, 'w')
        file.write(response.read())
        print("File downloaded: " + filename)
        file.close()
