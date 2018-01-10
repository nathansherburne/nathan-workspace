import os
import urllib2
from datetime import datetime, date, timedelta
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
