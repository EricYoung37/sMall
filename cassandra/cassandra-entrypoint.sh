#!/bin/bash

DEFAULT_PWD=cassandra

# Function to check if Cassandra is ready
is_cassandra_up() {
  # Try with updated password
  if cqlsh -u "${CASSANDRA_USER}" -p "${CASSANDRA_PWD}" -e "DESCRIBE KEYSPACES;" >/dev/null 2>&1; then
    return 0
  fi
  # Try with default password
  if cqlsh -u "${CASSANDRA_USER}" -p "$DEFAULT_PWD" -e "DESCRIBE KEYSPACES;" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

log() {
  echo "[cassandra-entrypoint] $1"
}

log "Starting Cassandra in background..."
/usr/local/bin/docker-entrypoint.sh cassandra -R &

# Retry logic using countdown
MAX_RETRIES=8
RETRY_INTERVAL=20

log "Waiting for Cassandra to be ready..."

until is_cassandra_up; do
  if [ "$MAX_RETRIES" -le 0 ]; then
    log "Cassandra did not become ready in time."
    exit 1
  fi
  log "Cassandra not ready. Retrying in $RETRY_INTERVAL seconds... (Remaining attempts: $MAX_RETRIES)"
  sleep "$RETRY_INTERVAL"
  MAX_RETRIES=$((MAX_RETRIES - 1))
done

log "Cassandra is ready. Checking superuser password..."

# first time connect or connect with default password to see if it's been updated
if cqlsh -u ${CASSANDRA_USER} -p "$DEFAULT_PWD" -e "SELECT release_version FROM system.local;" >/dev/null 2>&1; then
  # Create the keyspace if not exists
  log "Ensuring keyspace '${CASSANDRA_KEYSPACE}' exists..."
  cqlsh -u "${CASSANDRA_USER}" -p "$DEFAULT_PWD" -e "
  CREATE KEYSPACE IF NOT EXISTS ${CASSANDRA_KEYSPACE}
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};"
  log "Keyspace '${CASSANDRA_KEYSPACE}' is ready."

  # UDT order_item
    log "Ensuring UDT 'order_item' exists..."
    cqlsh -u "${CASSANDRA_USER}" -p "$DEFAULT_PWD" -k "${CASSANDRA_KEYSPACE}" -e "
    CREATE TYPE IF NOT EXISTS order_item (
      item_id uuid,
      item_name text,
      quantity int,
      refund_quantity int,
      unit_price double,
      merchant_id uuid
    );"
    log "UDT 'order_item' is ready."

  # user_email won't be used as partition key if table is automatically created by order-service
  log "Ensuring table 'orders' exists with proper primary key..."
  cqlsh -u "${CASSANDRA_USER}" -p "$DEFAULT_PWD" -k "${CASSANDRA_KEYSPACE}" -e "
  CREATE TABLE IF NOT EXISTS orders (
    user_email text,
    order_id uuid,
    status text,
    created_at timestamp,
    updated_at timestamp,
    items list<frozen<order_item>>,
    total_price double,
    refund_price double,
    shipping_address text,
    PRIMARY KEY (user_email, order_id)
  );"
  log "Table 'orders' is ready."

  log "Default password still in use. Updating superuser password..."
  cqlsh -u ${CASSANDRA_USER} -p "$DEFAULT_PWD" -e "ALTER USER '${CASSANDRA_USER}' WITH PASSWORD '${CASSANDRA_PWD}';"
  log "Superuser password updated."
else
  log "Superuser password has already been changed."

  # The keyspace was created along with the password update.
fi

log "Stopping temporary Cassandra process..."
pkill -f 'java.*cassandra'

log "Starting Cassandra in foreground..."
exec /usr/local/bin/docker-entrypoint.sh cassandra -f
