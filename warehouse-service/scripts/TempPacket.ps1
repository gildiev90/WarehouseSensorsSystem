$udp = [System.Net.Sockets.UdpClient]::new(
    [System.Net.Sockets.AddressFamily]::InterNetwork
)

$endpoint = [System.Net.IPEndPoint]::new(
    [System.Net.IPAddress]::Loopback, 3344
)

try {
    while ($true) {
        $id = Get-Random -Minimum 1 -Maximum 11
        $value = Get-Random -Minimum 20 -Maximum 41

        $message = "sensor_id=t$id; value=$value"
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($message)

        [void]$udp.Send($bytes, $bytes.Length, $endpoint)

        Write-Host $message

        Start-Sleep -Seconds 1
    }
}
finally {
    $udp.Dispose()
}