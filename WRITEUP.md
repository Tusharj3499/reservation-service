
This project is a seat reservation service built using Spring Boot, Java, MySQL, and Spring Data JPA.

The main goal of the service is to handle multiple users trying to reserve seats at the same time without allowing the same seat to be booked twice.

The reservation API uses a database transaction for the complete reservation operation.

When a request comes in, the service first validates the request and checks the idempotency key.

After that, the requested seat IDs are sorted before the seats are locked.

The service uses a pessimistic write lock on the seat rows.

This means that when two users try to reserve the same seat at the same time, only one transaction can get the lock and continue with the reservation.

The other transaction waits for the lock and then checks the latest seat status.

If the seat is already held or reserved, that request is rejected.

Because the seat check and seat update happen inside the same transaction, two users cannot successfully reserve the same seat.

For multiple seats, the seat IDs are always sorted before locking them.

For example, if the request contains seats 10, 5 and 8, they are locked in the order 5, 8 and 10.

Using the same order for different transactions helps reduce the chance of deadlocks when multiple users are booking multiple seats.

The reservation is initially created with "HELD" status.

A hold is valid for five minutes.

The reservation stores the hold expiry time, and the selected seats are changed from "AVAILABLE" to "HELD".

A scheduled process runs every 30 seconds and checks for expired holds.

When a hold expires, the related seats are changed back to "AVAILABLE" and the reservation is changed to "EXPIRED".

The user can confirm a held reservation using the confirmation API.

During confirmation, the reservation and related seats are locked again before changing their status.

After successful confirmation, the reservation becomes "CONFIRMED" and the seats become "RESERVED".

The service also has a per-user limit of five confirmed reservations.

The user's reservation count is stored in the database and is checked while the user record is locked.

The API also supports idempotency using the "Idempotency-Key" header.

For every request, a SHA-256 hash is created using the user ID, sorted seat IDs and amount.

The idempotency key and request hash are stored in the database.

If the same key and same request are received again, the existing reservation is returned instead of creating another reservation.

If the same key is used with different request data, the request is rejected.

This prevents duplicate reservations during client retries and also prevents incorrect reuse of an idempotency key.

The service uses integer paise for money instead of floating-point values.

For example, ₹50 is stored as 5000 paise.

Prometheus metrics are exposed through "/actuator/prometheus".

The service tracks confirmed reservations, declined reservations by reason, and the number of available seats.

The application also generates a correlation ID for every request.

The correlation ID is returned in the response and is added to the application logs, which makes it easier to trace a request.

If MySQL is unavailable, the service does not try to continue with a reservation using incomplete data.

Instead, it fails closed and returns "503 Service Unavailable".

A PowerShell burst script is also included in the project to test multiple users trying to reserve the same seat at the same time.

During the hot-seat test, 50 requests were sent for the same seat.

The result was one successful "201" response and 49 "409" responses.

The Prometheus metric for "seat-taken" also showed 49 declined requests, which matched the burst test result.

AI was used during development to help understand concurrency, locking, idempotency, exception handling, metrics, logging and testing.

The final code was integrated into the project and tested during development.

Further improvements can include Flyway database migrations, more automated concurrency tests, better burst-test reporting, dashboards and production deployment.