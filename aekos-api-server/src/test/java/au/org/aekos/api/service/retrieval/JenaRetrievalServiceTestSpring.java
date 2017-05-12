package au.org.aekos.api.service.retrieval;

import static au.org.aekos.api.TraitOrEnvVarMatcher.isTraitOrVar;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import au.org.aekos.api.model.EnvironmentDataRecord;
import au.org.aekos.api.model.EnvironmentDataResponse;
import au.org.aekos.api.model.ResponseHeader;
import au.org.aekos.api.model.SpeciesDataResponseV1_0;
import au.org.aekos.api.model.TraitDataRecord;
import au.org.aekos.api.model.TraitDataResponse;
import au.org.aekos.api.model.TraitOrEnvironmentalVariable;
import au.org.aekos.api.model.VisitInfo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/au/org/aekos/api/retrievalContext-test.xml")
public class JenaRetrievalServiceTestSpring {

	@Autowired
	private JenaRetrievalService objectUnderTest;
	
	@Autowired
	@Qualifier("testGetSpeciesDataCsv01_expected")
	private String testGetSpeciesDataCsv01_expected;

	@Autowired
	@Qualifier("testGetSpeciesDataCsv02_expected")
	private String testGetSpeciesDataCsv02_expected;

	@Autowired
	@Qualifier("testGetSpeciesDataCsv03_expected")
	private String testGetSpeciesDataCsv03_expected;

	@Autowired
	@Qualifier("testGetEnvironmentalDataCsv01_expected")
	private String testGetEnvironmentalDataCsv01_expected;

	@Autowired
	@Qualifier("testGetTraitDataCsv01_expected")
	private String testGetTraitDataCsv01_expected;

	@Autowired
	@Qualifier("testGetAllSpeciesDataCsv01_expected")
	private String testGetAllSpeciesDataCsv01_expected;
	
	/**
	 * Can we get all the records that are available for the specified species?
	 */
	@Test
	public void testGetSpeciesDataJsonV1_0_01() throws Throwable {
		SpeciesDataResponseV1_0 result = objectUnderTest.getSpeciesDataJsonV1_0(Arrays.asList("Calotis hispidula"), 0, 10);
		assertThat(result.getResponse().size(), is(2));
	}

	/**
	 * Can we get all the records that are available for multiple species?
	 */
	@Test
	public void testGetSpeciesDataJsonV1_0_02() throws Throwable {
		SpeciesDataResponseV1_0 result = objectUnderTest.getSpeciesDataJsonV1_0(Arrays.asList("Calotis hispidula", "Goodenia havilandii"), 0, 10);
		assertThat(result.getResponse().size(), is(3));
	}

	/**
	 * Can we limit the records that we get for the specified species?
	 */
	@Test
	public void testGetSpeciesDataJsonV1_0_03() throws Throwable {
		int onlyOne = 1;
		SpeciesDataResponseV1_0 result = objectUnderTest.getSpeciesDataJsonV1_0(Arrays.asList("Calotis hispidula"), 0, onlyOne);
		assertThat(result.getResponse().size(), is(1));
		assertThat(result.getResponseHeader().getNumFound(), is(2));
	}
	
	/**
	 * Can we defend against a SPARQL injection attack?
	 */
	@Test
	public void testGetSpeciesDataJsonV1_0_04() throws Throwable {
		SpeciesDataResponseV1_0 result = objectUnderTest.getSpeciesDataJsonV1_0(Arrays.asList("\" UNDEF \""), 0, 1);
		assertThat("should be nothing because no species match the escaped text", result.getResponse().size(), is(0));
		assertThat(result.getResponseHeader().getNumFound(), is(0));
	}
	
	/**
	 * Can we get a species record that uses taxonRemarks rather than scientificName?
	 */
	@Test
	public void testGetSpeciesDataJsonV1_0_05() throws Throwable {
		SpeciesDataResponseV1_0 result = objectUnderTest.getSpeciesDataJsonV1_0(Arrays.asList("Hakea obtusa"), 0, 1);
		assertThat(result.getResponse().size(), is(1));
		assertThat(result.getResponse().get(0).getTaxonRemarks(), is("Hakea obtusa"));
	}

	/**
	 * Can we get all the species records as JSON?
	 */
	@Test
	public void testGetAllSpeciesDataJson01() throws Throwable {
		SpeciesDataResponseV1_0 result = objectUnderTest.getAllSpeciesDataJsonV1_0(0, 10);
		assertThat(result.getResponse().size(), is(8));
	}
	
	/**
	 * Can we get all the records that are available for the specified species?
	 */
	@Test
	public void testGetSpeciesDataCsv01() throws Throwable {
		Writer writer = new StringWriter();
		objectUnderTest.getSpeciesDataCsvV1_0(Arrays.asList("Calotis hispidula"), 0, 20, writer);
		String compareStr = testGetSpeciesDataCsv01_expected;
		if(SystemUtils.IS_OS_WINDOWS){
			compareStr = testGetSpeciesDataCsv01_expected.replaceAll("\r", "");
		}
		assertEquals(compareStr, writer.toString());
	}

	/**
	 * Can we get all the records that are available for multiple species?
	 */
	@Test
	public void testGetSpeciesDataCsv02() throws Throwable {
		Writer writer = new StringWriter();
		objectUnderTest.getSpeciesDataCsvV1_0(Arrays.asList("Calotis hispidula", "Goodenia havilandii"), 0, 20, writer);
		String compareStr = testGetSpeciesDataCsv02_expected;
		if(SystemUtils.IS_OS_WINDOWS){
			compareStr = testGetSpeciesDataCsv02_expected.replaceAll("\r", "");
		}
		assertEquals(compareStr, writer.toString());
	}

	/**
	 * Can we limit the records that we get for the specified species?
	 */
	@Test
	public void testGetSpeciesDataCsv03() throws Throwable {
		int onlyOne = 1;
		Writer writer = new StringWriter();
		objectUnderTest.getSpeciesDataCsvV1_0(Arrays.asList("Calotis hispidula"), 0, onlyOne, writer);
		String compareStr = testGetSpeciesDataCsv03_expected;
		if(SystemUtils.IS_OS_WINDOWS){
			compareStr = testGetSpeciesDataCsv03_expected.replaceAll("\r", "");
		}
		assertEquals(compareStr, writer.toString());
	}
	
	/**
	 * Can we get all the species records as CSV?
	 */
	@Test
	public void testGetAllSpeciesDataCsv01() throws Throwable {
		Writer writer = new StringWriter();
		objectUnderTest.getAllSpeciesDataCsvV1_0(0, 20, writer);
		String compareStr = testGetAllSpeciesDataCsv01_expected;
		if(SystemUtils.IS_OS_WINDOWS){
			compareStr = testGetAllSpeciesDataCsv01_expected.replaceAll("\r", "");
		}
		assertEquals(compareStr, writer.toString());
	}
	
	/**
	 * Can we get all the environmental data records that are available for the specified species?
	 */
	@Test
	public void testGetEnvironmentalDataCsv01() throws Throwable {
		Writer writer = new StringWriter();
		objectUnderTest.getEnvironmentalDataCsv(Arrays.asList("Calotis hispidula", "Hakea obtusa", "Lasiopetalum compactum", "Dodonaea concinna Benth."),
				Collections.emptyList(), 0, 20, writer);
		String compareStr = testGetEnvironmentalDataCsv01_expected;
		if(SystemUtils.IS_OS_WINDOWS){
			compareStr = testGetEnvironmentalDataCsv01_expected.replaceAll("\r", "");
		}
		assertEquals(compareStr, writer.toString());
	}
	
	/**
	 * Can we get all the environmental data records that are available for the specified species?
	 */
	@Test
	public void testGetEnvironmentalDataJson01() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Calotis hispidula"), Collections.emptyList(), 0, 20);
		List<EnvironmentDataRecord> response = result.getResponse();
		assertThat(response.size(), is(1));
		EnvironmentDataRecord record = response.get(0);
		assertThat(record.getDecimalLatitude(), is(-23.5318398476576d));
		assertThat(record.getDecimalLongitude(), is(138.321378247854d));
		assertThat(record.getGeodeticDatum(), is("GDA94"));
		assertThat(record.getEventDate(), is("1990-03-30"));
		assertThat(record.getMonth(), is(3));
		assertThat(record.getYear(), is(1990));
		assertThat(record.getLocationID(), is("aekos.org.au/collection/sydney.edu.au/DERG/Plum%20Pudding"));
		assertThat(record.getBibliographicCitation(), is("Wardle GMW, Dickman CRD, Long Term Ecological Research Network (2015). "
				+ "Desert Ecology Research Group Plots (1990-2011) and Long Term Ecological Research Network (2012-2015), Simpson Desert, "
				+ "Western Queensland, Australia, Version 3/2016. Persistent Hyperlink: http://www.aekos.org.au/collection/sydney.edu.au/DERG. "
				+ "&#198;KOS Data Portal, rights owned by University of Sydney via LTERN's Data Portal. Accessed [dd mmm yyyy, e.g. 01 Jan 2016]."));
		assertThat(record.getSamplingProtocol(), is("aekos.org.au/collection/sydney.edu.au/DERG"));
		Collection<TraitOrEnvironmentalVariable> vars = record.getVariables();
		assertThat(vars.size(), is(0));
	}
	
	/**
	 * Can we get the total numFound?
	 */
	@Test
	public void testGetEnvironmentalDataJson02() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Calotis hispidula", "Rosa canina"), Collections.emptyList(), 0, 1);
		assertThat(result.getResponse().size(), is(1));
		assertThat(result.getResponseHeader().getNumFound(), is(2));
	}
	
	/**
	 * Can we get all the environmental data variables that aren't already tested?
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetEnvironmentalDataJson03() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Rosa canina"), Collections.emptyList(), 0, 20);
		List<EnvironmentDataRecord> response = result.getResponse();
		assertThat(response.size(), is(1));
		EnvironmentDataRecord record = response.get(0);
		assertThat(record.getLocationID(), is("aekos.org.au/collection/adelaide.edu.au/trend/SATFLB0026"));
		Collection<TraitOrEnvironmentalVariable> vars = record.getVariables();
		assertThat(vars.size(), is(17));
		assertThat(vars, hasItems(
			isTraitOrVar("disturbanceEvidenceCover", "50", "percent"),
			isTraitOrVar("slope", "10", "degrees"),
			isTraitOrVar("aspect", "230", "degrees"),
			isTraitOrVar("erosionEvidenceType", "No evidence", ""),
			isTraitOrVar("surfaceType", "Flat", ""),
			isTraitOrVar("erosionEvidenceState", "Good", ""),
			isTraitOrVar("visibleFireEvidence", "No evidence", ""),
			isTraitOrVar("soilTexture", "Coarse", ""),
			isTraitOrVar("soilType", "Clay", ""),
			isTraitOrVar("disturbanceEvidenceType", "No effective disturbance", ""),
			isTraitOrVar("latestLandUse", "Farming", ""),
			isTraitOrVar("ph", "5.4", "pH"),
			isTraitOrVar("silt", "A lot", "siltiness"),
			isTraitOrVar("clay", "Very", "dunno"),
			isTraitOrVar("sand", "it's everywhere", "sandiness"),
			isTraitOrVar("totalOrganicCarbon", "1.34", "percent"),
			isTraitOrVar("electricalConductivity", "4", "millisiemens per metre")
		));
	}
	
	/**
	 * Can we filter the variables that are returned?
	 */
	@Test
	public void testGetEnvironmentalDataJson04() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Calotis hispidula", "Rosa canina"),
				Arrays.asList("aspect", "soilType", "sand"), 0, 20);
		assertThat(result.getResponseHeader().getNumFound(), is(1));
		List<EnvironmentDataRecord> response = result.getResponse();
		assertThat(response.size(), is(1));
		EnvironmentDataRecord record = response.get(0);
		assertThat(record.getLocationID(), is("aekos.org.au/collection/adelaide.edu.au/trend/SATFLB0026"));
		Collection<TraitOrEnvironmentalVariable> vars = record.getVariables();
		assertThat(vars.size(), is(3));
		Iterator<TraitOrEnvironmentalVariable> varsIterator = vars.iterator();
		assertThat(varsIterator.next(), isTraitOrVar("aspect", "230", "degrees"));
		assertThat(varsIterator.next(), isTraitOrVar("soilType", "Clay", ""));
		assertThat(varsIterator.next(), isTraitOrVar("sand", "it's everywhere", "sandiness"));
	}
	
	/**
	 * Can we defend against a SPARQL injection attack on the species name?
	 */
	@Test
	public void testGetEnvironmentalDataJson05() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("\" UNDEF \""), Collections.emptyList(), 0, 1);
		assertThat("should be nothing because no species match the escaped text", result.getResponse().size(), is(0));
		assertThat(result.getResponseHeader().getNumFound(), is(0));
	}
	
	/**
	 * Can we tell which species is associated with which environmental record in the result when we supply more than one species?
	 */
	@Test
	public void testGetEnvironmentalDataJson06() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Calotis hispidula", "Rosa canina"), Collections.emptyList(), 0, 10);
		assertThat(result.getResponse().size(), is(2));
		Set<String> scientificNamesForFirstRecord = result.getResponse().get(0).getScientificNames();
		assertThat(scientificNamesForFirstRecord.size(), is(1));
		assertThat(scientificNamesForFirstRecord, hasItems("Rosa canina"));
		Set<String> scientificNamesForSecondRecord = result.getResponse().get(1).getScientificNames();
		assertThat(scientificNamesForSecondRecord.size(), is(1));
		assertThat(scientificNamesForSecondRecord, hasItems("Calotis hispidula"));
	}
	
	/**
	 * Can we get an environment record with multiple species that appear at the same site?
	 */
	@Test
	public void testGetEnvironmentalDataJson07() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Dodonaea concinna Benth.", "Hakea obtusa"), Collections.emptyList(), 0, 10);
		assertThat(result.getResponse().size(), is(1));
		Set<String> scientificNamesForFirstRecord = result.getResponse().get(0).getScientificNames();
		assertThat(scientificNamesForFirstRecord.size(), is(1));
		assertThat(scientificNamesForFirstRecord, hasItems("Dodonaea concinna Benth."));
		Set<String> taxonRemarksForFirstRecord = result.getResponse().get(0).getTaxonRemarks();
		assertThat(taxonRemarksForFirstRecord.size(), is(1));
		assertThat(taxonRemarksForFirstRecord, hasItems("Hakea obtusa"));
	}
	
	/**
	 * Do we only get the visit with the same date as the species record (and not other visits to the same location)?
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetEnvironmentalDataJson08() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Acacia binata Maslin"), Collections.emptyList(), 0, 10);
		assertThat(result.getResponse().size(), is(1));
		Set<String> scientificNamesForFirstRecord = result.getResponse().get(0).getScientificNames();
		assertThat(scientificNamesForFirstRecord.size(), is(1));
		assertThat(scientificNamesForFirstRecord, hasItems("Acacia binata Maslin"));
		Collection<TraitOrEnvironmentalVariable> vars = result.getResponse().get(0).getVariables();
		assertThat(vars.size(), is(2));
		assertThat(vars, hasItems(
			isTraitOrVar("slope", "5", "degrees"),
			isTraitOrVar("aspect", "216", "degrees")
		));
	}
	
	/**
	 * Can we request two species at the same location but different visits and have the records correctly
	 * record which species appeared in which visit?
	 */
	@Test
	public void testGetEnvironmentalDataJson09() throws Throwable {
		EnvironmentDataResponse result = objectUnderTest.getEnvironmentalDataJson(Arrays.asList("Acacia binata Maslin", "Lasiopetalum compactum"), Collections.emptyList(), 0, 10);
		assertThat(result.getResponse().size(), is(2));
		EnvironmentDataRecord firstRecord = result.getResponse().get(0);
		assertThat(firstRecord.getScientificNames(), hasItems("Acacia binata Maslin"));
		assertThat(firstRecord.getScientificNames().size(), is(1));
		EnvironmentDataRecord secondRecord = result.getResponse().get(1);
		assertThat(secondRecord.getScientificNames(), hasItems("Lasiopetalum compactum"));
		assertThat(secondRecord.getScientificNames().size(), is(1));
		assertThat(firstRecord.getLocationID(), is(secondRecord.getLocationID()));
	}
	
	/**
	 * Can we get the visit info for two species at the same site?
	 */
	@Test
	public void testGetVisitInfoFor01() throws Throwable {
		VisitTracker result = objectUnderTest.getVisitInfoFor(Arrays.asList("Dodonaea concinna Benth.", "Hakea obtusa"));
		assertThat(result.visitSize(), is(1));
		VisitInfo locInfo = result.getVisitInfoFor("aekos.org.au/collection/wa.gov.au/ravensthorpe/R002", "1990-03-30");
		assertThat(locInfo.getBibliographicCitation(), startsWith("Department of Parks and Wildlife (Biogeography Program) (2012)."));
		assertThat(locInfo.getSamplingProtocol(), is("aekos.org.au/collection/wa.gov.au/ravensthorpe"));
		Set<String> scientificNames = locInfo.getScientificNames();
		assertThat(scientificNames.size(), is(1));
		assertThat(scientificNames, hasItems("Dodonaea concinna Benth."));
		Set<String> taxonRemarks = locInfo.getTaxonRemarks();
		assertThat(taxonRemarks.size(), is(1));
		assertThat(taxonRemarks, hasItems("Hakea obtusa"));
	}
	
	/**
	 * Can we map all the variables for a trait data record?
	 */
	@Test
	public void testGetTraitDataJson01() throws Throwable {
		TraitDataResponse result = objectUnderTest.getTraitDataJson(Arrays.asList("Goodenia havilandii"), Collections.emptyList(), 0, 20);
		ResponseHeader header = result.getResponseHeader();
		assertThat(header.getPageNumber(), is(1));
		assertThat(header.getTotalPages(), is(1));
		assertThat(header.getNumFound(), is(1));
		List<TraitDataRecord> response = result.getResponse();
		assertThat(response.size(), is(1));
		TraitDataRecord record = response.get(0);
		Collection<TraitOrEnvironmentalVariable> traits = record.getTraits();
		assertThat(traits.size(), is(3));
		Iterator<TraitOrEnvironmentalVariable> traitsIterator = traits.iterator();
		assertThat(traitsIterator.next(), isTraitOrVar("phenology", "1", "thingies"));
		assertThat(traitsIterator.next(), isTraitOrVar("totalLength", "0.3", "meters"));
		assertThat(traitsIterator.next(), isTraitOrVar("heightOfBreak", "5", "meters"));
	}
	
	/**
	 * Can we correctly filter for trait names?
	 */
	@Test
	public void testGetTraitDataJson02() throws Throwable {
		TraitDataResponse result1 = objectUnderTest.getTraitDataJson(Arrays.asList("Calotis hispidula"), Collections.emptyList(), 0, 20);
		assertThat(result1.getResponseHeader().getNumFound(), is(2));
		TraitDataResponse result2 = objectUnderTest.getTraitDataJson(Arrays.asList("Calotis hispidula"), Arrays.asList("lifeStage"), 0, 20);
		assertThat(result2.getResponseHeader().getNumFound(), is(1));
		List<TraitDataRecord> response = result2.getResponse();
		assertThat(response.size(), is(1));
		TraitDataRecord record = response.get(0);
		assertThat(record.getYear(), is(1990));
	}
	
	/**
	 * Can we defend against a SPARQL injection attack on the species name?
	 */
	@Test
	public void testGetTraitDataJson03() throws Throwable {
		TraitDataResponse result = objectUnderTest.getTraitDataJson(Arrays.asList("\" UNDEF \""), Collections.emptyList(), 0, 20);
		assertThat("should be nothing because no species match the escaped text", result.getResponse().size(), is(0));
		assertThat(result.getResponseHeader().getNumFound(), is(0));
	}
	
	/**
	 * Can we get trait data for a record with a taxonRemarks field?
	 */
	@Test
	public void testGetTraitDataJson04() throws Throwable {
		TraitDataResponse result = objectUnderTest.getTraitDataJson(Arrays.asList("Hakea obtusa"), Collections.emptyList(), 0, 20);
		ResponseHeader header = result.getResponseHeader();
		assertThat(header.getPageNumber(), is(1));
		assertThat(header.getTotalPages(), is(1));
		assertThat(header.getNumFound(), is(1));
		List<TraitDataRecord> response = result.getResponse();
		assertThat(response.size(), is(1));
		TraitDataRecord record = response.get(0);
		Collection<TraitOrEnvironmentalVariable> traits = record.getTraits();
		assertThat(traits.size(), is(2));
		Iterator<TraitOrEnvironmentalVariable> traitsIterator = traits.iterator();
		assertThat(traitsIterator.next(), isTraitOrVar("averageHeight", "2", "metres"));
		assertThat(traitsIterator.next(), isTraitOrVar("cover", "<10% cover", ""));
	}
	
	/**
	 * Can we get the trait data CSV?
	 */
	@Test
	public void testGetTraitDataCsv01() throws Throwable {
		Writer writer = new StringWriter();
		objectUnderTest.getTraitDataCsv(Arrays.asList("Calotis hispidula"), Collections.emptyList(), 0, 20, writer);
		String compareStr = testGetTraitDataCsv01_expected;
		if(SystemUtils.IS_OS_WINDOWS){
			compareStr = testGetTraitDataCsv01_expected.replaceAll("\r", "");
		}
		assertEquals(compareStr, writer.toString());
	}
	
	/**
	 * Can we count species records when there are some?
	 */
	@Test
	public void testGetTotalRecordsHeldForSpeciesName01() {
		int result = objectUnderTest.getTotalRecordsHeldForSpeciesName("Calotis hispidula");
		assertThat(result, is(2));
	}
	
	/**
	 * Can we count species records when there are none?
	 */
	@Test
	public void testGetTotalRecordsHeldForSpeciesName02() {
		int result = objectUnderTest.getTotalRecordsHeldForSpeciesName("blah blah");
		assertThat(result, is(0));
	}
	
	/**
	 * Can we count all the species records?
	 */
	@Test
	public void testGetTotalSpeciesRecordsHeld01() throws Throwable {
		int result = objectUnderTest.getTotalSpeciesRecordsHeld();
		assertThat(result, is(8));
	}
}