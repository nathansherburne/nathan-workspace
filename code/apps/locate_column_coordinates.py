# Input: an image with a table in it. (Note: the table must have lines that delineate its border.
# Output: the x coordinates of the each column..
# NOTE: Works for Mexico.
import cv2
import numpy as np
from matplotlib import pyplot as plt
from wand.image import Image
import argparse
import sys
import os
import my_utils
import subprocess

def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', required=True, help='the input PDF file')
    parser.add_argument('-s', '--save', required=False, help='save the image with the table(s) outlined in the specified directory')
    parser.add_argument('-d', '--debug', action='store_true', required=False, help='display intermediate images one by one during execution (keypress to move forward)')
    parser.add_argument('-p', '--page', required=False, help='the page to analyze (Default is first page: page 1')
    args = parser.parse_args()
    inputFile = args.input

    if args.page:
        page_num = int(args.page) - 1
    else:
        page_num = 0

    # Get PDF with only text in it since the image operations in this script 
    # are only concerned with and perform optimally with only the text (i.e.
    # no lines, images, boxes, etc...)
    home_dir = os.path.expanduser('~')
    cwd = os.getcwd()
    just_text_filepath = os.path.join(cwd, "just_text.pdf")
    pdfop_app_path = os.path.join(home_dir, "Dropbox/LEPR03/nathan-workspace/code/apps/pdfop.jar")
    bashCommand = "java -jar " + pdfop_app_path + " --keep-text -o " + just_text_filepath + " -i " + inputFile
    process = subprocess.Popen(bashCommand.split(), stdout=subprocess.PIPE)
    output, error = process.communicate()

    # Convert PDF to PNG
    image_filepath = os.path.join(cwd, "page.png")
    with Image(filename=just_text_filepath) as all_pages:
        single_page = all_pages.sequence[page_num]
        with Image(single_page).convert('png') as converted:
            converted.save(filename=image_filepath)

    # Get image
    color = cv2.imread(image_filepath)
    orig_copy = color.copy()
    image_height, image_width, image_channels = color.shape
    gray = cv2.imread(image_filepath, 0)

    if args.debug:
        cv2.imshow('original image', color)
        cv2.waitKey(0)
        cv2.imshow('original image (grayscale)', gray)
        cv2.waitKey(0)

#    # Fourier analysis
#    img = gray.copy()
#    dft = cv2.dft(np.float32(img),flags = cv2.DFT_COMPLEX_OUTPUT)
#    dft_shift = np.fft.fftshift(dft)
#
#    magnitude_spectrum = 20*np.log(cv2.magnitude(dft_shift[:,:,0],dft_shift[:,:,1]))
#    rows, cols = img.shape
#    crow,ccol = rows/2 , cols/2
#
#    # create a mask first, center square is 1, remaining all zeros
#    mask = np.zeros((rows,cols,2),np.uint8)
#    mask[0:image_height, image_width/2 - 10:image_width/2 + 10] = 1
#    mask = 1 - mask
#
#    # apply mask and inverse DFT
#    fshift = dft_shift*mask
#    f_ishift = np.fft.ifftshift(fshift)
#    d_shift = np.array(np.dstack([f_ishift.real,f_ishift.imag]))
#    img_back = cv2.idft(d_shift)
#    img_back = cv2.magnitude(img_back[:,:,0],img_back[:,:,1])
#    img_back = cv2.normalize(img_back, alpha=0, beta=1, norm_type=cv2.NORM_MINMAX, dtype=cv2.CV_32F, dst=img_back)
#    cv2.imshow('t',img_back)
#    cv2.waitKey(0)

    # Threshold it so that the lines of the table are white and background is black
    bw = cv2.adaptiveThreshold(gray, 255,cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV,11,2)
    if args.debug:
        cv2.imshow('thresholded image', bw)
        cv2.waitKey(0)

    # Remove vertical lines from image
    vscale = 25
    verticalSize = image_height / vscale
    vKernel = np.ones((verticalSize,1),np.uint8)
    vertical = cv2.morphologyEx(bw,cv2.MORPH_OPEN,vKernel)
    bw = bw - vertical

    if args.debug:
        cv2.imshow('vertical lines removed', bw)
        cv2.waitKey(0)

    # Connect letters of words together
    hscale = 100
    horizontalSize = image_width / hscale
    hKernel = np.ones((1, horizontalSize),np.uint8)
    merge_letters = cv2.dilate(bw,hKernel,iterations = 1)

    # Connect cells within a column
    vscale = 20
    verticalSize = image_height / vscale
    vKernel = np.ones((verticalSize,1),np.uint8)
    merge_words = cv2.dilate(merge_letters,vKernel,iterations = 1)

    if args.debug:
        cv2.imshow('letters in each word merged', merge_letters)
        cv2.waitKey(0)
        cv2.imshow('words in each column merged', merge_words)
        cv2.waitKey(0)

    # Isolate (disconnect) connected columns so that we can get the contour of each separate columns
    # Now that each column is a vertical white bar, we can erode anything else that is not a
    # significant vertical line. This will leave only vertical lines/bars where the columns are
    # aproximately.
    vscale = 5
    verticalSize = image_height / vscale
    vKernel = np.ones((verticalSize,1),np.uint8)
    just_columns = cv2.erode(merge_words,vKernel,iterations = 1)
    if args.debug:
        cv2.imshow('just columns', just_columns)
        cv2.waitKey(0)

    im2, contours, hierarchy = cv2.findContours(just_columns,cv2.RETR_CCOMP,cv2.CHAIN_APPROX_SIMPLE)

    if args.debug:
        cv2.drawContours(orig_copy, contours, -1, (0,255,0), 3)
        cv2.imshow("contours", orig_copy)
        cv2.waitKey(0)

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
        if args.debug:
            x = cx
            y0 = 0
            y1 = image_height
            orig_copy = cv2.line(orig_copy, (x,y0), (x,y1), (175,40,200), 2)
    if args.debug:
        cv2.imshow("column lines", orig_copy)
        cv2.waitKey(0)
    
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
        num_ones_min = image_height # Max initial value
        while cur_coord >= prev_col_coord:
            num_ones = np.count_nonzero(merge_words[:,cur_coord])
            if num_ones < num_ones_min:  # Min num ones should be somewhere in the void between columns.
                num_ones_min = num_ones
                min_ones_coord = cur_coord
            cur_coord -= 1
        col_space_coords.append(min_ones_coord)
        prev_col_coord = cur_col_coord

    if args.debug:
        for col_space_coord in col_space_coords:
            x = col_space_coord
            y0 = 0
            y1 = image_height
            orig_copy = cv2.line(orig_copy, (x,y0), (x,y1), (142,100,79), 2)
        cv2.imshow("column hole lines", orig_copy)
        cv2.waitKey(0)
    print ','.join(map(str,sorted(col_space_coords)))

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

    cv2.destroyAllWindows()

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
if __name__ == "__main__":
    main(sys.argv[1:])
