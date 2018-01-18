# Input: an image with a table in it. (Note: the table must have lines that delineate its border.
# Output: the rectangular coordinates of the (each?) table.
#       The output format is top,left,bottom,right
import cv2
import numpy as np
from wand.image import Image
from wand.color import Color
import argparse
import sys
import os
import my_utils
import table_locating

def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', required=True, help='the input PDF file')
    parser.add_argument('-s', '--save', required=False, help='save the image with the table(s) outlined in the specified directory')
    parser.add_argument('-d', '--debug', action='store_true', required=False, help='display intermediate images one by one during execution (keypress to move forward)')
    parser.add_argument('-p', '--page', required=False, help='the page to analyze (Default is first page: page 1')
    args = parser.parse_args()

    if args.page:
        page_num = int(args.page) - 1
    else:
        page_num = 0

    if args.save:
        save_path = args.save
    else:
        save_path = None

    top,left,bottom,right = table_locating.get_table_bbox(args.input, page_num, args.debug, save_path)
    cv2.destroyAllWindows()
    print str(top) + ',' + str(left) + ',' + str(bottom) + ',' + str(right)
    return

if __name__ == "__main__":
    main(sys.argv[1:])
