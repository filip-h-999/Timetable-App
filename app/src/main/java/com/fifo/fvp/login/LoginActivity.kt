package com.fifo.fvp.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fifo.fvp.MainActivity
import com.fifo.fvp.R
import de.sematre.dsbmobile.DSBMobile


class LoginActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.kennwort)
        val password = findViewById<EditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val loading = findViewById<ProgressBar>(R.id.loading)
        val alreadyLoggedIn = getSharedPreferences("preferences", Context.MODE_PRIVATE)
            .getBoolean("already_logged_in", false)

        if (alreadyLoggedIn) {
            runMainActivity()
            finishActivity(0)
        }

        login.setOnClickListener {
            if (username.text.toString().equals("") || password.text.toString().equals("")) {
                Toast.makeText(this, "Username or Password blank", Toast.LENGTH_LONG).show()
            } else {
                loading.visibility =ProgressBar.VISIBLE
                login.isEnabled = false
                StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
                val dsbMobileData =
                    DSBMobile(username.text.toString(), password.text.toString()).pullData()

                if (dsbMobileData["Resultcode"].asInt == 0) {//ok
                    val editor = getSharedPreferences("preferences", Context.MODE_PRIVATE).edit()
                    editor.putString("usr", username.text.toString())
                    editor.putString("pwd", password.text.toString())
                    editor.putBoolean("already_logged_in", true)
                    editor.apply()
                    runMainActivity()
                    finishActivity(0)
                } else {
                    loading.visibility = ProgressBar.INVISIBLE
                    login.isEnabled = true
                    Toast.makeText(this, "Username or Password Do not Match", Toast.LENGTH_LONG)
                        .show()
                }

            }

        }
    }

    fun runMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        //intent.putExtra("name", username.toString())
        startActivity(intent)
    }
}
