package io.heckel.ntfy.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import io.heckel.ntfy.R

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<TextInputEditText>(R.id.login_username_input)
        val passwordInput = findViewById<TextInputEditText>(R.id.login_password_input)
        val loginButton = findViewById<Button>(R.id.btn_login)

        loginButton.setOnClickListener {
            val user = usernameInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                // 1. Guardamos las credenciales donde el AddFragment las va a buscar
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("global_username", user)
                    .putString("global_password", pass)
                    .apply()

                // 2. Pasamos a la pantalla principal de ntfy
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

                // 3. Cerramos esta pantalla para que no se pueda regresar con el botón "Atrás"
                finish()
            } else {
                Toast.makeText(this, "Por favor, ingresa usuario y contraseña", Toast.LENGTH_SHORT).show()
            }
        }
    }
}