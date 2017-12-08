from bs4 import BeautifulSoup
import urllib2
import re
import os
import shutil
import csv
from selenium import webdriver
from selenium.common.exceptions import NoSuchElementException
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
from datetime import datetime, date, timedelta

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

# Script using beautifulsoup to download pdfs from WHO.
def getWHOData(out_dir, update_only=False):
    if update_only:
        url = ("http://www.wpro.who.int/" +
                "emerging_diseases/DengueSituationUpdates/en/")

        request = urllib2.Request(url)
        html_page = urllib2.urlopen(request)
        soup = BeautifulSoup(html_page,"lxml")

        link = soup.find('a', {"class":"link_media"} )
        link = link['href']

        dateString = getDate(link)
        filename = ""+dateString + "_WPRO_WHO.pdf"
        write_path = out_dir + '/' + filename
        baseURL = "http://www.wpro.who.int"

        if not os.path.isfile(write_path):
            download_url = baseURL + link
            try:
                response = urllib2.urlopen(download_url)
            except urllib2.HTTPError:
               exit(0)
            file = open( write_path, 'w' )
            file.write(response.read())
            print("File downloaded: " + filename)
            file.close()

    else:
        year = date.today().year
        url = ("http://www.wpro.who.int/emerging_diseases/documents/dengue."
                "updates."+str(year)+"/en/")
        print("Downloading PDFs from " + url)
        print("...")
        request = urllib2.Request(url)
        html_page = urllib2.urlopen(request)
        soup = BeautifulSoup(html_page)

        allLinks = []
        for link in soup.findAll('a', { "class":"link_media" } ):
            allLinks.append(link)

        for i in range( 0, len(allLinks) ):
            allLinks[i] = allLinks[i]['href']

        baseURL = "http://www.wpro.who.int"

        for biWeekly in (allLinks):
            dateString = getDate(biWeekly)
            filename = ""+dateString + "_WPRO_WHO.pdf"
            write_path = out_dir + '/' + filename

            if not os.path.isfile(write_path):
                download_url = baseURL + biWeekly
                try:
                    response = urllib2.urlopen(download_url)
                except urllib2.HTTPError:
                    exit(0)
                file = open( write_path, 'w' )
                file.write(response.read())
                print("File downloaded: " + filename)
                file.close()

def getDate(string):
    c = ""
    for i in range(0, len(string)):
        if string[i].isdigit():
            for j in range (0,9):
                if string[i+j] == '.':
                    break
                c+= string[i+j]
            return c

def getPAHOData(out_dir, update_only=False):
    url = "http://www.paho.org/hq/index.php?option=com_topics&view=rdmore&cid=6290&Itemid=40734"
    print("Downloading PDFs from " + url)
    print("...")
    request = urllib2.Request(url)
    request.add_header('User-agent', 'Mozilla/5.0 (Linux i686)')
    html_page = urllib2.urlopen(request)
    soup = BeautifulSoup(html_page, "lxml")

    all_links_html = []
    for link in soup.findAll('a'):
        all_links_html.append(link)

    pdf_links_html = []
    # Get only the relevant links
    for link_html in all_links_html:
        if link_html.get('href').startswith("/hq/index.php?option=com_docman&task=doc_download&Itemid=270&gid=") and link_html.getText != "":
            pdf_links_html.append(link_html)

    # Download PDF files
    prefix = "http://www.paho.org"
    for html in pdf_links_html:
        full_link = prefix + html.get('href')
        title = html.getText()
        year = re.findall('\d+', title)
        if(len(year) == 0): continue
        year = year[0]
        if update_only & (year != CURRENT_YEAR):
            continue
        if year == CURRENT_YEAR:
            epi_week = re.findall('EW [0-9]+', title)[0].replace(' ','_')
            filename = year + '_' + epi_week + '.pdf'
            write_path = os.path.join(out_dir, filename)
        else:
            filename = year + '.pdf'
            write_path = os.path.join(out_dir, filename)
        if os.path.isfile(write_path):
            continue
        response = urllib2.urlopen(full_link)
        file = open(write_path, 'w')
        file.write(response.read())
        print("File downloaded: " + filename)
        file.close()

def getMexicoData(out_dir, update_only=False):
    end_year = int(CURRENT_YEAR)
    if update_only:
        start_year = end_year - 1  # Mexico is always at least a year behind.
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

# Function to get pdf data from WHO on Dengue. PDF files on Dengue Situation
# Updates.
def getWHO2Data(out_dir, update_only=False):
    # Initialize the current date
    myDate = date.today()

    # Set the date to be two weeks before.
    change = 1 - myDate.weekday()
    myDate += timedelta(days=change-14)

    # If updating then attempt to download past two Tuesdays and this Tuesday
    if update_only:
        # For loop to download twice
        for year in range( 0, 3 ):
            # Initialize dateString, filename "YearMonthDate_WPRO_WHO.pdf",
            # output path.
            dateString = '{:%Y%m%d}'.format(myDate)
            filename = dateString + "_WPRO_WHO.pdf"
            write_path = out_dir + '/' + filename

            # Copy Mexico function and edit appropriately.
            if not os.path.isfile(write_path):
                # Initialize string for the unique file date
                download_url = (
                        "http://www.wpro.who.int/emerging_diseases/dengue_biweekly_report_"
                        + dateString + ".pdf?ua=1" )
                try:
                    response = urllib2.urlopen(download_url)
                except urllib2.HTTPError:
                    exit(0)
                file = open(write_path, 'w')
                file.write(response.read())
                print("File downloaded: " + filename)
                file.close()
            myDate += timedelta(days=7)


def getTennesseeData(out_dir, update_only = False):
    base_url = "http://tn.gov/assets/entities/health/attachments/"
    firstRecordedYear = 2009
    firstRecordedWeek = 32  # Tennessee started recording in week 32 of 2009
    endYear = int(CURRENT_YEAR)
    if update_only:
        # Find most recently downloaded PDF file so that
        # we know when to start downloading.
        maxYear = firstRecordedYear
        maxWeek = firstRecordedWeek - 1
        for filename in os.listdir(out_dir):
            if filename.endswith(".pdf"):
                numbers = map(int, re.findall(r'\d+', filename))
                week = None
                year = None
                for number in numbers:
                    if number > 2000 and number < 2050:
                        year = number
                    elif number > 0 and number < 54:
                        week = number
                if week is None or year is None:
                    # Print error to log file
                    print "Error: could not find week or year number in filename: " + filename
                    continue
                if year > maxYear:
                    maxYear = year
                    maxWeek = week
                elif year == maxYear and week > maxWeek:
                    maxWeek = week
        firstYear = maxYear    # Doesn't handle year boundaries well because we don't
        firstWeek = maxWeek+1  # know if there are 52 or 53 weeks in which year.
    else:
        firstYear = firstRecordedYear
        firstWeek = firstRecordedWeek
    for year in range(firstYear, (endYear + 1)):
        if year == firstYear:
            startWeek = firstWeek
        else:
            startWeek = 1
        endWeek = 53  # TODO: auto-determine which years have week 53. For now, just handle error.

        for week in range(startWeek, endWeek):
            # The format of the url changed in week 47 of 2017.
            if (year == 2017 and week >= 47) or year > 2017:
                url = base_url + "ILI_spnreport_" + str(year) + "_" + str(week) + "c.pdf"
                # Make the filename for the new format the same as they used to be so they are all the same.
                filename = "week" + str(week) + "ILI_spnreport_" + str(year) + ".pdf"  
            else:
                url = base_url + "week" + str(week) + "ILI_spnreport_" + str(year) + ".pdf"
                filename = os.path.basename(url)
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

def getPeruData(out_dir, update_only=False):
    url = "http://www.dge.gob.pe/portal/index.php?option=com_content&view=article&id=404&Itemid=0"
    print("Downloading PDFs from " + url)
    print("...")
    html_page = urllib2.urlopen(url)
    soup = BeautifulSoup(html_page)

    all_links_html = []
    for link in soup.findAll('a'):
        all_links_html.append(link)

    dengue_PDF_links = []
    # Get only the relevant links
    for link_html in all_links_html:
        if "DENGUE" in link_html.get('href'):
            dengue_PDF_links.append(link_html.get('href'))

    # Download them
    for link in dengue_PDF_links:
        spl_link = link.split('/')
        perus_filename = spl_link[len(spl_link)-1]
        Ep_Wk = spl_link[len(spl_link)-2]
        Year = spl_link[len(spl_link)-3]
        my_filename = Year + '_' + Ep_Wk + '_' + '_'.join(perus_filename.split('%20'))
        write_path = out_dir + '/' + my_filename
        if not os.path.isfile(write_path):
                response = urllib2.urlopen(link)
                file = open(write_path, 'w')
                file.write(response.read())
                print("File downloaded: " + my_filename)
                file.close()

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
        temp = []
        for link in links:
            if CURRENT_YEAR in link:
                temp.append(link)
        links = temp

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

def getTaiwanData(out_dir, update_only=False):
    """
    # Uses Selenium web browser to download CSVs from  Javascript interface.
    """
    DEBUG = False # Don't download CSVs
    if os.path.isdir(out_dir):
        backup_dir = os.path.abspath(out_dir) + '-backup'
        if os.path.exists(backup_dir):
            shutil.rmtree(backup_dir)
        os.rename(out_dir, backup_dir)
        os.mkdir(out_dir)
    Taiwan_URL = "https://nidss.cdc.gov.tw/en/SingleDisease.aspx?dc=1&dt=4&disease=061"

    chrome_options = webdriver.ChromeOptions()
    prefs = {"download.default_directory" : os.path.abspath(out_dir)}
    chrome_options.add_experimental_option("prefs",prefs)
    driver = webdriver.Chrome(chrome_options=chrome_options)
    driver.get(Taiwan_URL)

    # Change 'Period' to start on year 1998
    period_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_y_s']/option[1]"
    year_1998_option = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, period_xpath)))
    year_1998_option.click()

    i = 2
    while True:
        try:
            region_dropdown_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_area']/option[" + str(i) + "]"
            # Wait up to 10 seconds
            nextRegion = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, region_dropdown_xpath)))
            regName = nextRegion.text
            # Select next region
            nextRegion.click()
            # Wait for dropdown refresh
            # a.k.a. Wait until "All"(the first option) is selected.
            first_option = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_city']/option[1]"
            WebDriverWait(driver, 10).until(EC.element_located_to_be_selected((By.XPATH, first_option)))
            i += 1
        except TimeoutException:
            # No more options in dropdown
            break

        j = 2
        while True:
            try:
                city_dropdown_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_city']/option[" + str(j) + "]"
                nextCity = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, city_dropdown_xpath)))
                cityName = nextCity.text
                # Select next City/County
                nextCity.click()
                # Wait for dropdown refresh
                # a.k.a. Wait until "All"(the first option) is selected.
                first_option = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_town']/option[1]"
                WebDriverWait(driver, 10).until(EC.element_located_to_be_selected((By.XPATH, first_option)))
                j += 1
            except TimeoutException:
                # No more options in dropdown
                break

            k = 2
            distName = ""
            while True:
                if distName == "Other": # Other is the last option fir districts
                    break
                try:
                    dist_dropdown_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_town']/option[" + str(k) + "]"
                    # Select next District/Township
                    nextDist = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, dist_dropdown_xpath)))
                    distName = nextDist.text
                    nextDist.click()
                    k += 1
                except TimeoutException:
                    # No more options in dropdown
                    break

                # Click 'Query' Button
                query_Button_ID = "ctl00_NIDSSContentPlace_NIDSS_query1_btnSend"
                query_Button = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.ID, query_Button_ID)))
                query_Button.click()

                if DEBUG:
                    continue
                # Check if this distric actually has data/chart
                chart_class = "highcharts-container"
                try:
                    chart = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CLASS_NAME, chart_class)))
                except TimeoutException:
                    # No data for this district
                    # Need to create a dummy CSV file so that this district (even though it is missing) will be included.
                    # Going to use the same filename format as the others.
                    dataType = "dengue-fever-"
                    caseOrigin = "indigenous-and-imported-"
                    period = "week-01-1998---week-24-2017"
                    distName = distName.replace(" ", "-").lower()
                    cityName = cityName.replace(" ", "-").lower()
                    regName = regName.replace(" ", "-").lower()

                    filename = dataType + ",-" + distName + "," + cityName + "," + regName + ",-" + caseOrigin + ",-" + period + ".csv"

                    f = open(os.path.join(out_dir, filename), 'w')
                    writer = csv.writer(f)
                    writer.writerow( ('Onset Year-Week', 'Number of Confirmed Cases') )
                    for year in range(1998,2018):
                        # Some years have 52 weeks, others have 53 weeks.
                        # The current year, 2017, is 24 weeks in.
                        weeks_52 = {2010, 2011, 2012, 2013, 2015, 2016}
                        weeks_24 = {2017}
                        if year in weeks_52:
                            weeks_limit = 53
                        elif year in weeks_24:
                            weeks_limit = 25
                        else:
                            weeks_limit = 54
                        for week in range(1,weeks_limit):
                            writer.writerow( (str(year) + str(week).zfill(2), "NA") )
                    f.close()
                    continue
                # Hamburger button that opens downloads dropdown menu
                hamburger_name = "highcharts-button"
                hamburger_Button = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CLASS_NAME, hamburger_name)))
                hamburger_Button.click()
                # Click "Download CSV" option (option 6)
                CSV_download_xpath = "//div[@class='highcharts-container']/div[1]/div[1]/div[6]"
                CSV_download_button = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH,
                    CSV_download_xpath)))
                print "Download"
                CSV_download_button.click()

    ## Get week to date info into CSV ##
    filename = "Taiwan_weeks_2_dates"
    f = open(out_dir + '/' + filename + '.csv', 'w')
    writer = csv.writer(f)
    writer.writerow( ('Year', 'Week', 'Start_Date', 'End_Date') )

    i = 1
    while True:
        try:
            # Start year
            year_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_y_s']/option[" + str(i) + "]"
            next_year = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, year_xpath)))
            next_year.click()
            # End year
            year_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_y_e']/option[" + str(i) + "]"
            next_year = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, year_xpath)))
            next_year.click()
            year = next_year.text
        except TimeoutException:
            # Done with all years
            break

        i += 1
        j = 1
        while True:
            try:
                # Start week
                week_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_w_s']/option[" + str(j) + "]"
                next_week = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, week_xpath)))
                next_week.click()
                # End week
                week_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_w_e']/option[" + str(j) + "]"
                next_week = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, week_xpath)))
                next_week.click()
                week = next_week.text
            except TimeoutException:
                # Done with all weeks in this year
                break

            # Click 'Query' Button
            query_Button_ID = "ctl00_NIDSSContentPlace_NIDSS_query1_btnSend"
            query_Button = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.ID, query_Button_ID)))
            query_Button.click()

            try:
                notes = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, "//table[@class='note']")))
            except TimeoutException:
                break

            # Get dates
            li_xpath = "//table[@class='note']/tbody[1]/tr[1]/td[1]/ol[1]/li[1]"
            li = WebDriverWait(driver, 2).until(EC.presence_of_element_located((By.XPATH, li_xpath)))
            li_text = li.text

            match = re.findall(r'\d{4}/\d{2}/\d{2}', li_text)
            begin_date = datetime.strptime(match[0], '%Y/%m/%d').date()
            end_date = datetime.strptime(match[1], '%Y/%m/%d').date()

            writer.writerow((str(year),  str(week).zfill(2), str(begin_date), str(end_date)))
            j += 1

    f.close()

def getSriLankaData(out_dir, update_only=False):
    sri_URL_monthly = "http://www.epid.gov.lk/web/index.php?option=com_casesanddeaths#"
    sri_URL_weekly = "http://www.epid.gov.lk/web/index.php?option=com_casesanddeaths&section=trends&lang=en#"
    sri_URLs = [sri_URL_monthly, sri_URL_weekly]
    monthly_table_id = "viewDeseases"
    weekly_table_id = "viewDeseasesSumry"
    table_ids = [monthly_table_id, weekly_table_id]
    data_freq = ["monthly", "weekly"]


    driver = webdriver.Chrome()
    i = 0
    for URL in sri_URLs:
        driver.get(URL)
        dropdown_option = 1
        while True:
            try:
                year_xpath = "//select[@id='year']/option[" + str(dropdown_option) + "]"
                # Wait up to 10 seconds
                nextYear = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, year_xpath)))
                year = nextYear.text
                # Select next year
                nextYear.click()
                # Wait for refresh
                selected_option = year_xpath
                WebDriverWait(driver, 10).until(EC.element_located_to_be_selected((By.XPATH, selected_option)))
            except TimeoutException:
                # No more options in dropdown
                break
            ## Just save the whole page
            table_xpath = "//table[@class='" + table_ids[i] + "']"
            table = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, table_xpath)))
            table_html = table.get_attribute('outerHTML')
            html_file = open(os.path.join(out_dir, year + '_' + data_freq[i] + '.html'), 'w')
            html_file.write(table_html.encode('utf8'))
            html_file.close()
            if update_only:  # Most recent data is first in the dropdown.
                break
            dropdown_option += 1
        i += 1

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

