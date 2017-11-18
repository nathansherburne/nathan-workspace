import re 
import os

# Set up directories
homDir = os.path.expanduser('~')
tenDir = os.path.join(homDir, "Dropbox/LEPR03/nathan-workspace/data/dengue/rio/")
dwnDir = os.path.join(tenDir, "download")
cvtDir = os.path.join(tenDir, "convert")

# Find the most recently converted HTML file so that
# we know which PDFs still need conversion.
dwnTimestampPairs = list()
for filename in os.listdir(dwnDir):
    if (filename.endswith(".pdf") or filename.endswith(".htm")) and "weekly" in filename:
        filepath = os.path.join(dwnDir, filename)
        pair = (filename, os.path.getmtime(filepath))
        dwnTimestampPairs.append(pair)

csvTimestampPairs = list()
for filename in os.listdir(cvtDir):
    if filename.endswith(".csv") and "weekly" in filename:
        name, ext = filename.split('.')
        filepath = os.path.join(cvtDir, filename)
        pair = (name, os.path.getmtime(filepath))
        csvTimestampPairs.append(pair)
        
toConvert = list()
for dwnFile in dwnTimestampPairs:
    found = False
    dwnName, dwnExt = dwnFile[0].split('.')
    for csv in csvTimestampPairs:
        if dwnName == csv[0]:  # same filename
            found = True
            if dwnFile[1] - csv[1] > 0:  # pdf more recently updated
                toConvert.append(dwnFile)
            else:
                break
    if found is False:  # No CSV with the same filename
        toConvert.append(dwnFile)

for dwnFile in toConvert:
    print os.path.join(dwnDir, dwnFile[0])
