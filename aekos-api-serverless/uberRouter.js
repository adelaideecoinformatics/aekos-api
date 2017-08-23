'use strict'
let r = require('./response-helper')
const mapping = {
  '/v1/speciesAutocomplete.json': require('./speciesAutocomplete-json'),
  '/v2/speciesAutocomplete.json': require('./speciesAutocomplete-json'),
  '/v1/getTraitVocab.json': require('./traitVocab-json'),
  '/v2/getTraitVocab.json': require('./traitVocab-json')
}
module.exports.handler = (event, context, callback) => {
  let db = require('./db-helper')
  doHandle(event, callback, db)
}

module.exports._testonly = {
  doHandle: doHandle
}

function doHandle (event, callback, db) {
  let path = event.requestContext.path
  let mappedHandler = mapping[path]
  if (!mappedHandler) {
    let msg = `The resource '${path}' does not exist.`
    console.error(`Programmer error: ${msg} But it should which means something isn't configured right.`)
    r.json.notFound(callback, msg)
    return
  }
  mappedHandler.doHandle(event, callback, db)
}