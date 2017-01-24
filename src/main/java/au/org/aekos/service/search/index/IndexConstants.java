package au.org.aekos.service.search.index;

/**
 * These are all document field names
 * 
 * Document types in DocumentType enum,  BUT might put the name Strings here for consistency
 */
public class IndexConstants {
	
    //Fields for the autocomplete species search
	public static final String FLD_DISPLAY_VALUE = "displayValue";
	public static final String FLD_SEARCH = "search";
	public static final String FLD_SEARCH_LEV = "search_lev";
	public static final String FLD_SEARCH_SUB= "search_sub";
	public static final String FLD_INSTANCE_COUNT = "instanceCount";
	
	public static final String FLD_DOC_INDEX_TYPE = "doc_index_type";
	
	//Index document types for FLD_DOC_INDEX_TYPE
	public static final String TRAIT_SPECIES = DocumentType.TRAIT_SPECIES.name();
	public static final String SPECIES_ENV = DocumentType.SPECIES_ENV.name();
	public static final String SPECIES_SUMMARY = DocumentType.SPECIES_SUMMARY.name();
	public static final String SPECIES_RECORD = DocumentType.SPECIES_RECORD.name();
	public static final String ENV_RECORD = DocumentType.ENV_RECORD.name();
	
	//Unique term for loading unique checking
	public static final String FLD_UNIQUE_ID = "uid";
	
	//Exact match field name
	public static final String FLD_SPECIES = "species";
	public static final String FLD_TRAIT = "trait";
	public static final String FLD_ENVIRONMENT = "environment";
}
