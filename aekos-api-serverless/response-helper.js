'use strict'
let accepts = require('accepts')
let yaml = require('yamljs')
const speciesNamesParam = yaml.load('./constants.yml').paramNames.speciesName.multiple
const traitNamesParam = yaml.load('./constants.yml').paramNames.traitName.multiple
const varNamesParam = yaml.load('./constants.yml').paramNames.varName.multiple
const pageSizeParam = yaml.load('./constants.yml').paramNames.PAGE_SIZE
const pageNumParam = yaml.load('./constants.yml').paramNames.PAGE_NUM
const startParam = yaml.load('./constants.yml').paramNames.START
const rowsParam = yaml.load('./constants.yml').paramNames.ROWS
const pageSizeMin = yaml.load('./constants.yml').minValues.PAGE_SIZE
const pageNumMin = yaml.load('./constants.yml').minValues.PAGE_NUM
const startMin = yaml.load('./constants.yml').minValues.START
const rowsMin = yaml.load('./constants.yml').minValues.ROWS
const pageSizeMax = yaml.load('./constants.yml').maxValues.PAGE_SIZE
const rowsMax = yaml.load('./constants.yml').maxValues.ROWS
const downloadParam = yaml.load('./constants.yml').paramNames.DOWNLOAD
const msg500 = yaml.load('./constants.yml').messages.public.internalServerError
let codeToLabelMapping = require('./ontology/code-to-label.json')
let querystring = require('querystring')
const jsonContentType = "'application/json'"
const csvContentType = "'text/csv'"
function getHeaders (contentType) {
  return {
    'Access-Control-Allow-Origin': '*', // Required for CORS support to work
    'Access-Control-Allow-Credentials': true,
    'Access-Control-Expose-Headers': 'link',
    'Content-Type': contentType // Required for cookies, authorization headers with HTTPS
  }
}

function doResponse (theCallback, theBody, statusCode, contentType, extraHeadersCallback) {
  let body = contentType === csvContentType ? theBody : JSON.stringify(theBody)
  let headers = getHeaders(contentType)
  if (extraHeadersCallback) {
    extraHeadersCallback(headers)
  }
  const response = {
    statusCode: statusCode,
    headers: headers,
    body: body
  }
  theCallback(null, response)
}

function handleJsonPost (event, callback, db,
    validator/* (queryStringParameters, requestBody):{isValid:boolean, message:string} */,
    responder/* (requestBody, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider) {
  if (typeof event.body === 'undefined') {
    jsonResponseHelpers.badRequest(callback, 'Programmer problem: request body is undefined. Most likely you ' +
      "haven't defined the test input or wired things up correctly. Or maybe AWS has changed the event object")
    return
  }
  let requestBodyGetter = event => {
    return JSON.parse(event.body)
  }
  handleJsonGeneric(event, callback, db, validator, responder, extrasProvider, requestBodyGetter)
}

function handleJsonGet (event, callback, db,
    validator/* (queryStringParameters):{isValid:boolean, message:string} */,
    responder/* (_, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider) {
  let requestBodyGetter = event => {
    return 'not used'
  }
  handleJsonGeneric(event, callback, db, validator, responder, extrasProvider, requestBodyGetter)
}

function handleJsonGeneric (event, callback, db,
    validator/* (queryStringParameters, requestBody):{isValid:boolean, message:string} */,
    responder/* (requestBody, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider, requestBodyGetter/* (event) */) {
  let responseCaller = (event, callback, requestBody, db, queryStringObj, extrasProvider) => {
    responder(requestBody, db, queryStringObj, extrasProvider).then(responseWrapper => {
      jsonResponseHelpers.ok(callback, responseWrapper.body, event, responseWrapper.linkHeaderData)
    }).catch(error => {
      console.error('Failed to execute handler', error)
      jsonResponseHelpers.internalServerError(callback)
    })
  }
  handleGeneric(event, callback, db, validator, responder, extrasProvider, requestBodyGetter, responseCaller)
}

function handleCsvPost (event, callback, db,
    validator/* (queryStringParameters, requestBody):{isValid:boolean, message:string} */,
    responder/* (requestBody, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider) {
  if (typeof event.body === 'undefined') {
    jsonResponseHelpers.badRequest(callback, 'Programmer problem: request body is undefined. Most likely you ' +
      "haven't defined the test input or wired things up correctly. Or maybe AWS has changed the event object")
    return
  }
  let requestBodyGetter = event => {
    return JSON.parse(event.body)
  }
  handleCsvGeneric(event, callback, db, validator, responder, extrasProvider, requestBodyGetter)
}

function handleCsvGet (event, callback, db,
    validator/* (queryStringParameters):{isValid:boolean, message:string} */,
    responder/* (_, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider) {
  let requestBodyGetter = event => {
    return 'not used'
  }
  handleCsvGeneric(event, callback, db, validator, responder, extrasProvider, requestBodyGetter)
}

function handleCsvGeneric (event, callback, db,
    validator/* (queryStringParameters, requestBody):{isValid:boolean, message:string} */,
    responder/* (requestBody, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider, requestBodyGetter/* (event) */) {
  let responseCaller = (event, callback, requestBody, db, queryStringObj, extrasProvider) => {
    responder(requestBody, db, queryStringObj, extrasProvider).then(responseWrapper => {
      csvResponseHelpers.ok(callback, responseWrapper.body, responseWrapper.downloadFileName, event, responseWrapper.linkHeaderData)
    }).catch(error => {
      console.error('Failed to execute handler', error)
      jsonResponseHelpers.internalServerError(callback)
    })
  }
  handleGeneric(event, callback, db, validator, responder, extrasProvider, requestBodyGetter, responseCaller)
}

function handleGeneric (event, callback, db,
    validator/* (queryStringParameters, requestBody):{isValid:boolean, message:string} */,
    responder/* (requestBody, databaseHelper, queryStringParameters):Promise<{}> */,
    extrasProvider, requestBodyGetter/* (event) */,
    responseCaller/* (event, callback, requestBody, db, queryStringObj, extrasProvider):void */) {
  let requestBody = null
  try {
    requestBody = requestBodyGetter(event)
  } catch (e) {
    const msg = 'Could not parse the supplied JSON body'
    console.error(`${msg}. Body = '${event.body}'`)
    jsonResponseHelpers.badRequest(callback, msg)
    return
  }
  let queryStringObj = event.queryStringParameters
  let validationResult = validator(queryStringObj, requestBody)
  if (!validationResult.isValid) {
    jsonResponseHelpers.badRequest(callback, validationResult.message)
    return
  }
  let errorHandler = error => {
    console.error('Failed to execute handler', error)
    jsonResponseHelpers.internalServerError(callback)
  }
  try {
    responseCaller(event, callback, requestBody, db, queryStringObj, extrasProvider)
  } catch (error) {
    errorHandler(error)
  }
}

function isQueryStringParamPresent (event, paramName) {
  let queryStringParams = event.queryStringParameters
  if (!queryStringParams || typeof (queryStringParams[paramName]) === 'undefined') {
    return false
  }
  return true
}

function getOptionalStringParam (event, paramName, defaultValue) {
  if (!isQueryStringParamPresent(event, paramName)) {
    return defaultValue
  }
  let rawValue = event.queryStringParameters[paramName]
  let valueType = typeof (rawValue)
  if (valueType === 'string') {
    return rawValue
  }
  throw new Error(`Data problem: supplied value '${rawValue}' of type '${valueType}' is not a string.`)
}

function getOptionalNumberParam (event, paramName, defaultValue) {
  return getOptionalNumber(event.queryStringParameters, paramName, defaultValue)
}

function getOptionalNumber (containerObj, paramName, defaultValue) {
  let isValueNotPresent = !containerObj || typeof containerObj[paramName] === 'undefined'
  if (isValueNotPresent) {
    return parseInt(defaultValue)
  }
  let rawValue = containerObj[paramName]
  let valueType = typeof (rawValue)
  if (valueType === 'number') {
    return rawValue
  }
  let parsedValue = parseInt(rawValue)
  if (isNaN(parsedValue)) {
    throw new Error(`Data problem: supplied value '${rawValue}' of type '${valueType}' is not a number.`)
  }
  return parsedValue
}

function getOptionalArray (containerObj, key, db) {
  let unescapedResult = containerObj[key]
  if (unescapedResult && unescapedResult.constructor !== Array) {
    throw new Error(`Programmer problem: the '${key}' field (value='${unescapedResult}') is not an array, this should've been caught by validation`)
  }
  if (!unescapedResult) {
    unescapedResult = null
  }
  let escapedResult = null
  if (unescapedResult) {
    escapedResult = db.toSqlList(unescapedResult)
  }
  return {
    escaped: escapedResult,
    unescaped: unescapedResult
  }
}

function calculateOffset (pageNum, pageSize) {
  return (pageNum - 1) * pageSize
}

function assertNumber (val) {
  let valType = typeof (val)
  if (valType === 'number') {
    return
  }
  throw new Error(`Data problem: expected value '${val}' of type '${valType}' to be a number`)
}

const contentTypes = {
  json: 'json',
  csv: 'csv',
  unhandled: 'UNHANDLED'
}

function getContentType (event) {
  let accept = accepts(castEventToFakeExpressReq(event))
  switch (accept.type(['application/json', 'text/csv'])) {
    case 'application/json':
      return contentTypes.json
    case 'text/csv':
      return contentTypes.csv
    default:
      return contentTypes.unhandled
  }
}

function castEventToFakeExpressReq (event) {
  return {
    headers: {
      accept: event.headers.Accept
    }
  }
}

function resolveVocabCode (code) {
  let result = codeToLabelMapping[code]
  if (typeof result === 'undefined') {
    console.warn(`Data problem: no label defined for the code '${code}', reverting to the code`)
    result = code
  }
  return result
}

function calculatePageNumber (start, numFound, totalPages) {
  assertNumber(start)
  assertNumber(numFound)
  assertNumber(totalPages)
  if (numFound === 0) {
    return 0
  }
  if (start === 0) {
    return 1
  }
  let decimalProgress = (start + 1) / numFound
  let decimalPageNumber = decimalProgress * totalPages
  return Math.ceil(decimalPageNumber)
}

function calculateTotalPages (rows, numFound) {
  return Math.ceil(numFound / rows)
}

function assertIsSupplied (escapedSpeciesNames) {
  if (!escapedSpeciesNames) {
    throw new Error(`Programmer problem: no escaped species names were supplied='${escapedSpeciesNames}'.`)
  }
}

function now () {
  return new Date().getTime()
}

function elapsedTimeCalculator (startMs) {
  return now() - startMs
}

function newContentNegotiationHandler (jsonHandler, csvHandler) {
  return function (event, callback, db, elapsedTimeCalculator) {
    let urlSuffix = getUrlSuffix(event)
    let contentTypeIndicator
    switch (urlSuffix) {
      case 'json':
        contentTypeIndicator = contentTypes.json
        break
      case 'csv':
        contentTypeIndicator = contentTypes.csv
        break
      default:
        contentTypeIndicator = getContentType(event)
        break
    }
    switch (contentTypeIndicator) {
      case contentTypes.json:
        jsonHandler.doHandle(event, callback, db, elapsedTimeCalculator)
        break
      case contentTypes.csv:
        csvHandler.doHandle(event, callback, db, elapsedTimeCalculator)
        break
      case contentTypes.unhandled:
        jsonResponseHelpers.badRequest(callback, `Cannot handle the specified Accept header '${event.headers.Accept}`)
        break
      default:
        throw new Error(`Programmer error: unknown content type indicator returned '${contentTypeIndicator}'`)
    }
  }
}

function getUrlSuffix (event) {
  if (!event || !event.path) {
    return null
  }
  let path = event.path
  let regex = /\.\w+$/
  let match = regex.exec(path)
  if (!match) {
    return null
  }
  return match[0].replace('.', '')
}

function newVersionHandler (config) {
  if (typeof config !== 'object') {
    throw new Error('Programmer problem: supplied config is not an object')
  }
  return new VersionHandler(config)
}

class VersionHandler {
  constructor (config) {
    this._versionPrefixLength = 4
    this._config = config
  }

  handle (event) {
    let path = event.path // this path won't have stage prefixes
    let versionPrefix = path.substr(0, this._versionPrefixLength)
    let result = this._config[versionPrefix]
    if (result) {
      return result
    }
    throw new Error(`Programmer problem: unhandled path prefix '${versionPrefix}' extracted from path '${path}'`)
  }
}

function buildRfc5988Link (parts) {
  function appendCommaIfNecessary (linkHeader) {
    if (linkHeader.length > 0) {
      return linkHeader + ', '
    }
    return linkHeader
  }

  function createLinkHeader (href, rel) {
    return '<' + href + '>; rel="' + rel + '"'
  }
  return parts.reduce((previous, current) => {
    let currFragment = createLinkHeader(current.href, current.rel)
    let result = previous
    result = appendCommaIfNecessary(result)
    result += currFragment
    return result
  }, '')
}

function rfc5988LinkPart (href, rel) {
  return {href, rel}
}

function buildHateoasLinkHeader (event, linkHeaderData) {
  let strategyHasStart = {
    offsetForNext: linkHeaderData => {
      return linkHeaderData.start + linkHeaderData.rows
    },
    offsetForPrev: linkHeaderData => {
      return linkHeaderData.start - linkHeaderData.rows
    },
    offsetForFirst: () => {
      return 0
    },
    offsetForLast: linkHeaderData => {
      return (linkHeaderData.totalPages - 1) * linkHeaderData.rows
    },
    queryStringWithOffset: offset => {
      let params = event.queryStringParameters || {}
      params.start = offset
      return querystring.stringify(params)
    }
  }
  let strategyHasNoStart = {
    offsetForNext: linkHeaderData => {
      return linkHeaderData.pageNumber + 1
    },
    offsetForPrev: linkHeaderData => {
      return linkHeaderData.pageNumber - 1
    },
    offsetForFirst: () => {
      return 1
    },
    offsetForLast: linkHeaderData => {
      return linkHeaderData.totalPages
    },
    queryStringWithOffset: offset => {
      let params = event.queryStringParameters || {}
      params.pageNum = offset
      return querystring.stringify(params)
    }
  }
  let key = typeof linkHeaderData.start
  let strategyMapping = {
    'number': strategyHasStart,
    'undefined': strategyHasNoStart
  }
  let strategy = strategyMapping[key]
  if (typeof strategy === 'undefined') {
    throw new Error(`Programmer problem: no strategy found for key='${key}'`)
  }
  if (typeof event === 'undefined') {
    throw new Error('Programmer problem: event was not supplied')
  }
  // FIXME should handle if expected values aren't available
  let pageNumber = linkHeaderData.pageNumber
  let totalPages = linkHeaderData.totalPages
  let scheme = event.headers['X-Forwarded-Proto']
  let host = event.headers.Host
  let path = event.requestContext.path // we use the requestContext.path because it WILL have a stage prefix if needed
  let fromPath = `${scheme}://${host}${path}`
  let linkParts = []
  let hasNextPage = pageNumber < totalPages
  if (hasNextPage) {
    let offsetForNextPage = strategy.offsetForNext(linkHeaderData)
    let qs = strategy.queryStringWithOffset(offsetForNextPage)
    let uriForNextPage = `${fromPath}?${qs}`
    linkParts.push(rfc5988LinkPart(uriForNextPage, 'next'))
  }
  let hasPrevPage = pageNumber > 1
  if (hasPrevPage) {
    let offsetForPrevPage = strategy.offsetForPrev(linkHeaderData)
    let qs = strategy.queryStringWithOffset(offsetForPrevPage)
    let uriForPrevPage = `${fromPath}?${qs}`
    linkParts.push(rfc5988LinkPart(uriForPrevPage, 'prev'))
  }
  let hasFirstPage = pageNumber > 1
  if (hasFirstPage) {
    let offsetForFirstPage = strategy.offsetForFirst()
    let qs = strategy.queryStringWithOffset(offsetForFirstPage)
    let uriForFirstPage = `${fromPath}?${qs}`
    linkParts.push(rfc5988LinkPart(uriForFirstPage, 'first'))
  }
  let hasLastPage = pageNumber < totalPages
  if (hasLastPage) {
    let offsetForLastPage = strategy.offsetForLast(linkHeaderData)
    let qs = strategy.queryStringWithOffset(offsetForLastPage)
    let uriForLastPage = `${fromPath}?${qs}`
    linkParts.push(rfc5988LinkPart(uriForLastPage, 'last'))
  }
  return buildRfc5988Link(linkParts)
}

const validatorIsValid = { isValid: true }
const validatorNotValid = message => {
  return { isValid: false, message: message }
}

function speciesNamesValidator (_, requestBody) {
  return genericMandatoryNamesValidator(speciesNamesParam, _, requestBody)
}

function traitNamesMandatoryValidator (_, requestBody) {
  return genericMandatoryNamesValidator(traitNamesParam, _, requestBody)
}

// Validates that the target property MUST be present, is the right type, has items and
// the items are the right type
function genericMandatoryNamesValidator (namesParam, _, requestBody) {
  let isRequestBodyNotSupplied = requestBody === null || typeof requestBody === 'undefined'
  if (isRequestBodyNotSupplied) {
    return validatorNotValid('No request body was supplied')
  }
  let names = requestBody[namesParam]
  let isFieldNotSupplied = typeof names === 'undefined'
  if (isFieldNotSupplied) {
    return validatorNotValid(`The '${namesParam}' field was not supplied`)
  }
  let isFieldNotArray = names.constructor !== Array
  if (isFieldNotArray) {
    return validatorNotValid(`The '${namesParam}' field must be an array (of strings)`)
  }
  let isArrayEmpty = names.length < 1
  if (isArrayEmpty) {
    return validatorNotValid(`The '${namesParam}' field is mandatory and was not supplied`)
  }
  let isAnyElementNotStrings = names.some(element => { return typeof element !== 'string' })
  if (isAnyElementNotStrings) {
    return validatorNotValid(`The '${namesParam}' field must be an array of strings. You supplied a non-string element.`)
  }
  return validatorIsValid
}

function traitNamesOptionalValidator (_, requestBody) {
  return genericOptionalNamesValidator(traitNamesParam, _, requestBody)
}

function envVarNamesOptionalValidator (_, requestBody) {
  return genericOptionalNamesValidator(varNamesParam, _, requestBody)
}

// Validates that IF the target property is present then it is the right type and
// if it has items that the items are the right type
function genericOptionalNamesValidator (namesParam, _, requestBody) {
  let isRequestBodyNotSupplied = requestBody === null || typeof requestBody === 'undefined'
  if (isRequestBodyNotSupplied) {
    return validatorIsValid
  }
  let names = requestBody[namesParam]
  let isFieldNotSupplied = typeof names === 'undefined'
  if (isFieldNotSupplied) {
    return validatorIsValid
  }
  let isFieldNotArray = names.constructor !== Array
  if (isFieldNotArray) {
    return validatorNotValid(`The '${namesParam}' field must be an array (of strings)`)
  }
  let isArrayEmpty = names.length < 1
  if (isArrayEmpty) {
    return validatorIsValid
  }
  let isAnyElementNotStrings = names.some(element => { return typeof element !== 'string' })
  if (isAnyElementNotStrings) {
    return validatorNotValid(`The '${namesParam}' field must be an array of strings. You supplied a non-string element.`)
  }
  return validatorIsValid
}

function compositeValidator (validatorArray) {
  return (queryStringObj, requestBody) => {
    for (let i = 0; i < validatorArray.length; i++) {
      let currValidator = validatorArray[i]
      let currResult = currValidator(queryStringObj, requestBody)
      if (!currResult.isValid) {
        return currResult
      }
    }
    return validatorIsValid
  }
}

function queryStringParamIsNumberInRangeIfPresentValidator (paramName, minValue, maxValue) {
  return (queryStringObj, _) => {
    let isQueryStringNotPresent = queryStringObj === null || typeof queryStringObj === 'undefined'
    if (isQueryStringNotPresent) {
      return validatorIsValid
    }
    let value = queryStringObj[paramName]
    let isParamNotPresent = typeof value === 'undefined'
    if (isParamNotPresent) {
      return validatorIsValid
    }
    let parsedValue = parseInt(value)
    let isParamNotANumber = isNaN(parsedValue)
    if (isParamNotANumber) {
      return validatorNotValid(`The '${paramName}' must be a number when supplied. Supplied value = '${value}'`)
    }
    let isParamLessThanMin = parsedValue < minValue
    if (isParamLessThanMin) {
      return validatorNotValid(`The '${paramName}' must be a number >= ${minValue}. Supplied value = '${value}'`)
    }
    let isParamGreaterThanMax = parsedValue > maxValue
    if (isParamGreaterThanMax) {
      return validatorNotValid(`The '${paramName}' must be a number <= ${maxValue}. Supplied value = '${value}'`)
    }
    return validatorIsValid
  }
}

function queryStringParamIsBooleanIfPresentValidator (paramName) {
  return (queryStringObj, _) => {
    let isQueryStringNotPresent = queryStringObj === null || typeof queryStringObj === 'undefined'
    if (isQueryStringNotPresent) {
      return validatorIsValid
    }
    let value = queryStringObj[paramName]
    let isParamNotPresent = typeof value === 'undefined'
    if (isParamNotPresent) {
      return validatorIsValid
    }
    let isParamNotString = typeof value !== 'string'
    if (isParamNotString) { // unlikely because AWS should stringify everything
      return validatorNotValid(`The '${paramName}' must be a stringified boolean when supplied. Supplied value = '${value}'`)
    }
    let lowercaseValue = value.toLowerCase()
    switch (lowercaseValue) {
      case 'true':
      case 'false':
        return validatorIsValid
      default:
        return validatorNotValid(`The '${paramName}' must be a stringified boolean when supplied. Supplied value = '${value}'.` +
          ` Acceptable values are (case insensitive) 'true' or 'false'.`)
    }
  }
}

function buildLinkHeaderData (start, rows, pageNumber, totalPages) {
  return { // Uses ECMAScript 2015 shorthand property names
    start,
    rows,
    pageNumber,
    totalPages
  }
}

function transformResponseToLinkHeaderData (input) {
  let start = input.responseHeader.params.start
  let rows = input.responseHeader.params.rows
  let pageNumber = input.responseHeader.pageNumber
  let totalPages = input.responseHeader.totalPages
  return buildLinkHeaderData(start, rows, pageNumber, totalPages)
}

function toLinkHeaderDataAndResponseObj (input) {
  return {
    body: input,
    linkHeaderData: transformResponseToLinkHeaderData(input)
  }
}

const jsonResponseHelpers = {
  ok: (theCallback, theBody, event, linkHeaderData) => {
    let extraHeadersCallback = function () { }
    if (linkHeaderData) {
      extraHeadersCallback = headers => {
        headers.link = buildHateoasLinkHeader(event, linkHeaderData)
      }
    }
    doResponse(theCallback, theBody, 200, jsonContentType, extraHeadersCallback)
  },
  badRequest: (theCallback, theMessage) => {
    let statusCode = 400
    let body = {
      message: theMessage,
      statusCode: statusCode
    }
    doResponse(theCallback, body, statusCode, jsonContentType)
  },
  notFound: (theCallback, theMessage) => {
    let statusCode = 404
    let body = {
      message: theMessage,
      statusCode: statusCode
    }
    doResponse(theCallback, body, statusCode, jsonContentType)
  },
  internalServerError: (theCallback) => {
    let statusCode = 500
    let body = {
      message: msg500,
      statusCode: statusCode
    }
    doResponse(theCallback, body, statusCode, jsonContentType)
  }
}

const csvResponseHelpers = {
  ok: (theCallback, theBody, downloadFileName, event, dataForHateoas) => {
    let extraHeadersCallback = headers => {
      if (typeof downloadFileName !== 'undefined' && downloadFileName !== null) {
        headers['Content-Disposition'] = `attachment;filename=${downloadFileName}`
      }
      if (dataForHateoas) {
        headers.link = buildHateoasLinkHeader(event, dataForHateoas)
      }
    }
    doResponse(theCallback, theBody, 200, csvContentType, extraHeadersCallback)
  }
}

module.exports = {
  json: jsonResponseHelpers,
  csv: csvResponseHelpers,
  getContentType: getContentType,
  isQueryStringParamPresent: isQueryStringParamPresent,
  assertNumber: assertNumber,
  assertIsSupplied: assertIsSupplied,
  getOptionalStringParam: getOptionalStringParam,
  getOptionalNumberParam: getOptionalNumberParam,
  getOptionalNumber: getOptionalNumber,
  getOptionalArray: getOptionalArray,
  calculateOffset: calculateOffset,
  resolveVocabCode: resolveVocabCode,
  now: now,
  calculateElapsedTime: elapsedTimeCalculator,
  contentTypes: contentTypes,
  calculatePageNumber: calculatePageNumber,
  calculateTotalPages: calculateTotalPages,
  newContentNegotiationHandler: newContentNegotiationHandler,
  handleJsonPost: handleJsonPost,
  handleCsvPost: handleCsvPost,
  handleJsonGet: handleJsonGet,
  handleCsvGet: handleCsvGet,
  buildHateoasLinkHeader: buildHateoasLinkHeader,
  speciesNamesValidator: speciesNamesValidator,
  traitNamesMandatoryValidator: traitNamesMandatoryValidator,
  traitNamesOptionalValidator: traitNamesOptionalValidator,
  envVarNamesOptionalValidator: envVarNamesOptionalValidator,
  compositeValidator: compositeValidator,
  nullValidator: () => { return validatorIsValid },
  pageSizeValidator: queryStringParamIsNumberInRangeIfPresentValidator(pageSizeParam, pageSizeMin, pageSizeMax),
  pageNumValidator: queryStringParamIsNumberInRangeIfPresentValidator(pageNumParam, pageNumMin),
  startValidator: queryStringParamIsNumberInRangeIfPresentValidator(startParam, startMin),
  rowsValidator: queryStringParamIsNumberInRangeIfPresentValidator(rowsParam, rowsMin, rowsMax),
  downloadParamValidator: queryStringParamIsBooleanIfPresentValidator(downloadParam),
  newVersionHandler: newVersionHandler,
  buildLinkHeaderData: buildLinkHeaderData,
  toLinkHeaderDataAndResponseObj: toLinkHeaderDataAndResponseObj,
  buildRfc5988Link: buildRfc5988Link,
  rfc5988LinkPart: rfc5988LinkPart,
  _testonly: {
    queryStringParamIsNumberInRangeIfPresentValidator: queryStringParamIsNumberInRangeIfPresentValidator,
    queryStringParamIsBooleanIfPresentValidator: queryStringParamIsBooleanIfPresentValidator,
    genericMandatoryNamesValidator: genericMandatoryNamesValidator,
    genericOptionalNamesValidator: genericOptionalNamesValidator,
    getUrlSuffix: getUrlSuffix
  }
}
