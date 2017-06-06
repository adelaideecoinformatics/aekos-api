'use strict'
let objectUnderTest = require('../v1-speciesData-json')
let StubDB = require('./StubDB')

describe('v1-speciesData-json', () => {
  describe('doHandle', () => {
    it('should return a 200 response when all params are supplied', (done) => {
      let stubDb = new StubDB()
      stubDb.setExecSelectPromiseResponses([
        [ {recordNum: 1}, {recordNum: 2} ],
        [ {recordsHeld: 31} ]
      ])
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          rows: '15',
          start: '0'
        }
      }
      let callback = (error, result) => {
        if (error) {
          fail('Responded with error: ' + JSON.stringify(error))
        }
        expect(result.statusCode).toBe(200)
        expect(result.headers).toEqual({
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Credentials': true,
          'Content-Type': "'application/json'"
        })
        expect(JSON.parse(result.body)).toEqual({
          responseHeader: {
            elapsedTime: 42,
            numFound: 31,
            pageNumber: 1,
            params: {
              rows: 15,
              start: 0,
              speciesName: 'species one'
            },
            totalPages: 3
          },
          response: [
            {recordNum: 1},
            {recordNum: 2}
          ]
        })
        done()
      }
      objectUnderTest._testonly.doHandle(event, callback, stubDb, () => { return 42 })
    })

    it('should calculate paging information when only start is provided', (done) => {
      let stubDb = new StubDB()
      stubDb.setExecSelectPromiseResponses([
        [ {recordNum: 1} ],
        [ {recordsHeld: 31} ]
      ])
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          start: '0'
        }
      }
      let callback = (error, result) => {
        if (error) {
          fail('Responded with error: ' + JSON.stringify(error))
        }
        expect(result.statusCode).toBe(200)
        let responseHeader = JSON.parse(result.body).responseHeader
        expect(responseHeader.pageNumber).toBe(1)
        expect(responseHeader.totalPages).toBe(2)
        done()
      }
      objectUnderTest._testonly.doHandle(event, callback, stubDb, () => { return 0 })
    })

    it('should behave weirdly when the user supplies wierd params', (done) => {
      let stubDb = new StubDB()
      stubDb.setExecSelectPromiseResponses([
        [ {recordNum: 1} ],
        [ {recordsHeld: 14} ]
      ])
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          start: '10' // start is less than the default rows value
        }
      }
      let callback = (error, result) => {
        if (error) {
          fail('Responded with error: ' + JSON.stringify(error))
        }
        expect(result.statusCode).toBe(200)
        let responseHeader = JSON.parse(result.body).responseHeader
        expect(responseHeader.params.rows).toBe(20) // we get default rows which is greater than the start
        expect(responseHeader.pageNumber).toBe(1) // should probably be 2 but it's a weird edge case
        expect(responseHeader.totalPages).toBe(1) // are there 1,2 or 3 pages?
        done()
      }
      objectUnderTest._testonly.doHandle(event, callback, stubDb, () => { return 0 })
    })
  })

  describe('extractParams', () => {
    it('should extract the params when they are present', () => {
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          rows: '15',
          start: '0'
        }
      }
      let result = objectUnderTest.extractParams(event, new StubDB())
      expect(result.speciesName).toBe("'species one'")
      expect(result.unescapedSpeciesName).toBe('species one')
      expect(result.rows).toBe(15)
      expect(result.start).toBe(0)
    })

    it('should default the paging info', () => {
      let event = {
        queryStringParameters: {
          speciesName: 'species two'
        }
      }
      let result = objectUnderTest.extractParams(event, new StubDB())
      expect(result.speciesName).toBe("'species two'")
      expect(result.unescapedSpeciesName).toBe('species two')
      expect(result.rows).toBe(20)
      expect(result.start).toBe(0)
    })
  })

  let expectedRecordsSql1 = `
    SELECT
    
    s.scientificName,
    s.taxonRemarks,
    s.individualCount,
    s.eventDate,
    e.\`month\`,
    e.\`year\`,
    e.decimalLatitude,
    e.decimalLongitude,
    e.geodeticDatum,
    s.locationID,
    e.locationName,
    e.samplingProtocol,
    c.bibliographicCitation,
    c.datasetName
    FROM species AS s
    LEFT JOIN env AS e
    ON s.locationID = e.locationID
    AND s.eventDate = e.eventDate
    LEFT JOIN citations AS c
    ON e.samplingProtocol = c.samplingProtocol
    
    WHERE (
      s.scientificName IN ('species one')
      OR s.taxonRemarks IN ('species one')
    )
    ORDER BY 1
    LIMIT 33 OFFSET 0;`
  let expectedRecordsSql2 = `
    SELECT
    s.id,
    s.scientificName,`
  describe('getRecordsSql', () => {
    it('should be able to handle a single species', () => {
      let result = objectUnderTest._testonly.getRecordsSql("'species one'", 0, 33, false)
      expect(result).toBe(expectedRecordsSql1)
    })

    it('should throw an error when we do not supply a species', () => {
      let undefinedSpeciesName
      expect(() => {
        objectUnderTest._testonly.getRecordsSql(undefinedSpeciesName)
      }).toThrow()
    })

    it('should be able to include the species ID fragment', () => {
      let includeSpeciesId = true
      let result = objectUnderTest._testonly.getRecordsSql("'species one'", 0, 33, includeSpeciesId)
      expect(result.substr(0, expectedRecordsSql2.length)).toBe(expectedRecordsSql2)
    })
  })
})