package com.game.puzzle2048.chalkboard

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.game.puzzle2048.R

class MainActivity : AppCompatActivity() {

    private lateinit var chalkboardView: ChalkboardView
    private lateinit var buttonClear: Button
    private lateinit var buttonRed: Button
    private lateinit var buttonWhite: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chalkboard)

        chalkboardView = findViewById(R.id.chalkboardView)
        buttonClear = findViewById(R.id.buttonClear)
        buttonRed = findViewById(R.id.buttonRed)
        buttonWhite = findViewById(R.id.buttonWhite)

        buttonClear.setOnClickListener {
            chalkboardView.clearChalkboard()
        }

        buttonRed.setOnClickListener {
            chalkboardView.setChalkColor(Color.RED)
        }

        buttonWhite.setOnClickListener {
            chalkboardView.setChalkColor(Color.WHITE)
        }
    }
}