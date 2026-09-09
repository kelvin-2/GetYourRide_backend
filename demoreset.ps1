# Prepares demo data: puts the student's finished trips back to SCHEDULED so they read
# "NOT STARTED / Waiting for driver..." and can each be started fresh during the demo.
# Uses the fixed /schedule endpoint, which also clears stale position, leg index, arrival
# time and ARRIVED stops - so a reset trip is genuinely un-started.
$body = '{"email":"test@mandela.ac.za","password":"test123"}'
$auth = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/student/login' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 30
$h = @{ Authorization = "Bearer $($auth.token)" }
$out = New-Object System.Collections.Generic.List[string]

# Every trip the student has a booking on; reset any that are finished.
$bookings = Invoke-RestMethod -Uri 'http://localhost:8080/api/trips/my-bookings' -Headers $h -TimeoutSec 60
foreach ($b in $bookings) {
    $id = $b.trip.tripId
    $st = $b.trip.status
    if ($st -match 'COMPLETED|CANCELLED|IN_PROGRESS') {
        try {
            $r = Invoke-RestMethod -Uri "http://localhost:8080/api/trips/$id/schedule" -Method Patch -Headers $h -TimeoutSec 60
            $out.Add("reset trip $id : $st -> $($r.status)  (lat=$($r.currentLat) arrival=$($r.arrivalTime))")
        } catch { $out.Add("reset trip $id FAILED: $($_.Exception.Message)") }
    } else {
        $out.Add("trip $id already $st - left alone")
    }
}

$out.Add('')
$out.Add('--- final demo state for test@mandela.ac.za ---')
$after = Invoke-RestMethod -Uri 'http://localhost:8080/api/trips/my-bookings' -Headers $h -TimeoutSec 60
foreach ($b in $after) {
    $stopCount = @($b.trip.stops).Count
    $out.Add(("trip {0,-4} {1,-10} {2,-28} -> {3,-22} stops={4}" -f $b.trip.tripId, $b.trip.status, $b.trip.departureStop, $b.trip.destinationStop, $stopCount))
}
$out | Set-Content (Join-Path $PSScriptRoot 'demoreset_out.txt')
