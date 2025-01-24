# Banking Application

This is a simple banking application that provides functionalities like deposit, withdrawal, and transfer for accounts. It uses Spring Boot, Spring Data JPA, and PostgreSQL as the database. The application also implements retry logic with optimistic locking for concurrent transactions.

## Features

- **Deposit**: Add money to an account.
- **Withdraw**: Withdraw money from an account.
- **Transfer**: Transfer money from one account to another.

## Technologies Used

- **Spring Boot**: Framework for building Java applications.
- **Spring Data JPA**: Provides JPA-based data access.
- **PostgreSQL**: Relational database used to store account data.
- **Docker**: Containerization of the application and database.
- **Spring Retry**: For retrying operations like transfer in case of concurrency issues.

## Database Setup with Docker

To run the application with a PostgreSQL database in Docker, follow these steps:

### Prerequisites

1. **Docker**: Make sure you have Docker installed on your machine. You can download Docker from [here](https://www.docker.com/get-started).

2. **Docker Compose**: If you haven't already installed Docker Compose, follow the installation guide [here](https://docs.docker.com/compose/install/).

### Running the Containers

1. **Clone the repository:**

    ```bash
    git clone https://github.com/your-repo/banking-application.git
    cd banking-application
    ```

2. **Start the containers:**

   In the root directory of the project, create a `docker-compose.yml` file if not already present, or check if it’s already configured.

   Here's an example of a `docker-compose.yml` file for PostgreSQL:

    ```yaml
    version: '3'
    services:
      postgres:
        image: postgres:13
        container_name: postgres-db
        environment:
          POSTGRES_USER: user
          POSTGRES_PASSWORD: password
          POSTGRES_DB: banking
        ports:
          - "5432:5432"
        volumes:
          - postgres-data:/var/lib/postgresql/data
    volumes:
      postgres-data:
    ```

   Now, run the following command to start the database container:

    ```bash
    docker-compose up -d
    ```

3. **Verify the database container is running:**

    ```bash
    docker ps
    ```

   You should see the `postgres-db` container running on port `5432`.

4. **Access the PostgreSQL database:**

   You can connect to the running PostgreSQL container using any PostgreSQL client with the following credentials:

    - **Host**: `localhost`
    - **Port**: `5432`
    - **User**: `user`
    - **Password**: `password`
    - **Database**: `banking`

   Alternatively, you can connect directly via Docker:

    ```bash
    docker exec -it postgres-db psql -U user -d banking
    ```

5. **Running the Application:**

   Once the database container is running, you can start your Spring Boot application:

    ```bash
    ./mvnw spring-boot:run
    ```

   The application will be available at `http://localhost:8080`.


6. **Access Swagger UI:**

   After starting the application, open the following URL in your browser to access the Swagger UI:

    - **Swagger UI**: `http://localhost:8080/swagger-ui.html`


## Why Optimistic Locking?

In a banking application, concurrent updates to the same account can lead to data inconsistencies, especially when two or more transactions modify the same account balance simultaneously. To handle this, we use **optimistic locking** to avoid conflicts without locking the records, which could cause performance degradation.

### Optimistic Locking vs Pessimistic Locking

- **Pessimistic Locking**: With pessimistic locking, a lock is placed on the resource when it is read, and no other transaction can modify the data until the lock is released. This can result in deadlocks or performance bottlenecks if there are many concurrent operations.

- **Optimistic Locking**: Optimistic locking allows multiple transactions to work with the data at the same time. When a transaction updates a record, it checks whether another transaction has modified the record since it was read. If there’s a conflict (i.e., the version number of the record has changed), an exception is thrown, and the transaction can be retried. This approach is more suitable for high-concurrency scenarios, such as banking applications, where the likelihood of conflicts is lower.

In our application, we use **OptimisticLockingFailureException** to detect conflicts and retry the operation using **Spring Retry**. This ensures that the application remains responsive even under heavy load and avoids the pitfalls of pessimistic locking, which could lead to resource contention.

## Conclusion

This banking application demonstrates how to handle account transactions with retries, optimistic locking, and concurrent transactions. By using Docker, we simplify the setup of the PostgreSQL database and ensure that the environment is consistent across different machines.
