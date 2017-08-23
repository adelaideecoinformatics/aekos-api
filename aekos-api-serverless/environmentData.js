'use strict'
let r = require('./response-helper')
let environmentDataJson = require('./environmentData-json')
let environmentDataCsv = require('./environmentData-csv')

module.exports.doHandle = r.newContentNegotiationHandler(environmentDataJson, environmentDataCsv)
