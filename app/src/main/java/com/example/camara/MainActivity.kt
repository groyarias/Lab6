package com.example.camara

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_llamada.*
import kotlinx.android.synthetic.main.dialog_mensaje.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var map:GoogleMap
    //Audio
    private var isPlaying = false
    private var mediaPlayer:MediaPlayer? = null

    //GPS
    private var mostrarMapa:Boolean = false

    //Sensores
    //Acelerometro
    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null
    private var currentX:Float = 0.0f
    private var currentY:Float = 0.0f
    private var currentZ:Float = 0.0f
    private var lastX:Float = 0.0f
    private var lastY:Float = 0.0f
    private var lastZ:Float = 0.0f
    private var differenceX:Float = 0.0f
    private var differenceY:Float = 0.0f
    private var differenceZ:Float = 0.0f
    private var itIsNotFirstTime:Boolean = false
    private val shakeThreshold:Float = 10f
    private var vibrator: Vibrator? = null
    //Luz
    private var brigthness:Sensor? = null



    companion object{

        //Camara
        private const val CAMERA_PERMISSION_CODE = 1 //Para el permiso
        private const val CAMERA_REQUEST_CODE = 2 //Para el intent

        //SMS-Llamada
        private const val NUMERO_TELEFONICO = 88163040
        private const val SMS_PERMISSION_CODE = 3
        private const val CALL_PERMISSION_CODE = 4

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        handleCamera()
        handleMensaje()
        handleLlamada()
        handleAudio()
        handleMapa()
        handleSensorAcelerometro()
        handleSensorLuz()

    }

    private fun handleCamera(){
        btnCamera.setOnClickListener {
            //Verificar si se cuenta con persmisos
            if(ContextCompat.checkSelfPermission(
                    this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }else{
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            }
        }
    }

    private fun handleMensaje(){
        fabMensaje.setOnClickListener {
            if(ContextCompat.checkSelfPermission(
                    this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
                dialogMensaje()
            }else{
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.SEND_SMS),SMS_PERMISSION_CODE)
            }
        }
    }

    private fun handleLlamada(){
        fabLlamar.setOnClickListener {
            if(ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
                dialogLlamada()
            }else{
                ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CALL_PHONE), CALL_PERMISSION_CODE)
            }
        }
    }

    private fun handleAudio(){
        mediaPlayer = MediaPlayer.create(this,R.raw.instructivo)

        fabInstructivo.setOnClickListener {
            togglePlaying()
            if (isPlaying){
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener {
                    //isPlaying = false
                    togglePlaying()
                    Toast.makeText(this,"Instructivo finalizado",Toast.LENGTH_SHORT).show()
                }
            }else{
                mediaPlayer?.pause()
            }
        }

        fabInstructivo.setOnLongClickListener(object: View.OnLongClickListener{
            override fun onLongClick(v: View?): Boolean {
                mediaPlayer?.pause()
                mediaPlayer?.seekTo(0)
                if(isPlaying) togglePlaying()
                Toast.makeText(v?.context,"Instructivo reiniciado",Toast.LENGTH_SHORT).show()
                return true
            }
        })

    }

    private fun handleMapa(){
        createFragment()
        btnUbicacion.setOnClickListener(View.OnClickListener {
            mostrarOcultarMapa()
        })
        mostrarOcultarMapa()
    }

    private fun handleSensorAcelerometro(){
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun handleSensorLuz() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        brigthness = sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)

    }

    fun dialogMensaje(){
        val dialogMensaje = Dialog(this, R.style.Theme_Dialog)
        dialogMensaje.setCancelable(true)
        dialogMensaje.setContentView(R.layout.dialog_mensaje)

        dialogMensaje.textInputEditTextNumber.setText(NUMERO_TELEFONICO.toString())

        //setOnClickListener Enviar
        dialogMensaje.btnEnviarMensaje.setOnClickListener {
            var numTelefonico = dialogMensaje.textInputEditTextNumber.text.toString()
            var mensaje = dialogMensaje.textInputEditTextMensaje.text.toString()

            if(numTelefonico.isNotEmpty() && mensaje.isNotEmpty()){
                try{
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(numTelefonico,null,mensaje,null,null)
                    dialogMensaje.dismiss()
                    Toast.makeText(this, "Mensaje enviado exitosamente",Toast.LENGTH_SHORT).show()
                }catch (e:Exception){
                    Toast.makeText(this, "Fallo al enviar el mensaje",Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Permisos para la camara fueron denegados",Toast.LENGTH_LONG).show()
            }
        }
        dialogMensaje.show()
    }

    fun dialogLlamada(){
        val dialogLlamada = Dialog(this, R.style.Theme_Dialog)
        dialogLlamada.setCancelable(true)
        dialogLlamada.setContentView(R.layout.dialog_llamada)

        dialogLlamada.tietNumber.setText(NUMERO_TELEFONICO.toString())
        dialogLlamada.btnLlamar.setOnClickListener {
            val numeroTelefonico = dialogLlamada.tietNumber.text.toString()
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:" + numeroTelefonico)
            dialogLlamada.dismiss()
            startActivity(intent)
        }
        dialogLlamada.show()
    }

    private fun togglePlaying(){
        isPlaying = !isPlaying
        if(isPlaying){
            fabInstructivo.setImageResource(R.drawable.ic_pause)
        }else{
            fabInstructivo.setImageResource(R.drawable.ic_play)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }else{
                Toast.makeText(this, "Permisos para la camara fueron denegados",Toast.LENGTH_LONG).show()
            }
        }else if(requestCode == SMS_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                dialogMensaje()
            }else{
                Toast.makeText(this, "Permisos para enviar mensajes fueron denegados",Toast.LENGTH_LONG).show()
            }
        }
        else if(requestCode == CALL_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                dialogLlamada()
            }else{
                Toast.makeText(this, "Permisos para realizar llamadas fueron denegados",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK){
            if(requestCode == CAMERA_REQUEST_CODE){
                val thumbNail:Bitmap = data!!.extras!!.get("data") as Bitmap
                ivFoto.setImageBitmap(thumbNail)
            }
        }
    }

    private fun createFragment() {
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        createMarker()
    }

    private fun createMarker() {
        val coordinates = LatLng(9.981157, -84.159648)
        val marker = MarkerOptions().position(coordinates).title("Place To Work")
        map.addMarker(marker)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(coordinates, 15f),4000,null
        )
    }

    fun mostrarOcultarMapa(){
        if(mostrarMapa){
            val fm: FragmentManager = supportFragmentManager
            fm.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .show(fm.findFragmentById(R.id.map)!!)
                .commit()
        }else{
            val fm: FragmentManager = supportFragmentManager
            fm.beginTransaction()
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .hide(fm.findFragmentById(R.id.map)!!)
                .commit()
        }
        mostrarMapa = !mostrarMapa
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_LIGHT){
            val light = event.values[0]
            if(light.toInt() < 5){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }else if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            currentX = event?.values!![0]
            currentY = event?.values!![1]
            currentZ = event?.values!![2]

            if(itIsNotFirstTime){
                differenceX = Math.abs(lastX - currentX)
                differenceY = Math.abs(lastY - currentY)
                differenceZ = Math.abs(lastZ - currentZ)

                if((differenceX > shakeThreshold && differenceY > shakeThreshold) ||
                    (differenceX > shakeThreshold && differenceZ > shakeThreshold) ||
                    (differenceY > shakeThreshold && differenceZ > shakeThreshold)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        limpiarCampos()
                        Toast.makeText(this,"Campos borrados",Toast.LENGTH_SHORT).show()
                        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    }else{
                        limpiarCampos()
                        Toast.makeText(this,"Campos borrados",Toast.LENGTH_SHORT).show()
                        vibrator?.vibrate(500)
                    }
                }
            }

            lastX = currentX
            lastY = currentZ
            lastZ = currentZ
            itIsNotFirstTime = true
        }
    }

    private fun limpiarCampos(){
        tietNombre.text?.clear()
        tietTelefono.text?.clear()
        tietDireccion.text?.clear()
        tietCiudad.text?.clear()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this,accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager?.registerListener(this,brigthness,SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

}
