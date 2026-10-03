**## Reservation API**



**### Get Reservation**



**Returns the current state of a reservation.**





**GET /api/reservations/{reservationId}**



**Example**



**GET http://localhost:8080/api/reservations/1**

**{**

&#x20; **"id": 1,**

&#x20; **"userId": 101,**

&#x20; **"status": "CONFIRMED",**

&#x20; **"totalAmountPaise": 5068**

**}**

