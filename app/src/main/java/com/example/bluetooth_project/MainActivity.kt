package com.example.bluetooth_project

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var sendButton: Button

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothDevice: BluetoothDevice? = null
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private var selectedDevice: BluetoothDevice? = null



    val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    val readBuffer = msg.obj as ByteArray
                    val readMessage = String(readBuffer, 0, msg.arg1)
                    // ทำสิ่งที่คุณต้องการกับข้อมูลที่ได้รับ
                    processReceivedData(readMessage)
                }
            }
        }
    }

    private fun processReceivedData(data: String) {
        // ทำสิ่งที่คุณต้องการกับข้อมูลที่ได้รับ
        // ตัวอย่าง: แสดงข้อมูลใน TextView
        val textView: TextView = findViewById(R.id.textView)
        textView.text = data

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        sendButton = findViewById(R.id.sendButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // ปุ่ม Connect

        // ปุ่ม Send
        sendButton.setOnClickListener {
            sendData("Hello, Bluetooth!")
        }
        connectButton.setOnClickListener {
            // ตรวจสอบสิทธิ์ Bluetooth ก่อนที่จะใช้
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // คุณมีสิทธิ์ Bluetooth ให้ดำเนินการต่อ
                // แสดงรายการอุปกรณ์ Bluetooth ที่พร้อมให้เชื่อมต่อ
                val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                val deviceNames = mutableListOf<String>()
                val devices = mutableListOf<BluetoothDevice>()

                for (pairedDevice in pairedDevices) {
                    deviceNames.add(pairedDevice.name)
                    devices.add(pairedDevice)
                }

                val deviceNamesArray = deviceNames.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Choose a device")
                    .setItems(deviceNamesArray) { _, which ->
                        // ผู้ใช้เลือกอุปกรณ์ที่ต้องการ
                        val selectedDevice = devices[which]
                        connectToDevice(selectedDevice)
                    }
                    .show()
            } else {
                // หากไม่มีสิทธิ์ Bluetooth ให้ขอสิทธิ์จากผู้ใช้
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        // ตรวจสอบ Permission ก่อนเรียกใช้งาน Bluetooth
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                for (device in pairedDevices) {
                    if (device.name == "Emulator") {
                        bluetoothDevice = device
                        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        try {
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                            bluetoothSocket.connect()

                            inputStream = bluetoothSocket.inputStream
                            outputStream = bluetoothSocket.outputStream

                            // เริ่ม Thread สำหรับการรับข้อมูล
                            val thread = Thread {
                                val buffer = ByteArray(1024)
                                var bytes: Int

                                while (true) {
                                    try {
                                        bytes = inputStream.read(buffer)
                                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                                    } catch (e: IOException) {
                                        break
                                    }
                                }
                            }
                            thread.start()
                        } catch (e: IOException) {
                            // จัดการข้อผิดพลาดในการเชื่อมต่อ BluetoothSocket
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else {
            // ขออนุญาต Bluetooth จากผู้ใช้
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun sendData(message: String) {
        val msgBuffer = message.toByteArray()
        try {
            outputStream.write(msgBuffer)
        } catch (e: IOException) {
            // จัดการข้อผิดพลาดในการส่งข้อมูล
        }
    }

    companion object {
        const val MESSAGE_READ = 0
        const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    }
}