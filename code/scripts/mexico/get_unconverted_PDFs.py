import re 
import os

# Set up directories
homDir = os.path.expanduser('~')
mexDir = os.path.join(homDir, "Dropbox/LEPR03/nathan-workspace/data/dengue/mexico")
dwnDir = os.path.join(mexDir, "download")
cvtDir = os.path.join(mexDir, "convert")

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


