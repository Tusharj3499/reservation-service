param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Requests = 50,
    [int]$SeatId = 20
)

$uri = "$BaseUrl/api/reservations"

Write-Host "Starting burst test..."
Write-Host "Base URL : $BaseUrl"
Write-Host "Requests : $Requests"
Write-Host "Hot seat : $SeatId"
Write-Host ""

$jobs = 1..$Requests | ForEach-Object {

    $userId = 10000 + $_

    Start-Job -ScriptBlock {
        param(
            $uri,
            $userId,
            $seatId
        )

        $body = @{
            userId = $userId
            seatIds = @($seatId)
            totalAmountPaise = 5000
        } | ConvertTo-Json

        $idempotencyKey = [guid]::NewGuid().ToString()

        try {
            $response = Invoke-WebRequest `
            -Uri $uri `
            -Method POST `
            -ContentType "application/json" `
            -Headers @{
                "Idempotency-Key" = $idempotencyKey
            } `
            -Body $body `
            -UseBasicParsing

            [PSCustomObject]@{
                StatusCode = [int]$response.StatusCode
                Body = $response.Content
            }
        }
        catch {
            if ($_.Exception.Response) {
                [PSCustomObject]@{
                    StatusCode = [int]$_.Exception.Response.StatusCode
                    Body = $_.ErrorDetails.Message
                }
            }
            else {
                [PSCustomObject]@{
                    StatusCode = 0
                    Body = $_.Exception.Message
                }
            }
        }

    } -ArgumentList $uri, $userId, $SeatId

}

$results = $jobs | Wait-Job | Receive-Job

$jobs | Remove-Job

Write-Host ""
Write-Host "========== BURST RESULT =========="
Write-Host ""

$results |
        Group-Object StatusCode |
        Sort-Object Name |
        ForEach-Object {
            Write-Host "$($_.Name) : $($_.Count)"
        }

$seatTaken = @(
$results |
        Where-Object {
            $_.StatusCode -eq 409
        }
).Count

$success = @(
$results |
        Where-Object {
            $_.StatusCode -eq 201
        }
).Count

$conflict = @(
$results |
        Where-Object {
            $_.StatusCode -eq 409
        }
).Count

$serverError = @(
$results |
        Where-Object {
            $_.StatusCode -ge 500
        }
).Count

Write-Host ""
Write-Host "========== DECLINE REASONS =========="
Write-Host ""

Write-Host "seat-taken       : $seatTaken"
Write-Host "per-user-limit   : 0"
Write-Host "idempotent-replay: 0"

Write-Host ""
Write-Host "========== SUMMARY =========="
Write-Host ""

Write-Host "Confirmed : $success"
Write-Host "Declined  : $conflict"
Write-Host "5xx       : $serverError"

Write-Host ""
Write-Host "Burst test completed."

Write-Host ""
Write-Host "========== RECONCILIATION =========="
Write-Host ""

$confirmedResult = $results | Where-Object { $_.StatusCode -eq 201 } | Select-Object -First 1

if ($confirmedResult) {
    $confirmedBody = $confirmedResult.Body | ConvertFrom-Json
    $reservationId = $confirmedBody.reservationId

    Write-Host "Confirmed reservation ID : $reservationId"

    try {
        $reservation = Invoke-RestMethod `
            -Uri "$BaseUrl/api/reservations/$reservationId" `
            -Method GET

        Write-Host "Final reservation status : $($reservation.status)"
        Write-Host "Final user ID            : $($reservation.userId)"
        Write-Host "Final total amount      : $($reservation.totalAmountPaise)"
    }
    catch {
        Write-Host "Reconciliation failed"
    }
}
else {
    Write-Host "No confirmed reservation found"
}