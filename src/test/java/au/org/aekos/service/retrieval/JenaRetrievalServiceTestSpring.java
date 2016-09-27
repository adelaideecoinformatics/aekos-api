package au.org.aekos.service.retrieval;

import static au.org.aekos.TraitOrEnvVarMatcher.isTraitOrVar;
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
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import au.org.aekos.model.EnvironmentDataRecord;
import au.org.aekos.model.EnvironmentDataResponse;
import au.org.aekos.model.LocationInfo;
import au.org.aekos.model.ResponseHeader;
import au.org.aekos.model.SpeciesDataResponse;
import au.org.aekos.model.TraitDataRecord;
import au.org.aekos.model.TraitDataResponse;
import au.org.aekos.model.TraitOrEnvironmentalVariable;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/au/org/aekos/retrievalContext-test.xml")
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
	public void testGetSpeciesDataJson01() throws Throwable {
		SpeciesDataResponse result = objectUnderTest.getSpeciesDataJson(Arrays.asList("Calotis hispidula"), 0, 10);
		assertThat(result.getResponse().size(), is(2));
	}

	/**
	 * Can we get all the records that are available for multiple species?
	 */
	@Test
	public void testGetSpeciesDataJson02() throws Throwable {
		SpeciesDataResponse result = objectUnderTest.getSpeciesDataJson(Arrays.asList("Calotis hispidula", "Goodenia havilandii"), 0, 10);
		assertThat(result.getResponse().size(), is(3));
	}

	/**
	 * Can we limit the records that we get for the specified species?
	 */
	@Test
	public void testGetSpeciesDataJson03() throws Throwable {
		int onlyOne = 1;
		SpeciesDataResponse result = objectUnderTest.getSpeciesDataJson(Arrays.asList("Calotis hispidula"), 0, onlyOne);
		assertThat(result.getResponse().size(), is(1));
		assertThat(result.getResponseHeader().getNumFound(), is(2));
	}
	
	/**
	 * Can we defend against a SPARQL injection attack?
	 */
	@Test
	public void testGetSpeciesDataJson04() throws Throwable {
		SpeciesDataResponse result = objectUnderTest.getSpeciesDataJson(Arrays.asList("\" UNDEF \""), 0, 1);
		assertThat("should be nothing because no species match the escaped text", result.getResponse().size(), is(0));
		assertThat(result.getResponseHeader().getNumFound(), is(0));
	}
	
	/**
	 * Can we get a species record that uses taxonRemarks rather than scientificName?
	 */
	@Test
	public void testGetSpeciesDataJson05() throws Throwable {
		SpeciesDataResponse result = objectUnderTest.getSpeciesDataJson(Arrays.asList("Hakea obtusa"), 0, 1);
		assertThat(result.getResponse().size(), is(1));
		assertThat(result.getResponse().get(0).getTaxonRemarks(), is("Hakea obtusa"));
	}

	/**
	 * Can we get all the species records as JSON?
	 */
	@Test
	public void testGetAllSpeciesDataJson01() throws Throwable {
		SpeciesDataResponse result = objectUnderTest.getAllSpeciesDataJson(0, 10);
		assertThat(result.getResponse().size(), is(7));
	}
	
	/**
	 * Can we get all the records that are available for the specified species?
	 */
	@Test
	public void testGetSpeciesDataCsv01() throws Throwable {
		Writer writer = new StringWriter();
		objectUnderTest.getSpeciesDataCsv(Arrays.asList("Calotis hispidula"), 0, 20, writer);
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
		objectUnderTest.getSpeciesDataCsv(Arrays.asList("Calotis hispidula", "Goodenia havilandii"), 0, 20, writer);
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
		objectUnderTest.getSpeciesDataCsv(Arrays.asList("Calotis hispidula"), 0, onlyOne, writer);
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
		objectUnderTest.getAllSpeciesDataCsv(0, 20, writer);
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
		assertThat(record.getLocationID(), is("aekos.org.au/collection/adelaide.edu.au/trend/SATFLB0025"));
		assertThat(record.getBibliographicCitation(), is("TERN Australian Transect Network, Biomes of Australian Soils Consortium (2015). Transects for Environmental "
				+ "Monitoring and Decision Making (TREND) (2013-2015) and TREND-Biome of Australia Soil Environments (BASE) - "
				+ "Soil samples for physical structure and chemical analysis (14 sites) throughout Australia (2013), Version 11/2015. "
				+ "Persistent Hyperlink: http://www.aekos.org.au/collection/adelaide.edu.au/trend. &#198;KOS Data Portal, rights "
				+ "owned by The University of Adelaide (TERN Australian Transects Network - TREND), Bioplatforms Australia Ltd. "
				+ "Accessed [dd mmm yyyy, e.g. 01 Jan 2016]."));
		assertThat(record.getSamplingProtocol(), is("aekos.org.au/collection/adelaide.edu.au/trend"));
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
		assertThat(scientificNamesForFirstRecord, hasItems("Calotis hispidula"));
		Set<String> scientificNamesForSecondRecord = result.getResponse().get(1).getScientificNames();
		assertThat(scientificNamesForSecondRecord.size(), is(1));
		assertThat(scientificNamesForSecondRecord, hasItems("Rosa canina"));
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
	 * Can we get the location mapping for two species at the same site?
	 */
	@Test
	public void testGetLocations01() throws Throwable {
		Map<String, LocationInfo> result = objectUnderTest.getLocations(Arrays.asList("Dodonaea concinna Benth.", "Hakea obtusa"));
		assertThat(result.size(), is(1));
		LocationInfo locInfo = result.get("aekos.org.au/collection/wa.gov.au/ravensthorpe/R002");
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
		assertThat(record.getYear(), is(2013));
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
		assertThat(result, is(7));
	}
}
