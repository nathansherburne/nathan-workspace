import os
import urllib2
from bs4 import BeautifulSoup
from datetime import datetime, date, timedelta

# Script using beautifulsoup to download pdfs from WHO.
def getWHOData(out_dir, update_only=False):
    if update_only:
        url = ("http://www.wpro.who.int/" +
                "emerging_diseases/DengueSituationUpdates/en/")

        request = urllib2.Request(url)
        try:
            html_page = urllib2.urlopen(request)
        except urllib2.HTTPError:
            print "HTTPError: Could not find URL: " + url
            print "Exiting..."
            exit(1)
        soup = BeautifulSoup(html_page,"lxml")

        link = soup.find('a', {"class":"link_media"} )
        link = link['href']

        dateString = getDate(link)
        filename = ""+dateString + "_WPRO_WHO.pdf"
        write_path = os.path.join(out_dir, filename)
        baseURL = "http://www.wpro.who.int"

        if not os.path.isfile(write_path):
            download_url = baseURL + link
            try:
                response = urllib2.urlopen(download_url)
            except urllib2.HTTPError:
                print "HTTPError: Could not find URL: " + download_url
                print "Exiting..."
                exit(1)
            file = open( write_path, 'w' )
            file.write(response.read())
            print("File downloaded: " + write_path)
            file.close()
        else:
            print "File: " + write_path + " already exists."

    else:
        year = date.today().year
        url = ("http://www.wpro.who.int/emerging_diseases/documents/dengue."
                "updates."+str(year)+"/en/")
        print("Downloading PDFs from " + url)
        print("...")
        request = urllib2.Request(url)
        try:
            html_page = urllib2.urlopen(request)
        except urllib2.HTTPError:
            print "HTTPError: Could not find URL: " + url
            print "Exiting..."
            exit(1)
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
            write_path = os.path.join(out_dir, filename)

            if not os.path.isfile(write_path):
                download_url = baseURL + biWeekly
                try:
                    response = urllib2.urlopen(download_url)
                except urllib2.HTTPError:
                    print "HTTPError: Could not find URL: " + download_url
                    print "Exiting..."
                    exit(1)
                file = open( write_path, 'w' )
                file.write(response.read())
                print("File downloaded: " + filename)
                file.close()
            else:
                print "File: " + write_path + " already exists."

def getDate(string):
    c = ""
    for i in range(0, len(string)):
        if string[i].isdigit():
            for j in range (0,9):
                if string[i+j] == '.':
                    break
                c+= string[i+j]
            return c
