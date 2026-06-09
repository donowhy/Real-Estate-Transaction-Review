#!/bin/sh
set -eu

for database in orderdb inventorydb paymentdb deliverydb notificationdb; do
  psql --set ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    CREATE DATABASE ${database};
SQL
done
