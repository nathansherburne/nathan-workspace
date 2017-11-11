import re 
import os

# Set up directories
homDir = os.path.expanduser('~')
pahDir = os.path.join(homDir, "Dropbox/LEPR03/nathan-workspace/data/dengue/paho/")
dwnDir = os.path.join(pahDir, "download")
cvtDir = os.path.join(pahDir, "convert")

# Find the most recently converted HTML file so that
# we know which PDFs still need conversion.
maxYear = 0
maxWeek = 0
for filename in os.listdir(cvtDir):
    if filename.endswith(".csv") and "EW" in filename:
        year, week = map(int, re.findall(r'\d+', filename))
        if year > maxYear:
            maxYear = year
            maxWeek = week
        elif year == maxYear and week > maxWeek:
            maxWeek = week

pdfsToConvert = list()
for filename in os.listdir(dwnDir):
    if filename.endswith(".pdf") and "EW" in filename:
        year, week = map(int, re.findall(r'\d+', filename))
        if year > maxYear:
            pdfsToConvert.append(filename)
        elif year == maxYear and week > maxWeek:
            pdfsToConvert.append(filename)

for pdfFilename in pdfsToConvert:
    print os.path.join(dwnDir, pdfFilename)


