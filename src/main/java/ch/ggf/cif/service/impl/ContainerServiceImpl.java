package ch.ggf.cif.service.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ch.ggf.cif.dao.ContainerLangDao;
import ch.ggf.cif.dao.FileNameDao;
import ch.ggf.cif.service.ContainerService;
import ch.ggf.cif.service.client.AvaloqServiceClient;
import ch.ggf.common.domain.Container;


public class ContainerServiceImpl implements ContainerService {

	private static String LANG_ES_AVALOQ = "Spanish";


	@Autowired
	protected FileNameDao fileNameDao;

	@Autowired
	protected ContainerLangDao containerLangDao;


	@Autowired
	protected AvaloqServiceClient avaloqServiceClient;


	protected Logger log = LoggerFactory.getLogger(ContainerServiceImpl.class.getName());

 	public void setAvaloqServiceClient(AvaloqServiceClient avaloqServiceClient) {
		this.avaloqServiceClient = avaloqServiceClient;
	}

	@Override
	public String getContainerNr(String fileName) {
 		String encryptedKey = fileNameDao.getEncrytedKey(fileName);
		String containerNr = avaloqServiceClient.getContainerNr(encryptedKey);
		return containerNr;
	}

 	@Override
	public int getYear(String fileName) {
 		return fileNameDao.getYear(fileName);
	}

 	@Override
	public Container getAccountHolders(String containerNr) {
		return avaloqServiceClient.getAccountHolders(containerNr);
	}

 	@Override
	public String getLanguage(String containerNr) {
 		String lang = null;
		try {
			lang = containerLangDao.getLang(containerNr);
		} catch (IOException e) {
			e.printStackTrace();
		}

 		if (lang == null){
 			lang = avaloqServiceClient.getLanguage(containerNr);
 		}

		if (lang.equals(LANG_ES_AVALOQ)){
			lang = "es";
		}else{
			lang = "en";
		}

 		return lang;

	}


}
