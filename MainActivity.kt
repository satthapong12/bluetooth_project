package com.example.bluetooth_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var connectButton: Button
    private lateinit var sendButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var selectedDevice: BluetoothDevice

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private lateinit var selectImageButton: Button
    private val PICK_IMAGE_REQUEST = 1

    val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val readBuffer = msg.obj as ByteArray
                val readMessage = String(readBuffer, 0, msg.arg1)
                processReceivedData(readMessage)
            }
        }
        true
    }

    private fun processReceivedData(data: String) {
        val textView: TextView = findViewById(R.id.receivecd)
        textView.text = data
    }



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        sendButton = findViewById(R.id.sendButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        disconnectButton.visibility = View.GONE
        selectImageButton = findViewById(R.id.selectImageButton)
        selectImageButton.setOnClickListener {
            openImageChooser()
        }
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val uuidTextView: TextView = findViewById(R.id.uuidTextView)

        // แสดงค่า UUID ใน TextView
        uuidTextView.text = "UUID: $uuid"

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        sendButton.setOnClickListener {
            if (::bluetoothSocket.isInitialized) {
                try {
                    sendData("Hello Bluetooth!")

                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error sending data: ${e.message}")
                    Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Not connected to Bluetooth", Toast.LENGTH_SHORT).show()

            }
        }

        connectButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                showBluetoothDevices()
            } else {
                requestBluetoothPermissions()
            }
        }

        disconnectButton.setOnClickListener {
            disconnectBluetooth()
        }
    }

    private fun showBluetoothDevices() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
                    val selectedDevice = devices[which]
                    connectToDevice(selectedDevice)
                }
                .show()
        } else {
            requestBluetoothPermissions()
        }
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            ),
            BLUETOOTH_PERMISSION_REQUEST_CODE
        )
    }
    private fun connectToDevice(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket.connect()

                inputStream = bluetoothSocket.inputStream
                outputStream = bluetoothSocket.outputStream

                // Start a thread for receiving data
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

                onBluetoothConnected()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            requestBluetoothPermissions()
        }
    }
    private fun sendData(message: String) {
        // ตรวจสอบว่า bluetoothSocket ถูกสร้างและเชื่อมต่อแล้วหรือไม่
        if (!::bluetoothSocket.isInitialized || !bluetoothSocket.isConnected) {
            // หากยังไม่ได้เชื่อมต่อ หรือ bluetoothSocket ยังไม่ถูกสร้าง ให้เรียก connectToDevice และคืนค่า
            connectToDevice(selectedDevice)
            return
        }

        // ต่อไปทำงานเมื่อ bluetoothSocket เชื่อมต่อแล้ว
        val msgBuffer = message.toByteArray()
        try {
            Log.d("Bluetooth", "Attempting to send data: $message")

            // ตรวจสอบว่าการเชื่อมต่อยังเปิดอยู่หรือไม่
            if (!bluetoothSocket.isConnected) {
                Log.e("Bluetooth", "Bluetooth is not connected")
                return
            }

            // ส่งข้อมูล
            outputStream.write(msgBuffer)

            // แสดงข้อความแจ้งความสำเร็จ
            Toast.makeText(this, "Data sent successfully", Toast.LENGTH_SHORT).show()
            Log.e("BluetoothSuccessfu", "sending data: $message")

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("BluetoothError", "Error sending data: ${e.message}")
            Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnectBluetooth() {
        try {
            bluetoothSocket.use {
                it.close()
                disconnectButton.visibility = View.GONE
                Toast.makeText(this, "Bluetooth Disconnected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }



    private fun onBluetoothConnected() {
        disconnectButton.visibility = View.VISIBLE
        Toast.makeText(this, "Bluetooth Connected", Toast.LENGTH_SHORT).show()
    }

    private fun sendImage(bitmap: Bitmap) {
        try {
            if (bluetoothSocket.isConnected) {
                outputStream.use { output ->
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val byteArray = byteArrayOutputStream.toByteArray()

                    output.write(byteArray)
                    Log.d("Bluetooth", "Image sent successfully")
                }
            } else {
                Log.e("Bluetooth", "Bluetooth is not connected")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Error sending image: ${e.message}")
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            sendImage(bitmap)
        }
    }

    companion object {
        const val MESSAGE_READ = 0
        const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    }
}
