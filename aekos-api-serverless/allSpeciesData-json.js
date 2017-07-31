'use strict'
let r = require('./response-helper')
const recordsHeldField = 'recordsHeld'
let yaml = require('yamljs')
const startParam = yaml.load('./constants.yml').paramNames.START
const rowsParam = yaml.load('./constants.yml').paramNames.ROWS
const defaultStart = yaml.load('./constants.yml').defaults.START
const defaultRows = yaml.load('./constants.yml').defaults.ROWS

module.exports.handler = (event, context, callback) => {
  let db = require('./db-helper')
  doHandle(event, callback, db, r.calculateElapsedTime)
}

const validator = r.compositeValidator([
  r.startValidator,
  r.rowsValidator
])
module.exports.validator = validator

function doHandle (event, callback, db, elapsedTimeCalculator) {
  r.handleJsonGet(event, callback, db, validator, responder, {
    event: event,
    elapsedTimeCalculator: elapsedTimeCalculator
  })
}

module.exports.responder = responder
function responder (db, queryStringParameters, extrasProvider) {
  let processStart = r.now()
  let params = extractParams(queryStringParameters)
  return doAllSpeciesQuery(extrasProvider.event, params, processStart, db, extrasProvider.elapsedTimeCalculator)
}

module.exports.doAllSpeciesQuery = doAllSpeciesQuery
function doAllSpeciesQuery (event, params, processStart, db, elapsedTimeCalculator) {
  const noWhereFragmentForAllSpecies = ''
  const recordsSql = getRecordsSql(params.start, params.rows, false, noWhereFragmentForAllSpecies)
  const countSql = getCountSql(noWhereFragmentForAllSpecies)
  return doQuery(event, params, processStart, db, elapsedTimeCalculator, recordsSql, countSql)
}

module.exports.doQuery = doQuery
function doQuery (event, params, processStart, db, elapsedTimeCalculator, recordsSql, countSql) {
  let recordsPromise = db.execSelectPromise(recordsSql)
  let countPromise = db.execSelectPromise(countSql)
  return Promise.all([recordsPromise, countPromise]).then(values => {
    let records = values[0]
    let count = values[1]
    if (count.length !== 1) {
      throw new Error('SQL result problem: result from count query did not have exactly one row. Result=' + JSON.stringify(count))
    }
    let numFound = count[0][recordsHeldField]
    let isRecordCountMismatch = records.length === 0 && numFound > 0
    if (isRecordCountMismatch) {
      throw new Error(`Data problem: records.length=0 while numFound=${numFound}, suspect the record query is broken`)
    }
    let totalPages = r.calculateTotalPages(params.rows, numFound)
    let pageNumber = r.calculatePageNumber(params.start, numFound, totalPages)
    let result = {
      responseHeader: {
        elapsedTime: elapsedTimeCalculator(processStart),
        numFound: numFound,
        pageNumber: pageNumber,
        params: {
          rows: params.rows,
          start: params.start
        },
        totalPages: totalPages
      },
      response: records
    }
    let strategy = getStrategyForVersion(event)
    strategy(result)
    return result
  })
}

function getStrategyForVersion (event) {
  const doNothing = () => {}
  let versionHandler = r.newVersionHandler({
    '/v1/': removeV2FieldsFrom,
    '/v2/': doNothing
  })
  return versionHandler.handle(event)
}

module.exports.removeV2FieldsFrom = removeV2FieldsFrom
function removeV2FieldsFrom (successResult) {
  successResult.response.forEach(curr => {
    delete curr.locationName
    delete curr.datasetName
  })
}

module.exports._testonly = {
  doHandle: doHandle
}

module.exports.getRecordsSql = getRecordsSql
function getRecordsSql (start, rows, includeSpeciesRecordId, whereFragment) {
  let speciesIdFragment = ''
  if (includeSpeciesRecordId) {
    speciesIdFragment = 's.id,'
  }
  return `
    SELECT
    ${speciesIdFragment}
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
    FROM (
      SELECT id
      FROM species
      ${whereFragment}
      ORDER BY 1
      LIMIT ${rows} OFFSET ${start}
    ) AS lateRowLookup
    INNER JOIN species AS s
    ON lateRowLookup.id = s.id
    LEFT JOIN env AS e
    ON s.locationID = e.locationID
    AND s.eventDate = e.eventDate
    LEFT JOIN citations AS c
    ON e.samplingProtocol = c.samplingProtocol;`
}

module.exports.getCountSql = getCountSql
function getCountSql (whereFragment) {
  return `
    SELECT count(*) AS ${recordsHeldField}
    FROM species AS s
    INNER JOIN env AS e
    ON s.locationID = e.locationID
    AND s.eventDate = e.eventDate
    INNER JOIN citations AS c
    ON e.samplingProtocol = c.samplingProtocol
    ${whereFragment};`
}

module.exports.extractParams = extractParams
function extractParams (queryStringParameters) {
  return {
    start: r.getOptionalNumber(queryStringParameters, startParam, defaultStart),
    rows: r.getOptionalNumber(queryStringParameters, rowsParam, defaultRows)
  }
}