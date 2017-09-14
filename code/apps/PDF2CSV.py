#!/usr/local/env python2.7
import sys, getopt
import os
import pdfToCSVlib as p2c
import argparse
from argparse_lib import is_dir
from argparse_lib import is_pdf
import logging

def main(argv):
    # Set up command line arguments
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.epilog = get_epilog(parser.prog)
    parser.add_argument("input", metavar='file', type=is_pdf, nargs='+', help="the input file(s)")
    parser.add_argument("-o", "--output", metavar='dir', type=is_dir, default=os.getcwd(), help="the output directory")
    parser.add_argument("-s", "--separate", action='store_true', default=False, help="Each page of PDF is a separate table")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("-b", "--boundary", action="store_true", help="differentiate cells by cell borders")
    group.add_argument("-c", "--coordinate", action="store_true", help="differentiate cells by text element coordinates")
    args = parser.parse_args()

    # Set up logs directory
    directory = os.path.join(args.output, 'logs')
    if not os.path.exists(directory):
            os.makedirs(directory)

    for inputfile in args.input:
        # Skip non-PDF files
        if os.path.splitext(inputfile)[-1] != '.pdf':
            continue
        # Set up this PDFs logging
        PDF_name = os.path.basename(inputfile).split('.')[0]
        cur_log_dir = os.path.join(directory, PDF_name)
        if not os.path.exists(cur_log_dir):
                os.makedirs(cur_log_dir)
        logging.basicConfig(filename=os.path.join(cur_log_dir, 'extract.log'), filemode='w', level=logging.DEBUG)
        os.chdir(cur_log_dir)  # Change to logs folder so that plots are saved in it
        # Generate String table from PDF
        string_table = p2c.pdfToTable(inputfile, by_coords=args.coordinate, by_bounds=args.boundary,
                sep_pages=args.separate)
        # Create output path + filename
        # Convert String table to CSV
        i = 1
        logging.debug("String table length: " + str(len(string_table)))
        for table in string_table:
            if len(string_table) == 1:
                page_no = ""
            else:
                page_no = '_' + str(i)
            csv_filename = os.path.splitext(inputfile.split('/')[-1])[0] + page_no + '.csv'
            outputfile = os.path.join(args.output, csv_filename)
            p2c.stringTableToCSV(table, outputfile)
            i += 1

def get_epilog(progname):
    return '''
Examples   : %s country/PDFs/* -o country/CSVs/ -b
           : %s country/PDFs/weekly_pages.pdf -o country/weekly-CSVs/ -c -s
''' % (progname, progname)

if __name__ == "__main__":
    main(sys.argv[1:])
