**# API Endpoints**



**### Create Reservation**



**POST /api/reservations**



**Creates a reservation and temporarily holds the requested seats.**



**Headers:**

**Content-Type: application/json**

**Idempotency-Key: <unique-key>**



**Request:**

**{**

&#x20; **"userId": 101,**

&#x20; **"seatIds": \[1, 2],**

&#x20; **"totalAmountPaise": 5068**

**}**



**### Confirm Reservation**



**POST /api/reservations/{reservationId}/confirm**



**Confirms a held reservation and reserves the seats.**



**Example:**

**POST /api/reservations/1/confirm**



**### Get Reservation**



**GET /api/reservations/{reservationId}**



**Returns the current state of a reservation.**



**Example:**

**GET /api/reservations/1**



**### Health Check**



**GET /actuator/health**



**Checks whether the reservation service is running.**



**Example:**

**GET http://localhost:8080/actuator/health**

