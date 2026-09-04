# Resets and starts a trip so there is a live one to track, and reports the student's ride list
# so we can confirm completed rides no longer appear as active.
$body = '{"email":"test@mandela.ac.za","password":"test123"}'
$auth = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/student/login' -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 30
$h = @{ Authorization = "Bearer $($auth.token)" }
$out = New-Object System.Collections.Generic.List[string]

$tripId = 563   # Newton Park -> Missionvale Campus, single leg, ~75s to complete
try {
    $null = Invoke-RestMethod -Uri "http://localhost:8080/api/trips/$tripId/schedule" -Method Patch -Headers $h -TimeoutSec 60
    $s = Invoke-RestMethod -Uri "http://localhost:8080/api/trips/$tripId/start" -Method Post -Headers $h -TimeoutSec 180
    $out.Add("STARTED $tripId : status=$($s.status) lat=$($s.currentLat) lng=$($s.currentLng)")
} catch { $out.Add("start $tripId failed: $($_.Exception.Message)") }

$out.Add('')
$out.Add('--- student booking list: trip status vs booking status ---')
$bookings = Invoke-RestMethod -Uri 'http://localhost:8080/api/trips/my-bookings' -Headers $h -TimeoutSec 60
foreach ($b in $bookings) {
    $out.Add(("booking {0,-3} trip {1,-4} tripStatus={2,-12} bookingStatus={3}" -f $b.bookingId, $b.trip.tripId, $b.trip.status, $b.bookingStatus))
}
$out | Set-Content (Join-Path $PSScriptRoot 'gostart_out.txt')
