import re
from bs4 import BeautifulSoup
import os
import numpy as np
import argparse
import csv

def main():
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('-i', '--input', help='svg file with paths to get x,y coords from')
    parser.add_argument('-o', '--output', help='the output image path')
    args = parser.parse_args()

    with open(args.input, 'rb') as svgfile:
        with open(args.output, 'wb') as csvfile:
            csvwriter = csv.writer(csvfile)
            csvwriter.writerow(('id','x','y'))

            soup = BeautifulSoup(svgfile.read(), 'lxml')
            for path in soup.find_all("path"):
                for coord in coords_from_d(path['d']):
                    csvwriter.writerow([path['id']] + coord)
            
def coords_from_d(d):
    svg_commands = list(filter(None, map((lambda s: s.strip()), re.split('[A-z]|\\n', d))))
    for command in svg_commands:
        yield(coord_from_svg_command(command))
        
def coord_from_svg_command(command):
    """Get the move-to coordinate from an SVG command.

    Also make any negative coordinates 0.

    Args:
        command: an SVG command.

    Example 1:
        command: M 10 25
        output: (10, 25)

    Example 2:
        command: C 10,10 5,15 120,25
        output: (120, 25)

    Basically just gets the last two coordinates.
    """
    all_coords = re.split(' |,', command)
    x = all_coords[-2]
    y = all_coords[-1]
    return [neg_to_zero(x), neg_to_zero(y)]

def neg_to_zero(n):
    return '0.00' if float(n) < 0 else n

if __name__ == "__main__":
    main()
