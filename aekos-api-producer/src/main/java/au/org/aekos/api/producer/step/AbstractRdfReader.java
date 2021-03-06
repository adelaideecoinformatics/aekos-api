package au.org.aekos.api.producer.step;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public abstract class AbstractRdfReader<T> implements ItemReader<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRdfReader.class);
	private Dataset ds;
	private boolean isInitialised = false;
	private ResultSet theResults;
	private int processedRecords = 0;
	private long start;
	private QueryExecution qexec;
	
	@Override
	public synchronized T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		checkInit();
		if (qexec.isClosed()) { // FIXME not sure why we still get a call after we've returned the pill
			return null;
		}
		boolean isNoMoreRecords = !theResults.hasNext();
		if (isNoMoreRecords) {
			long elapsed = (now() - start) / 1000;
			logger.info(String.format("Processed %d %s records in %d seconds", processedRecords, getRecordTypeName(), elapsed));
			T poisonPill = null;
			close();
			return poisonPill;
		}
		QuerySolution currSolution = theResults.next();
		try {
			processedRecords++;
			return mapSolution(currSolution);
		} catch (Throwable t) {
			Iterable<String> iterable = () -> currSolution.varNames();
			Set<String> vars = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toSet());
			Set<String> pairs = vars.stream().map(currVar -> currVar + "=" + currSolution.get(currVar)).collect(Collectors.toSet());
			throw new RuntimeException("Failed while processing a solution. Available values: " + pairs, t);
		}
	}

	public abstract T mapSolution(QuerySolution solution);

	public abstract String getRecordTypeName();
	
	public abstract String getSparqlQuery();

	private void close() {
		qexec.close();
	}

	private void checkInit() {
		if (isInitialised) return;
		String sparql = getSparqlQuery();
		Query query = QueryFactory.create(sparql);
		start = now();
		qexec = QueryExecutionFactory.create(query, ds);
		theResults = qexec.execSelect();
		if (!theResults.hasNext()) {
			throw new IllegalStateException("Data problem: no results were found. "
					+ "Do you have RDF AEKOS data loaded?");
		}
		isInitialised = true;
	}

	private long now() {
		return new Date().getTime();
	}

	public void setDs(Dataset ds) {
		this.ds = ds;
	}
}
