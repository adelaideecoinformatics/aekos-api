'use strict'
let Set = require('collections/set')
let objectUnderTest = require('../v1-environmentData-json')

describe('v1-environmentData-json', () => {
  describe('appendVars', () => {
    it('should map variables to records', () => {
      let records = [
        {
          visitKey: 'aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001#2014-05-09',
          eventDate: '2014-05-09',
          month: 5,
          year: 2014,
          decimalLatitude: -36.759165,
          decimalLongitude: 149.435125,
          geodeticDatum: 'GDA94',
          locationID: 'aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001',
          locationName: 'NSFSEC0001',
          samplingProtocol: 'aekos.org.au/collection/adelaide.edu.au/TAF',
          bibliographicCitation: 'Wood SW, Bowman DMJ...',
          datasetName: 'TERN AusPlots Forests Monitoring Network'
        }
      ]
      let varsLookup = {
        'aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001#2014-05-09': [
          {
            varName: 'disturbanceType',
            varValue: 'none',
            varUnit: null
          },
          {
            varName: 'aspect',
            varValue: '260',
            varUnit: 'degrees'
          },
          {
            varName: 'slope',
            varValue: '4',
            varUnit: 'degrees'
          }
        ]
      }
      objectUnderTest._testonly.appendVars(records, varsLookup)
      let result = records
      let firstRecord = result[0]
      expect(firstRecord.visitKey).toBe('aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001#2014-05-09')
      expect(firstRecord.variables.length).toBe(3)
      let firstVar = firstRecord.variables[0]
      expect(firstVar.varName).toBe('disturbanceType')
      expect(firstVar.varValue).toBe('none')
      expect(firstVar.varUnit).toBeNull()
      let secondVar = firstRecord.variables[1]
      expect(secondVar.varName).toBe('aspect')
      expect(secondVar.varValue).toBe('260')
      expect(secondVar.varUnit).toBe('degrees')
      let thirdVar = firstRecord.variables[2]
      expect(thirdVar.varName).toBe('slope')
      expect(thirdVar.varValue).toBe('4')
      expect(thirdVar.varUnit).toBe('degrees')
    })
  })

  describe('appendSpeciesNames', () => {
    it('should map species names to records', () => {
      let records = [
        {
          visitKey: 'aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001#2014-05-09',
          eventDate: '2014-05-09',
          month: 5,
          year: 2014,
          decimalLatitude: -36.759165,
          decimalLongitude: 149.435125,
          geodeticDatum: 'GDA94',
          locationID: 'aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001',
          locationName: 'NSFSEC0001',
          samplingProtocol: 'aekos.org.au/collection/adelaide.edu.au/TAF',
          bibliographicCitation: 'Wood SW, Bowman DMJ...',
          datasetName: 'TERN AusPlots Forests Monitoring Network'
        }
      ]
      let speciesNameLookup = {
        'aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001#2014-05-09': {
          scientificNames: new Set(['Acacia dealbata', 'species two']),
          taxonRemarks: new Set(['Big tree', 'Some other taxon remark', 'taxon three'])
        }
      }
      objectUnderTest._testonly.appendSpeciesNames(records, speciesNameLookup)
      let result = records
      let firstRecord = result[0]
      expect(firstRecord.visitKey).toBe('aekos.org.au/collection/adelaide.edu.au/TAF/NSFSEC0001#2014-05-09')
      expect(firstRecord.scientificNames.length).toBe(2)
      expect(firstRecord.scientificNames).toContain('Acacia dealbata', 'species two')
      expect(firstRecord.scientificNames.constructor).toBe(Array)
      expect(firstRecord.taxonRemarks.length).toBe(3)
      expect(firstRecord.taxonRemarks).toContain('Big tree', 'Some other taxon remark', 'taxon three')
      expect(firstRecord.taxonRemarks.constructor).toBe(Array)
    })
  })

  describe('stripVisitKeys', () => {
    it('should remove the visitKey from all records', () => {
      let records = [
        {
          visitKey: 'aekos.org.au/collection/test.edu.au/ONE/RECORD0001#2001-01-01',
          datasetName: 'Record 1'
        }, {
          visitKey: 'aekos.org.au/collection/test.edu.au/TWO/RECORD0002#2002-02-02',
          datasetName: 'Record 2'
        }
      ]
      objectUnderTest._testonly.stripVisitKeys(records)
      let result = records
      result.forEach(e => {
        expect(e.visitKey).toBeUndefined()
      })
    })
  })

  let expectedRecordsSql1 = `
    SELECT DISTINCT
    CONCAT(e.locationID, '#', e.eventDate) AS visitKey,
    e.eventDate,
    e.\`month\`,
    e.\`year\`,
    e.decimalLatitude,
    e.decimalLongitude,
    e.geodeticDatum,
    e.locationID,
    e.locationName,
    e.samplingProtocol,
    c.bibliographicCitation,
    c.datasetName
    FROM species AS s
    INNER JOIN env AS e
    ON s.locationID = e.locationID
    AND s.eventDate = e.eventDate
    AND (
      s.scientificName IN ('species one')
      OR s.taxonRemarks IN ('species one')
    )
    INNER JOIN citations AS c
    ON e.samplingProtocol = c.samplingProtocol
    ORDER BY 1
    LIMIT 20 OFFSET 0;`
  describe('getRecordsSql', () => {
    it('should be able to handle a single species', () => {
      let result = objectUnderTest._testonly.getRecordsSql("'species one'", 0, 20)
      expect(result).toBe(expectedRecordsSql1)
    })

    it('should throw an error when we do not supply a species', () => {
      let undefinedSpeciesName
      expect(() => {
        objectUnderTest._testonly.getRecordsSql(undefinedSpeciesName, 0, 20)
      }).toThrow()
    })
  })

  let expectedCountSql1 = `
    SELECT count(DISTINCT locationID, eventDate) as recordsHeld
    FROM species
    WHERE (
      scientificName IN ('species one')
      OR taxonRemarks IN ('species one')
    );`
  describe('getCountSql', () => {
    it('should be able to handle a single species', () => {
      let result = objectUnderTest._testonly.getCountSql("'species one'")
      expect(result).toBe(expectedCountSql1)
    })

    it('should throw an error when we do not supply a species', () => {
      let undefinedSpeciesName
      expect(() => {
        objectUnderTest._testonly.getCountSql(undefinedSpeciesName)
      }).toThrow()
    })
  })

  let expectedVarsSql1 = `
    SELECT
    CONCAT(locationID, '#', eventDate) AS visitKey,
    varName,
    varValue,
    varUnit
    FROM envvars
    WHERE (locationID, eventDate) in ('location-1#2001-01-01', 'location-2#2002-02-02')
    ORDER BY 1;`
  describe('getVarsSql', () => {
    it('should produce expected SQL when the visit key clause is supplied', () => {
      let result = objectUnderTest._testonly.getVarsSql("'location-1#2001-01-01', 'location-2#2002-02-02'")
      expect(result).toBe(expectedVarsSql1)
    })

    it('should throw an error when we do not supply any visit key clauses', () => {
      let undefinedVisitKeyClauses
      expect(() => {
        objectUnderTest._testonly.getVarsSql(undefinedVisitKeyClauses)
      }).toThrow()
    })
  })

  let expectedSpeciesNamesSql1 = `
    SELECT DISTINCT
    CONCAT(locationID, '#', eventDate) AS visitKey,
    scientificName,
    taxonRemarks
    FROM species
    WHERE (
      scientificName IN ('species one')
      OR taxonRemarks IN ('species one')
    )
    AND (locationID, eventDate) in (('location-1#2001-01-01', 'location-2#2002-02-02'))
    ORDER BY 1;`
  describe('getSpeciesNamesSql', () => {
    it('should produce the expected SQL when a visit key clause and a species name clause are supplied', () => {
      let result = objectUnderTest._testonly.getSpeciesNamesSql("('location-1#2001-01-01', 'location-2#2002-02-02')", "'species one'")
      expect(result).toBe(expectedSpeciesNamesSql1)
    })

    it('should throw an error when we do not supply any visit key clauses', () => {
      let undefinedVisitKeyClauses
      expect(() => {
        objectUnderTest._testonly.getSpeciesNamesSql(undefinedVisitKeyClauses, "'species one'")
      }).toThrow()
    })

    it('should throw an error when we do not supply any species name clauses', () => {
      let undefinedSpeciesNameClause
      expect(() => {
        objectUnderTest._testonly.getSpeciesNamesSql("('location-1#2001-01-01', 'location-2#2002-02-02')", undefinedSpeciesNameClause)
      }).toThrow()
    })
  })

  describe('getVisitKeyClauses', () => {
    it('should produce the expected SQL fragment', () => {
      let result = objectUnderTest._testonly.getVisitKeyClauses(['location-1#2001-01-01', 'location-2#2002-02-02'])
      expect(result).toBe("('location-1','2001-01-01')\n,('location-2','2002-02-02')")
    })
  })

  describe('extractParams', () => {
    it('should extract the params when they are present', () => {
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          // TODO add varNames param support
          rows: 15,
          start: 0
        }
      }
      let result = objectUnderTest.extractParams(event)
      expect(result.speciesName).toBe("'species one'")
      expect(result.unescapedSpeciesName).toBe('species one')
      expect(result.varName).toBeNull()
      expect(result.rows).toBe(15)
      expect(result.start).toBe(0)
    })

    it('should extract the params when they are present', () => {
      let event = {
        queryStringParameters: {
          speciesName: 'species two'
        }
      }
      let result = objectUnderTest.extractParams(event)
      expect(result.speciesName).toBe("'species two'")
      expect(result.unescapedSpeciesName).toBe('species two')
      expect(result.varName).toBeNull()
      expect(result.rows).toBe(20)
      expect(result.start).toBe(0)
    })
  })
})