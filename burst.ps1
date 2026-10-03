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

            return [PSCustomObject]@{
                StatusCode = [int]$response.StatusCode
                Body = $response.Content
            }

        } catch {

            if ($_.Exception.Response) {

                return [PSCustomObject]@{
                    StatusCode = [int]$_.Exception.Response.StatusCode
                    Body = $_.ErrorDetails.Message
                }

            }

            return [PSCustomObject]@{
                StatusCode = 0
                Body = $_.Exception.Message
            }
        }

    } -ArgumentList $uri, $userId, $SeatId
}

$results = $jobs | Wait-Job | Receive-Job

$jobs | Remove-Job



$results |
    Group-Object StatusCode |
    Sort-Object Name |
    ForEach-Object {
        Write-Host "$($_.Name) : $($_.Count)"
    }



$success = @($results | Where-Object { $_.StatusCode -eq 201 }).Count
$conflict = @($results | Where-Object { $_.StatusCode -eq 409 }).Count
$serverError = @($results | Where-Object { $_.StatusCode -ge 500 }).Count
$other = @($results | Where-Object {
    $_.StatusCode -ne 201 -and
    $_.StatusCode -ne 409 -and
    $_.StatusCode -lt 500
}).Count

Write-Host "Confirmed/Created : $success"
Write-Host "Declined (409)    : $conflict"
Write-Host "5xx               : $serverError"
Write-Host "Other             : $other"

Write-Host ""
Write-Host "Burst test completed."