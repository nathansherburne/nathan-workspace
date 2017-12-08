#! /usr/bin/env/python2.7
import csv
import argparse
from collections import Counter

parser = argparse.ArgumentParser()
parser.add_argument("input", metavar='file', help="the HTML file(s) you want to convert to CSV.")
args = parser.parse_args()

filename = args.input
with open(filename, "rb") as csvFile:
    for row in  csv.reader(csvFile, delimiter=','):
        most_common = Counter(row).most_common(1)[0][0]
        if most_common != "" and most_common != "NA":
            print ",".join(row)
