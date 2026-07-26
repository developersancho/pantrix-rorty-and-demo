package com.developersancho.pantrixrortyanddemo.feature.lab

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.databinding.FragmentCrashLabBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Real crashes, captured by the SDK and reported on the NEXT launch — so after tapping one, reopen
 * the app before looking in the dashboard.
 *
 * The Android set differs from the iOS demo's on purpose. iOS can raise signals directly (SIGABRT,
 * SIGBUS, SIGILL); on Android the interesting cases are the JVM ones plus ANR, and — the one with no
 * iOS counterpart — a crash from a **minified** build, which only resolves to real class and method
 * names if the R8 mapping actually reached the backend.
 */
@AndroidEntryPoint
class CrashLabFragment : Fragment(R.layout.fragment_crash_lab) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = FragmentCrashLabBinding.bind(view).container

        container.sectionHeader("Uncaught exceptions")
        container.actionRow("RuntimeException", "The ordinary case — thrown on the main thread") {
            throw RuntimeException("CrashLab: deliberate RuntimeException")
        }
        container.actionRow("IllegalStateException from a nested call", "Exercises a deeper stack") {
            crashDeepA()
        }
        container.actionRow("Crash on a background thread", "An uncaught throwable off the main thread") {
            Thread { throw IllegalArgumentException("CrashLab: background thread crash") }.start()
        }
        container.actionRow("NullPointerException", "A platform NPE, not a Kotlin null check") {
            val nothing: String? = null
            @Suppress("KotlinConstantConditions")
            nothing!!.length
        }

        container.sectionHeader("Errors")
        container.actionRow("StackOverflowError", "Unbounded recursion") { recurse(0) }
        container.actionRow("OutOfMemoryError", "Allocate until the heap gives up") {
            val hog = mutableListOf<ByteArray>()
            while (true) hog += ByteArray(16 * 1024 * 1024)
        }

        container.sectionHeader("ANR")
        container.actionRow("Block the main thread (12s)", "Long enough for the watchdog to fire") {
            Handler(Looper.getMainLooper()).post { Thread.sleep(12_000) }
        }
    }

    // Named, non-inlined frames so the dashboard has something recognisable to show — and, in a
    // minified build, something that is only recognisable when the mapping was uploaded.
    private fun crashDeepA(): Nothing = crashDeepB()
    private fun crashDeepB(): Nothing = crashDeepC()
    private fun crashDeepC(): Nothing = throw IllegalStateException("CrashLab: three frames deep")

    private fun recurse(depth: Int): Int = recurse(depth + 1) + 1
}
