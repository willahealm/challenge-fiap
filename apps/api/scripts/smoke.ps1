param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
function Invoke-DemoApi([string]$Method, [string]$Path, [string]$Token = '', $Body = $null) {
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; ContentType = 'application/json; charset=utf-8' }
    if ($Token) { $params.Headers = @{ Authorization = "Bearer $Token" } }
    if ($null -ne $Body) { $params.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10)) }
    Invoke-RestMethod @params
}
function Login-Demo([string]$Email, [string]$Password) {
    (Invoke-DemoApi POST '/v1/auth/demo' '' @{ email = $Email; password = $Password }).accessToken
}
function Assert-Equal($Actual, $Expected, [string]$Label) {
    if ($Actual -ne $Expected) { throw "$Label expected '$Expected', got '$Actual'" }
}
Assert-Equal (Invoke-DemoApi GET '/actuator/health').status 'UP' 'health'
$operatorToken = Login-Demo 'operador@demo.local' 'Operador123!'
$null = Invoke-DemoApi POST '/v1/demo/reset' $operatorToken
$operatorToken = Login-Demo 'operador@demo.local' 'Operador123!'
$passengerToken = Login-Demo 'lucas@demo.local' 'Demo123!'
$deviceToken = Login-Demo 'totem@demo.local' 'Totem123!'
try {
    Assert-Equal (Invoke-DemoApi GET '/v1/me/trips/next' $passengerToken).id 'trip-demo' 'next trip'
    $view = Invoke-DemoApi PATCH '/v1/journeys/journey-demo/checklist' $passengerToken @{ checklist = @{ document = $true } }
    Assert-Equal $view.journey.checklist.document $true 'checklist'
    $handoff = Invoke-DemoApi POST '/v1/journeys/journey-demo/handoffs' $passengerToken
    $session = Invoke-DemoApi POST '/v1/totems/totem-tiete-01/handoffs/consume' $deviceToken @{ token = $handoff.code }
    Assert-Equal $session.journey.currentPointId 'point-totem-01' 'checkpoint'
    $help = Invoke-DemoApi POST '/v1/totems/totem-tiete-01/help-requests' $session.accessToken @{ category = 'MOBILITY' }
    Assert-Equal $help.tripId 'trip-demo' 'contextual help'
    $null = Invoke-DemoApi POST '/v1/ops/trips/trip-demo/alerts' $operatorToken @{ type = 'PLATFORM_CHANGE'; severity = 'CRITICAL'; message = 'Embarque na plataforma 21.'; platform = '21' }
    $view = Invoke-DemoApi GET '/v1/trips/trip-demo/journey' $passengerToken
    Assert-Equal $view.trip.platform '21' 'platform'
    Assert-Equal $view.route.toPointId 'point-platform-21' 'route'
    $view = Invoke-DemoApi GET '/v1/trips/trip-demo/journey' $session.accessToken
    Assert-Equal $view.trip.platform '21' 'kiosk synchronization'
    $null = Invoke-DemoApi PATCH "/v1/ops/help-requests/$($help.id)" $operatorToken @{ status = 'RESOLVED' }
    $null = Invoke-DemoApi POST '/v1/journeys/journey-demo/complete' $passengerToken
    $feedback = Invoke-DemoApi POST '/v1/feedback' $passengerToken @{ journeyId = 'journey-demo'; rating = 5; tags = @('orientation'); comment = 'Smoke test' }
    Assert-Equal $feedback.rating 5 'feedback'
    Write-Output 'PASS: health, auth, checklist, short-code handoff, checkpoint, help, platform, kiosk synchronization, completion, feedback.'
} finally {
    $null = Invoke-DemoApi POST '/v1/demo/reset' $operatorToken
    Write-Output 'Demo reset complete; all test sessions revoked.'
}
