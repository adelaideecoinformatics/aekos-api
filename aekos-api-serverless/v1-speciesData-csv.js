'use strict'
let r = require('./response-helper')
let speciesDataJson = require('./v1-speciesData-json')
let allSpeciesDataCsv = require('./v1-allSpeciesData-csv')

module.exports.handler = (event, context, callback) => {
  let db = require('./db-helper')
  doHandle(event, callback, db, r.calculateElapsedTime)
}

function doHandle (event, callback, db, elapsedTimeCalculator) {
  let processStart = r.now()
  let params = speciesDataJson.extractParams(event, db)
  speciesDataJson.doQuery(params, processStart, false, db, elapsedTimeCalculator).then(successResult => {
    let result = allSpeciesDataCsv.mapJsonToCsv(successResult.response)
    r.csv.ok(callback, result)
  }).catch(error => {
    console.error('Failed while building result', error)
    r.json.internalServerError(callback, 'Sorry, something went wrong')
  })
}

module.exports._testonly = {
  doHandle: doHandle
}
