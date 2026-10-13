param(
    [int]$Port = 3344,
    [int]$SensorCount = 100,
    [string]$Prefix = "t",
    [int]$MinValue = 20,
    [int]$MaxValue = 40,
    [int]$IntervalMs = 5000
)

$udp = [System.Net.Sockets.UdpClient]::new(
    [System.Net.Sockets.AddressFamily]::InterNetwork
)

$endpoint = [System.Net.IPEndPoint]::new(
    [System.Net.IPAddress]::Loopback, $Port
)

# Spread sensor messages over the update interval
$delayMs = [Math]::Max(
    1,
    [int][Math]::Floor($IntervalMs / $SensorCount)
)

Write-Host "Simulating $SensorCount sensors on port $Port"
Write-Host "Interval: $IntervalMs ms per sensor"

try {
    while ($true) {
        for ($i = 1; $i -le $SensorCount; $i++) {

            $value = Get-Random `
                -Minimum $MinValue `
                -Maximum ($MaxValue + 1)

            $message = "sensor_id=${Prefix}${i}; value=$value"

            $bytes = [System.Text.Encoding]::UTF8.GetBytes(
                $message
            )

            [void]$udp.Send(
                $bytes,
                $bytes.Length,
                $endpoint
            )

            Write-Host $message

            Start-Sleep -Milliseconds $delayMs
        }
    }
}
finally {
    $udp.Dispose()
}

# Run temp
#
#  .\sens-simulator.ps1 -Port 3344 -SensorCount 100 -Prefix t -MinValue 20 -MaxValue 40 -IntervalMs 5000
# .\sens-simulator.ps1 -Port 3355 -SensorCount 200 -Prefix h -MinValue 30 -MaxValue 80 -IntervalMs 5000