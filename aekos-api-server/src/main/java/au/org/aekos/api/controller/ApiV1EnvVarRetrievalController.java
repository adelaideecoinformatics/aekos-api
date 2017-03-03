package au.org.aekos.api.controller;

import static au.org.aekos.api.controller.ControllerHelper.CONTENT_NEGOTIATION_FRAGMENT;
import static au.org.aekos.api.controller.ControllerHelper.DATA_RETRIEVAL_BY_SPECIES_TAG;
import static au.org.aekos.api.controller.ControllerHelper.DEFAULT_ROWS;
import static au.org.aekos.api.controller.ControllerHelper.DEFAULT_START;
import static au.org.aekos.api.controller.ControllerHelper.RETRIEVAL_BY_SPECIES_DESC;
import static au.org.aekos.api.controller.ControllerHelper.TEXT_CSV_MIME;
import static au.org.aekos.api.controller.ControllerHelper.buildHateoasLinkHeader;
import static au.org.aekos.api.controller.ControllerHelper.extractFullUrl;

import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import au.org.aekos.api.Constants;
import au.org.aekos.api.model.EnvironmentDataResponse;
import au.org.aekos.api.service.retrieval.AekosApiRetrievalException;
import au.org.aekos.api.service.retrieval.RetrievalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Api(description=RETRIEVAL_BY_SPECIES_DESC, produces=MediaType.APPLICATION_JSON_VALUE, tags=DATA_RETRIEVAL_BY_SPECIES_TAG)
@RestController
@RequestMapping(path=Constants.V1_0, method=RequestMethod.GET)
public class ApiV1EnvVarRetrievalController {

	private static final String SITE_FRAGMENT = "site/study location/plot visits";
	private static final String ENVVAR_FILTERING_FRAGMENT = " If you supply "
			+ "environmental variable names then the result will have the environmental variables filtered down to only the environmental variables "
			+ "you've asked for, otherwise all environmental variables are returned.";
	private static final String NO_VARS_NOTE_FRAGMENT = " Note: not all sites have environmental variables available. ";

	@Autowired
	private RetrievalService retrievalService;
	
    @RequestMapping(path="/environmentData.json", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get environmental variable data in JSON format",
    		notes = "Gets environmental variable data records for the " + SITE_FRAGMENT + " that the supplied species "
    				+ "name(s) appear at in JSON format." + ENVVAR_FILTERING_FRAGMENT + NO_VARS_NOTE_FRAGMENT)
    public EnvironmentDataResponse environmentDataDotJson(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(name="envVarName", required=false) String[] envVarNames,
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		HttpServletRequest req, HttpServletResponse resp) throws AekosApiRetrievalException {
    	List<String> varNames = envVarNames != null ? Arrays.asList(envVarNames) : Collections.emptyList();
		EnvironmentDataResponse result = retrievalService.getEnvironmentalDataJson(Arrays.asList(speciesNames), varNames, start, rows);
		resp.addHeader(HttpHeaders.LINK, buildHateoasLinkHeader(UriComponentsBuilder.fromHttpUrl(extractFullUrl(req)), RetrievalResponseHeader.newInstance(result)));
    	return result;
	}
    
    @RequestMapping(path="/environmentData", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get environmental variable data",
			notes = "Gets environmental variable data records for the " + SITE_FRAGMENT + " that the supplied species "
					+ "name(s) appear at " + CONTENT_NEGOTIATION_FRAGMENT + ENVVAR_FILTERING_FRAGMENT + NO_VARS_NOTE_FRAGMENT
					+ " This resource honours <code>Accept</code> headers represented by any of the <code>/environmentData.*</code> resources.",
			produces=MediaType.APPLICATION_JSON_VALUE + ", " + TEXT_CSV_MIME) // Forcing Swagger content negotiation until support for two methods is in)
    public EnvironmentDataResponse environmentDataJson(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(name="envVarName", required=false) String[] envVarNames,
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		HttpServletRequest req, HttpServletResponse resp) throws AekosApiRetrievalException {
    	return environmentDataDotJson(speciesNames, envVarNames, start, rows, req, resp);
	}
    
    @RequestMapping(path="/environmentData.csv", produces=TEXT_CSV_MIME)
    @ApiOperation(value = "Get environmental variable data in CSV format",
    		notes = "Gets environmental variable data records for the " + SITE_FRAGMENT + " that the supplied species "
    				+ "name(s) appear at in CSV format." + ENVVAR_FILTERING_FRAGMENT + NO_VARS_NOTE_FRAGMENT)
    public void environmentDataDotCsv(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(name="envVarName", required=false) String[] envVarNames,
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		HttpServletRequest req, HttpServletResponse resp, Writer responseWriter) throws AekosApiRetrievalException {
    	resp.setContentType(TEXT_CSV_MIME);
    	List<String> varNames = envVarNames != null ? Arrays.asList(envVarNames) : Collections.emptyList();
    	RetrievalResponseHeader header = retrievalService.getEnvironmentalDataCsv(Arrays.asList(speciesNames), varNames, start, rows, responseWriter);
		resp.addHeader(HttpHeaders.LINK, buildHateoasLinkHeader(UriComponentsBuilder.fromHttpUrl(extractFullUrl(req)), header));
	}
    
    @RequestMapping(path="/environmentData", produces=TEXT_CSV_MIME, headers="Accept="+TEXT_CSV_MIME)
    // Not defining another @ApiOperation as it won't generate the expected swagger doco. Remove @ApiIgnore when fixed
    // See https://github.com/springfox/springfox/issues/1367 for more info about when this is coming.
    @ApiIgnore
    public void environmentDataCsv(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(name="envVarName", required=false) String[] envVarNames,
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		HttpServletRequest req, HttpServletResponse resp, Writer responseWriter) throws AekosApiRetrievalException {
    	environmentDataDotCsv(speciesNames, envVarNames, start, rows, req, resp, responseWriter);
	}
}
