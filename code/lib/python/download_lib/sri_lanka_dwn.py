import os
from selenium import webdriver
from selenium.common.exceptions import NoSuchElementException
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities

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
        print "(" + data_freq[i] + ") Browsing " + URL + "\n"
        driver.get(URL)
        dropdown_option = 1
        while True:
            try:
                year_xpath = "//select[@id='year']/option[" + str(dropdown_option) + "]"
                # Wait up to 10 seconds
                nextYear = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, year_xpath)))
                year = nextYear.text
                # Select next year
                print "Selecting year " + year + " from dropdown menu."
                nextYear.click()
                # Wait for refresh
                selected_option = year_xpath
                WebDriverWait(driver, 10).until(EC.element_located_to_be_selected((By.XPATH, selected_option)))
            except TimeoutException:
                # No more options in dropdown
                break
            ## Just save the whole page
            table_xpath = "//table[@class='" + table_ids[i] + "']"
            try:
                print "Locating table: " + table_xpath
                table = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, table_xpath)))
                table_html = table.get_attribute('outerHTML')
                # Save the table's html
                filename = year + '_' + data_freq[i] + '.html'
                write_path = os.path.join(out_dir, filename)
                print "Saving to " + write_path
                html_file = open(write_path, 'w')
                html_file.write(table_html.encode('utf8'))
                html_file.close()
                if update_only:  # Most recent data is first in the dropdown.
                    break
            except TimeoutException:
                # Happens right after the New Year when there is 
                # no data, or even a table, for the present year.
                print "Warning: No table located for " + year
            dropdown_option += 1
            print
        i += 1
        print

