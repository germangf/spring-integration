package ch.ggf.cif.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import ch.ggf.cif.dao.ContainerLangDao;
import ch.ggf.cif.dao.FileNameDao;
import ch.ggf.cif.dao.impl.ContainerLangDaoImpl;
import ch.ggf.cif.dao.impl.FileNameDaoImpl;
import ch.ggf.cif.service.ContainerService;
import ch.ggf.cif.service.DocumentService;
import ch.ggf.cif.service.client.AvaloqServiceClient;
import ch.ggf.cif.service.impl.ContainerServiceImpl;
import ch.ggf.common.domain.Container;
import ch.ggf.common.domain.Holder;
import ch.ggf.common.domain.HolderRelation;
import ch.ggf.common.domain.statement.TaxStatementData;
import ch.ggf.common.util.PDFs;


@Import(ch.ggf.birt.pdf.config.Config.class)
@ComponentScan({"ch.ggf.cif.service", "ch.ggf.cif.dao"})
@Configuration
@PropertySource("${cif.properties}")
public class Config {

	protected Logger log = LoggerFactory.getLogger(Config.class.getName());
	protected Logger errorLog = LoggerFactory.getLogger("error");
	protected Logger successLog = LoggerFactory.getLogger("success");

	@Value("${inputDir}")
	private String inputDir;

	@Value("${outputDir}")
	private String outputDir;

	@Value("${archivedDir}")
	private String archivedDir;

	@Value("${errorDir}")
	private String errorDir;

	@Value("${container.lang.file}")
	private String langFile;


	@Autowired
	private DocumentService documentService;

	// spring integration
	@ServiceActivator(inputChannel = "errorChannel", outputChannel = "endChannel")
	public String handleError(ErrorMessage message) {
		File file = getFile(message);
		try {
			Files.move(file, new File(errorDir, file.getName()));
		} catch (IOException e) {
			log.error("Error occurs moving the file {} in error directory", file.getAbsolutePath());
		}
		errorLog.info("{}", file.getName());

		return "DONE";
	}

	private File getFile(ErrorMessage message) {
		Object payload = message.getPayload();
		if (payload instanceof MessageTransformationException) {
			Object failedMessagePayload = ((MessageTransformationException) message.getPayload()).getFailedMessage().getPayload();
			if (failedMessagePayload instanceof File) {
				return (File) failedMessagePayload;
			}
			return ((TaxStatementData)failedMessagePayload).getTax();
		} else if (payload instanceof MessageHandlingException) {
			Object failedMessagePayload = ((MessageHandlingException) message.getPayload()).getFailedMessage().getPayload();
			return ((TaxStatementData)failedMessagePayload).getTax();
		}
		return null;
	}

	@Bean
	@InboundChannelAdapter(value = "fileInputChannel", poller = @Poller(fixedDelay = "5000"))
	public MessageSource<File> fileReadingMessageSource() {
		FileReadingMessageSource source = new FileReadingMessageSource();
		source.setDirectory(new File(inputDir));
		source.setFilter(new RegexPatternFileListFilter(Pattern.compile("(IN000|ES000).+\\.(pdf|PDF)")));
		return source;
	}

	@Bean
	public MessageChannel fileInputChannel() {
		return new DirectChannel();
	}

	@Transformer(inputChannel = "fileInputChannel", outputChannel = "containerNrChannel")
	public Message<TaxStatementData> getContainerNr(Message<File> message) {
		TaxStatementData data = new TaxStatementData();
		File payload = message.getPayload();

		String containerNr = containerService().getContainerNr(payload.getName());
		int year = containerService().getYear(payload.getName());

		data.setYear(year);
		data.setTax(new File(payload.getAbsolutePath()));
		data.setContainer(containerService().getAccountHolders(containerNr));

		return new GenericMessage<TaxStatementData>(data);
	}

	@Bean
	public MessageChannel containerNrChannel() {
		return new DirectChannel();
	}

	@Transformer(inputChannel = "containerNrChannel", outputChannel = "langChannel")
	public Message<TaxStatementData> getLang(Message<TaxStatementData> message) {
		String lang = containerService().getLanguage(message.getPayload().getContainer().getContainerNr());
		message.getPayload().setLang(lang);
		return message;
	}

	@Bean
	public MessageChannel langChannel() {
		return new DirectChannel();
	}

	@Transformer(inputChannel = "langChannel", outputChannel = "indexChannel")
	public Message<TaxStatementData> createIndexPage(Message<TaxStatementData> message) throws IOException {
		message.getPayload().setCover(documentService.createCoverPage(message.getPayload().getLang(), message.getPayload().getYear()));
		return message;
	}

	@Bean
	public MessageChannel indexChannel() {
		return new DirectChannel();
	}

	@Transformer(inputChannel = "indexChannel", outputChannel = "ownershipChannel")
	public Message<TaxStatementData> createOwnershipPage(Message<TaxStatementData> message) throws IOException {

		TaxStatementData payload = message.getPayload();
		Container container = payload.getContainer();
		for(Holder accountHolder: container.getHolders()) {
			if (accountHolder.hasRelation(HolderRelation.HOLDER) || accountHolder.hasRelation(HolderRelation.BENEFICIARY)) {
				payload.addOwnership(documentService.createOwnershipPage(container, accountHolder, payload.getLang()));
			}
		}

		return message;
	}

	@Bean
	public MessageChannel ownershipChannel() {
		return new DirectChannel();
	}

	@Transformer(inputChannel = "ownershipChannel", outputChannel = "concatChannel")
	public Message<TaxStatementData> concat(Message<TaxStatementData> message) throws COSVisitorException, IOException {
		TaxStatementData taxStatementData = message.getPayload();
		List<File> files = Lists.newArrayList();
		files.add(taxStatementData.getCover());
		files.addAll(taxStatementData.getOwnerships());
		files.add(taxStatementData.getTax());

		PDFs.concat(files, new File(outputDir, taxStatementData.getTax().getName()).getAbsolutePath());

		return message;
	}

	@Bean
	public MessageChannel concatChannel() {
		return new DirectChannel();
	}

	@ServiceActivator(inputChannel = "concatChannel", outputChannel = "endChannel")
	public String storeFile(Message<TaxStatementData> message) throws COSVisitorException, IOException {
		TaxStatementData taxStatementData = message.getPayload();
		File tax = taxStatementData.getTax();
		List<File> files = Lists.newArrayList();
		files.add(taxStatementData.getCover());
		files.addAll(taxStatementData.getOwnerships());
		files.add(taxStatementData.getTax());

		PDFs.concat(files, new File(archivedDir, taxStatementData.getTax().getName()).getAbsolutePath());
		tax.delete();

		successLog.info("{}", tax.getName());

		return "DONE";
	}

	@Bean
	public MessageChannel endChannel() {
		return new QueueChannel();
	}

	@Bean
	public FileNameDao fileNameDao() {
		return new FileNameDaoImpl();
	}

	@Bean
	public ContainerLangDao ContainerLangDao() {
		return new ContainerLangDaoImpl(langFile);
	}

	@Bean
	public ContainerService containerService() {
		return new ContainerServiceImpl();
	}

	@Bean
	public AvaloqServiceClient avaloqServiceClient() {
		return new AvaloqServiceClient();
	}


}
