import re 
import os

# Set up directories
homDir = os.path.expanduser('~')
tenDir = os.path.join(homDir, "Dropbox/LEPR03/nathan-workspace/data/dengue/rio/")
dwnDir = os.path.join(tenDir, "download")
cvtDir = os.path.join(tenDir, "convert")

# Find the most recently converted HTML file so that
# we know which PDFs still need conversion.
pdfTimestampPairs = list()
for filename in os.listdir(dwnDir):
    if filename.endswith(".pdf") and "weekly" in filename:
        name, ext = filename.split('.')
        filepath = os.path.join(dwnDir, filename)
        pair = (name, os.path.getmtime(filepath))
        pdfTimestampPairs.append(pair)

csvTimestampPairs = list()
for filename in os.listdir(cvtDir):
    if filename.endswidth(".csv") and "weekly" in filename:
        name, ext = filename.split('.')
        filepath = os.path.join(cvtDir, filename)
        pair = (name, os.path.getmtime(filepath))
        csvTimestampPairs.append(pair)
        


#for pdfFilename in pdfsToConvert:
#    print os.path.join(dwnDir, pdfFilename)


