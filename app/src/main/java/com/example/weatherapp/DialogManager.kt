package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context

object DialogManager {
    fun locationDialog(context: Context, listener: Listener){
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Error")
        dialog.setMessage("Location is disabled. Do you want enable it?")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes"){_,_, ->
            listener.onClick()
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No"){_,_, ->
            dialog.dismiss()
        }
        dialog.show()

    }
    interface Listener {
        fun onClick()
    }
}