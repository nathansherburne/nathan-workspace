package tests;

import java.util.ArrayList;
import java.util.List;

import driver.Driver;

public class SplitTester {

	public static void main(String[] args) {
		String t = "/this/is/a/file.path/wi..th/wi_eo / stf./hello.txt";
		System.out.println(Driver.addStringBeforeExtension(t, "_N01"));

	}

}
