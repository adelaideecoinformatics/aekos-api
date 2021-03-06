#!/usr/bin/env bash
# Downloads the current Swagger definition from AWS API Gateway
cd `dirname $0`
set -e
STAGE=$1
if [ -z "$STAGE" ]; then
  STAGE=dev
fi
API_ID=`./get-rest-api-id.sh $STAGE`
FILE_TYPE=json
REGION=ap-southeast-2
OUTPUT=swagger-aekos-api-$STAGE.$FILE_TYPE
TEMP_FILE=$OUTPUT.temp
printf "Downloading Swagger definition to ./$OUTPUT
  API ID: $API_ID
   Stage: $STAGE
  Accept: $FILE_TYPE
  Region: $REGION\n\n"

aws apigateway get-export \
  --rest-api-id=$API_ID \
  --stage-name=$STAGE \
  --export-type=swagger \
  --accept=application/$FILE_TYPE \
  --region=$REGION \
  $TEMP_FILE

node ammend-swagger.js $TEMP_FILE > $OUTPUT
rm -f $TEMP_FILE
