package com.example.wordlelike

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var texts: MutableList<MutableList<TextView>>
    private val rowCount = 7
    private val colCount = 5
    private var countGames = 0
    private var countWins = 0
    private lateinit var gameCore: GameCore
    private lateinit var btnEnter: Button
    private lateinit var btnErase: Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameCore = GameCore(rowCount)
        initTexts()
        setEventListeners()

        newRound()
    }

    private fun setEventListeners() {
        for (c in 90 downTo 65) {
            val resID = resources.getIdentifier("button${c.toChar()}", "id", packageName)
            val btn = findViewById<Button>(resID)
            btn.setOnClickListener {
                if (gameCore.isPouse()) {
                    gameCore.startOver()
                    newRound()
                }
                val row = gameCore.getCurRow()
                val col = gameCore.getCurCol()
                if (gameCore.setNextChar(c.toChar())) {
                    texts[row][col].text = c.toChar().toString()
                }
            }
        }

        btnEnter = findViewById<Button>(R.id.buttonEnter)
        btnEnter.setOnClickListener {
            if (gameCore.isPouse()) {
                gameCore.startOver()
                newRound()
            }
            val row = gameCore.getCurRow()
            if (gameCore.enter()) {
                for (col in 0 until colCount) {
                    val id = when (gameCore.validateChar(row, col)) {
                        gameCore.IN_WORD -> {
                            R.drawable.letter_in_word
                        }

                        gameCore.IN_PLACE -> {
                            R.drawable.letter_in_place
                        }

                        else -> {
                            R.drawable.letter_not_in
                        }
                    }

                    texts[row][col].background = ContextCompat.getDrawable(this, id)
                }
                if (gameCore.getResult()) {
                    countWins++
                    Toast.makeText(this, "Congratulations! You won!", Toast.LENGTH_SHORT).show()
                    if (gameCore.isPouse()) {
                        gameCore.startOver()
                        newRound()
                    }
                } else {
                    Toast.makeText(this, "You Lost!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnErase = findViewById<Button>(R.id.buttonErase)
        btnErase.setOnClickListener {
            if (gameCore.isPouse()) {
                gameCore.startOver()
                newRound()
            }
            gameCore.erase()
            val row = gameCore.getCurRow()
            val col = gameCore.getCurCol()
            texts[row][col].text = " "
        }
    }

    private fun initTexts() {
        texts = MutableList(rowCount) { mutableListOf() }
        for (row in 0 until rowCount) {
            for (col in 0 until colCount) {
                val resID =
                    resources.getIdentifier("text${col + 1}col${row + 1}row", "id", packageName)
                texts[row].add(findViewById(resID))
            }
        }
    }

    private fun newRound() {
        fetchRandomWordFromFirebase { randomWord ->
            gameCore.startOver()
            gameCore.setWord(randomWord)

            for (row in 0 until rowCount) {
                for (col in 0 until colCount) {
                    texts[row][col].background = ContextCompat.getDrawable(this, R.drawable.letter_border)
                    texts[row][col].text = " "
                }
            }
            val textGames = findViewById<TextView>(R.id.games)
            val textWins = findViewById<TextView>(R.id.wins)
            textGames.text = "Games: $countGames"
            textWins.text = "Wins: $countWins"
            countGames++

            Log.e("Word", "Random Word: $randomWord")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val charTyped = event.unicodeChar.toChar()

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                btnEnter.performClick()
                Toast.makeText(this, "pressed enter", Toast.LENGTH_SHORT).show()
            } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                btnErase.performClick()
            } else if (charTyped.isLetter()) {
                handleKeyPress(charTyped)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleKeyPress(charTyped: Char) {
        val row = gameCore.getCurRow()
        val col = gameCore.getCurCol()
        if (gameCore.setNextChar(charTyped.uppercaseChar())) {
            texts[row][col].text = charTyped.uppercaseChar().toString()
        }
    }

    private fun fetchRandomWordFromFirebase(onResult: (String) -> Unit) {
        val database = Firebase.database
        val wordsRef = database.getReference("words")

        wordsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wordList = mutableListOf<String>()

                for (childSnapshot in snapshot.children) {
                    val word = childSnapshot.getValue(String::class.java)
                    word?.let { wordList.add(it) }
                }

                val randomWord = if (wordList.isNotEmpty()) {
                    wordList.random()
                } else {
                    "APPLE" // fallback
                }

                onResult(randomWord)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching words: ${error.message}")
                onResult("APPLE") // fallback
            }
        })
    }
}