import os
import csv
import re
import shutil
from selenium import webdriver
from selenium.common.exceptions import NoSuchElementException
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
from selenium.webdriver.common.action_chains import ActionChains
from datetime import datetime
import math

now = datetime.now()
CURRENT_YEAR = now.year
CURRENT_MONTH = now.month
CURRENT_DAY = now.day
CURRENT_WEEK = int(math.ceil(CURRENT_DAY / 7.0))

def getTaiwanData(out_dir, update_only=False):
    """
    # Uses Selenium web browser to download CSVs from Javascript interface.
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
    print "Opening Taiwan_URL..."
    driver.get(Taiwan_URL)

    # Change 'Period' to start on year 1998
    period_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_y_s']/option[1]"
    year_1998_option = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, period_xpath)))
    year_1998_option.click()

    i = 2
    print "Beginning downloads."
    while True:
        try:
            region_dropdown_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_area']/option[" + str(i) + "]"
            # Wait up to 10 seconds
            nextRegion = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, region_dropdown_xpath)))
            regName = nextRegion.text
            # Select next region
            print "Downloading from region: " + regName
            nextRegion.click()
            # Wait for dropdown refresh
            # a.k.a. Wait until "All"(the first option) is selected.
            first_option = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_city']/option[1]"
            WebDriverWait(driver, 10).until(EC.element_located_to_be_selected((By.XPATH, first_option)))
            i += 1
        except TimeoutException:
            # No more options in dropdown
            print "Reached end of \"Regions\" dropdown."
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
                print "Reached end of \"City\" dropdown."
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
                    print "Reached end of \"District\" dropdown."
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
                    # Hamburger button that opens downloads dropdown menu
                    # TODO: Figure out how to make this work when headless(i.e., crontab or ssh (from shadow)).
                    # The hamburger "button" is not really a button. So it is not clickable when there's no actual display.
                    # I've tried executing the click via webdriver.execute_script(), which executes a JavaScript script.
                    # I've tried using ActionChains to click as well. None of these are able to click the hamburger button
                    # in headless mode.
                    # Why not just forget the hamburger button and skip to clicking "Download CSV" option:
                    # The "context menu" (i.e., the html for it) that contains the "Download CSV" option is dynamically created when the 
                    # hamburger button is clicked for the first time. So we get a NoSuchElement exception if we try to locate the
                    # "Download CSV" option before clicking the hamburger button.
                    # NOTE: As it is right now, this program can run as long as you run it while on a desktop display.
                    # Locate the hamburger button
                    hamburger_name = "highcharts-button"
                    hamburger_Button = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CLASS_NAME, hamburger_name)))
                    # Try to click it
                    # Method 1:
                    # Works when run with physical desktop display.
                    # Doesn't work when run without physical desktop display (i.e., from ssh or shadow)
                    # Why it doesn't work:
                    # Error Message: "Element is not clickable at point..."
                    hamburger_Button.click()  # Click using Selenium .click() function

                    # Method 2:
                    # Doesn't work at all for hamburger button.
                    # Why it doesn't work:
                    # The element must have an "onClick" attribute.
                    # Error Message: "arguments[0].click is not a function..."
                    #driver.execute_script("arguments[0].click()", hamburger_Button)  # Click using JavaScript .click() function. 
            
                    # Method 3:
                    # Works when run with physical desktop display.
                    # Doesn't work when run without physical desktop display.
                    # Why it doesn't work:
                    # Not sure. No error messages or exceptions raised. It just has no effect.
                    #action=ActionChains(driver)
                    #action.move_to_element(hamburger_Button).click(hamburger_Button).perform()
                    
                    # Other things I've tried to make the button clickable / visible:
                    #driver.execute_script("arguments[0].checked = true", hamburger_Button) # Force it to be visible
                    #driver.execute_script("arguments[0].scrollIntoView()", hamburger_Button)
                    #driver.execute_script("arguments[0].style.height='auto'; arguments[0].style.visibility='visible'", hamburger_Button)
                    
                    # Click "Download CSV" option (option 6 of hamburger button menu)
                    CSV_download_xpath = "//div[@class='highcharts-container']/div[1]/div[1]/div[6]"
                    try:
                        CSV_download_button = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, CSV_download_xpath)))
                    except TimeoutException:
                        print "Error: Could not locate \"CSV Download\" option. This is probably because the hamburger button was not actually clicked (See comments in code/lib/python/download_lib/taiwan_dwn.py starting at line 114)."
                        exit(1)
                    CSV_download_button.click() 
                    #driver.execute_script("arguments[0].click()", CSV_download_button)
                    print "Downloaded CSV for: " + regName + ", " + cityName + ", " + distName
                except TimeoutException:
                    # No data for this district
                    # Need to create a dummy CSV file so that this district (even though it is missing) will be included.
                    # Going to use the same filename format as the others.
                    print "No chart for this item: " + regName + ", " + cityName + ", " + distName
                    print "Creating dummy CSV file..."
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
                    for year in range(1998, CURRENT_YEAR + 1):
                        # Some years have 52 weeks, others have 53 weeks.
                        # The current year, 2017, is 24 weeks in.
                        # TODO: Get number of weeks in a given year automatically.
                        weeks_52 = {2010, 2011, 2012, 2013, 2015, 2016, 2017}
                        weeks_incomplete = {CURRENT_YEAR}
                        if year in weeks_52:
                            weeks_limit = 53
                        elif year in weeks_incomplete:
                            weeks_limit = CURRENT_WEEK
                        else:
                            weeks_limit = 54
                        for week in range(1,weeks_limit):
                            writer.writerow( (str(year) + str(week).zfill(2), "NA") )
                    f.close()
                    print "Dummy file created: " + filename
                    continue

    ## Get week to date info into CSV ##
    print "Getting exact week / date correspondence (i.e., how Taiwan defines their week numbers)."
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
            print "Reached end of \"Year\" dropdown."
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
                print "Reached end of \"Week\" dropdown."
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
            print "Extracting date info for this week."
            li_xpath = "//table[@class='note']/tbody[1]/tr[1]/td[1]/ol[1]/li[1]"
            li = WebDriverWait(driver, 2).until(EC.presence_of_element_located((By.XPATH, li_xpath)))
            li_text = li.text

            match = re.findall(r'\d{4}/\d{2}/\d{2}', li_text)
            begin_date = datetime.strptime(match[0], '%Y/%m/%d').date()
            end_date = datetime.strptime(match[1], '%Y/%m/%d').date()

            writer.writerow((str(year),  str(week).zfill(2), str(begin_date), str(end_date)))
            j += 1

    f.close()

