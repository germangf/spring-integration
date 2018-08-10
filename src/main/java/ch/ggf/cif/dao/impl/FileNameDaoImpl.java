package ch.ggf.cif.dao.impl;

import java.util.List;

import com.google.common.base.Splitter;

import ch.ggf.cif.dao.FileNameDao;

public class FileNameDaoImpl implements FileNameDao {

	private static String year_regex = "_J";
	private static String encryted_key_regex = "BVACH_K";

	private static int  year_length = 2;
	private static int  encryted_key_length = 25;


	@Override
	public String getEncrytedKey(String fileName){

		List<String> elements = Splitter.on(encryted_key_regex)
	       .trimResults()
	       .omitEmptyStrings()
	       .splitToList(fileName);

		return elements.get(1).substring(0, encryted_key_length);
	}


	@Override
	public int getYear(String fileName){

		List<String> elements = Splitter.on(year_regex)
			       .trimResults()
			       .omitEmptyStrings()
			       .splitToList(fileName);

		return Integer.parseInt(elements.get(1).substring(0, year_length)) + 2000;

	}

}
