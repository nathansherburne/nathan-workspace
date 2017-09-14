#!/usr/bin/env python2.7
# -*- coding: iso-8859-1 -*-
import sys
import os
import HTMLParser
import re
import argparse
from argparse_lib import is_dir
from argparse_lib import is_html
from bs4 import BeautifulSoup
import csv

try:    import psyco ; psyco.jit()  # If present, use psyco to accelerate the program
except: pass

def main(argv):
    parser = argparse.ArgumentParser(description="A coarse HTML tables to CSV (Comma-Separated Values) converter.",
            formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.epilog = get_epilog(parser.prog)
    parser.add_argument("input", metavar='file', type=is_html, nargs='+', help="the HTML file(s) you want to convert to CSV.")
    parser.add_argument("-o", "--output", metavar='dir', type=is_dir, default=os.getcwd(), help="the output directory")
    parser.add_argument("-r", "--recursive", action='store_true', default=False, help="To get all leaf tables from HTML with nested tables")

    args = parser.parse_args()

    for htmlfilename in args.input:
        name, ext = os.path.splitext(os.path.basename(htmlfilename))
        htmlfile = open(htmlfilename, 'rb')
        if args.recursive:
            html_tables = getLeafTables(htmlfile)
        else:
            html_tables = [htmlfile.read()]
        i = 0
        for table in html_tables:
            outputfilename = name + "_table-" + str(i) + ".csv"
            csvfile = open(os.path.join(args.output, outputfilename), 'w+b')
            data = str(table)
            csvfile.write(getCSV(data))
            csvfile.close()
            i += 1
        htmlfile.close()

def getCSV(html_stream):
    html_parser = html2csv()
    html_parser.feed(html_stream)
    return html_parser.getCSV(True)

def getLeafTables(htmlfile):
    soup = BeautifulSoup(htmlfile)
    tables = soup.find_all("table")
    leaf_tables = []
    for table in tables:
        if len(table.find_all("table")) is not 0:
            # Only get 'leaf' tables (i.e. tables with no tables inside of them)
            continue
        leaf_tables.append(table)
    return leaf_tables

def extractLeafTables(htmlfilename):
    soup = BeautifulSoup(open(htmlfilename))
    tables = soup.find_all("table")

    leaf_tables = []
    i = 0
    for table in tables:
        if len(table.find_all("table")) is not 0:
            # Only extract 'leaf' tables (i.e. tables with no tables inside of them)
            continue

        rows = []
        headings = [th.get_text() for th in table.find("tr").find_all("th")]
        rows.append(headings)
        for row in table.find_all("tr")[1:]:
            cells = []
            for td in row.find_all("td"):
                cell_content = td.get_text()
                cell_content = cell_content.replace('\r', '').replace('\n', '').replace('\t', '')
                cells.append(cell_content)
            rows.append(cells)
        leaf_tables.append(rows)
    return leaf_tables

def get_epilog(progname):
    return '''
Examples   : python %s  mypage.html -r
           : python %s  *.html -o output/CSVs/
''' % (progname, progname)

class html2csv(HTMLParser.HTMLParser):
    ''' A basic parser which converts HTML tables into CSV.
        Feed HTML with feed(). Get CSV with getCSV(). (See example below.)
        All tables in HTML will be converted to CSV (in the order they occur
        in the HTML file).
        You can process very large HTML files by feeding this class with chunks
        of html while getting chunks of CSV by calling getCSV().
        Should handle badly formated html (missing <tr>, </tr>, </td>,
        extraneous </td>, </tr>...).
        This parser uses HTMLParser from the HTMLParser module,
        not HTMLParser from the htmllib module.
        Example: parser = html2csv()
                 parser.feed( open('mypage.html','rb').read() )
                 open('mytables.csv','w+b').write( parser.getCSV() )
        This class is public domain.
        Author: Sébastien SAUVAGE <sebsauvage at sebsauvage dot net>
                http://sebsauvage.net
        Versions:
           2002-09-19 : - First version
           2002-09-20 : - now uses HTMLParser.HTMLParser instead of htmllib.HTMLParser.
                        - now parses command-line.
        To do:
            - handle <PRE> tags
            - convert html entities (&name; and &#ref;) to Ascii.
            '''
    def __init__(self):
        HTMLParser.HTMLParser.__init__(self)
        self.CSV = ''      # The CSV data
        self.CSVrow = ''   # The current CSV row beeing constructed from HTML
        self.inTD = 0      # Used to track if we are inside or outside a <TD>...</TD> tag.
        self.inTR = 0      # Used to track if we are inside or outside a <TR>...</TR> tag.
        self.re_multiplespaces = re.compile('\s+')  # regular expression used to remove spaces in excess
        self.rowCount = 0  # CSV output line counter.
    def handle_starttag(self, tag, attrs):
        if tag == 'tr': self.start_tr()
        elif tag == 'td' or tag == 'th': self.start_td()
    def handle_endtag(self, tag):
        if tag == 'tr': self.end_tr()
        elif tag == 'td' or tag == 'th': self.end_td()         
    def start_tr(self):
        if self.inTR: self.end_tr()  # <TR> implies </TR>
        self.inTR = 1
    def end_tr(self):
        if self.inTD: self.end_td()  # </TR> implies </TD>
        self.inTR = 0            
        if len(self.CSVrow) > 0:
            self.CSV += self.CSVrow[:-1]
            self.CSVrow = ''
        self.CSV += '\n'
        self.rowCount += 1
    def start_td(self):
        if not self.inTR: self.start_tr() # <TD> implies <TR>
        self.CSVrow += '"'
        self.inTD = 1
    def end_td(self):
        if self.inTD:
            self.CSVrow += '",'  
            self.inTD = 0
    def handle_data(self, data):
        if self.inTD:
            self.CSVrow += self.re_multiplespaces.sub(' ',data.replace('\t',' ').replace('\n','').replace('\r','').replace('"','""'))
    def getCSV(self,purge=False):
        ''' Get output CSV.
            If purge is true, getCSV() will return all remaining data,
            even if <td> or <tr> are not properly closed.
            (You would typically call getCSV with purge=True when you do not have
            any more HTML to feed and you suspect dirty HTML (unclosed tags). '''
        if purge and self.inTR: self.end_tr()  # This will also end_td and append last CSV row to output CSV.
        dataout = self.CSV[:]
        self.CSV = ''
        return dataout


if __name__ == "__main__":
    main(sys.argv[1:])
