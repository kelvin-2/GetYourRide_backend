# Probes the endpoints the user says broke (carpool search, shuttle booking) against the
# local backend, to find whether the fault is backend, data, or the app's base URL.
$out = New-Object System.Collections.Generic.List[string]

function Probe($name, $method, $url, $headers, $body) {
    try {
        if ($body) {
            $r = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -ContentType 'application/json' -Body $body -UseBasicParsing -TimeoutSec 40 -ErrorAction Stop
        } else {
            $r = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -UseBasicParsing -TimeoutSec 40 -ErrorAction Stop
        }
        $len = $r.Content.Length
        $preview = $r.Content.Substring(0, [Math]::Min(220, $len)) -replace '\s+', ' '
        $out.Add("[$name] HTTP $($r.StatusCode) len=$len")
        $out.Add("    $preview")
    } catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'NO-RESPONSE' }
        $detail = ''
        try { $detail = ($_.ErrorDetails.Message -replace '\s+', ' ') } catch {}
        $out.Add("[$name] FAILED $code :: $detail")
        if (-not $detail) { $out.Add("    $($_.Exception.Message)") }
    }
}

# --- is the backend even up? ---
try {
    $null = Invoke-WebRequest -Uri 'http://localhost:8080/api/trips' -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
    $out.Add('backend: UP (unauthenticated call reached it)')
} catch {
    $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'DOWN' }
    $out.Add("backend: reachable=$code (403 means up + auth enforced)")
}

# --- log in as the student ---
$h = $null
try {
    $auth = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/student/login' -Method Post -ContentType 'application/json' -Body '{"email":"test@mandela.ac.za","password":"test123"}' -TimeoutSec 30
    $h = @{ Authorization = "Bearer $($auth.token)" }
    $out.Add("login: OK (studentId=$($auth.id), isFunded=$($auth.isFunded))")
} catch {
    $out.Add("login: FAILED $($_.Exception.Message)")
    $out | Set-Content (Join-Path $PSScriptRoot 'diagnose_out.txt'); exit
}

$out.Add('')
$out.Add('===== CARPOOL SEARCH =====')
Probe 'search by coordinates (South Campus -> Missionvale)' 'GET' 'http://localhost:8080/api/trips/search?depLat=-34.00809&depLng=25.67319&destLat=-33.87253&destLng=25.55223' $h $null
Probe 'search by name' 'GET' 'http://localhost:8080/api/trips/search?departure=South%20Campus&destination=Missionvale%20Campus' $h $null
Probe 'trips by status SCHEDULED' 'GET' 'http://localhost:8080/api/trips/status/SCHEDULED' $h $null
Probe 'geocode suggestions (search depends on this)' 'GET' 'http://localhost:8080/api/geocode/suggestions?query=South%20Campus' $h $null

$out.Add('')
$out.Add('===== SHUTTLE =====')
Probe 'shuttle stops' 'GET' 'http://localhost:8080/api/shuttle-stops' $h $null
Probe 'shuttle time slots' 'GET' 'http://localhost:8080/api/shuttle-stops/time-slots' $h $null

$out | Set-Content (Join-Path $PSScriptRoot 'diagnose_out.txt')
