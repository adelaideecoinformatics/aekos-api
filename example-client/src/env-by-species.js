var app = angular.module('aekosapiexample')

app.config(function ($urlRouterProvider, $stateProvider) {
  $stateProvider.state({
    name: 'exampleParent.envBySpecies',
    url: '/env-by-species',
    component: 'envBySpecies'
  })
})

app.component('envBySpecies', {
  templateUrl: './src/env-by-species.html',
  controller: 'EnvBySpeciesController'
})

app.controller('EnvBySpeciesController', function ($http, $scope, baseUrlService, commonHelpersService) {
  var speciesRows = 20
  $scope.envDataAccept = 'application/json'

  $scope.isLoadVocabClicked = false
  $scope.loadVocabs = function () {
    $scope.vars = []
    resetStep3()
    $scope.isLoadVocabClicked = true
    $scope.isLoadingVars = true
    $http.get(baseUrlService.getBaseUrl() + '/v2/getEnvironmentalVariableVocab.json').then(function (response) {
      $scope.isLoadingVars = false
      $scope.vars = response.data
    }, errorHandler)
  }

  function resetStep2 () {
    $scope.species = []
    $scope.isLoadSpeciesClicked = false
  }

  $scope.findSpecies = function () {
    $scope.speciesPaging = {
      start: 0,
      rows: speciesRows
    }
    speciesSearchHelper()
  }
  $scope.speciesNextPage = function () {
    var nextStart = $scope.speciesPaging.start + $scope.speciesPaging.rows
    $scope.speciesPaging = {
      start: nextStart,
      rows: speciesRows
    }
    speciesSearchHelper()
  }

  function speciesSearchHelper () {
    resetStep2()
    resetStep3()
    $scope.isLoadSpeciesClicked = true
    $scope.isLoadingSpecies = true
    var partialSpeciesName = $scope.partialSpeciesName
    var config = {
      params: {
        start: $scope.speciesPaging.start,
        rows: $scope.speciesPaging.rows,
        q: partialSpeciesName
      }
    }
    $http.get(baseUrlService.getBaseUrl() + '/v2/speciesAutocomplete.json', config).then(function (response) {
      $scope.isLoadingSpecies = false
      $scope.species = response.data
    }, errorHandler)
  }

  $scope.isApplyVarFilter = false
  function resetStep3 () {
    $scope.envRecords = []
    $scope.envRecordsHeader = {}
    $scope.isLoadEnvDataClicked = false
  }

  $scope.getEnvData = function () {
    envDataHelper(baseUrlService.getBaseUrl() + '/v2/environmentData')
  }

  $scope.envDataChangePage = function (url) {
    envDataHelper(url)
  }

  $scope.prettyEnvDataJson = function () {
    return JSON.stringify($scope.envRecords, null, 2)
  }

  function envDataHelper (url) {
    resetStep3()
    var speciesNames = commonHelpersService.getChecked($scope.species, 'speciesName')
    if (speciesNames.length === 0) {
      alert('[ERROR] you need to select at least one species name from step 2 first')
      return
    }
    $scope.isLoadEnvDataClicked = true
    $scope.isLoadingEnvData = true
    var varCodes = []
    if ($scope.isApplyVarFilter) {
      varCodes = commonHelpersService.getChecked($scope.vars, 'code')
    }
    var data = {
      speciesNames: speciesNames,
      varNames: varCodes
    }
    var config = {
      headers: {
        Accept: $scope.envDataAccept
      }
    }
    $http.post(url, data, config).then(function (response) {
      $scope.isLoadingEnvData = false
      var acceptHeader = $scope.envDataAccept
      switch (acceptHeader) {
        case 'application/json':
          $scope.envRecords = response.data.response
          $scope.envRecordsHeader = response.data.responseHeader
          $scope.envDataResultWindow = 'json'
          break
        case 'text/csv':
          $scope.envRecords = response.data
          $scope.envDataResultWindow = 'csv'
          break
        default:
          throw new Error('Programmer problem: unhandled accept header=' + acceptHeader)
      }
      // uses wombleton/link-headers so we don't hardcode paging URLs
      var rawLinkHeader = response.headers('link')
      var parsedLinkHeader = $.linkheaders(rawLinkHeader)
      $scope.envDataFirstPageUrl = commonHelpersService.getLink(parsedLinkHeader, 'first')
      $scope.envDataPrevPageUrl = commonHelpersService.getLink(parsedLinkHeader, 'prev')
      $scope.envDataNextPageUrl = commonHelpersService.getLink(parsedLinkHeader, 'next')
      $scope.envDataLastPageUrl = commonHelpersService.getLink(parsedLinkHeader, 'last')
    }, errorHandler)
  }
})

function errorHandler (error) {
  var msg = 'Something went wrong:'
  alert(msg + error.message)
  throw new Error(msg, error)
}