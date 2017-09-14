/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package processing;

import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import com.google.common.collect.TreeBasedTable;

import data_structures.MyGridSet;

/**
 * Allows text and graphics to be processed in a custom manner. The reason
 * PDFGraphicsStreamEngine is extended is solely to use the "appendRectangle()"
 * method. This method allows us to extract the points which define the
 * rectangles and lines of a PDF page.
 * 
 * These points are used to generate lines, which are analyzed to generate more
 * points based on their intersections with one another. Once all of these
 * points are generated, they can be used to create rectangles. These rectangles
 * can be used to find the text within them.
 * 
 *
 * <p>
 * See {@link PDFStreamEngine} for further methods which may be overridden.
 * 
 * @author Nathan Sherburne
 */
public class CustomGraphicsStreamEngine extends PDFGraphicsStreamEngine {
	private MyLines my_lines;
	private double line_threshold;
	TreeBasedTable<Double, Double, String> table;
	TreeSet<MyPoint> grid;
	BufferedImage image;
	Graphics g2d;

	/**
	 * Constructor.
	 *
	 * @param page
	 *            PDF Page
	 */
	public CustomGraphicsStreamEngine(PDPage page) {
		super(page);
		my_lines = new MyLines();
		table = TreeBasedTable.create();
		grid = new TreeSet<MyPoint>();
		image = new BufferedImage((int) page.getCropBox().getWidth(), (int) page.getCropBox().getHeight(),
				BufferedImage.TYPE_INT_RGB);
		g2d = image.getGraphics();
		line_threshold = 2.0;
	}

	public enum DIRECTION {
		ABOVE, BELOW, LEFT, RIGHT;
	}

	public class MyPoint implements Comparable<MyPoint> {
		private double x;
		private double y;
		private Point2D translated_point;
		private EnumSet<DIRECTION> neighboors;

		public MyPoint(double x, double y) {
			this.x = x;
			this.y = y;
			translated_point = new Point2D.Double(x, getPageHeight() - y);
			neighboors = EnumSet.noneOf(DIRECTION.class);
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public void addNeighboor(DIRECTION d) {
			neighboors.add(d);
		}

		public EnumSet<DIRECTION> getNeighboors() {
			return neighboors;
		}

		/**
		 * PDF origin (0,0) is in the lower-left corner. So when performing functions
		 * that assume an upper-left corner origin (such as drawing), use the translated
		 * y-value instead of getY().
		 * 
		 * @return The "flipped" y-value, based on the page's crop box height.
		 */
		public double getTranslatedY() {
			return translated_point.getY();
		}

		@Override
		public int compareTo(MyPoint p) {
			if (this.getY() < p.getY()) {
				return 1;
			} else if (this.getY() > p.getY()) {
				return -1;
			} else if (this.getX() < p.getX()) {
				return -1;
			} else if (this.getX() > p.getX()) {
				return 1;
			}
			return 0;
		}

		@Override
		public String toString() {
			return "[x: " + getX() + ", y: " + getY() + "]";
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (!(o instanceof MyPoint)) {
				return false;
			}
			final MyPoint p = (MyPoint) o;
			if (Double.compare(p.getX(), this.getX()) == 0 && Double.compare(p.getY(), this.getY()) == 0) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Double.hashCode(this.getX()) * 13 + Double.hashCode(this.getY()) * 53;
		}

	}

	public class MyLine implements Comparable<MyLine> {
		protected Line2D line;

		public MyLine(Line2D line) {
			double x1 = line.getX1() < line.getX2() ? line.getX1() : line.getX2();
			double y1 = line.getY1() < line.getY2() ? line.getY1() : line.getY2();
			double x2 = line.getX1() < line.getX2() ? line.getX2() : line.getX1();
			double y2 = line.getY1() < line.getY2() ? line.getY2() : line.getY1();
			this.line = new Line2D.Double(x1, y1, x2, y2);

		}

		public Line2D getLine() {
			return line;
		}

		private double getMaxX() {
			return line.getX1() >= line.getX2() ? line.getX1() : line.getX2();
		}

		private double getMaxY() {
			return line.getY1() >= line.getY2() ? line.getY1() : line.getY2();
		}

		private double getMinX() {
			return line.getX1() <= line.getX2() ? line.getX1() : line.getX2();
		}

		private double getMinY() {
			return line.getY1() <= line.getY2() ? line.getY1() : line.getY2();
		}

		public boolean isHorizontal() {
			return getMaxY() == getMinY();
		}

		public boolean isVertical() {
			return getMaxX() == getMinX();
		}

		/**
		 * 
		 * @param line
		 * @return True if the given line is nearby and parallel to the calling line.
		 *         For a definition of "nearby", see getLineThreshold().
		 */
		public boolean isAdjacent(MyLine line) {
			return false;
		}

		@Override
		public String toString() {
			return "[(x1: " + getMinX() + ", y1: " + getMaxY() + "), (x2: " + getMaxX() + ", y2: " + getMinY() + ")]";
		}

		@Override
		public int compareTo(MyLine l) {
			// Sort up and down, then left and right. Y-coordinates are flipped.
			if (this.getMaxY() < l.getMaxY()) {
				return 1;
			} else if (this.getMaxY() > l.getMaxY()) {
				return -1;
			} else if (this.getMinX() < l.getMinX()) {
				return -1;
			} else if (this.getMinX() > l.getMinX()) {
				return 1;
			}
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof MyLine))
				return false;
			MyLine l = (MyLine) o;
			return Double.compare(this.getMaxX(), l.getMaxX()) == 0 && Double.compare(this.getMinX(), l.getMinX()) == 0
					&& Double.compare(this.getMaxY(), l.getMaxY()) == 0
					&& Double.compare(this.getMinY(), l.getMinY()) == 0;
		}

		@Override
		public int hashCode() {
			return line.hashCode();
		}
	}

	public class MyVerticalLine extends MyLine {

		public MyVerticalLine(Line2D line) {
			super(line);
		}

		public double getMinY() {
			return line.getY1() <= line.getY2() ? line.getY1() : line.getY2();
		}

		public double getMaxY() {
			return line.getY1() >= line.getY2() ? line.getY1() : line.getY2();
		}

		public double getX() {
			return line.getX1();
		}

		public double getLength() {
			return getMaxY() - getMinY();
		}

		/**
		 * 
		 * @param x
		 * @param y
		 * @return true if the given point with the line. "Within" is fuzzy--see
		 *         getLineThreshold().
		 */
		public boolean contains(double x, double y) {
			return y >= this.getMinY() - getLineThreshold() && y <= this.getMaxY() + getLineThreshold()
					&& Math.abs(x - this.getX()) <= getLineThreshold();
		}

		/**
		 * 
		 * @param l
		 * @return true if the given line is within the line. "Within" is fuzzy--see
		 *         getLineThreshold().
		 */
		public boolean contains(MyVerticalLine l) {
			return this.contains(l.getX(), l.getMaxY()) && this.contains(l.getX(), l.getMinY());
		}

		/**
		 * Creates one line segment out of two that are in line with one anther.
		 * 
		 * @param l
		 * @return The merged line segment, or null if they are not aligned.
		 */
		public MyVerticalLine merge(MyVerticalLine l) {
			if (Double.compare(this.getX(), l.getX()) != 0) {
				return null;
			}
			double x = this.getX();
			double minY = this.getMinY() < l.getMinY() ? this.getMinY() : l.getMinY();
			double maxY = this.getMaxY() < l.getMaxY() ? this.getMaxY() : l.getMaxY();
			return new MyVerticalLine(new Line2D.Double(x, minY, x, maxY));
		}

		@Override
		public boolean isAdjacent(MyLine line) {
			if (!(line instanceof MyVerticalLine)) {
				return false;
			}
			MyVerticalLine v_line = (MyVerticalLine) line;
			return this.contains(v_line) || v_line.contains(this);
		}

		@Override
		public int compareTo(MyLine l) {
			if (this.getX() < l.getMinX()) {
				return -1;
			} else if (this.getX() > l.getMinX()) {
				return 1;
			} else if (this.getMaxY() < l.getMaxY()) {
				return 1;
			} else if (this.getMaxY() > l.getMaxY()) {
				return -1;
			}
			return 0;
		}
	}

	public class MyHorizontalLine extends MyLine {

		public MyHorizontalLine(Line2D line) {
			super(line);
		}

		public double getMaxX() {
			return line.getX1() >= line.getX2() ? line.getX1() : line.getX2();
		}

		public double getMinX() {
			return line.getX1() <= line.getX2() ? line.getX1() : line.getX2();
		}

		public double getY() {
			return line.getY1();
		}

		public double getLength() {
			return getMaxX() - getMinX();
		}

		public boolean contains(double x, double y) {
			return x >= this.getMinX() - getLineThreshold() && x <= this.getMaxX() + getLineThreshold()
					&& Math.abs(y - this.getY()) <= getLineThreshold();
		}

		/**
		 * Creates one line segment out of two that are in line with one anther.
		 * 
		 * @param l
		 * @return The merged line segment, or null if they are not aligned.
		 */
		public MyHorizontalLine merge(MyHorizontalLine l) {
			if (Double.compare(this.getY(), l.getY()) != 0) {
				return null;
			}
			double y = this.getY();
			double min_x = this.getMinX() < l.getMinX() ? this.getMinX() : l.getMinX();
			double max_x = this.getMaxX() < l.getMaxX() ? this.getMaxX() : l.getMaxX();
			return new MyHorizontalLine(new Line2D.Double(min_x, y, max_x, y));
		}

		/**
		 * 
		 * @param v
		 * @return the intersection point if the lines intersect, null if they don't.
		 */
		public MyPoint getIntersection(MyVerticalLine v) {
			EnumSet<DIRECTION> neighboors;
			if ((neighboors = this.intersects(v)).size() != 0) {
				MyPoint p = new MyPoint(v.getX(), this.getY());
				for (DIRECTION d : neighboors) {
					p.addNeighboor(d);
				}
				return p;
			}
			return null;
		}

		/**
		 * 
		 * @param v
		 * @return True if the vertical line intersects the horizontal line by which
		 *         this method was called.
		 */
		public EnumSet<DIRECTION> intersects(MyVerticalLine v) {
			EnumSet<DIRECTION> neighboors = EnumSet.noneOf(DIRECTION.class);
			// The horizontal line is within vertical and vertical is within horizontal.
			if (this.getY() < v.getMaxY() + getLineThreshold() && this.getY() > v.getMinY() - getLineThreshold()
					&& v.getX() < this.getMaxX() + getLineThreshold()
					&& v.getX() > this.getMinX() - getLineThreshold()) {
				// The lines intersect somewhere.
				if (this.getY() > v.getMaxY() - getLineThreshold()) {
					// Horizontal line intersects top of vertical line.
					neighboors.add(DIRECTION.BELOW);
				} else if (this.getY() < v.getMinY() + getLineThreshold()) {
					// Horizontal line intersects bottom of vertical line.
					neighboors.add(DIRECTION.ABOVE);
				} else {
					// Horizontal line intersects midpoint of vertical line.
					neighboors.add(DIRECTION.BELOW);
					neighboors.add(DIRECTION.ABOVE);
				}
				if (v.getX() < this.getMinX() + getLineThreshold()) {
					// Vertical line intersects left of horizontal line.
					neighboors.add(DIRECTION.RIGHT);
				} else if (v.getX() > this.getMaxX() - getLineThreshold()) {
					// Vertical line intersects right of horizontal line.
					neighboors.add(DIRECTION.LEFT);
				} else {
					// Vertical line intersects midpoint of horizontal line
					neighboors.add(DIRECTION.RIGHT);
					neighboors.add(DIRECTION.LEFT);
				}
			}
			return neighboors;
		}

		@Override
		public boolean isAdjacent(MyLine line) {
			if (!(line instanceof MyHorizontalLine)) {
				return false;
			}
			MyHorizontalLine h_line = (MyHorizontalLine) line;
			return Double.compare(Math.abs(this.getY() - h_line.getY()), getLineThreshold()) < 0;
			// && Double.compare(this.getLength(), h_line.getLength()) == 0;
		}
	}

	public class MyLines {
		TreeSet<MyHorizontalLine> horizontal;
		TreeSet<MyVerticalLine> vertical;

		public MyLines() {
			horizontal = new TreeSet<MyHorizontalLine>();
			vertical = new TreeSet<MyVerticalLine>();
		}

		public boolean add(MyLine line) {
			if (line.isHorizontal()) {
				horizontal.add(new MyHorizontalLine(line.getLine()));
				return true;
			} else if (line.isVertical()) {
				vertical.add(new MyVerticalLine(line.getLine()));
				return true;
			}
			return false;
		}

		public TreeSet<MyHorizontalLine> getHorizontalLines() {
			return horizontal;
		}

		public TreeSet<MyVerticalLine> getVerticalLines() {
			return vertical;
		}

		/**
		 * Merges all vertical line segments with the same x coordinate.
		 */
		public void mergeVertical() {
			TreeSet<MyVerticalLine> tree = new TreeSet<MyVerticalLine>();
			Iterator<MyVerticalLine> iterator = getVerticalLines().iterator();
			MyVerticalLine prev = null, cur = null, merged_line = null;
			if (iterator.hasNext()) {
				cur = iterator.next();
				merged_line = cur;
			}
			while (iterator.hasNext()) {
				prev = cur;
				cur = iterator.next();
				if (Double.compare(prev.getX(), cur.getX()) == 0) {
					// merge
					merged_line = merged_line.merge(cur);
					if (!iterator.hasNext()) {
						tree.add(merged_line); // Add last element(s) edge case.
					}
				} else {
					tree.add(merged_line);
					merged_line = cur;
				}
			}
			vertical = tree;
		}

		/**
		 * Merges all vertical line segments with the same x coordinate.
		 */
		public void mergeHorizontal() {
			TreeSet<MyHorizontalLine> tree = new TreeSet<MyHorizontalLine>();
			Iterator<MyHorizontalLine> iterator = getHorizontalLines().iterator();
			MyHorizontalLine prev = null, cur = null, merged_line = null;
			if (iterator.hasNext()) {
				cur = iterator.next();
				merged_line = cur;
			}
			while (iterator.hasNext()) {
				prev = cur;
				cur = iterator.next();
				if (Double.compare(prev.getY(), cur.getY()) == 0) {
					// merge
					merged_line = merged_line.merge(cur);
					if (!iterator.hasNext()) {
						tree.add(merged_line); // Add last element(s) edge case.
					}
				} else {
					tree.add(merged_line);
					merged_line = cur;
				}
			}
			horizontal = tree;
		}

		/**
		 * Merges vertical lines that are adjacent to one another, creating a new line
		 * with the average of the x coordinates and the longest line's length.
		 */
		public void averageVertical() {
			TreeSet<MyVerticalLine> tree = new TreeSet<MyVerticalLine>();
			Iterator<MyVerticalLine> iterator = getVerticalLines().iterator();
			MyVerticalLine prev = null, cur = null;
			TreeSet<MyVerticalLine> to_average = new TreeSet<MyVerticalLine>();
			if (iterator.hasNext()) {
				cur = iterator.next();
				to_average.add(cur);
			}
			while (iterator.hasNext()) {
				prev = cur;
				cur = iterator.next();
				if (cur.isAdjacent(prev)) {
					to_average.add(cur);
					if (!iterator.hasNext()) {
						tree.add(averageLinesV(to_average)); // Add last element(s) edge case.
					}
				} else {
					tree.add(averageLinesV(to_average));
					to_average.add(cur);
				}
			}
			vertical = tree;
		}

		/**
		 * Merges horizontal lines that are adjacent to one another, creating a new line
		 * with the average of the y coordinates and the longest line's length.
		 */
		public void averageHorizontal() {
			TreeSet<MyHorizontalLine> tree = new TreeSet<MyHorizontalLine>();
			Iterator<MyHorizontalLine> iterator = getHorizontalLines().iterator();
			MyHorizontalLine prev = null, cur = null;
			TreeSet<MyHorizontalLine> to_average = new TreeSet<MyHorizontalLine>();
			if (iterator.hasNext()) {
				cur = iterator.next();
				to_average.add(cur);
			}
			while (iterator.hasNext()) {
				prev = cur;
				cur = iterator.next();
				if (cur.isAdjacent(prev)) {
					to_average.add(cur);
					if (!iterator.hasNext()) {
						tree.add(averageLinesH(to_average)); // Add last element(s) edge case.
					}
				} else {
					tree.add(averageLinesH(to_average));
					to_average.add(cur);
				}
			}
			horizontal = tree;
		}

		public MyHorizontalLine averageLinesH(TreeSet<MyHorizontalLine> lines) {
			double min_x, max_x = 0, avg_y = 0;
			min_x = lines.first().getMinX();
			int size = lines.size();
			MyHorizontalLine line;
			while ((line = lines.pollFirst()) != null) {
				avg_y += line.getY() / size;
				if (line.getMaxX() > max_x) {
					max_x = line.getMaxX();
				}
			}
			return new MyHorizontalLine(new Line2D.Double(min_x, avg_y, max_x, avg_y));
		}

		public MyVerticalLine averageLinesV(TreeSet<MyVerticalLine> lines) {
			double min_y, max_y = 0, avg_x = 0;
			min_y = lines.first().getMinY();
			int size = lines.size();
			MyVerticalLine line;
			while ((line = lines.pollFirst()) != null) {
				avg_x += line.getX() / size;
				if (line.getMaxY() > max_y) {
					max_y = line.getMaxY();
				}
			}
			return new MyVerticalLine(new Line2D.Double(avg_x, min_y, avg_x, max_y));
		}

		/**
		 * Create a grid of points based on the intersection of horizontal and vertical
		 * lines. Y coordinates are flipped.
		 * 
		 * @return a treeset of MyPoint(s).
		 */
		public TreeSet<MyPoint> createGrid() {
			TreeSet<MyPoint> grid = new TreeSet<MyPoint>();
			MyPoint intersection;
			for (MyHorizontalLine h : horizontal) {
				// iterate over horizontal lines
				for (MyVerticalLine v : vertical) {
					// find intersection point (if one) of each vertical line.
					if ((intersection = h.getIntersection(v)) != null) {
						grid.add(intersection);
					}
				}
			}
			return grid;
		}
	}

	/**
	 * The line threshold is used since lines and points often do not exactly line
	 * up. Since PDF is used for visual purposes, coordinate values can be slightly
	 * off and remain visually equal.
	 * 
	 * @return The threshold value used for approximation.
	 */
	public double getLineThreshold() {
		return line_threshold;
	}

	/**
	 * 
	 * @return The height of this page's crop box.
	 */
	public double getPageHeight() {
		return getPage().getCropBox().getHeight();
	}

	/**
	 * Runs the engine on the current page. Processes the page, finds lines,
	 * rectangles, and puts String content into an ordered table.
	 *
	 * @throws IOException
	 *             If there is an IO error while drawing the page.
	 */
	public void run() throws IOException {
		processPage(getPage());

		for (PDAnnotation annotation : getPage().getAnnotations()) {
			showAnnotation(annotation);
		}
		
		for(MyHorizontalLine h : my_lines.getHorizontalLines()) {
			System.out.println(h);
		}
		// Process line segments which logically belong to the same line.
		my_lines.mergeHorizontal();
		my_lines.averageHorizontal();
		my_lines.mergeVertical();
		my_lines.averageVertical();

		grid = my_lines.createGrid();

		// Create intermediate mapping in order to obtain row and column keys
		// (coordinates are the keys).
		MyGridSet my_grid = new MyGridSet();
		for (MyPoint p : grid) {
			my_grid.add(p);
		}

		initializeTable(my_grid.startRowKeySet(), my_grid.columnKeySet());
		//System.out.println(my_grid.first());

		populateTable(my_grid);

		removeBlankColumns();
		removeBlankRows();
	}

	/**
	 * Fills table with blank strings, based on the row and column keys provided.
	 * This is so that even if a row, column pair is not populated, we can still
	 * have a rectangular map.
	 * 
	 * @param row_keys
	 * @param col_keys
	 */
	private void initializeTable(Set<Double> row_keys, Set<Double> col_keys) {
		// Populate table with empty Strings so that empty cells will be mapped with a
		// String value. Non-empty cells will be overwritten.
		for (double row_key : row_keys) {
			for (double col_key : col_keys) {
				table.put(row_key, col_key, "");
			}
		}
	}

	/**
	 * Adds String values of each cell to table.
	 * 
	 * @param grid
	 * @throws IOException
	 */
	private void populateTable(MyGridSet grid) throws IOException {
		// Overwrite non-empty cells.
		PDFTextStripperByArea stripper = new PDFTextStripperByArea();
		stripper.setSortByPosition(true);
		Rectangle2D cell;
		for (MyPoint p : grid) {
			if ((cell = grid.getRectangle(p)) != null) {
				stripper.addRegion("class1", cell);
				stripper.extractRegions(getPage());
				String cell_content = stripper.getTextForRegion("class1").replaceAll("\n", "").trim();
				if (StringUtils.isNumericSpace(cell_content)) {
					cell_content = cell_content.replaceAll(" ", "");
				}
				table.put(p.getTranslatedY(), p.getX(), cell_content);
			}
		}
	}

	/**
	 * Removes rows from table which consist of one or less populated cells.
	 */
	private void removeBlankRows() {
		List<Double> blank_rows = new ArrayList<Double>();
		int not_blank;
		for (Double row_key : table.rowKeySet()) {
			not_blank = 0;
			for (Double col_key : table.columnKeySet()) {
				String content = table.get(row_key, col_key);
				if (!content.equals("")) {
					not_blank++;
				}
			}
			if (not_blank <= 1) {
				blank_rows.add(row_key);
			}
		}
		for (Double row_key : blank_rows) {
			table.rowMap().remove(row_key);
		}
	}

	/**
	 * Removes columns from table which consist of one or less populated cells.
	 */
	private void removeBlankColumns() {
		List<Double> blank_cols = new ArrayList<Double>();
		int not_blank;
		for (Double col_key : table.columnKeySet()) {
			not_blank = 0;
			for (Double row_key : table.rowKeySet()) {
				String content = table.get(row_key, col_key);
				if (!content.equals("")) {
					not_blank++;
				}
			}
			if (not_blank <= 1) {
				blank_cols.add(col_key);
			}
		}
		for (Double col_key : blank_cols) {
			table.columnMap().remove(col_key);
		}
	}

	/**
	 * Prints the Strings in the table, distinguishing columns and rows.
	 */
	public void printTable() {
		for (Iterator<Double> row_iter = table.rowKeySet().iterator(); row_iter.hasNext();) {
			double row_key = row_iter.next();
			for (Iterator<Double> col_iter = table.row(row_key).keySet().iterator(); col_iter.hasNext();) {
				double col_key = col_iter.next();
				System.out.print(table.get(row_key, col_key) + " | ");
			}
			System.out.println("ROW");
		}
	}

	/**
	 * Draws all MyPoints.
	 *
	 * @return BufferedImage of all the points.
	 */
	public BufferedImage drawGrid() {
		for (MyPoint p : grid) {
			g2d.drawLine((int) p.getX(), (int) p.getTranslatedY(), (int) p.getX(), (int) p.getTranslatedY());
		}
		return image;
	}

	public String getCSVString() {
		StringBuilder sb = new StringBuilder();
		for (double row_key : table.rowKeySet()) {
			for (double col_key : table.columnKeySet()) {
				sb.append(table.get(row_key, col_key));
				sb.append(',');
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	public MyLines getLines() {
		return my_lines;
	}

	/**
	 * This method is used to extract points from the PDF. Instead of simply storing
	 * each point, lines are created in order to later find where other lines
	 * intersect their midpoints. These intersections will be used to create other
	 * points which do not exist in the original PDF.
	 */
	@Override
	public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
		//moveTo((float) p0.getX(),(float) p0.getY());

		// only examine top-half of page
		double th = getPage().getBBox().getHeight() / 2;
		if(p0.getY() < th || p3.getY() < th) {
			return;
		}
		
		MyLine bottom = new MyHorizontalLine(new Line2D.Double(p0, p1));
		MyLine right = new MyVerticalLine(new Line2D.Double(p1, p2));
		MyLine top = new MyHorizontalLine(new Line2D.Double(p3, p2));
		MyLine left = new MyVerticalLine(new Line2D.Double(p0, p3));
		
		if (bottom.isAdjacent(top)) {
			my_lines.add(top);
		} else if (right.isAdjacent(left)) {
			my_lines.add(left);
		} else {
			my_lines.add(right);
			my_lines.add(left);
			my_lines.add(bottom);
			my_lines.add(top);
		}
	}

	@Override
	public void drawImage(PDImage pdImage) throws IOException {
	}

	@Override
	public void clip(int windingRule) throws IOException {
	}

	@Override
	public void moveTo(float x, float y) throws IOException {
	}

	@Override
	public void lineTo(float x, float y) throws IOException {
	}

	@Override
	public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
	}

	@Override
	public void closePath() throws IOException {
	}

	@Override
	public void endPath() throws IOException {
	}

	@Override
	public void strokePath() throws IOException {
	}

	@Override
	public void fillPath(int windingRule) throws IOException {
	}

	@Override
	public void fillAndStrokePath(int windingRule) throws IOException {
	}

	@Override
	public void shadingFill(COSName shadingName) throws IOException {
	}

	/**
	 * Overridden from PDFStreamEngine.
	 */
	@Override
	public void showTextString(byte[] string) throws IOException {
		super.showTextString(string);
	}

	/**
	 * Overridden from PDFStreamEngine.
	 */
	@Override
	public void showTextStrings(COSArray array) throws IOException {
		super.showTextStrings(array);

	}

	/**
	 * Overridden from PDFStreamEngine.
	 */
	@Override
	protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement)
			throws IOException {

		super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);
	}

	@Override
	public Point2D getCurrentPoint() throws IOException {
		return null;
	}

	// NOTE: there are may more methods in PDFStreamEngine which can be overridden
	// here too.
}
