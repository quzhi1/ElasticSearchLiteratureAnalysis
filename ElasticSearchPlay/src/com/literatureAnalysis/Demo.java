package com.literatureAnalysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;


public class Demo {

	public static void main(String[] args) throws FileNotFoundException, IOException, URISyntaxException {
//		Analects.bookToJSONFile("E:\\My_Books\\屈直收藏\\论语\\", "E:\\lunyu.json");
		Analects.JSONToES("E:\\lunyu.json");
//		Analects.delete();
	}

}
