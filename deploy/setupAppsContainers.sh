#!/bin/bash
#set -xv

SERVICE_CODE="app"

PG_PORT="5532"
PG_ADMIN_USER="postgres"
PG_CONTAINER="tapis_${SERVICE_CODE}_postgres"
PG_USER="tapis_${SERVICE_CODE}"
PG_SCHEMA="tapis_${SERVICE_CODE}"
PG_DATABASE="tapis${SERVICE_CODE}db"
PG_PASSWORD="password"

function usage() {
  echo "$0 [-t tenant] [-p password] [-u user] [--dev | --prod | --staging]"

  echo "OPTIONS:"
  echo "     -p --port"
  echo "        The port to run postgres on"
  echo 
  echo "     -u --pguser"
  echo "        The postgres user for the service"
  echo 
  echo "     -w --pgpass"
  echo "        The postgres password for the service"
  echo 
  echo "     -d --pgdb"
  echo "        The postgres database name for the service"
  echo 
  echo "     -s --pgschema"
  echo "        The postgres schema for the service"
  echo 
  exit 1
}

function announce() {
  echo ---==== $@ ====---
}

while [[ $# -gt 0 ]]; do
  case $1 in
    -p|--port)
      PG_PORT="$2"
      shift # past argument
      shift # past value
      ;;
    -u|--pguser)
      PG_USER="$2"
      shift # past argument
      shift # past value
      ;;
    -w|--pgpass)
      PG_PASSWORD="$2"
      shift # past argument
      shift # past value
      ;;
    -d|--pgdb)
      PG_DATABASE="$2"
      shift # past argument
      shift # past value
      ;;
    -s|--pgschema)
      PG_SCHEMA="$2"
      shift # past argument
      shift # past value
      ;;
    -*|--*)
      echo "Unknown option $1"
      usage
      ;;
    *)
      echo "Unknown positional arguement $1"
      usage
  esac
done

announce "starting database container on port ${PG_PORT}"
export PG_PORT
docker compose up --wait

announce "pause a moment to ensure database is running ok"
sleep 2

announce "create database and user"
docker exec -i ${PG_CONTAINER} psql -U ${PG_ADMIN_USER} <<EOD
CREATE DATABASE ${PG_DATABASE} ENCODING='UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';
CREATE USER ${PG_USER} WITH ENCRYPTED PASSWORD '${PG_PASSWORD}';
GRANT ALL PRIVILEGES ON DATABASE ${PG_DATABASE} TO ${PG_USER};
EOD

announce "create schema and set privileges"
docker exec -i ${PG_CONTAINER} psql -U ${PG_USER} ${PG_DATABASE} <<EOD
CREATE SCHEMA IF NOT EXISTS ${PG_SCHEMA} AUTHORIZATION ${PG_USER};
SET search_path TO ${PG_SCHEMA};
GRANT USAGE ON SCHEMA ${PG_SCHEMA} TO ${PG_USER};
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ${PG_SCHEMA} TO ${PG_USER};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ${PG_SCHEMA} TO ${PG_USER};
EOD

announce "set search_path for user and database"
docker exec -i ${PG_CONTAINER} psql -U ${PG_ADMIN_USER} <<EOD
ALTER DATABASE ${PG_DATABASE} SET search_path TO ${PG_SCHEMA};
ALTER ROLE ${PG_USER} SET search_path='${PG_SCHEMA}';
EOD

announce "set the following postgres related variables in your run configuration"
echo "TAPIS_DB_JDBC_URL=jdbc:postgresql://localhost:${PG_PORT}/${PG_DATABASE}"
echo "TAPIS_DB_PASSWORD=${PG_PASSWORD}"
echo "TAPIS_DB_USER=${PG_USER}"
