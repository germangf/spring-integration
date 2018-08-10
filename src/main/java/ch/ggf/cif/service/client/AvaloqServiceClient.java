package ch.ggf.cif.service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2RestOperations;

import ch.ggf.cif.dao.FileNameDao;
import ch.ggf.cif.service.impl.ContainerServiceImpl;
import ch.ggf.common.domain.Container;
import ch.ggf.common.util.OauthRestTemplates;

public class AvaloqServiceClient {

	@Autowired
	protected OAuth2RestOperations oauthRestOperations;

	@Value("${config.oauth2.resourceURI}")
	protected String resourceURI;


	@Autowired
	protected FileNameDao fileNameDao;

	protected Logger log = LoggerFactory.getLogger(ContainerServiceImpl.class.getName());

	public AvaloqServiceClient() {
	}

	private String getUrl(String path) {
		return resourceURI + path;
	}

	public String getLanguage(String containerNr) {
		String lang = OauthRestTemplates.getForObject(oauthRestOperations, getUrl("/avaloq/container/{containerNr}/language"), String.class, containerNr);
 		return lang;

	}

	public Container getAccountHolders(String containerNr) {
		return OauthRestTemplates.getForObject(oauthRestOperations, getUrl("/avaloq/container/{containerNr}/account-holders"), Container.class, containerNr);
	}

	public String getContainerNr(String encryptedKey) {
		return OauthRestTemplates.getForObject(oauthRestOperations, getUrl("/avaloq/container/{encryptedKey}/container-number"), String.class, encryptedKey);
	}


}




