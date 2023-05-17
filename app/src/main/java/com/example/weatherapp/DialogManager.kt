package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color.BLACK


object DialogManager {
    //Alert dialog для вимкненого місця розташування
    fun locationDialog(context: Context, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Error")
        dialog.setMessage("Location is disabled. Do you want enable it?")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes") { _, _ ->
            listener.onClick(null)
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No") { _, _ ->
            dialog.dismiss()
        }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(BLACK)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BLACK)
    }

    //Alert dialog для некоректної назви міста (Volley error)
    fun incorrectCityName(context: Context) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Error!")
        dialog.setMessage("Incorrect city name. Try again...")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            dialog.dismiss()
        }

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BLACK)
    }

    //Alert dialog для відсутнього інтернет з'єднання (Volley error)
    fun noConnection(context: Context) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Error!")
        dialog.setMessage("Can't connect to the network. Try again...")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { _, _ ->
            dialog.dismiss()
        }

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BLACK)
    }

    interface Listener {
        fun onClick(name: String?)
    }
}