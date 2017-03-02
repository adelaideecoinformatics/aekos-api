package au.org.aekos.api.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.org.aekos.api.controller.ApiV1SpeciesRetrievalControllerTest.ApiV1SpeciesRetrievalControllerTestContext;
import au.org.aekos.api.model.ResponseHeader;
import au.org.aekos.api.model.SpeciesDataParams;
import au.org.aekos.api.model.SpeciesDataResponse;
import au.org.aekos.api.model.SpeciesOccurrenceRecord;
import au.org.aekos.api.service.retrieval.RetrievalService;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableWebMvc
@WebAppConfiguration
@ContextConfiguration(classes=ApiV1SpeciesRetrievalControllerTestContext.class)
@ActiveProfiles({"test", "force-only-ApiV1SpeciesRetrievalControllerTest"})
public class ApiV1SpeciesRetrievalControllerTest {
	
	@Autowired
	private WebApplicationContext wac;
	
	private MockMvc mockMvc;
	
	@Before
	public void setup() {
		this.mockMvc = webAppContextSetup(this.wac).build();
	}
	
	@Autowired
	@Qualifier("testSpeciesDataDotJson01_expected")
	private String testSpeciesDataDotJson01_expected;
	
	@Autowired
	@Qualifier("testSpeciesDataJson01_expected")
	private String testSpeciesDataJson01_expected;
	
	@Autowired
	@Qualifier("testSpeciesDataDotCsv01_expected")
	private String testSpeciesDataDotCsv01_expected;
	
	@Autowired
	@Qualifier("testSpeciesDataCsv01_expected")
	private String testSpeciesDataCsv01_expected;
	
	/**
	 * Can we get species data in JSON format?
	 */
	@Test
	public void testSpeciesDataDotJson01() throws Exception {
		mockMvc.perform(get("/v1/speciesData.json")
				.param("speciesName", "Acacia aneura")
				.param("start", "0")
				.param("rows", "20"))
            .andExpect(status().isOk())
            .andExpect(content().string(testSpeciesDataDotJson01_expected));
	}
	
	/**
	 * Can we get species data in JSON format based on accept headers?
	 */
	@Test
	public void testSpeciesDataJson01() throws Exception {
		mockMvc.perform(get("/v1/speciesData")
				.param("speciesName", "Acacia aneura")
				.param("start", "0")
				.param("rows", "20")
    			.accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(testSpeciesDataJson01_expected));
	}
	
	/**
	 * Can we get species data in CSV format?
	 */
	@Test
	public void testSpeciesDataDotCsv01() throws Exception {
		mockMvc.perform(get("/v1/speciesData.csv")
				.param("speciesName", "Acacia aneura")
				.param("start", "0")
				.param("rows", "20"))
            .andExpect(status().isOk())
            .andExpect(content().string(testSpeciesDataDotCsv01_expected));
	}
	
	/**
	 * Can we get species data in CSV format based on accept headers?
	 */
	@Test
	public void testSpeciesDataCsv01() throws Exception {
		mockMvc.perform(get("/v1/speciesData")
				.param("speciesName", "Acacia aneura")
				.param("start", "0")
				.param("rows", "20")
				.accept(ControllerHelper.TEXT_CSV_MIME))
            .andExpect(status().isOk())
            .andExpect(content().string(testSpeciesDataCsv01_expected));
	}
	
	@TestConfiguration
	@ComponentScan(
			basePackages={"au.org.aekos.api.controller"},
			includeFilters={
					@Filter(type=FilterType.ASSIGNABLE_TYPE, classes=ApiV1SearchController.class),
			},
			excludeFilters={
					@Filter(type=FilterType.REGEX, pattern="au.org.aekos.api.(?!controller.ApiV1SpeciesRetrieval).*"),	
			})
	@Profile("force-only-ApiV1SpeciesRetrievalControllerTest")
	static class ApiV1SpeciesRetrievalControllerTestContext {
		@Bean
		public RetrievalService retrievalService() throws Throwable {
			RetrievalService result = mock(RetrievalService.class);
			when(result.getSpeciesDataJson(Arrays.asList("Acacia aneura"), 0, 20)).thenReturn(
					new SpeciesDataResponse(new ResponseHeader(1, 1, 1, 54, new SpeciesDataParams(0, 20, Arrays.asList("Acacia aneura"))),
							Arrays.asList(new SpeciesOccurrenceRecord(-27.4046619945479d, 120.681093054999d, "GDA94",
									"aekos.org.au/collection/wa.gov.au/swatt/Wanjarri", "Acacia aneura", 1, "2013-08-27", 2013, 8,
									"TERN Australian Transect Network, Department of Parks and Wildlife, Western Australia (2013). South "
									+ "West Australian Transitional Transect (SWATT), Version 11/2014. Persistent Hyperlink: "
									+ "http://www.aekos.org.au/collection/wa.gov.au/swatt. &#198;KOS Data Portal, rights owned by The "
									+ "University of Adelaide, Adelaide, South Australia and the State of Western Australia (Department "
									+ "of Parks and Wildlife). Accessed [dd mmm yyyy, e.g. 01 Jan 2016].",
									"aekos.org.au/collection/wa.gov.au/swatt"))));
			when(result.getSpeciesDataCsv(eq(Arrays.asList("Acacia aneura")), eq(0), eq(20), any(Writer.class))).then(getSpeciesDataCsvAnswer());
			return result;
		}
		
		private Answer<RetrievalResponseHeader> getSpeciesDataCsvAnswer() {
			Answer<RetrievalResponseHeader> result = new Answer<RetrievalResponseHeader>() {
				@Override
				public RetrievalResponseHeader answer(InvocationOnMock invocation) throws Throwable {
					Writer w = invocation.getArgumentAt(3, Writer.class);
					w.append(SpeciesOccurrenceRecord.getCsvHeader() + "\n");
					CharSequence record = new SpeciesOccurrenceRecord(-27.4046619945479d, 120.681093054999d, "GDA94",
							"aekos.org.au/collection/wa.gov.au/swatt/Wanjarri", "Acacia aneura", 1, "2013-08-27", 2013, 8,
							"TERN Australian Transect Network, Department of Parks and Wildlife, Western Australia (2013). South "
							+ "West Australian Transitional Transect (SWATT), Version 11/2014. Persistent Hyperlink: "
							+ "http://www.aekos.org.au/collection/wa.gov.au/swatt. &#198;KOS Data Portal, rights owned by The "
							+ "University of Adelaide, Adelaide, South Australia and the State of Western Australia (Department "
							+ "of Parks and Wildlife). Accessed [dd mmm yyyy, e.g. 01 Jan 2016].",
							"aekos.org.au/collection/wa.gov.au/swatt").toCsv();
					w.append(record);
					return new RetrievalResponseHeader(0, 20, 1, 1);
				}
			};
			return result;
		}

		@Bean public String testSpeciesDataDotJson01_expected() throws IOException { return getJsonWithoutWhitespace("testSpeciesDataDotJson01_expected.json"); }
		@Bean public String testSpeciesDataJson01_expected(String testSpeciesDataDotJson01_expected) {
			return testSpeciesDataDotJson01_expected;
		}
		@Bean public String testSpeciesDataDotCsv01_expected() throws IOException { return getFileVerbatim("testSpeciesDataDotCsv01_expected.csv"); }
		@Bean public String testSpeciesDataCsv01_expected(String testSpeciesDataDotCsv01_expected) {
			return testSpeciesDataDotCsv01_expected;
		}
		
		private String getJsonWithoutWhitespace(String fileName) throws IOException {
			return new ObjectMapper().readValue(getFileVerbatim(fileName), JsonNode.class).toString();
		}
		
		private String getFileVerbatim(String fileName) throws IOException {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("au/org/aekos/api/retrieval/" + fileName);
			OutputStream out = new ByteArrayOutputStream();
			StreamUtils.copy(is, out);
			return out.toString();
		}
	}
}
