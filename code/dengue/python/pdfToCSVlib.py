# Use 'pdfToTableFromCellBounds(filepath)' 
from __future__ import division
import math
import random
import pdfminer
from matplotlib import patches
from collections import defaultdict
import collections
from pdfminer.pdfparser import PDFParser
from pdfminer.pdfdocument import PDFDocument
from pdfminer.pdfpage import PDFPage
from pdfminer.pdfpage import PDFTextExtractionNotAllowed
from pdfminer.pdfinterp import PDFResourceManager
from pdfminer.pdfinterp import PDFPageInterpreter
from pdfminer.layout import LAParams
from pdfminer.converter import PDFPageAggregator
import matplotlib.pyplot as plt
import matplotlib as mpl
# warning: pdfminer uses python 2

from itertools import groupby
import sys
import os
import csv
from collections import Counter
import logging

# Elements of PDF page that text can be found in.
TEXT_ELEMENTS = [
    pdfminer.layout.LTTextBox,
    pdfminer.layout.LTTextBoxHorizontal,
    pdfminer.layout.LTTextLine,
    pdfminer.layout.LTTextLineHorizontal
]

# Custom classes so that custom functions can be applied to PDF elements.
class MyLTChar:
    def __init__(self, LTChar):
        self.bbox = LTChar.bbox
        self.x0 = LTChar.x0
        self.y0 = LTChar.y0
        self.x1 = LTChar.x1
        self.y1 = LTChar.y1
        self.matrix = LTChar.matrix
        self.text = LTChar.get_text()
        self.height = LTChar.height
        self.width = LTChar.width
        self.upright = LTChar.upright

class MyLTTextLine:
    def __init__(self, LTTextLine = None, bbox = None, x0 = None, y0 = None, x1 = None, y1 = None, text = None, height = None, width = None,
            chars = None, all_elements = None):
        if LTTextLine is not None:
            self.LTTextLine = LTTextLine
            self.bbox = LTTextLine.bbox
            self.x0 = LTTextLine.x0
            self.y0 = LTTextLine.y0
            self.x1 = LTTextLine.x1
            self.y1 = LTTextLine.y1
            self.text = LTTextLine.get_text()
            self.height = LTTextLine.height
            self.width = LTTextLine.width
            self.chars = [MyLTChar(c) for c in _extract_characters(LTTextLine)]
            self.all_elements = [el for el in LTTextLine]
        else:
            self.LTTextLine = LTTextLine
            self.bbox = bbox
            self.x0 = x0
            self.y0 = y0
            self.x1 = x1
            self.y1 = y1
            self.text = text
            self.height = height
            self.width = width
            self.chars = chars
            self.all_elements = all_elements

    def getInfo(self):
        return str(self.bbox) + ' ' + self.text

    def split(self, index):
            """
            # Splits self into two MyLTTextLine's.
            #
            # Args:
            #   index: the index of the first MyLTChar in the second subdivision.
            #
            # Return:
            #   MyLine_1, MyLine2: two MyLTTextLine objects divided from self.
            """
            if index == 0:
                x0 = self.x0
                y0 = self.y0
                x1 = self.x1
                y1 = self.y1
                bbox = (x0, y0, x1, y1)
                height = x1 - x0
                width = y1 - y0
                all_elements = self.all_elements
                chars = [MyLTChar(c) for c in _extract_characters(all_elements)]
                text = "".join([e.text for e in chars])
                MyLine = MyLTTextLine(bbox = bbox, x0 = x0, y0 = y0, x1 = x1, y1 = y1, text = text, height =
                        height, width = width, chars = chars, all_elements = all_elements)
                return MyLine, None

            if isinstance(self.all_elements[index], pdfminer.layout.LTChar):
                begin_2 = self.all_elements[index]
            else:
                i = index + 1
                while not isinstance(self.all_elements[i], pdfminer.layout.LTChar):
                    i += 1
                begin_2 = self.all_elements[i]
            end_1 = self.all_elements[index - 1]

            x0_2 = begin_2.x0
            y0_2 = begin_2.y0
            x1_2 = self.x1
            y1_2 = self.y1
            bbox_2 = (x0_2, y0_2, x1_2, y1_2)
            height_2 = x1_2 - x0_2
            width_2 = y1_2 - y0_2
            all_elements_2 = self.all_elements[index:]
            chars_2 = [MyLTChar(c) for c in _extract_characters(all_elements_2)]
            text_2 = "".join([e.text for e in chars_2])
            MyLine_2 = MyLTTextLine(bbox = bbox_2, x0 = x0_2, y0 = y0_2, x1 = x1_2, y1 = y1_2, text = text_2, height =
                    height_2, width = width_2, chars = chars_2, all_elements = all_elements_2)

            x0_1 = self.x0
            y0_1 = self.y0
            x1_1 = end_1.x1
            y1_1 = end_1.y1
            bbox_1 = (x0_1, y0_1, x1_1, y1_1)
            height_1 = x1_1 - x0_1
            width_1 = y1_1 - y0_1
            all_elements_1 = self.all_elements[:index]
            chars_1 = [MyLTChar(c) for c in _extract_characters(all_elements_1)]
            text_1 = "".join([e.text for e in chars_1])
            MyLine_1 = MyLTTextLine(bbox = bbox_1, x0 = x0_1, y0 = y0_1, x1 = x1_1, y1 = y1_1, text = text_1, height =
                    height_1, width = width_1, chars = chars_1, all_elements = all_elements_1)

            return MyLine_1, MyLine_2
    def nonCharIndex(self):
        """ 
        # Finds the index of the first non-LTChar.
        #
        # Return:
        #   the index of the first non-LTChar, or 0 if none are present.
        """
        all_elements = self.all_elements
        i = 0  # all_elements index
        for e in all_elements:
            if not isinstance(e, pdfminer.layout.LTChar):
                if not (i == 0 or i == len(all_elements) - 1):
                    # Only count if it's in the middle (it actually creates a meaningful subdivision).
                    return i
            i += 1
        return 0
    def overlapIndex(self):
        """
        # Finds the index of where to split a MyLTLine by overlapping characters.
        #
        # Return:
        #   the index of the first overlapping MyLTChar, or 0 if none are present.
        """
        i = 0
        cur_x = 0
        for c in self.chars:
            prev_x = cur_x
            cur_x = c.x0
            if cur_x <= prev_x:
                return i
            i += 1
        return 0
    def splitNonChar(self):
        """
        # Splits a MyLTTextLine if there are non-LTChar elements present.
        # 
        # Pdfminer might use other elements as logical separators within a cell.
        #
        # Return:
        #   split1: a list of MyLTTextLine(s) that have been split and thus have only LTChars.
        """
        split1, split2 =  self.split(self.nonCharIndex())
        split1 = [split1]
        if split2 is not None:
            split1.extend(split2.splitNonChar())
        return split1
    def splitOverlap(self):
        """
        # Splits a MyLTTextLine if there are overlapping characters/cells.
        # 
        # Cells too narrow for their text content might have remaining text go 'underneath' the next cell,
        # causing PDFminer to consider them the same cell.
        #
        # Return:
        #   split1: a list of MyLTTextLine(s) that are not overlapping.
        """
        split1, split2 =  self.split(self.overlapIndex())
        split1 = [split1]
        if split2 is not None:
            split1.extend(split2.splitOverlap())
        return split1
    def splitAll(self):
        """
        # Splits a MyLTTextLine recursively into multiple MyLTTextLines. It is split by:
        #   1. Every non-LTChar element.
        #   2. Overlapping LTChars.
        #
        # Return:
        #   split: a list of MyLTTextLine(s).
        """
        non_char_split = self.splitNonChar()
        split = []
        for s in non_char_split:
            split.extend(s.splitOverlap())
        return split

class Delta:
    """
    # Use to group elements in a sorted list based on a threshold.
    # Use with itertools.groupby()
    """
    def __init__(self, delta):
        self.last = None
        self.delta = delta
        self.key = 1
    def __call__(self, value):
        if self.last is not None and abs(self.last - value) > self.delta:
            # Compare with the last value (`self.last`)
            # If difference is larger than delta, advance to
            # next group
            self.key += 1
        self.last = value  # Remeber the last value.
        return self.key

def _extract_layout_by_page(pdf_path):
    """
    # Extracts LTPage objects from a pdf file.
    #
    # slightly modified from
    # https://euske.github.io/pdfminer/programming.html
    """
    laparams = LAParams()
    fp = open(pdf_path, 'rb')
    parser = PDFParser(fp)
    document = PDFDocument(parser)

    # Check if the document allows text extraction. If not, abort.
    if not document.is_extractable:
        raise PDFTextExtractionNotAllowed

    rsrcmgr = PDFResourceManager()
    device = PDFPageAggregator(rsrcmgr, laparams=laparams)
    interpreter = PDFPageInterpreter(rsrcmgr, device)

    layouts = []
    for page in PDFPage.create_pages(document):
        interpreter.process_page(page)
        layouts.append(device.get_result())

    return layouts
def flatten1(lst):
    """Flattens a list"""
    return [elem for elem in lst]
def flatten2(lst):
    """Flattens a list of lists"""
    return [subelem for elem in lst for subelem in elem]
def _extract_characters(element):
    """
    # Recursively extracts individual characters from 
    # text elements. 
    """
    if isinstance(element, pdfminer.layout.LTChar):
        return [element]

    if any(isinstance(element, i) for i in TEXT_ELEMENTS):
        return flatten2([_extract_characters(e) for e in element])

    if isinstance(element, list):
        return flatten2([_extract_characters(l) for l in element])

    return []
def _extract_lines(element):
    """
    # Recursively extracts individual lines from 
    # text elements. 
    """
    if isinstance(element, pdfminer.layout.LTTextLine):
        return [element]

    if any(isinstance(element, i) for i in TEXT_ELEMENTS):
        return flatten2([_extract_lines(e) for e in element])

    if isinstance(element, list):
        return flatten2([_extract_lines(l) for l in element])

    return []
def _width(rect):
    x0, y0, x1, y1 = rect.bbox
    return min(x1 - x0, y1 - y0)
def _area(rect):
    x0, y0, x1, y1 = rect.bbox
    return (x1 - x0) * (y1 - y0)
def _cast_as_line(rect):
    """
    # Replaces a retangle with a line based on its longest dimension.
    """
    x0, y0, x1, y1 = rect.bbox

    if x1 - x0 > y1 - y0:
        return (x0, y0, x1, y0, "H")
    else:
        return (x0, y0, x0, y1, "V")
def _does_it_intersect(x, (xmin, xmax)):
    return (x <= xmax and x >= xmin)
def _find_bounding_rectangle((x, y), lines):
    """
    # Given a collection of lines, and a point, try to find the rectangle 
    # made from the lines that bounds the point. If the point is not 
    # bounded, return None.
    """
    v_intersects = [l for l in lines
                    if l[4] == "V"
                    and _does_it_intersect(y, (l[1], l[3]))]
    h_intersects = [l for l in lines
                    if l[4] == "H"
                    and _does_it_intersect(x, (l[0], l[2]))]
    if len(v_intersects) < 2 or len(h_intersects) < 2:
        return None
    v_left = [v[0] for v in v_intersects if v[0] < x]
    v_right = [v[0] for v in v_intersects if v[0] > x]
    if len(v_left) == 0 or len(v_right) == 0:
        return None
    x0, x1 = max(v_left), min(v_right)
    h_down = [h[1] for h in h_intersects if h[1] < y]
    h_up = [h[1] for h in h_intersects if h[1] > y]
    if len(h_down) == 0 or len(h_up) == 0:
        return None
    y0, y1 = max(h_down), min(h_up)
    return (x0, y0, x1, y1)
def _chars_to_string(chars):
    """
    # Converts a collection of characters into a string, by ordering them left to right, 
    # then top to bottom.
    """
    if not chars:
        return ""
    rows = sorted(list(set(c.bbox[1] for c in chars)), reverse=True)
    text = ""
    for row in rows:
        sorted_row = sorted([c for c in chars if c.bbox[1] == row], key=lambda c: c.bbox[0])
        text += "".join(c.get_text() for c in sorted_row)
    return text
def _boxes_to_table(box_record_dict, ):
    """
    # Converts a dictionary of cell:characters mapping into a python list
    # of lists of strings. Tries to split cells into rows, then for each row 
    # breaks it down into columns.
    """
    boxes = box_record_dict.keys()
    rows = sorted(list(set(b[1] for b in boxes)), reverse=True)
    table = []
    for row in rows:
        sorted_row = sorted([b for b in boxes if b[1] == row], key=lambda b: b[0])
        table.append([_chars_to_string(box_record_dict[b]) for b in sorted_row])
    return table

def draw_rect_bbox((x0,y0,x1,y1), ax, color):
    """
    Draws an unfilled rectable onto ax.
    """
    ax.add_patch(
        patches.Rectangle(
            (x0, y0),
            x1 - x0,
            y1 - y0,
            fill=False,
            color=color
        )
     )

def draw_rect(rect, ax, color="black"):
    draw_rect_bbox(rect.bbox, ax, color)

def pdfPageToTableByCellBounds(page):
    """ 
    # Use cell boundary lines in pdf table to make a table of Strings. 
    #
    # Arguments:
    #   page: a page layout from a PDF to be read and converted.
    #
    # Return:
    #   string_table: a 2d list of Strings that correspond to the table contents of the PDF page.
    """
    texts = []
    rects = []

    # seperate text and rectangle elements
    for e in page:
        if isinstance(e, pdfminer.layout.LTTextBoxHorizontal):
            texts.append(e)
        elif isinstance(e, pdfminer.layout.LTRect):
            rects.append(e)

    # Get all LTChars
    characters = _extract_characters(texts)

    xmin, ymin, xmax, ymax = page.bbox
    lines = [_cast_as_line(r) for r in rects
                     if _width(r) < 3 and
                              _area(r) > 1]


    
    ## Logging / Debugging ##
    for r in rects:
        logging.debug(r)
    xmin, ymin, xmax, ymax = page.bbox
    size = 6
    fig, ax = plt.subplots(figsize = (size, size * (ymax/xmax)))
    for l in lines:
        x0,y0,x1,y1,_ = l
        plt.plot([x0, x1], [y0, y1], 'k-')
    plt.xlim(xmin, xmax)
    plt.ylim(ymin, ymax)
    plt.savefig('lines.png')

    fig, ax = plt.subplots(figsize = (size, size * (ymax/xmax)))
    for rect in rects:
        draw_rect(rect, ax)
    plt.xlim(xmin, xmax)
    plt.ylim(ymin, ymax)
    plt.savefig('LTRects.png')

    fig, ax = plt.subplots(figsize = (size, size * (ymax/xmax)))
    for rect in rects:
        draw_rect(rect, ax)
    for c in characters:
        draw_rect(c, ax, "red")
    plt.xlim(xmin, xmax)
    plt.ylim(ymin, ymax)
    plt.savefig('LTRects-with-LTChars.png')
    #####

    box_char_dict = {}

    for c in characters:
        # choose the bounding box that occurs the majority of times for each of these:
        bboxes = defaultdict(int)
        l_x, l_y = c.bbox[0], c.bbox[1]
        bbox_l = _find_bounding_rectangle((l_x, l_y), lines)
        bboxes[bbox_l] += 1

        c_x, c_y = math.floor((c.bbox[0] + c.bbox[2]) / 2), math.floor((c.bbox[1] + c.bbox[3]) / 2)
        bbox_c = _find_bounding_rectangle((c_x, c_y), lines)
        bboxes[bbox_c] += 1

        u_x, u_y = c.bbox[2], c.bbox[3]
        bbox_u = _find_bounding_rectangle((u_x, u_y), lines)
        bboxes[bbox_u] += 1

        # if all values are in different boxes, default to character center.
        # otherwise choose the majority.
        if max(bboxes.values()) == 1:
            bbox = bbox_c
        else:
            bbox = max(bboxes.items(), key=lambda x: x[1])[0]

        if bbox is None:
            continue

        if bbox in box_char_dict.keys():
            box_char_dict[bbox].append(c)
            continue

        box_char_dict[bbox] = [c]

    for x in range(int(xmin), int(xmax), 10):
        for y in range(int(ymin), int(ymax), 10):
            bbox = _find_bounding_rectangle((x, y), lines)

            if bbox is None:
                continue

            if bbox in box_char_dict.keys():
                continue

            box_char_dict[bbox] = []

    string_table = _boxes_to_table(box_char_dict)
    return string_table

def pdfPageToTableByCoords(page):
    """ 
    # Use coordinates of LTChars and LTTextLines to make a table of Strings.
    #
    # Arguments:
    #    page: a page layout from a PDF to be read and converted.
    #
    # Return:
    #   string_table: a 2d list of Strings that correspond to the table contents of the PDF page.
    """
    texts = []
    # seperate text elements
    for e in page:
        if isinstance(e, pdfminer.layout.LTTextBoxHorizontal):
            texts.append(e)

    # separate into lines and make them "MyLTTextLine" so that I can apply my functions to them.
    # Note: each cell of the PDF is considered an LTTextLine.
    lines = _extract_lines(texts)
    my_lines = []
    for l in lines:
        my_lines.append(MyLTTextLine(l))

    # Split overlapping, concatenated cells into two.
    split_lines = []
    for l in my_lines:
        split_lines.extend(l.splitAll())

    # Sort cells by row so that itertools.groupby can group them.
    sorted_lines = sorted(split_lines, key=lambda line: -line.y0)
    # Group lines based on row (y0). Use a threshold of 5
    # since coordinates in the same row can differ slightly.
    table_rows = []
    keyfunc = Delta(5)
    for key, grp in groupby(sorted_lines, key=lambda line: keyfunc(line.y0)):
        table_rows.append(list(grp))

    # Sort each row by column.
    sorted_table = []
    for row in table_rows:
        sorted_row = sorted(row, key=lambda line: line.x0)
        sorted_table.append(sorted_row)

    # Logging...
    for row in sorted_table:
        logging.debug("START ROW")
        for line in row:
            logging.debug('%s', line.getInfo())
        logging.debug("END ROW")

    # Remove text that doesn't belong in the table.
    # (i.e. it doesn't have the correct number of columns)
    rect_table = makeTableRectangular(sorted_table)

    # Make MyLTTextLine table into String table.
    string_table = []
    for row in rect_table:
        string_table.append([])
        for cell in row:
            string_table[len(string_table) - 1].append(cell.text)

    return string_table   
    
def stringTableToCSV(lis, out_path):
    """ 
    # Creates a CSV file from a 2 dimensional list of Strings.
    #
    # Arguments:
    #    lis: the 2d list of Strings.
    #    out_path: the path of the file to be saved.
    """
    csv_out = open(out_path, 'w')
    wr = csv.writer(csv_out, quoting=csv.QUOTE_ALL)
    for row in lis:
        wr.writerow([unicode(s).encode("utf-8") for s in row])
    csv_out.close()
    print "CSV created: " + out_path

def makeTableRectangular(table):
    """
    # Makes a jagged table rectangular by finding the mode
    # of the row lengths and returning those rows.
    #
    # When extracting tables from PDFs, it is common to need to 
    # throw out unnecessary PDF content. Since tables are rectangular,
    # it can be assumed that "rows" that are too short or long belong
    # to this excess data.
    #
    # Arguments:
    #    table: a jagged table
    #
    # Return:
    #    rect_table: a table with all equal row lengths.
    """
    if len(table) == 0:
        logging.warning("Table is empty!")
        return [] 
    row_lengths = []
    for row in table:
        row_lengths.append(len(row))
    counts = Counter(row_lengths)
    ncol = counts.most_common(1)[0][0]
    rect_table = []
    for row in table:
        if len(row) == ncol:
            rect_table.append(row)
    return rect_table

def checkEqual(iterator):
    iterator = iter(iterator)
    try:
        first = next(iterator)
    except StopIteration:
        return True
    return all(first == rest for rest in iterator)

def pdfToTable(pdf_path, by_bounds=False, by_coords=False, sep_pages=False):
    """ 
    # Converts a PDF to a String table.
    #
    # Arguments:
    #   pdf_path: the path to a PDF file.
    #   BY_CELL_BOUNDS = boolean, how to distinguish cells.
    #   BY_COORDS = boolean, how to distinguish cells.
    #   VERBOSE: display helpful info while converting PDF.
    #
    # Return:
    #   a list of 2d String tables. Each table is independent.
    """
    # Mutually exclusive parameters
    if by_bounds == by_coords:
        logging.warning("error: pdfToTable(): One of by_cell_bounds and by_coords must be true and the other false.")
        sys.exit(1)
    # Get PDF elements from PDF file.
    page_layouts = _extract_layout_by_page(pdf_path)
    # Convert layouts into a String table.
    all_pgs_sep = []
    for i in range(0,len(page_layouts)):
        page_height = page_layouts[i].height
        page_width = page_layouts[i].width
        if by_bounds:
            all_pgs_sep.append(pdfPageToTableByCellBounds(page_layouts[i]))
        elif by_coords:
            all_pgs_sep.append(pdfPageToTableByCoords(page_layouts[i]))
    
    # Each page of PDF is a different CSV.
    if sep_pages:
        return all_pgs_sep
    # Every page of PDF belongs to the same CSV.
    all_pgs_com = []
    for page in all_pgs_sep:
        all_pgs_com.extend(page)
    # Remove pages of wrong length
    rect = makeTableRectangular(all_pgs_com)
    return [rect] # Make sure to return in list form even though there's only one table.
