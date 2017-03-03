package au.org.aekos.api.controller;

import static au.org.aekos.api.controller.ApiV1_0SpeciesRetrievalController.HONOURS_PREFIX;
import static au.org.aekos.api.controller.ApiV1_0SpeciesRetrievalController.HONOURS_SUFFIX;
import static au.org.aekos.api.controller.ControllerHelper.CONTENT_NEGOTIATION_FRAGMENT;
import static au.org.aekos.api.controller.ControllerHelper.DATA_RETRIEVAL_ALL_TAG;
import static au.org.aekos.api.controller.ControllerHelper.DEFAULT_ROWS;
import static au.org.aekos.api.controller.ControllerHelper.DEFAULT_START;
import static au.org.aekos.api.controller.ControllerHelper.DL_PARAM_MSG;
import static au.org.aekos.api.controller.ControllerHelper.RETRIEVAL_ALL_DESC;
import static au.org.aekos.api.controller.ControllerHelper.TEXT_CSV_MIME;
import static au.org.aekos.api.controller.ControllerHelper.buildHateoasLinkHeader;
import static au.org.aekos.api.controller.ControllerHelper.extractFullUrl;
import static au.org.aekos.api.controller.ControllerHelper.generateDownloadFileName;

import java.io.Writer;
import java.util.Date;

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
import au.org.aekos.api.model.SpeciesDataResponseV1_0;
import au.org.aekos.api.service.retrieval.AekosApiRetrievalException;
import au.org.aekos.api.service.retrieval.RetrievalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Api(description=RETRIEVAL_ALL_DESC, produces=MediaType.APPLICATION_JSON_VALUE, tags=DATA_RETRIEVAL_ALL_TAG)
@RestController
@RequestMapping(path=Constants.V1_0, method=RequestMethod.GET)
public class ApiV1AllSpeciesRetrievalController {

	private static final String ALL_HONOURS_HEADERS = HONOURS_PREFIX + "allS" + HONOURS_SUFFIX;
	
	@Autowired
	private RetrievalService retrievalService;
	
    @RequestMapping(path="/allSpeciesData.json", produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all species occurrence data in JSON format",
    		notes = "Gets all Darwin Core records in JSON format.")
    public SpeciesDataResponseV1_0 allSpeciesDataDotJson(
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		HttpServletRequest req, HttpServletResponse resp) throws AekosApiRetrievalException {
		SpeciesDataResponseV1_0 result = retrievalService.getAllSpeciesDataJsonV1_0(start, rows);
		resp.addHeader(HttpHeaders.LINK, buildHateoasLinkHeader(UriComponentsBuilder.fromHttpUrl(extractFullUrl(req)), RetrievalResponseHeader.newInstance(result)));
    	return result;
	}
    
    @RequestMapping(path="/allSpeciesData", produces=MediaType.APPLICATION_JSON_VALUE,
    		headers="Accept="+MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all species data",
    		notes = "Gets all Darwin Core records" + CONTENT_NEGOTIATION_FRAGMENT + ALL_HONOURS_HEADERS,
    		produces=MediaType.APPLICATION_JSON_VALUE + ", " + TEXT_CSV_MIME) // Forcing Swagger content negotiation until support for two methods is in
    public SpeciesDataResponseV1_0 allSpeciesDataJson(
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		HttpServletRequest req, HttpServletResponse resp) throws AekosApiRetrievalException {
		return allSpeciesDataDotJson(start, rows, req, resp);
    }

	@RequestMapping(path="/allSpeciesData.csv", produces=TEXT_CSV_MIME)
    @ApiOperation(value = "Get all species occurrence data in CSV format",
    		notes = "Gets all Darwin Core records in CSV format.")
    public void allSpeciesDataDotCsv(
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		@RequestParam(required=false, defaultValue="false") @ApiParam(DL_PARAM_MSG) boolean download,
    		Writer responseWriter, HttpServletRequest req, HttpServletResponse resp) throws AekosApiRetrievalException {
		resp.setContentType(TEXT_CSV_MIME);
    	if (download) {
    		resp.setHeader("Content-Disposition", "attachment;filename=" + generateDownloadFileName("species", new Date()));
    	}
		RetrievalResponseHeader header = retrievalService.getAllSpeciesDataCsvV1_0(start, rows, responseWriter);
		resp.addHeader(HttpHeaders.LINK, buildHateoasLinkHeader(UriComponentsBuilder.fromHttpUrl(extractFullUrl(req)), header));
    }
	
    @RequestMapping(path="/allSpeciesData", produces=TEXT_CSV_MIME, headers="Accept="+TEXT_CSV_MIME)
    // Not defining another @ApiOperation as it won't generate the expected swagger doco. Remove @ApiIgnore when fixed
    // See https://github.com/springfox/springfox/issues/1367 for more info about when this is coming.
    @ApiIgnore
    public void allSpeciesDataCsv(
    		@RequestParam(required=false, defaultValue=DEFAULT_START) @ApiParam("0-indexed result page start") int start,
    		@RequestParam(required=false, defaultValue=DEFAULT_ROWS) @ApiParam("result page size") int rows,
    		Writer responseWriter, HttpServletRequest req, HttpServletResponse resp) throws AekosApiRetrievalException {
    	boolean dontDownload = false;
		allSpeciesDataDotCsv(start, rows, dontDownload, responseWriter, req, resp);
    }
}
