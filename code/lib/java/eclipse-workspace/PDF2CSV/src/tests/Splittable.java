package tests;

import java.util.ArrayList;
import java.util.List;

public class Splittable {
	private int num;
	public Splittable(int num) {
		this.num = num;
	}
	public List<Splittable> split() {
		int splitNum = num / 2;
		Splittable s1 = new Splittable(splitNum);
		Splittable s2 = new Splittable(splitNum);
		List<Splittable> list = new ArrayList<Splittable>();
		list.add(s1);
		list.add(s2);
		return list;
	}
	
	@Override
	public String toString() {
		return String.valueOf(num);
	}
}
