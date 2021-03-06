package au.org.aekos.api.producer.step.env;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import au.org.aekos.api.producer.ResourceStringifier;
import au.org.aekos.api.producer.step.env.in.InputEnvRecord;

public class AekosEnvRdfReaderTest {

	/**
	 * Can we map the solution when everything is present?
	 */
	@Test
	public void testMapSolution01() throws Throwable {
		AekosEnvRdfReader objectUnderTest = new AekosEnvRdfReader();
		Dataset ds = DatasetFactory.create();
		Model m = ds.getNamedModel("urn:someGraph");
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("au/org/aekos/api/producer/step/env/EnvRecord.ttl");
		m.read(in, null, "TURTLE");
		String sparql = new ResourceStringifier("au/org/aekos/api/producer/step/env/testMapSolution01.rq").getValue();
		Query query = QueryFactory.create(sparql);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
			ResultSet queryResults = qexec.execSelect();
			QuerySolution solution = queryResults.next();
			InputEnvRecord result = objectUnderTest.mapSolution(solution);
			assertThat(result.getRdfGraph(), is("urn:someGraph"));
			assertThat(result.getRdfSubject(), is("urn:test#record1"));
			assertThat(result.getDecimalLatitude(), is(-42.682963));
			assertThat(result.getDecimalLongitude(), is(146.649282));
			assertThat(result.getGeodeticDatum(), is("GDA94"));
			assertThat(result.getEventDate(), is("1990-03-30"));
			assertThat(result.getMonth(), is(3));
			assertThat(result.getYear(), is(1990));
			assertThat(result.getLocationID(), is("location1"));
			assertThat(result.getLocationName(), is("locName1"));
			assertThat(result.getSamplingProtocol(), is("sampling1"));
		}
	}

	/**
	 * Can we read a single record (the simplest case)?
	 */
	@Test
	public void testRead01() throws Throwable {
		AekosEnvRdfReader objectUnderTest = new AekosEnvRdfReader();
		Dataset ds = DatasetFactory.create();
		Model m = ds.getNamedModel("http://www.aekos.org.au/ontology/1.0.0/test_project#");
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("au/org/aekos/api/producer/step/env/singleLocSingleVisit.ttl");
		m.read(in, null, "TURTLE");
		objectUnderTest.setDs(ds);
		String siteVisitRecordsQuery = new ResourceStringifier("au/org/aekos/api/producer/sparql/site-visit-records.rq").getValue();
		objectUnderTest.setSiteVisitRecordsQuery(siteVisitRecordsQuery);
		InputEnvRecord result = objectUnderTest.read();
		assertThat(result.getLocationID(), is("aekos.org.au/collection/sydney.edu.au/DERG/Main Camp"));
		assertThat(result.getDecimalLatitude(), is(-23.5318398476576));
		assertThat(result.getDecimalLongitude(), is(138.321378247854));
		assertThat(result.getGeodeticDatum(), is("GDA94"));
		assertThat(result.getEventDate(), is("1990-03-30"));
		assertThat(result.getMonth(), is(3));
		assertThat(result.getYear(), is(1990));
		assertThat(result.getLocationName(), is("Main Camp"));
		assertThat(result.getRdfGraph(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#"));
		assertThat(result.getRdfSubject(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#STUDYLOCATIONVISITVIEW-T1493794229804"));
		assertThat(result.getSamplingProtocol(), is("aekos.org.au/collection/sydney.edu.au/DERG"));
	}
	
	/**
	 * Is the expected exception thrown when no eventDate info is available (because there are no observed items)?
	 */
	@Test
	public void testRead02() throws Throwable {
		AekosEnvRdfReader objectUnderTest = new AekosEnvRdfReader();
		Dataset ds = DatasetFactory.create();
		Model m = ds.getNamedModel("http://www.aekos.org.au/ontology/1.0.0/test_project#");
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("au/org/aekos/api/producer/step/env/singleLocSingleVisitNoObservedItems.ttl");
		m.read(in, null, "TURTLE");
		objectUnderTest.setDs(ds);
		String siteVisitRecordsQuery = new ResourceStringifier("au/org/aekos/api/producer/sparql/site-visit-records.rq").getValue();
		objectUnderTest.setSiteVisitRecordsQuery(siteVisitRecordsQuery);
		try {
			objectUnderTest.read();
			fail();
		} catch (RuntimeException e) {
			boolean isWrongException = !e.getClass().equals(IllegalStateException.class);
			boolean isWrongMessage = e.getMessage() == null || !e.getMessage().startsWith("Data problem: no results were found");
			if (isWrongException || isWrongMessage) {
				throw e;
			}
			// success
		}
	}
	
	/**
	 * Can we read two visit records from one location?
	 */
	@Test
	public void testRead03() throws Throwable {
		AekosEnvRdfReader objectUnderTest = new AekosEnvRdfReader();
		Dataset ds = DatasetFactory.create();
		Model m = ds.getNamedModel("http://www.aekos.org.au/ontology/1.0.0/test_project#");
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("au/org/aekos/api/producer/step/env/singleLocTwoVisit.ttl");
		m.read(in, null, "TURTLE");
		objectUnderTest.setDs(ds);
		String siteVisitRecordsQuery = new ResourceStringifier("au/org/aekos/api/producer/sparql/site-visit-records.rq").getValue();
		objectUnderTest.setSiteVisitRecordsQuery(siteVisitRecordsQuery);
		InputEnvRecord firstResult = objectUnderTest.read();
		assertThat(firstResult.getLocationID(), is("aekos.org.au/collection/sydney.edu.au/DERG/Main Camp"));
		assertThat(firstResult.getDecimalLatitude(), is(-23.5318398476576));
		assertThat(firstResult.getDecimalLongitude(), is(138.321378247854));
		assertThat(firstResult.getGeodeticDatum(), is("GDA94"));
		assertThat(firstResult.getEventDate(), is("1994-11-29"));
		assertThat(firstResult.getMonth(), is(11));
		assertThat(firstResult.getYear(), is(1994));
		assertThat(firstResult.getLocationName(), is("Main Camp"));
		assertThat(firstResult.getRdfGraph(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#"));
		assertThat(firstResult.getRdfSubject(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#STUDYLOCATIONVISITVIEW-T1493794238944"));
		assertThat(firstResult.getSamplingProtocol(), is("aekos.org.au/collection/sydney.edu.au/DERG"));
		InputEnvRecord secondResult = objectUnderTest.read();
		assertThat(secondResult.getLocationID(), is("aekos.org.au/collection/sydney.edu.au/DERG/Main Camp"));
		assertThat(secondResult.getDecimalLatitude(), is(-23.5318398476576));
		assertThat(secondResult.getDecimalLongitude(), is(138.321378247854));
		assertThat(secondResult.getGeodeticDatum(), is("GDA94"));
		assertThat(secondResult.getEventDate(), is("1990-03-30"));
		assertThat(secondResult.getMonth(), is(3));
		assertThat(secondResult.getYear(), is(1990));
		assertThat(secondResult.getLocationName(), is("Main Camp"));
		assertThat(secondResult.getRdfGraph(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#"));
		assertThat(secondResult.getRdfSubject(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#STUDYLOCATIONVISITVIEW-T1493794229804"));
		assertThat(secondResult.getSamplingProtocol(), is("aekos.org.au/collection/sydney.edu.au/DERG"));
	}
	
	/**
	 * Can we read two visit records, one from each of two locations?
	 */
	@Test
	public void testRead04() throws Throwable {
		AekosEnvRdfReader objectUnderTest = new AekosEnvRdfReader();
		Dataset ds = DatasetFactory.create();
		Model m = ds.getNamedModel("http://www.aekos.org.au/ontology/1.0.0/test_project#");
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("au/org/aekos/api/producer/step/env/twoLocSingleVisit.ttl");
		m.read(in, null, "TURTLE");
		objectUnderTest.setDs(ds);
		String siteVisitRecordsQuery = new ResourceStringifier("au/org/aekos/api/producer/sparql/site-visit-records.rq").getValue();
		objectUnderTest.setSiteVisitRecordsQuery(siteVisitRecordsQuery);
		InputEnvRecord firstResult = objectUnderTest.read();
		assertThat(firstResult.getLocationID(), is("aekos.org.au/collection/sydney.edu.au/DERG/Main Camp"));
		assertThat(firstResult.getDecimalLatitude(), is(-23.5318398476576));
		assertThat(firstResult.getDecimalLongitude(), is(138.321378247854));
		assertThat(firstResult.getGeodeticDatum(), is("GDA94"));
		assertThat(firstResult.getEventDate(), is("1994-11-29"));
		assertThat(firstResult.getMonth(), is(11));
		assertThat(firstResult.getYear(), is(1994));
		assertThat(firstResult.getLocationName(), is("Main Camp"));
		assertThat(firstResult.getRdfGraph(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#"));
		assertThat(firstResult.getRdfSubject(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#STUDYLOCATIONVISITVIEW-T1493794238944"));
		assertThat(firstResult.getSamplingProtocol(), is("aekos.org.au/collection/sydney.edu.au/DERG"));
		InputEnvRecord secondResult = objectUnderTest.read();
		assertThat(secondResult.getLocationID(), is("aekos.org.au/collection/sydney.edu.au/DERG/Main Camp"));
		assertThat(secondResult.getDecimalLatitude(), is(-23.5318398476576));
		assertThat(secondResult.getDecimalLongitude(), is(138.321378247854));
		assertThat(secondResult.getGeodeticDatum(), is("GDA94"));
		assertThat(secondResult.getEventDate(), is("1990-03-30"));
		assertThat(secondResult.getMonth(), is(3));
		assertThat(secondResult.getYear(), is(1990));
		assertThat(secondResult.getLocationName(), is("Main Camp"));
		assertThat(secondResult.getRdfGraph(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#"));
		assertThat(secondResult.getRdfSubject(), is("http://www.aekos.org.au/ontology/1.0.0/test_project#STUDYLOCATIONVISITVIEW-T1493794229804"));
		assertThat(secondResult.getSamplingProtocol(), is("aekos.org.au/collection/sydney.edu.au/DERG"));
	}
	
	// TODO test that having both options for location doesn't cause dupes
}
