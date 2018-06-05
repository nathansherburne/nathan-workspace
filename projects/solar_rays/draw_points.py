import os
import cv2
import numpy as np
import argparse
import csv

def main():
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('-i', '--input', nargs='+', help='csv/txt files of x,y coordinates to draw')
    parser.add_argument('-d', '--draw', help='image to draw coords on')
    parser.add_argument('-o', '--output', help='the output image path')
    args = parser.parse_args()

    draw = cv2.imread(args.draw)
    for p_coords in args.input:
        with open(p_coords, 'rb') as csvfile:
            for row in csv.DictReader(csvfile):
                cv2.circle(draw, (int(row['x']), int(row['y'])), 1, (0,0,255), -1)

    cv2.imwrite(args.output, draw)

if __name__ == "__main__":
    main()
                




