#! /usr/bin/env Rscript
library(data.table)
library(DICE)
library(stringr)

ROOT_DIR = "~/Dropbox/LEPR03/nathan-workspace/"
IN_DIR = paste0(ROOT_DIR, "data/dengue/rio/convert/")
MASTER.FILEPATH.1 = paste0(ROOT_DIR, "data/dengue/rio/merge/marged_weekly.csv")
source(paste0(ROOT_DIR, "code/scripts/generic_scripts/utils.R"))

