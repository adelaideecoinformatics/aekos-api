package au.org.aekos.controller;

import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import au.org.aekos.model.EnvironmentDataRecord;
import au.org.aekos.model.SpeciesDataRecord;
import au.org.aekos.model.TraitDataRecord;
import au.org.aekos.service.retrieval.AekosApiRetrievalException;
import au.org.aekos.service.retrieval.RetrievalService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Api(value = "AekosV1", produces=MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequestMapping("/v1")
public class ApiV1RetrievalController {

	private static final String TEXT_CSV_MIME = "text/csv";
	private static final String DL_PARAM_MSG = "Makes the response trigger a downloadable file rather than streaming the response";

	// TODO add content negotiation methods for all *data resources
	// TODO add lots more Swagger doco
	// TODO figure out how to get Swagger to support content negotiation with overloaded methods
	// TODO am I doing content negotiation correctly?
	// TODO define coord ref system
	// TODO do we accept LSID/species ID and/or a species name for the species related services?
	
	@Autowired
	@Qualifier("stubRetrievalService")
	private RetrievalService retrievalService;
	
	@RequestMapping(path="/speciesData.json", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get species data in JSON format", notes = "Gets Aekos data", tags="Data Retrieval")
    public List<SpeciesDataRecord> speciesDataDotJson(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(required=false) Integer limit, HttpServletResponse resp) {
		setCommonHeaders(resp);
		try {
			return retrievalService.getSpeciesDataJson(Arrays.asList(speciesNames), limit);
		} catch (AekosApiRetrievalException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new RuntimeException(e);
		}
	}
	
	@RequestMapping(path="/speciesData", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE,
    		headers="Accept="+MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get species data", notes = "Gets Aekos data", tags="Data Retrieval")
    public List<SpeciesDataRecord> speciesDataJson(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(required=false) Integer limit, HttpServletResponse resp) {
		setCommonHeaders(resp);
		try {
			return retrievalService.getSpeciesDataJson(Arrays.asList(speciesNames), limit);
		} catch (AekosApiRetrievalException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new RuntimeException(e);
		}
    }

	@RequestMapping(path="/speciesData.csv", method=RequestMethod.GET, produces=TEXT_CSV_MIME)
    @ApiOperation(value = "Get species data in CSV format", notes = "TODO", tags="Data Retrieval")
    public void speciesDataDotCsv(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(required=false) Integer limit,
    		@RequestParam(required=false, defaultValue="false") @ApiParam(DL_PARAM_MSG) boolean download,
    		@ApiIgnore Writer responseWriter, HttpServletResponse resp) {
		setCommonHeaders(resp);
		resp.setContentType(TEXT_CSV_MIME);
    	if (download) {
    		resp.setHeader("Content-Disposition", "attachment;filename=aekosSpeciesData.csv"); // TODO give a more dynamic name
    	}
		try {
			retrievalService.getSpeciesDataCsv(Arrays.asList(speciesNames), limit, download, responseWriter);
		} catch (AekosApiRetrievalException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new RuntimeException(e);
		}
    }
	
    @RequestMapping(path="/speciesData", method=RequestMethod.GET, produces=TEXT_CSV_MIME, headers="Accept="+TEXT_CSV_MIME)
    //FIXME what do I put in here? Do I copy from the other overloaded method?
    @ApiOperation(value = "Get species data blah", notes = "Gets Aekos data", tags="Data Retrieval")
    public void speciesDataCsv(
    		@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(required=false) Integer limit,
    		@ApiIgnore Writer responseWriter, HttpServletResponse resp) {
    	setCommonHeaders(resp);
    	resp.setContentType(TEXT_CSV_MIME);
    	try {
    		retrievalService.getSpeciesDataCsv(Arrays.asList(speciesNames), limit, false, responseWriter);
    	} catch (AekosApiRetrievalException e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			throw new RuntimeException(e);
		}
    }
    
    @RequestMapping(path="/traitData.json", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all trait data for the specified species", notes = "TODO", tags="Data Retrieval")
    public List<TraitDataRecord> traitDataJson(@RequestParam(name="speciesName") String[] speciesNames, 
    		@RequestParam(name="traitName") String[] traitNames, HttpServletResponse resp) {
    	// TODO do we include units in the field name, as an extra value or as a header/metadata object in the resp
    	setCommonHeaders(resp);
    	return retrievalService.getTraitData(Arrays.asList(speciesNames), Arrays.asList(traitNames));
	}
    
    @RequestMapping(path="/environmentData.json", method=RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all environment data for the specified species", notes = "TODO", tags="Data Retrieval")
    public List<EnvironmentDataRecord> environmentDataJson(@RequestParam(name="speciesName") String[] speciesNames,
    		@RequestParam(name="envVarName") String[] envVarNames, HttpServletResponse resp) {
    	// TODO do we include units in the field name, as an extra value or as a header/metadata object in the resp
    	setCommonHeaders(resp);
    	List<EnvironmentDataRecord> result = retrievalService.getEnvironmentalData(Arrays.asList(speciesNames), Arrays.asList(envVarNames));
    	return result;
	}
    
	private void setCommonHeaders(HttpServletResponse resp) {
		resp.setHeader("Access-Control-Allow-Origin", "*"); // FIXME replace with @CrossOrigin
	}
}