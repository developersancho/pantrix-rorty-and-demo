package com.developersancho.pantrixrortyanddemo.feature.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.developersancho.pantrixrortyanddemo.BuildConfig
import com.developersancho.pantrixrortyanddemo.R
import com.developersancho.pantrixrortyanddemo.app.BuildVariant
import com.developersancho.pantrixrortyanddemo.databinding.FragmentProfileBinding
import com.developersancho.pantrixrortyanddemo.feature.lab.actionRow
import com.developersancho.pantrixrortyanddemo.feature.lab.sectionHeader
import com.pantrix.api.Pantrix
import dagger.hilt.android.AndroidEntryPoint

/**
 * User identity, kept separate from the Lab — the Lab is about events, this is about *who* the events
 * belong to. Mirrors the iOS demo's Profile tab.
 *
 * One row iOS has is missing here on purpose: **there is no way to log out.** `Pantrix` on Android
 * exposes `setUser` / `setUserProperty` / `setUserProperties` / `unsetUserProperty` but no
 * `clearUser()`, which iOS does have. Individual properties can be removed; the user id cannot.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = FragmentProfileBinding.bind(view).container
        val variant = BuildVariant.current

        container.sectionHeader("Identity")
        container.actionRow(
            "Log in as demo user",
            "Pantrix.setUser(\"$DEMO_USER_ID\", …) — later events carry this user id"
        ) {
            Pantrix.setUser(DEMO_USER_ID, mapOf("plan" to "demo", "role" to "rorty-android"))
            toast("user set to $DEMO_USER_ID")
        }
        container.actionRow(
            "Set a user property",
            "Pantrix.setUserProperty(\"favorite_show\", …)"
        ) {
            Pantrix.setUserProperty("favorite_show", "Rick and Morty")
            toast("property set")
        }
        container.actionRow(
            "Set several properties",
            "Pantrix.setUserProperties(…) — one call, many keys"
        ) {
            Pantrix.setUserProperties(mapOf("region" to "eu", "tier" to "beta"))
            toast("properties set")
        }
        container.actionRow(
            "Unset a property",
            "Pantrix.unsetUserProperty(\"favorite_show\")"
        ) {
            Pantrix.unsetUserProperty("favorite_show")
            toast("property unset")
        }

        container.sectionHeader("This build")
        container.actionRow(
            "Variant: ${variant.name}",
            "applicationId ${BuildConfig.APPLICATION_ID}\n" +
                "backend ${variant.backendUrl}\n" +
                "token …${variant.ingestToken.takeLast(6)}\n" +
                "SDK logging ${if (variant.enableSdkLogging) "on" else "off"}"
        ) {
            toast(variant.name)
        }
    }

    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val DEMO_USER_ID = "demo-user-42"
    }
}
