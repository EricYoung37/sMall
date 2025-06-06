# SMall
**Author: M. Yang**

## Architectural Diagram

```mermaid
graph TD
    ClientApp["Client Application"] --> APIGateway[API Gateway]
    
    APIGateway <--> Eureka[Eureka - Service Discovery]
    Eureka <.-> AuthService["Authentication Service (JWT Issuer)"]
    Eureka <.-> AccountService[Account Service]
    Eureka <.-> OrderService[Order Service]
    Eureka <.-> PaymentService[Payment Service]

    AuthService --> MySQL_Credentials[MySQL - User Credentials]
    AuthService <==> Redis["Redis - User JWT Store"]
    APIGateway ==> Redis
    
    AccountService --> MySQL_Accounts[MySQL - User Accounts]
    OrderService --> CassandraOrders[Cassandra - Orders]
    PaymentService --> MySQL_Payments[MySQL - Payments]
    
    OrderService --Order Service Signal--> Kafka
    Kafka --Order Service Signal--> PaymentService
    PaymentService --Payment Id--> Kafka
    Kafka --Payment Id--> OrderService
    OrderService -- "Payment Id (to be Converted to Redirect URL)" --> ClientApp
    
    %% Define styles for different categories
    classDef clientApp fill:#f9c2ff,stroke:#6a1b9a;
    classDef apiGateway fill:#d1a7f7;
    classDef serviceDiscovery fill:#90caf9,stroke:#1e88e5;
    classDef services fill:#a5d6a7,stroke:#388e3c;
    classDef kafka fill:#ff7043,stroke:#bf360c;
    classDef redis fill:#ef9a9a,stroke:#c62828;
    classDef databases fill:#ffe082,stroke:#fbc02d;
    
    %% Apply styles
    class ClientApp clientApp;
    class APIGateway apiGateway;
    class AuthService,AccountService,OrderService,PaymentService services;
    class Eureka serviceDiscovery;
    class Kafka kafka;
    class Redis redis;
    class MySQL_Accounts,MySQL_Credentials,CassandraOrders,MySQL_Payments databases;
```

## Environment Variables
<details>
<summary>.env</summary>

```
API_GATEWAY_PORT=
AUTH_SERVICE_PORT=
ACCOUNT_SERVICE_PORT=
ORDER_SERVICE_PORT=
PAYMENT_SERVICE_PORT=

EUREKA_PORT=8761

MYSQL_USER=
MYSQL_PWD=
MYSQL_PORT=
MYSQL_DB=s_mall

JWT_SECRET=must-be-a-Base64-encoded-secret

INTERNAL_AUTH_TOKEN=jwt-recommended
INTERNAL_AUTH_HEADER=

REDIS_PORT=6379
REDIS_PWD=

CASSANDRA_PORT=9042
CASSANDRA_USER=cassandra
CASSANDRA_PWD=
CASSANDRA_KEYSPACE=s_mall

ZOOKEEPER_PORT=2181

KAFKA_EXTERNAL_PORT=9092
KAFKA_INTERNAL_PORT=29092
KAFKA_CONSUMER_GROUP=order-payment-group
```
</details>

<details>
<summary>./auth-service/.env</summary>

```
JWT_EXP_MS=
```
</details>


## Run the App
### Option 1: Docker Compose for *All*
```shell
# docker compose --env-file .env up --build
$ docker compose up
```

### Option 2: IntelliJ IDEA for *Spring Boot Services*
Use IDEA's run button to run these. Use the **Services** panel (bottom left) to run them all at once.
- [docker-compose.yml](docker-compose.yml)
- [ServiceDiscoverer](service-discoverer/src/main/java/com/small/backend/servicediscoverer/ServiceDiscovererApplication.java)
- [ApiGatewayApplication](api-gateway/src/main/java/com/small/backend/apigateway/ApiGatewayApplication.java)
- [AccountServiceApplication](account-service/src/main/java/com/small/backend/accountservice/AccountServiceApplication.java)
- [AuthServiceApplication](auth-service/src/main/java/com/small/backend/authservice/AuthServiceApplication.java)
- [PaymentServiceApplication](payment-service/src/main/java/com/small/backend/paymentservice/PaymentServiceApplication.java)
- [OrderServiceApplication](order-service/src/main/java/com/small/backend/orderservice/OrderServiceApplication.java)

Order **matters**.
**Some services** (e.g., Eureka, API gateway) and the **Cassandra** database must be fully ready before other services can communicate with them.

### Option 3: Terminal for *Spring Boot Services*
The app can also be run from the terminal if IDEA is not available. **Windows users** need to use **Git Bash**.
```shell
# project's root directory
# Cassandra may take around 5 minutes to boot
$ docker compose up
```

```shell
# in common/
# this runs mvn clean install for common
# without this, service modules like /auth-service won't compile
# don't use `$ . run.sh` because it can close the current shell immediately upon exit (nothing can be observed)
$ ./run.sh
```

```shell
# in service-discover/, api-gateway/, /auth-service, etc.
# Start service-discover first.
# don't use `$ . run.sh` because it can close the current shell immediately upon exit (nothing can be observed)
$ ./run.sh
```

## Helpful Commands

### Docker

After rebuilding an image with `docker compose up --build`, the old version becomes `<none>` (dangling).

Clean them up with:
```shell
# List dangling images
$ docker images -f dangling=true

# Prune dangling images
$ docker image prune
```

### Cassandra

These commands may be issued in the Docker container's shell.

```
-- Log in to Cassandra with the specified username and password
cqlsh -u cassandra -p $CASSANDRA_PASSWORD

-- Show all keyspaces in the cluster
DESCRIBE KEYSPACES;

-- Show all tables in keyspace `s_mall`
USE s_mall;
DESCRIBE orders;

-- Select the first 5 records from `orders`
SELECT * FROM orders LIMIT 5;

-- Clear all records from `orders`
TRUNCATE orders;
```

### MySQL

These commands may be issued in the Docker container's shell.

```
-- Log in to MySQL with username `user` and a password variable
mysql -u user -p $MYSQL_PASSWORD

-- Show all databases
SHOW DATABASES;

-- Select the `s_mall` database for use
USE s_mall;

-- Show all tables in the selected database
SHOW TABLES;

-- Select the first 5 records from `payments`
SELECT * FROM payments LIMIT 5;

-- Clear `payments`
-- This will reset all the auto incremental fields
TRUNCATE TABLE payments;

-- Clear `payments` but no reset
DELETE FROM payments;
```

### Windows Port Permissions
If this issue occurs when running a container
```
An attempt was made to access a socket in a way forbidden by its access permissions
```

Run Command Prompt or PowerShell as administrator and issue the commands below
```
net stop winnat
net start winnat
```