package com.mechanicai.pro.data.remote.obd

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a Bluetooth connection to an ELM327 OBD-II adapter and executes basic commands.
 */
@Singleton
class BluetoothObdManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?
) {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Returns the list of paired Bluetooth devices. Requires BLUETOOTH_CONNECT permission on Android 12+.
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Connects to the given Bluetooth device and initializes the ELM327 adapter.
     */
    suspend fun connect(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            _connectionState.value = ConnectionState.Connecting
            disconnectInternal()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID).also {
                this@BluetoothObdManager.socket = it
            }
            socket.connect()
            outputStream = socket.outputStream
            inputStream = socket.inputStream

            // Initialize ELM327 adapter
            sendCommand("ATZ") // Reset
            delay(1000)
            sendCommand("ATE0") // Echo off
            sendCommand("ATL1") // Line feeds on
            sendCommand("ATS0") // Spaces off
            sendCommand("ATH0") // Headers off
            sendCommand("ATSP0") // Auto protocol

            _connectionState.value = ConnectionState.Connected(device.name ?: device.address)
        }.onFailure { error ->
            _connectionState.value = ConnectionState.Error(error.message ?: "Connection failed")
        }
    }

    /**
     * Disconnects from the adapter.
     */
    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            disconnectInternal()
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Reads the current Diagnostic Trouble Codes (DTCs).
     */
    suspend fun readTroubleCodes(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = sendCommand("03")
            parseDtcs(response)
        }
    }

    /**
     * Clears stored Diagnostic Trouble Codes.
     */
    suspend fun clearTroubleCodes(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            sendCommand("04")
            Unit
        }
    }

    /**
     * Reads a few common live data parameters.
     */
    suspend fun readLiveData(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val data = mutableMapOf<String, String>()

            val rpm = sendCommand("010C")
            parseRpm(rpm)?.let { data["Engine RPM"] = "$it rpm" }

            val speed = sendCommand("010D")
            parseSpeed(speed)?.let { data["Vehicle Speed"] = "$it km/h" }

            val coolant = sendCommand("0105")
            parseCoolant(coolant)?.let { data["Coolant Temp"] = "$it °C" }

            data
        }
    }

    private fun disconnectInternal() {
        try { inputStream?.close() } catch (_: IOException) {}
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    private fun sendCommand(command: String): String {
        val out = outputStream ?: throw IllegalStateException("Not connected")
        val input = inputStream ?: throw IllegalStateException("Not connected")

        out.write("$command\r".toByteArray())
        out.flush()

        val buffer = StringBuilder()
        val readBuffer = ByteArray(1024)
        var lastRead = 0
        // Wait for the prompt character '>'
        while (!buffer.contains('>')) {
            val available = input.available()
            if (available > 0) {
                val read = input.read(readBuffer, 0, minOf(available, readBuffer.size))
                if (read > 0) {
                    buffer.append(String(readBuffer, 0, read))
                    lastRead = 0
                }
            } else {
                if (lastRead++ > 50) break // timeout after ~500ms of no data
                Thread.sleep(10)
            }
        }
        return buffer.toString()
    }

    private fun parseDtcs(response: String): List<String> {
        val cleaned = response.replace(Regex("[\r\n> ]"), "")
        if (cleaned.length < 6 || cleaned.startsWith("NODATA")) return emptyList()

        val codes = mutableListOf<String>()
        // First byte after 43 is the number of DTCs * 2? Actually format is 43 XX YY ZZ ...
        // For simplicity, parse every 4-hex digit chunk after the mode byte 43.
        val dtcData = cleaned.substringAfter("43", cleaned)
        for (i in 0 until dtcData.length - 3 step 4) {
            val chunk = dtcData.substring(i, i + 4)
            val dtc = decodeDtc(chunk)
            if (dtc != null && dtc != "P0000") codes.add(dtc)
        }
        return codes
    }

    private fun decodeDtc(raw: String): String? {
        if (raw.length != 4) return null
        val firstChar = raw[0]
        val prefix = when (firstChar) {
            '0' -> "P0"
            '1' -> "P1"
            '2' -> "P2"
            '3' -> "P3"
            '4' -> "C0"
            '5' -> "C1"
            '6' -> "C2"
            '7' -> "C3"
            '8' -> "B0"
            '9' -> "B1"
            'A' -> "B2"
            'B' -> "B3"
            'C' -> "U0"
            'D' -> "U1"
            'E' -> "U2"
            'F' -> "U3"
            else -> return null
        }
        return "$prefix${raw.substring(1)}"
    }

    private fun parseRpm(response: String): Int? {
        val cleaned = response.replace(Regex("[\r\n> ]"), "")
        val bytes = cleaned.substringAfter("41", "").chunked(2).mapNotNull { it.toIntOrNull(16) }
        return if (bytes.size >= 2) (bytes[0] * 256 + bytes[1]) / 4 else null
    }

    private fun parseSpeed(response: String): Int? {
        val cleaned = response.replace(Regex("[\r\n> ]"), "")
        val bytes = cleaned.substringAfter("41", "").chunked(2).mapNotNull { it.toIntOrNull(16) }
        return bytes.getOrNull(1)
    }

    private fun parseCoolant(response: String): Int? {
        val cleaned = response.replace(Regex("[\r\n> ]"), "")
        val bytes = cleaned.substringAfter("41", "").chunked(2).mapNotNull { it.toIntOrNull(16) }
        return bytes.getOrNull(1)?.minus(40)
    }

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val deviceName: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}
