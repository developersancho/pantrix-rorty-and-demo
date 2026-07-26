package com.developersancho.pantrixrortyanddemo.feature.lab

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.app.BuildVariant
import com.developersancho.pantrixrortyanddemo.databinding.FragmentLabBinding
import com.pantrix.api.Pantrix
import com.pantrix.core.processors.event.data.interaction.InteractionType
import com.pantrix.feedback.api.PantrixFeedback
import com.pantrix.inspector.api.PantrixInspector
import dagger.hilt.android.AndroidEntryPoint

/**
 * One row per SDK surface — the Android counterpart of the iOS demo's Lab screen. Each row does the
 * smallest real thing that produces an event, so "did it arrive?" can be answered in the dashboard.
 */
@AndroidEntryPoint
class LabFragment : Fragment(R.layout.fragment_lab) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = FragmentLabBinding.bind(view).container
        val variant = BuildVariant.current

        container.sectionHeader("Events")
        container.actionRow(
            "Track a custom event",
            "Pantrix.trackEvent(\"lab_button_tapped\", …)"
        ) {
            Pantrix.trackEvent("lab_button_tapped", mapOf("source" to "lab", "variant" to variant.name))
            toast("custom event sent")
        }
        container.actionRow(
            "Track an interaction",
            "Pantrix.trackInteraction(CLICK, …) — its own event category"
        ) {
            Pantrix.trackInteraction(InteractionType.CLICK, mapOf("target" to "lab_interaction_row"))
            toast("interaction sent")
        }
        container.actionRow(
            "Track a screen manually",
            "Pantrix.trackScreenView(\"LabManualScreen\") — obfuscation-proof name"
        ) {
            Pantrix.trackScreenView("LabManualScreen")
            toast("screen view sent")
        }

        container.sectionHeader("HTTP")
        container.actionRow(
            "Manual trackHttp",
            "For a client the SDK does not instrument"
        ) {
            val start = SystemClock.elapsedRealtime()
            Pantrix.trackHttp(
                url = "https://rickandmortyapi.com/api/character/1",
                path = "/api/character/1",
                method = "GET",
                startTime = start,
                endTime = SystemClock.elapsedRealtime(),
                statusCode = 200,
                error = null,
                client = "lab-manual"
            )
            toast("manual http event sent")
        }

        container.sectionHeader("Diagnostics")
        container.actionRow(
            "Trigger a handled exception",
            "Pantrix.trackException(…) — the app keeps running"
        ) {
            runCatching { error("Lab: a deliberately handled failure") }
                .onFailure { Pantrix.trackException(it, mapOf("screen" to "Lab")) }
            toast("handled exception reported")
        }

        container.sectionHeader("Debug tools")
        container.actionRow("Crash Lab", "Trigger real crashes captured on the next launch") {
            findNavController().navigate(R.id.action_lab_to_crashLab)
        }
        container.actionRow("Open Inspector", "PantrixInspector — or use the floating button") {
            PantrixInspector.show(requireActivity())
        }
        container.actionRow("Send Feedback", "PantrixFeedback.show(activity) — or shake the device") {
            PantrixFeedback.show(requireActivity())
        }
    }

    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}
