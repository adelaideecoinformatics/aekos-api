package au.org.aekos.api.producer.step.species.out;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

import au.org.aekos.api.producer.step.species.in.InputSpeciesRecord;

public class OutputSpeciesRecordTest {

private OutputSpeciesRecord objectUnderTest;
	
	@Before
	public void before() {
		objectUnderTest = new OutputSpeciesRecord(new InputSpeciesRecord("abc-123", "urn:someSpeciesRecord", "urn:someGraph",
				1, "location123", "macropus rufus", "buffalo grass", "2007-09-29"));
	}
	
	/**
	 * Is the expected exception thrown when we don't supply a scientificName or taxonRemarks?
	 */
	@Test
	public void testConstructor01() {
		String scientificName = null;
		String taxonRemarks = null;
		try {
			new InputSpeciesRecord(null, null, null, 0, null, scientificName, taxonRemarks, null);
			fail();
		} catch (IllegalStateException e) {
			// success!
		}
	}
	
	/**
	 * Is the id quoted?
	 */
	@Test
	public void testGetId01() {
		String result = objectUnderTest.getId();
		assertEquals("\"abc-123\"", result);
	}
	
	/**
	 * Is the scientific name quoted?
	 */
	@Test
	public void testGetScientificName01() {
		String result = objectUnderTest.getScientificName();
		assertEquals("\"macropus rufus\"", result);
	}
	
	/**
	 * Is the taxon remarks quoted?
	 */
	@Test
	public void testGetTaxonRemarks01() {
		String result = objectUnderTest.getTaxonRemarks();
		assertEquals("\"buffalo grass\"", result);
	}
	
	/**
	 * Is the taxon remarks replace with the null placeholder when the value is null?
	 */
	@Test
	public void testGetTaxonRemarks02() {
		String result = new OutputSpeciesRecord(isrWithNullTaxonRemarks()).getTaxonRemarks();
		assertEquals("\\N", result);
	}
	
	// TODO test all other fields
	
	/**
	 * Are all the declared field names present?
	 */
	@Test
	public void testGetCsvFields01() {
		String[] fields = OutputSpeciesRecord.getCsvFields();
		for (String curr : fields) {
			String methodName = "get" + StringUtils.capitalize(curr);
			try {
				OutputSpeciesRecord.class.getMethod(methodName);
				// success
			} catch (NoSuchMethodException e) {
				fail("Expected method to exist: " + methodName);
			}
		}
	}
	
	private InputSpeciesRecord isrWithNullTaxonRemarks() {
		return new InputSpeciesRecord(null, null, null, 0, null, "some to stop validation failures in constructor", null, null);
	}
}
