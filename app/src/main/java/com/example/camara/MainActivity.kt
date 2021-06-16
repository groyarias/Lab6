package com.example.camara

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_llamada.*
import kotlinx.android.synthetic.main.dialog_mensaje.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    companion object{

        private val NUMERO_TELEFONICO = 88163040
        private var isPlaying = false
        private var mediaPlayer:MediaPlayer? = null

        private const val CAMERA_PERMISSION_CODE = 1 //Para el permiso
        private const val CAMERA_REQUEST_CODE = 2 //Para el intent

        private const val SMS_PERMISSION_CODE = 3

        private const val CALL_PERMISSION_CODE = 4

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleCamera()
        handleMensaje()
        handleLlamada()
        handleAudio()


    }

    fun handleCamera(){
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

    fun handleMensaje(){
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

    fun handleLlamada(){
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

    fun handleAudio(){
        mediaPlayer = MediaPlayer.create(this,R.raw.southofheaven)

        fabInstructivo.setOnClickListener {
            togglePlaying()
            if (isPlaying){
                mediaPlayer?.start()
                mediaPlayer?.setOnCompletionListener {
                    /*isPlaying = false
                    togglePlaying()*/
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
}