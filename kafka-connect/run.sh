#!/usr/bin/env bash
# run the twitter connector
# cat is done to move out secrets from the twitter.properties file
connect-standalone.sh connect-standalone.properties $(cat twitter.properties secret.properties > tmp.properties | echo tmp.properties)

rm tmp.properties
