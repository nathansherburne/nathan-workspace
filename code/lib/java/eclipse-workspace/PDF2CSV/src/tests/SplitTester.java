package tests;

import java.util.ArrayList;
import java.util.List;

public class SplitTester {

	public static void main(String[] args) {
		List<Splittable> list = new ArrayList<Splittable>();
		Splittable s = new Splittable(6);
		list.add(s);
		List<Splittable> list2 = new ArrayList<Splittable>();
		list2.add(s);
		List<Splittable> split = list2.remove(0).split();
		list2.addAll(split);
		
		for(Splittable sp : list) {
			System.out.println(sp);
		}
		System.out.println();
		for(Splittable sp : list2) {
			System.out.println(sp);
		}

	}

}
