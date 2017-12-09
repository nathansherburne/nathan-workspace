import re 
import os

# Set up directories
homDir = os.path.expanduser('~')
sanDir = os.path.join(homDir, "Dropbox/LEPR03/nathan-workspace/data/flu/san_diego")
dwnDir = os.path.join(sanDir, "download")
cvtDir = os.path.join(sanDir, "convert")

# Find the most recently converted HTML file so that
# we know which PDFs still need conversion.
csvNames = list()
for filename in os.listdir(cvtDir):
    if filename.endswith(".csv"):
        name, ext = os.path.basename(filename).split('.')
        csvNames.append(name)

pdfNames = list()
for filename in os.listdir(dwnDir):
    if filename.endswith(".pdf"):
        name, ext = os.path.basename(filename).split('.')
        pdfNames.append(name)

pdfsToConvert = set(pdfNames).symmetric_difference(csvNames)

for pdfFilename in pdfsToConvert:
    pdfFilename = pdfFilename + '.pdf'
    print os.path.join(dwnDir, pdfFilename)


