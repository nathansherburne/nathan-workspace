import os
import cv2
import numpy as np
import argparse
import thinning

def main():
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('-i', '--input', nargs='+', help='input image of single contour of interest')
    parser.add_argument('-d', '--draw', help='image to draw coords on')
    parser.add_argument('-o', '--output', help='output directory')
    args = parser.parse_args()

    for p_input in args.input:
        gray = cv2.imread(p_input, 0)
        draw1 = cv2.imread(args.draw)
        draw2 = cv2.imread(p_input)

        ret,thresh = cv2.threshold(gray,1,255,cv2.THRESH_BINARY)
        
        thresh = _append_outer_border(thresh, 0)  # So the thinning function can handle edge case.
        thin = thinning.thinning(thresh)
        thin = _remove_outer_border(thin)  # So the thinning function can handle edge case.

        # If the line was too thin to begin with, there might be disconnected pixels.
        kernel = np.ones((3,3),np.uint8)
        thin = cv2.morphologyEx(thin, cv2.MORPH_CLOSE, kernel)

        ret,contours,hierarchy = cv2.findContours(thin, 1, 2)

        # Output text file with coords of edge contour.
        fname = os.path.splitext(os.path.basename(p_input))[0]
        p_coords = os.path.join(args.output, fname + '_coords.txt')
        f = open(p_coords, 'w')
        f.write("x,y\n")
        line_frm = "%d,%d\n"

        # Output image with contour points drawn.
        p_draw1 = os.path.join(args.output, fname + '_draw1.png')
        p_draw2 = os.path.join(args.output, fname + '_draw2.png')
        p_thin = os.path.join(args.output, fname + '_thin.png')
        p_dil = os.path.join(args.output, fname + '_dil.png')

        if len(contours) == 1:
            cnt = contours[0]

            epsilon = 0.001*cv2.arcLength(cnt,True)
            approx = cv2.approxPolyDP(cnt,epsilon,True)

            for coord in approx:
                x, y = coord[0]
                cv2.circle(draw1, (x, y), 1, (0,0,255), -1)
                cv2.circle(draw2, (x, y), 1, (0,0,255), -1)
                f.write(line_frm % (x, y))

            f.close()
            cv2.imwrite(p_draw1, draw1)
            cv2.imwrite(p_draw2, draw2)
        else:
            print p_input + ": wrong number of contours! See _thin.png"

        cv2.imwrite(p_thin, thin)

def _append_outer_border(img, val = 255):
    """Adds a 1-pixel wide border around an image.

    Useful for turning penninsulas on the edge of a binary image into
    islands.
    
    Args:
        img: the binary/grayscale image to add the border to.
        val: the value of the border.

    Returns:
        a binary image with a 1-pixel wide border appended to it. It will be
        2 pixels taller and 2 pixels wider than the input binary image.
    """
    ht, wd = img.shape[:2]
    row = np.ones((1,wd), dtype=np.uint8)
    row *= val
    img = cv2.vconcat([row, img, row])
    col = np.ones((ht+2,1), dtype=np.uint8)  # +2 for new rows.
    col *= val
    img = cv2.hconcat([col, img, col])
    return img

def _remove_outer_border(img):
    """Removes a 1-pixel wide border from the image.

    Used after append_outer_border().
    
    Args:
        img: the binary/grayscale image to remove the border from.

    Returns:
        a binary image with a 1-pixel wide border removed from it. It will 
        be 2 pixels shorter and 2 narrower than the input binary image.
    """
    ht, wd = img.shape[:2]
    img = img[1:ht-1,1:wd-1]
    return img

if __name__ == "__main__":
    main()
