import cv2
import numpy as np
from wand.image import Image
from wand.color import Color
import argparse
import sys
import os
import my_utils

def get_table_bbox(input_file, page_num, debug=False, save_path=None):
    # Convert PDF to PNG
    home_dir = os.path.expanduser('~')
    cwd = os.getcwd()
    image_filepath = os.path.join(cwd, "page.png")
    with Image(filename=input_file) as all_pages:
        single_page = all_pages.sequence[page_num]
        with Image(single_page) as i:
            i.background_color = Color('white')
            i.alpha_channel = 'remove'
            converted = i.convert('png')
            converted.save(filename=image_filepath)

    # Get image
    gray = cv2.imread(image_filepath, 0)
    color = cv2.imread(image_filepath)
    if debug:
        cv2.imshow('original image', color)
        cv2.waitKey(0)

    # Threshold it so that the lines of the table are white and background is black
    bw = cv2.adaptiveThreshold(gray, 255,cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV,11,2)
    if debug:
        cv2.imshow('thresholded image', bw)
        cv2.waitKey(0)

    # Get two images. One with just horizontal lines, thr other with vertical.
    horizontal = bw.copy()
    vertical = bw.copy()

    scale = 15
    image_height, image_width = bw.shape
    horizontalSize = image_width / scale
    verticalSize = image_height / scale

    hKernel = np.ones((1,horizontalSize),np.uint8)
    horizontal = cv2.morphologyEx(bw,cv2.MORPH_OPEN,hKernel)

    vKernel = np.ones((verticalSize,1),np.uint8)
    vertical = cv2.morphologyEx(bw,cv2.MORPH_OPEN,vKernel)

    if debug:
        cv2.imshow('vertical lines', vertical)
        cv2.waitKey(0)
    if debug:
        cv2.imshow('horizontal lines', horizontal)
        cv2.waitKey(0)

    # Combine vertical and horizontal images into one
    mask = horizontal + vertical

    if debug:
        cv2.imshow("mask", mask)
        cv2.waitKey(0)

    # Find the joints between the lines of the tables. Joints will
    # help discriminate between tables and images, boxes, etc...
    joints = cv2.bitwise_and(horizontal, vertical)

    if debug:
        cv2.imshow("joints", joints)
        cv2.waitKey(0)


    connectivity = 4
    numLabels, labels, stats, centroids = cv2.connectedComponentsWithStats(mask, connectivity, cv2.CV_32S)

    # Don't consider connected components with very small area (often lines)
    # or very large area (often just a border around the whole image or something).
    totalArea = image_width * image_height
    minArea = totalArea / 100
    maxArea = totalArea / 2

    # Sort the connected components by area so that we can remove joints along the way.
    # For example, if CC#1 contains CC#2 (i.e. CC#2 is smaller than CC#1), then all of
    # the joints in CC#2 are also inside of CC#1. But for each CC we only want to count
    # the joints that belong to each CC alone. That is why we start with the smallest
    # CC and remove the joints from the set of joints, so that no joint is counted
    # twice.
    areas = stats[:,cv2.CC_STAT_WIDTH] * stats[:,cv2.CC_STAT_HEIGHT]
    stats = stats[areas.argsort()]
    table_found = False
    for stat in stats:
        area = stat[cv2.CC_STAT_HEIGHT] * stat[cv2.CC_STAT_WIDTH]
        #if area < minArea or area > maxArea:
        #    continue

        roi_height = stat[cv2.CC_STAT_HEIGHT]
        roi_width = stat[cv2.CC_STAT_WIDTH]
        roi_top = stat[cv2.CC_STAT_TOP]
        roi_left = stat[cv2.CC_STAT_LEFT]
        roi_bottom = stat[cv2.CC_STAT_TOP] + roi_height
        roi_right = stat[cv2.CC_STAT_LEFT] + roi_width

        # Find the number of joints in this connected component (CC)
        roi = joints[roi_top:roi_bottom, roi_left:roi_right]
        im2, contours, hierarchy = cv2.findContours(roi,cv2.RETR_CCOMP,cv2.CHAIN_APPROX_SIMPLE)

        # Subract this joints subset from the set of all joints
        # roi expanded is the roi with the size of the original image it was
        # taken from, in the correct location. Basically 4 black borders are
        # added to the roi so that it can be subtracted fro mthe original.
        roi_expanded = cv2.copyMakeBorder(roi,roi_top,image_height-roi_bottom,roi_left,image_width-roi_right,cv2.BORDER_CONSTANT,value=[0,0,0])
        joints = joints - roi_expanded

        if debug:
            if len(contours) == 0:
                continue
            cv2.imshow("roi", roi_expanded)
            cv2.waitKey(0)
            cv2.imshow("joints", joints)
            cv2.waitKey(0)

        # If there are 4 or less joints in this CC, assume it is not a table.
        if len(contours) <= 20:
            continue

        # Table found
        table_found = True
        cv2.rectangle(color, (roi_left,roi_top), (roi_right,roi_bottom), (0,255,0), 3)
        cv2.drawContours(color, contours, -1, (255,0,255), 3, offset=(roi_left,roi_top))

        if debug:
            cv2.imshow('found table', color)
            cv2.waitKey(0)
        if save_path is not None:
            cv2.imwrite(save_path,color)
            print "File created: " + save_path

        return [roi_top, roi_left, roi_bottom, roi_right]

    # No table found, return entire dimension of image.
    return [0,0,image_height,image_width]




    #if debug:
    #    cv2.imshow("color", color)
    #    cv2.waitKey(0)
    #if args.save:
    #    inputBasename = os.path.basename(input_file)
    #    name,ext = inputBasename.split('.')
    #    outputBasename = name + '.png'
    #    outputFile = os.path.join(args.save, outputBasename)
    #    cv2.imwrite(outputFile, color)
    #    print "Debug image saved: " + outputFile

