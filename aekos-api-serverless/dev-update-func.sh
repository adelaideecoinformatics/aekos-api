#!/bin/bash
cd `dirname $0`
FUNC=$1
if [ -z "$FUNC" ]; then
  echo "Updates a single function (much quicker than a full deploy)."
  echo "The function name is one of the keys under 'functions' in serverless.yml."
  echo "Usage: $0 <function-name>"
  echo "   eg: $0 v10-getTraitVocab-json"
  exit 1
fi
serverless deploy function --function=$1 --stage=dev --region=us-west-1