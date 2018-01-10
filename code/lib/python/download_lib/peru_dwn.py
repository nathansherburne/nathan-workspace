import os
import urllib2
from bs4 import BeautifulSoup

def getPeruData(out_dir, update_only=False):
    # TODO: this link seems to be stuck on week 33 of 2017 (which is probably the week when I got this URL). Try to find the updated page.
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

