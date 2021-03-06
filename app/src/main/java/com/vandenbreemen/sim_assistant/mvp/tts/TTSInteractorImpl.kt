package com.vandenbreemen.sim_assistant.mvp.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.vandenbreemen.sim_assistant.api.sim.Sim
import com.vandenbreemen.sim_assistant.api.sim.SimParser
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.schedulers.Schedulers.computation
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

//  How to write to file
//  https://stackoverflow.com/a/4976327/2328196

class TTSInteractorImpl(context: Context) : TTSInteractor {


    companion object {
        const val TAG = "TTSInteractor"
        const val UTTERANCE_ID = "SimUtterance"
    }

    lateinit var tts:TextToSpeech

    var stringsToSpeak: List<String>? = null

    val indexOfCurrentStringBeingSpoken = AtomicInteger(-1)

    val currentlySpeaking = AtomicBoolean(false)

    val shouldExitNow = AtomicBoolean(false)

    val isInTheProcessOfSpeakingSims = AtomicBoolean(false)

    val paused = AtomicBoolean(false)

    init {
        tts = TextToSpeech(context, TextToSpeech.OnInitListener { status->
            if(status != ERROR){
                tts.language = Locale.US
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        currentlySpeaking.set(false)
                    }

                    override fun onDone(utteranceId: String?) {
                        currentlySpeaking.set(false)
                        Log.d(TAG, "Done Speaking $utteranceId")
                    }

                    override fun onError(utteranceId: String?) {

                    }

                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "Start speaking $utteranceId")
                    }

                })
            }
        })
    }

    private fun hasMoreStrings() = indexOfCurrentStringBeingSpoken.get() < (stringsToSpeak?.size
            ?: -1) - 1

    private fun waitForTTSCompletion(){
        while (!shouldExitNow.get() && (currentlySpeaking.get() || paused.get())) {
            sleep(20)
        }
    }

    override fun speakSims(sims: List<Sim>): Pair<SimDictationDetails, Observable<Int>> {

        val utterances = mutableListOf<String>()
        var index = 0
        val simToStartIndex = mutableMapOf<Sim, Int>()
        sims.forEach {
            simToStartIndex.put(it, index)

            val listOfUtterancesForSim = SimParser(it).toUtterances()
            utterances.addAll(listOfUtterancesForSim)

            index += listOfUtterancesForSim.size

        }
        stringsToSpeak = listOf(*utterances.toTypedArray())

        val observable = Observable.create(ObservableOnSubscribe<Int> { emitter ->

            isInTheProcessOfSpeakingSims.set(true)

            while (hasMoreStrings()) {
                waitForTTSCompletion()
                if(shouldExitNow.get()){
                    Log.d(TAG, "Exiting TTS immediately")
                    return@ObservableOnSubscribe
                }
                val nextIndex = indexOfCurrentStringBeingSpoken.incrementAndGet()
                emitter.onNext(nextIndex)
                Log.d(TAG, "Speaking\n${utterances[nextIndex]}")
                tts.speak(utterances[nextIndex], QUEUE_FLUSH, null, UTTERANCE_ID)
                currentlySpeaking.set(true) //  Set this here instead of in listener so that the driver thread (this one)
                //  knows immediately that speaking is supposed to be taking place
            }

            waitForTTSCompletion()
            indexOfCurrentStringBeingSpoken.set(-1)

            isInTheProcessOfSpeakingSims.set(false)
            emitter.onComplete()
        }).subscribeOn(computation())

        return Pair<SimDictationDetails, Observable<Int>>(SimDictationDetails(stringsToSpeak!!.size, simToStartIndex), observable)
    }

    override fun isInProcessOfSpeakingSims(): Boolean {
        return isInTheProcessOfSpeakingSims.get()
    }

    private fun doPause(): Boolean {
        stringsToSpeak?.let {
            paused.set(true)
            tts.stop()
            return true
        }
        return false
    }

    override fun pause() {
        if (doPause()) {
            indexOfCurrentStringBeingSpoken.decrementAndGet()
            println("Decrement to ${indexOfCurrentStringBeingSpoken.get()}")
        }
    }

    override fun seekTo(position: Int) {
        if (doPause()) {
            indexOfCurrentStringBeingSpoken.set(position - 1)
            resume()
        }
    }

    override fun isPaused(): Boolean {
        return paused.get()
    }

    override fun resume() {
        paused.set(false)
    }

    override fun close() {
        shouldExitNow.set(true)
        tts.stop()
        tts.shutdown()
    }
}
