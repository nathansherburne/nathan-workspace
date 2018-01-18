import cv2
import numpy as np
from matplotlib import pyplot as plt
from wand.image import Color
from wand.image import Image
import argparse
import sys
import os
import my_utils
import subprocess

def locate_columns_by_lines(orig, debug, roi_top, roi_left, roi_bottom, roi_right):
    orig_height, orig_width, orig_channels = orig.shape
    gray = cv2.cvtColor(orig, cv2.COLOR_BGR2GRAY)

    # Crop to ROI
    image = orig[roi_top:roi_bottom, roi_left:roi_right]
    gray = gray[roi_top:roi_bottom, roi_left:roi_right]

    display_debug(image, "Original Image", debug)
    display_debug(gray, "Original Image (grayscale)", debug)

    # Threshold it so that the lines of the table are white and background is black
    bw = cv2.adaptiveThreshold(gray, 255,cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV,11,2)
    display_debug(bw, "Thresholded Image", debug)

    # Get two images. One with just horizontal lines, the other with vertical.
    horizontal = bw.copy()
    vertical = bw.copy()

    scale = 15
    image_height, image_width = bw.shape
    horizontalSize = orig_width / scale
    verticalSize = orig_height / scale

    hKernel = np.ones((1,horizontalSize),np.uint8)
    horizontal = cv2.morphologyEx(bw,cv2.MORPH_OPEN,hKernel)

    vKernel = np.ones((verticalSize,1),np.uint8)
    vertical = cv2.morphologyEx(bw,cv2.MORPH_OPEN,vKernel)

    display_debug(vertical, "Vertical Lines", debug)
    display_debug(horizontal, "Horizontal Lines", debug)

    # Combine vertical and horizontal images into one
    mask = horizontal + vertical
    display_debug(mask, "Mask", debug)

    # Find the joints between the lines of the tables. Joints will
    # help discriminate between tables and images, boxes, etc...
    joints = cv2.bitwise_and(horizontal, vertical)
    display_debug(joints, "Joints", debug)

    im2, contours, hierarchy = cv2.findContours(joints,cv2.RETR_CCOMP,cv2.CHAIN_APPROX_SIMPLE)

    ## Getting column coordinates from joints ##
    col_coords = list()
    for contour in contours:
        # These contours should be very small, so no need to compute centroid.
        # Just assume the first contour point is a good estimate of centroid.
        centroid_x = contour[0][0][0]
        col_coords.append(centroid_x)
    # There are often duplicate (or nearly equal) x-coordinates since tables usually
    # have multiple joints on each column. So group similar joints together and
    # say the column coordinate is their average.
    max_gap = 5  # This value could be much more adaptive (eg. std or something)
    clusters = my_utils.cluster(col_coords, max_gap)
    roi_col_coords = list()
    for col in clusters:
        roi_col_coords.append(np.mean(col))

    # Roi coordinates are relative. So make sure to output the column coordinates
    # in relation to the whole page, not just the contour area.

    # Print column coordinates
    global_col_coords = [x+roi_left for x in roi_col_coords]
    print ','.join(map(str,global_col_coords))

    for x in global_col_coords:
        x = int(x)
        y0 = 0
        y1 = orig_height
        orig = cv2.line(orig, (x,y0), (x,y1), (175,40,200), 2)
    display_debug(orig, "Column coordinates", debug)


def locate_columns_by_text(orig, debug, roi_top, roi_left, roi_bottom, roi_right):
    orig_height, orig_width, orig_channels = orig.shape
    gray = cv2.cvtColor(orig, cv2.COLOR_BGR2GRAY)
    orig_copy = orig.copy()

    # Crop to ROI
    image = orig[roi_top:roi_bottom, roi_left:roi_right]
    gray = gray[roi_top:roi_bottom, roi_left:roi_right]

    display_debug(image, "Original Image", debug)
    display_debug(gray, "Original Image (grayscale)", debug)

    # Threshold it so that the lines of the table are white and background is black
    bw = cv2.adaptiveThreshold(gray, 255,cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV,11,2)
    display_debug(bw, "Thresholded Image", debug)

    # Remove vertical lines from image
    vscale = 25
    verticalSize = orig_height / vscale
    vKernel = np.ones((verticalSize,1),np.uint8)
    vertical = cv2.morphologyEx(bw,cv2.MORPH_OPEN,vKernel)
    bw = bw - vertical
    display_debug(bw, "Vertical lines removed", debug)

    # Connect letters of words together
    hscale = 100
    horizontalSize = orig_width / hscale
    hKernel = np.ones((1, horizontalSize),np.uint8)
    merge_letters = cv2.dilate(bw,hKernel,iterations = 1)
    display_debug(merge_letters, "Merged letters", debug)

    # Connect cells within a column
    vscale = 20
    verticalSize = orig_height / vscale
    vKernel = np.ones((verticalSize,1),np.uint8)
    merge_words = cv2.dilate(merge_letters,vKernel,iterations = 1)
    display_debug(merge_words, "Merged words", debug)

    # Isolate (disconnect) connected columns so that we can get the contour of each separate columns
    # Now that each column is a vertical white bar, we can erode anything else that is not a
    # significant vertical line. This will leave only vertical lines/bars where the columns are
    # aproximately.
    vscale = 5
    verticalSize = orig_height / vscale
    vKernel = np.ones((verticalSize,1),np.uint8)
    just_columns = cv2.erode(merge_words,vKernel,iterations = 1)
    display_debug(just_columns, "Just columns", debug)

    im2, contours, hierarchy = cv2.findContours(just_columns,cv2.RETR_CCOMP,cv2.CHAIN_APPROX_SIMPLE)
    cv2.drawContours(orig_copy, contours, -1, (0,255,0), 3)
    display_debug(orig_copy, "Contours", debug)

    # Find center of each contour. These x coordinates roughly correspond to the center of the column.
    avg_height = 0
    for contour in contours:
        x,y,w,h = cv2.boundingRect(contour)
        avg_height += h / len(contours)

    col_coords = list()
    for contour in contours:
        # Don't count contours much less than the average height (probably not a column)
        x,y,w,h = cv2.boundingRect(contour)
        if h < avg_height * 0.5:
            continue
        # Compute centroid of column's contour
        M = cv2.moments(contour)
        cx = int(M['m10'] / M['m00'])
        cy = int(M['m01'] / M['m00'])
        col_coords.append(cx)
        if debug:
            x = cx
            y0 = 0
            y1 = orig_height
            orig_copy = cv2.line(orig_copy, (x,y0), (x,y1), (175,40,200), 2)
    display_debug(orig_copy, "Column lines", debug)

    # But for column coordinates, we actually want the x coordinate of the spaces between the columns.
    # We will define the x coordinate of a column boundary as the vertical cross-section of the image
    # between two adjacent columns with the highest number of empty space (i.e. 0's), using the
    # word-merged binary image.
    col_coords = sorted(col_coords)
    col_space_coords = list()
    if len(col_coords) > 0:
        prev_col_coord = col_coords[0]
    for i in range(1,len(col_coords)):
        cur_col_coord = col_coords[i]
        cur_coord = cur_col_coord
        num_ones_min = orig_height # Max initial value
        while cur_coord >= prev_col_coord:
            num_ones = np.count_nonzero(merge_words[:,cur_coord])
            if num_ones < num_ones_min:  # Min num ones should be somewhere in the void between columns.
                num_ones_min = num_ones
                min_ones_coord = cur_coord
            cur_coord -= 1
        col_space_coords.append(min_ones_coord)
        prev_col_coord = cur_col_coord

    for col_space_coord in col_space_coords:
        x = col_space_coord
        y0 = 0
        y1 = orig_height
        orig_copy = cv2.line(orig_copy, (x,y0), (x,y1), (142,100,79), 2)
    print ','.join(map(str,sorted(col_space_coords)))
    display_debug(orig_copy, "Column hole (boundary) coordinates", debug)

# # The contour with the most children is probably the one with all
# # the holes (i.e. the column spaces) in it.
# parent_i = 3
# first_child_i = 2
# next_sibling_i = 0
# most_common_parent_i = my_utils.most_common(my_utils.column(hierarchy[0], parent_i))
# # Now get all of its children (i.e. the holes). The column boundaries will be
# # defined as the centroid of the holes.
# col_coords = list()
# next_child_i = hierarchy[0][most_common_parent_i][first_child_i]
# while next_child_i != -1:
#     # Compute centroid of hole contour
#     M = cv2.moments(contours[next_child_i])
#     cx = int(M['m10'] / M['m00'])
#     cy = int(M['m01'] / M['m00'])
#     next_child_i = hierarchy[0][next_child_i][next_sibling_i]
#     col_coords.append(cx)
# print ','.join(map(str,sorted(col_coords)))

# if args.debug:
#     orig_copy = color
#     for col_coord in col_coords:
#         x = col_coord
#         y0 = 0
#         y1 = image_height
#         orig_copy = cv2.line(orig_copy, (x,y0), (x,y1), (142,100,79), 2)
#     cv2.imshow("hole centroids", orig_copy)
#     cv2.waitKey(0)


#    if not table_found:
#        print "0,0," + str(image_height) + ',' + str(image_width)  # Return dimension of entire image
#
#    if args.debug:
#        cv2.imshow("color", color)
#        cv2.waitKey(0)
#    if args.save:
#        inputBasename = os.path.basename(inputFile)
#        name,ext = inputBasename.split('.')
#        outputBasename = name + '.png'
#        outputFile = os.path.join(args.save, outputBasename)
#        cv2.imwrite(outputFile, color)
#        print "Debug image saved: " + outputFile
#
def pdf2png(pdf_filepath, image_filepath, page_num):
    with Image(filename=pdf_filepath) as all_pages:
        single_page = all_pages.sequence[page_num]
        with Image(single_page) as i:
            i.background_color = Color('white')
            i.alpha_channel = 'remove'
            converted = i.convert('png')
            converted.save(filename=image_filepath)

def display_debug(image, title, debug):
    if debug:
        cv2.imshow(title, image)
        cv2.waitKey(0)

def get_just_text_pdf(input_pdf_filepath, output_pdf_filepath):
    # Get PDF with only text in it since the image operations in this script
    # are only concerned with and perform optimally with only the text (i.e.
    # no lines, images, boxes, etc...)
    home_dir = os.path.expanduser('~')
    cwd = os.getcwd()
    pdfop_app_path = os.path.join(home_dir, "Dropbox/LEPR03/nathan-workspace/code/apps/pdfop.jar")
    bashCommand = "java -jar " + pdfop_app_path + " --keep-text -o " + output_pdf_filepath + " -i " + input_pdf_filepath
    process = subprocess.Popen(bashCommand.split(), stdout=subprocess.PIPE)
    output, error = process.communicate()

