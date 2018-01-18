# Input: an image with a table in it. (Note: the table must have lines that delineate its border.
# Output: the x coordinates of the each column..
# NOTE: Works for Mexico.
import cv2
import numpy as np
from matplotlib import pyplot as plt
from wand.image import Color
from wand.image import Image
import argparse
import sys
import os
import my_utils
import column_locating as image_op

def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', required=True, help='the input PDF file')
    parser.add_argument('-s', '--save', required=False, help='save the image with the table(s) outlined in the specified directory')
    parser.add_argument('-d', '--debug', action='store_true', required=False, help='display intermediate images one by one during execution (keypress to move forward)')
    parser.add_argument('-p', '--page', required=False, help='the page to analyze (Default is first page: page 1)')
    parser.add_argument('-l', '--lines', action='store_true', default=False, required=False, help='set this flag if there are lines delineating the columns (default: False)')
    parser.add_argument('-a', '--area', required=False, help='a comma-separated list defining the ROI (top,left,bottom,right)')

    args = parser.parse_args()
    pdf_filepath = args.input

    if args.page:
        page_num = int(args.page) - 1
    else:
        page_num = 0

    home_dir = os.path.expanduser('~')
    cwd = os.getcwd()
    image_filepath = os.path.join(cwd, "page.png")
    files_to_delete = list()
    files_to_delete.append(image_filepath)

    if not args.lines:
        just_text_filepath = os.path.join(cwd, "just_text.pdf")
        image_op.get_just_text_pdf(pdf_filepath, just_text_filepath)
        image_op.pdf2png(just_text_filepath, image_filepath, page_num)
        files_to_delete.append(just_text_filepath)
    else:
        image_op.pdf2png(pdf_filepath, image_filepath, page_num)

    # Get image
    color = cv2.imread(image_filepath)
    gray = cv2.imread(image_filepath, 0)
    image_height, image_width, image_channels = color.shape

    # Crop the image if --area option was provided
    if args.area:
        tlbr = args.area
        top,left,bottom,right = map(int, tlbr.split(','))
    else:
        top,left,bottom,right = 0,0,image_height,image_width

    if args.lines:
        image_op.locate_columns_by_lines(color, args.debug, top, left, bottom, right)
    else:
        image_op.locate_columns_by_text(color, args.debug, top, left, bottom, right)

    cv2.destroyAllWindows()
    cleanup(files_to_delete)

def cleanup(filepaths):
    for filepath in filepaths:
        silentremove(filepath)

def silentremove(filename):
    try:
        os.remove(filename)
    except OSError as e: # this would be "except OSError, e:" before Python 2.6
        if e.errno != errno.ENOENT: # errno.ENOENT = no such file or directory
            raise # re-raise exception if a different error occurred

if __name__ == "__main__":
    main(sys.argv[1:])
