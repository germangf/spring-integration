package ch.ggf.cif.dao.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

import ch.ggf.cif.dao.ContainerLangDao;

public class ContainerLangDaoImpl implements ContainerLangDao {

	private String langFileName;

	public ContainerLangDaoImpl(String langFileName) {
		this.langFileName = langFileName;
	}


	@Override
	public String getLang(String containerNr) throws IOException{
		List<String> lines = Files.readLines(new File(langFileName), Charsets.UTF_8);
		for (String line : lines) {
			if (line.contains(containerNr)){
				return Splitter.on(";")
						       .trimResults()
//						       .omitEmptyStrings()
						       .splitToList(line).get(10);
			}
		}
		return null;
	}


}
