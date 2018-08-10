package ch.ggf.cif.service;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.ggf.birt.pdf.BirtParameters;
import ch.ggf.birt.pdf.service.PDFService;
import ch.ggf.common.domain.Container;
import ch.ggf.common.domain.Holder;
import ch.ggf.common.util.Dates;

@Service
public class DocumentService {

	private static String COVER_PAGE = "pdf/templates/cover.rptdesign";
	private static String OWNERSHIP_PAGE = "pdf/templates/ownership.rptdesign";

	@Autowired
	private PDFService pdfService;

	public File createCoverPage(String lang, int year) throws IOException {
		BirtParameters birtParameters = new BirtParameters();
		Locale locale = new Locale(lang);
		birtParameters.setLocale(locale);
		birtParameters.setRptdesign(COVER_PAGE);
		birtParameters.setOutputFile(File.createTempFile("cif-cover-", ".pdf"));

		birtParameters.addParameter("creationDate", formatDate(locale));
		birtParameters.addParameter("year", String.valueOf(year));

		birtParameters = pdfService.create(birtParameters);
		return birtParameters.getOutputFile();
	}

	public File createOwnershipPage(Container container, Holder holder, String lang) throws IOException {
		BirtParameters birtParameters = new BirtParameters();
		Locale locale = new Locale(lang);
		birtParameters.setLocale(locale);
		birtParameters.setRptdesign(OWNERSHIP_PAGE);
		birtParameters.setOutputFile(File.createTempFile("cif-ownership-", ".pdf"));

		birtParameters.addParameter("creationDate", formatDate(locale));
		birtParameters.addParameter("container", container);
		birtParameters.addParameter("holder", holder);

		birtParameters = pdfService.create(birtParameters);
		return birtParameters.getOutputFile();
	}

	private String formatDate(Locale locale) {
		if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage())) {
			return Dates.formatDateWithSuffix(new Date(), locale);
		}
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, locale);
		return dateFormat.format(new Date());
	}

}
