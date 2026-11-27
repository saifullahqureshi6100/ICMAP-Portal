package com.example.ui.utils

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

object MaleTutorTts {
    private val femaleTokens = listOf(
        "female", "woman", "girl", "lady", "uzma", "swara", "neerja", "heera",
        "kalpana", "veena", "lekha", "samantha", "victoria", "karen", "moira",
        "tessa", "zira", "hazel", "susan", "fiona", "serena", "jenny", "aria",
        "sonia", "libby", "mia", "aditi", "raveena", "kajal", "urf", "sfg",
        "tpf", "hia", "hie", "-f-", "_f_", "f00", "f01", "#female"
    )

    private val maleTokens = listOf(
        "male", "man", "asad", "salman", "bilal", "hamza", "usman", "tariq",
        "david", "mark", "george", "daniel", "ryan", "guy", "rishi", "prabhat",
        "madhur", "matthew", "joey", "arthur", "oliver", "urm", "iom", "tpd",
        "hid", "rjs", "gbd", "-m-", "_m_", "m00", "m01", "#male"
    )

    private fun isFemaleVoice(voice: Voice): Boolean {
        val signature = "${voice.name} ${voice.locale}".lowercase(Locale.ROOT)
        return femaleTokens.any { token -> signature.contains(token) }
    }

    private fun isMaleVoice(voice: Voice): Boolean {
        val signature = "${voice.name} ${voice.locale}".lowercase(Locale.ROOT)
        return maleTokens.any { token -> signature.contains(token) }
    }

    fun configureMaleVoice(
        engine: TextToSpeech?,
        preferUrdu: Boolean,
        speechSpeed: Float = 0.94f,
        pitchScale: Float = 0.85f,
        onDone: (() -> Unit)? = null
    ) {
        if (engine == null) return

        val urPk = Locale("ur", "PK")
        val targetLocale = if (preferUrdu) urPk else Locale.US
        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = if (preferUrdu) Locale("hi", "IN") else Locale.US
        }

        var selectedVerifiedMale = false
        try {
            val allVoices = engine.voices.orEmpty()
            // Remove every female voice from consideration
            val nonFemaleVoices = allVoices.filterNot { isFemaleVoice(it) }
            val candidates = nonFemaleVoices
                .filter { v ->
                    val lang = v.locale.language.lowercase(Locale.ROOT)
                    if (preferUrdu) {
                        lang == "ur" || lang == "hi" || lang == "en"
                    } else {
                        lang == "en"
                    }
                }
                .ifEmpty { nonFemaleVoices }

            val chosen = candidates.maxByOrNull { v ->
                var score = 0
                if (isMaleVoice(v)) score += 200
                val lang = v.locale.language.lowercase(Locale.ROOT)
                val country = v.locale.country.uppercase(Locale.ROOT)
                if (preferUrdu) {
                    when {
                        lang == "ur" && country == "PK" -> score += 100
                        lang == "ur" -> score += 90
                        lang == "hi" -> score += 65
                        lang == "en" && (country == "PK" || country == "IN") -> score += 50
                        lang == "en" -> score += 30
                    }
                } else {
                    when {
                        lang == "en" && (country == "GB" || country == "US") -> score += 80
                        lang == "en" -> score += 60
                    }
                }
                if (!v.isNetworkConnectionRequired) score += 15
                if (v.quality >= Voice.QUALITY_HIGH) score += 10
                score
            }

            if (chosen != null) {
                engine.voice = chosen
                selectedVerifiedMale = isMaleVoice(chosen)
            }
        } catch (_: Exception) {
            // Some OEM TTS engines do not expose voices list; pitch down below
        }

        // Enforce male mentor register: natural deep voice for verified male voices, lowered pitch otherwise
        val basePitch = pitchScale.coerceIn(0.65f, 0.95f)
        engine.setPitch(if (selectedVerifiedMale) basePitch else (basePitch * 0.82f).coerceAtLeast(0.65f))
        engine.setSpeechRate(speechSpeed.coerceIn(0.6f, 1.5f))

        if (onDone != null) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    onDone()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onDone()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onDone()
                }
            })
        }
    }
}
