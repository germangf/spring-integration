package ch.ggf.cif.service;

import ch.ggf.common.domain.Container;

public interface ContainerService {

	public String getContainerNr(String fileName);

	public int getYear(String fileName);

	public Container getAccountHolders(String containerNr);

	public String getLanguage(String containerNr);


}
