CME Scoreboard
====================

How to update:

To download:
python download_data.py -d CME -o data/cme_scoreboard/download/

To convert from html:
python HTML2CSV.py data/cme_scoreboard/download/* -o data/cme_scoreboard/convert/ -n

To format the CSVs:
cd cme_scoreboards/
./format.sh

To merge:
./merge.R
