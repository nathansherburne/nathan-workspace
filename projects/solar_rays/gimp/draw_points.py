import os
import cv2
import numpy as np
import argparse
import csv

COLORS = [(255,0,0), (0,0,255), (255,255,0), (0,255,255), (255,0,255)]

def main():
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('-i', '--input', help='csv/txt files of x,y coordinates to draw')
    parser.add_argument('-d', '--draw', help='image to draw coords on')
    parser.add_argument('-o', '--output', help='the output image path')
    args = parser.parse_args()

    draw = cv2.imread(args.draw)
    with open(args.input, 'rb') as csvfile:
        for row in csv.DictReader(csvfile):
            cv2.circle(draw, (to_i(row['x']), to_i(row['y'])), 1, clr_from_num(to_i(row['id'])), -1)

    cv2.imwrite(args.output, draw)

def clr_from_num(n):
    return COLORS[n % len(COLORS) - 1]

def to_i(s):
    return int(float(s))
    

if __name__ == "__main__":
    main()
                




