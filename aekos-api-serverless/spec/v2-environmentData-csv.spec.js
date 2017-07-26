'use strict'
let objectUnderTest = require('../environmentData-csv')
let StubDB = require('./StubDB')

describe('/v2/environmentData-csv', function () {
  describe('doHandle', () => {
    let result = null
    beforeEach(done => {
      let stubDb = new StubDB()
      const recordsResult = [{
        individualCount: 1,
        eventDate: '2007-10-03',
        month: 10,
        year: 2007,
        decimalLatitude: -33.59758852952833,
        decimalLongitude: 120.15956081537496,
        geodeticDatum: 'GDA94',
        locationID: 'aekos.org.au/collection/wa.gov.au/ravensthorpe/R181',
        samplingProtocol: 'aekos.org.au/collection/wa.gov.au/ravensthorpe',
        bibliographicCitation: 'Department of Par...',
        locationName: 'location1',
        datasetName: 'dataset1',
        visitKey: 'location1#2017-07-07'
      }]
      const countResult = [{ recordsHeld: 31 }]
      const varsResult = [{
        visitKey: 'location1#2017-07-07',
        varName: 'windSpeed',
        varValue: '6',
        varUnit: 'km/h'
      }]
      const speciesNamesResult = [{
        visitKey: 'location1#2017-07-07',
        scientificName: 'species one',
        taxonRemarks: null
      }]
      stubDb.setExecSelectPromiseResponses([
        recordsResult,
        countResult,
        varsResult,
        speciesNamesResult
      ])
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          rows: '1'
        },
        headers: {
          Host: 'api.aekos.org.au',
          'X-Forwarded-Proto': 'https'
        },
        requestContext: {
          path: '/v2/environmentData.csv'
        }
      }
      let callback = (_, theResult) => {
        result = theResult
        done()
      }
      objectUnderTest._testonly.doHandle(event, callback, stubDb, () => { return 42 })
    })

    it('should respond with 200 when we only supply a species name', () => {
      expect(result.statusCode).toBe(200)
      expect(result.headers).toEqual({
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Credentials': true,
        'Content-Type': "'text/csv'"
        // TODO need HATEOAS Link header here
      })
      expect(result.body.split('\n')).toEqual([
        `"decimalLatitude","decimalLongitude","geodeticDatum","locationID","locationName","datasetName","scientificNames","taxonRemarks","eventDate","year","month","bibliographicCitation","samplingProtocol","variable1Name","variable1Value","variable1Units"`,
        `-33.59758852952833,120.15956081537496,"GDA94","aekos.org.au/collection/wa.gov.au/ravensthorpe/R181","location1","dataset1","species one","","2007-10-03",2007,10,"Department of Par...","aekos.org.au/collection/wa.gov.au/ravensthorpe","windSpeed","6","km/h"`
      ])
    })
  })

  describe('doHandle', () => {
    let result = null
    beforeEach(done => {
      let stubDb = new StubDB()
      const recordsResult = [{
        locationName: 'location1',
        datasetName: 'dataset1',
        visitKey: 'location1#2017-07-07'
      }]
      const countResult = [{ recordsHeld: 31 }]
      const varsResult = [{
        visitKey: 'location1#2017-07-07',
        varName: 'windSpeed',
        varValue: '6',
        varUnit: 'km/h'
      }]
      const speciesNamesResult = [{
        visitKey: 'location1#2017-07-07',
        scientificName: 'species one',
        taxonRemarks: null
      }]
      stubDb.setExecSelectPromiseResponses([
        recordsResult,
        countResult,
        varsResult,
        speciesNamesResult
      ])
      let event = {
        queryStringParameters: {
          speciesName: 'species one',
          download: 'true' // trigger response as download
        },
        headers: {
          Host: 'api.aekos.org.au',
          'X-Forwarded-Proto': 'https'
        },
        requestContext: {
          path: '/v2/environmentData.csv'
        }
      }
      let callback = (_, theResult) => {
        result = theResult
        done()
      }
      objectUnderTest._testonly.doHandle(event, callback, stubDb, () => { return 42 })
    })

    it('should respond with expected header when we supply the download param', () => {
      expect(result.statusCode).toBe(200)
      expect(result.headers).toEqual({
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Credentials': true,
        'Content-Type': "'text/csv'",
        'Content-Disposition': 'attachment;filename=aekosEnvironmentData.csv'
      })
      expect(result.body.split('\n').length).toBe(2)
    })
  })

  describe('createCsvRow', () => {
    it('should map the object to a CSV row', function () {
      let record = {
        'eventDate': '2014-05-09',
        'month': 5,
        'year': 2014,
        'decimalLatitude': -38.759165,
        'decimalLongitude': 143.435125,
        'geodeticDatum': 'GDA94',
        'locationID': 'aekos.org.au/collection/test.edu.au/TEST/AAAA0001',
        'locationName': 'AAAA0001',
        'samplingProtocol': 'aekos.org.au/collection/test.edu.au/TEST',
        'bibliographicCitation': 'A Person, B Person...',
        'datasetName': 'TEST dataset',
        'variables': [
          {
            'varName': 'disturbanceType',
            'varValue': 'none',
            'varUnit': null
          },
          {
            'varName': 'slope',
            'varValue': '4',
            'varUnit': 'degrees'
          },
          {
            'varName': 'aspect',
            'varValue': '260',
            'varUnit': 'degrees'
          }
        ],
        'scientificNames': [
          'Acacia dealbata',
          'Acacia aneura'
        ],
        'taxonRemarks': [
          'Grass',
          'Clover'
        ]
      }
      let result = objectUnderTest.createCsvRow(record, objectUnderTest.v2CsvHeaders)
      expect(result).toBe('-38.759165,143.435125,"GDA94","aekos.org.au/collection/test.edu.au/TEST/AAAA0001",' +
        '"AAAA0001","TEST dataset","Acacia dealbata|Acacia aneura","Grass|Clover","2014-05-09",2014,5,"A Person, B Person...",' +
        '"aekos.org.au/collection/test.edu.au/TEST","disturbanceType","none",,"slope",' +
        '"4","degrees","aspect","260","degrees"')
    })
  })
})
