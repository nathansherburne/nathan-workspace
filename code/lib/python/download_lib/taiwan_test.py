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
    Taiwan_URL = "https://nidss.cdc.gov.tw/en/SingleDisease.aspx?dc=1&dt=4&disease=061"

    chrome_options = webdriver.ChromeOptions()
    prefs = {"download.default_directory" : os.path.abspath(out_dir)}
    chrome_options.add_experimental_option("prefs",prefs)
    driver = webdriver.Chrome(chrome_options=chrome_options)
    print "Opening Taiwan_URL..."
    driver.get(Taiwan_URL)

    period_xpath = "//select[@id='ctl00_NIDSSContentPlace_NIDSS_query1_y_s']/option[1]"
    year_1998_option = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.XPATH, period_xpath)))
    year_1998_option.click()
    try:
        el = driver.find_element_by_class_name('highcharts-contextmenu')
        print "Found"
    except NoSuchElementException:
        print "Not found"
    hamburger_name = "highcharts-button"
    hb = WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CLASS_NAME, hamburger_name)))
    #action = ActionChains(driver)
    #action.move_to_element(hb).click(hb).perform()
    driver.execute_script("scroll(250, 0)")
    #driver.execute_script("arguments[0].click();", hb)
    hb.click()
    try:
        el = driver.find_element_by_class_name('highcharts-contextmenu')
        print "Found"
    except NoSuchElementException:
        print "Not found"

if __name__ == "__main__":
    getTaiwanData("/Users/ndsherb/debug/", False)

