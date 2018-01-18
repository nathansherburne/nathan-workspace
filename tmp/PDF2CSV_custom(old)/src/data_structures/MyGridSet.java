package data_structures;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.TreeBasedTable;

import processing.CustomGraphicsStreamEngine.DIRECTION;
import processing.CustomGraphicsStreamEngine.MyPoint;

public class MyGridSet implements Iterable<MyPoint> {
	private TreeBasedTable<Double, Double, MyPoint> table;

	public MyGridSet() {
		table = TreeBasedTable.create();
	}

	public void add(MyPoint p) {
		table.put(p.getTranslatedY(), p.getX(), p);
	}
	
	public MyPoint first() {
		return (MyPoint) table.row(table.rowKeySet().first()).values().toArray()[0];
	}

	/**
	 * 
	 * @param p
	 * @return the MyPoint if a point below this one in the same row exists--null if
	 *         not.
	 */
	public MyPoint right(MyPoint p) {
		double col_key;
		for (Iterator<Double> iter = table.row(p.getTranslatedY()).keySet().iterator(); iter.hasNext();) {
			if ((col_key = iter.next()) > p.getX()) {
				return table.row(p.getTranslatedY()).get(col_key);
			}
		}
		return null;
	}

	/**
	 * 
	 * @param p
	 * @return the MyPoint if a point below this one in the same column exists--null
	 *         if not.
	 */
	public MyPoint below(MyPoint p) {
		double row_key;
		for (Iterator<Double> iter = table.rowKeySet().iterator(); iter.hasNext();) {
			if ((row_key = iter.next()) > p.getTranslatedY()) {
				if (table.row(row_key).containsKey(p.getX())) {
					return table.row(row_key).get(p.getX());
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param p
	 * @return Rectangle2D.Double representation of the rectangle whose top-left
	 *         corner is p, or null if p is not the top-left corner of any
	 *         rectangle.
	 */
	public Rectangle2D getRectangle(MyPoint p) {
		if (!isValidRectangleOrigin2(p))
			return null;
		double width = right(p).getX() - p.getX();
		double height = below(p).getTranslatedY() - p.getTranslatedY();
		return new Rectangle2D.Double(p.getX(), p.getTranslatedY(), width, height);
	}

	/**
	 * 
	 * @return a TreeSet of the row keys.
	 */
	public TreeSet<Double> rowKeySet() {
		return new TreeSet<Double>(table.rowKeySet());
	}

	/**
	 * 
	 * @return a TreeSet of the column keys.
	 */
	public TreeSet<Double> columnKeySet() {
		return new TreeSet<Double>(table.columnKeySet());
	}

	/**
	 * Returns a TreeSet of row keys, excluding keys which are not valid rectangle
	 * origins. These row keys are excluded because the bottom-left point of a
	 * rectangle (such as the bottom left point of a table) or stray points not
	 * belonging to rectangles should not be considered row origins.
	 * 
	 * @return a TreeSet of row keys.
	 */
	public TreeSet<Double> startRowKeySet() {
		TreeSet<Double> start_row_keys = new TreeSet<Double>();
		TreeMap<Double, MyPoint> column;
		for (double colkey : columnKeySet()) {
			column = new TreeMap<Double, MyPoint>(table.columnMap().get(colkey));
			for (Double row_key : column.keySet()) {
				MyPoint begin_row = column.get(row_key);
				if (isValidRectangleOrigin2(begin_row)) {
					start_row_keys.add(row_key);
				}
			}
		}
		return start_row_keys;
	}

	/**
	 * Assuming parameter p is the top-left corner of a rectangle, this method
	 * checks to see if the other three corners of the rectangle 1) exist, and 2)
	 * each agree that, based on who their neighboring points are, they are all a
	 * part of the same rectangle.
	 * 
	 * @param p
	 *            the upper-left corner of the rectangle to test.
	 * 
	 * @return true if p is the upper-left corner of a rectangle in which all four
	 *         corners agree that they belong to the same rectangle.
	 */
	public boolean isValidRectangleOrigin2(MyPoint p) {
		if (!(p.getNeighboors().contains(DIRECTION.RIGHT) && p.getNeighboors().contains(DIRECTION.BELOW)))
			return false;
		MyPoint right = null, below = null, b_right1 = null, b_right2 = null;

		// Check that top-right corner exists.
		MyPoint current = p;
		while ((current = right(current)) != null) {
			// once p and right agree, look for a below.
			if (current.getNeighboors().contains(DIRECTION.LEFT) && current.getNeighboors().contains(DIRECTION.BELOW)) {
				// p and right agree. Right also believes that bottom right corner exists.
				right = current;
				break;
			}
		}
		if (right == null)
			return false;

		// Check that bottom-left corner exists.
		current = p;
		while ((current = below(current)) != null) {
			// once p and below agree, check that lower right corner exists.
			if (current.getNeighboors().contains(DIRECTION.ABOVE)
					&& current.getNeighboors().contains(DIRECTION.RIGHT)) {
				// p and below agree. Below also believes that bottom right corner exists.
				below = current;
				break;
			}
		}
		if (below == null)
			return false;

		// Check that bottom right corner exists
		if ((b_right1 = right(below)) != null && (b_right2 = below(right)) != null) {
			if (b_right1.equals(b_right2)) {
				// Below right point exists Thus all points exist and agree.
				return true;
			}
		}
		return false;
	}

	/**
	 * An iterator that iterates from left to right, starting with the top row.
	 * 
	 * @author ndsherb
	 *
	 */
	private class IteratorHelper implements Iterator<MyPoint> {
		private MyPoint cursor;
		private double row_key, col_key;

		private IteratorHelper() {
			if(!table.isEmpty()) {
				row_key = rowKeySet().first();
				col_key = table.row(row_key).firstKey();
				cursor = table.get(row_key, col_key);
			}
		}

		@Override
		public boolean hasNext() {
			if(table.isEmpty()) return false;
			TreeSet<Double> col_keys = new TreeSet<Double>(table.row(row_key).keySet());
			return col_keys.higher(col_key) != null || rowKeySet().higher(row_key) != null;
		}

		@Override
		public MyPoint next() {
			Double next_row_key, next_col_key;
			MyPoint current = cursor;
			TreeSet<Double> col_keys = new TreeSet<Double>(table.row(row_key).keySet());
			if ((next_col_key = col_keys.higher(col_key)) != null) { // Next element is to the right in current row.
				next_row_key = row_key;
			} else { // Next element is first of next row.
				next_row_key = rowKeySet().higher(row_key);
				next_col_key = table.row(next_row_key).firstKey();
			}
			cursor = table.get(next_row_key, next_col_key);
			col_key = next_col_key;
			row_key = next_row_key;
			return current;
		}

	}

	@Override
	public Iterator<MyPoint> iterator() {
		return new IteratorHelper();
	}
}
