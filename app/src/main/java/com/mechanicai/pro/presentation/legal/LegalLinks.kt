package com.mechanicai.pro.presentation.legal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.mechanicai.pro.R

object LegalLinks {
    fun openPrivacyPolicy(context: Context) {
        open(context, context.getString(R.string.privacy_policy_url))
    }

    fun openTermsOfService(context: Context) {
        open(context, context.getString(R.string.terms_of_service_url))
    }

    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        runCatching {
            CustomTabsIntent.Builder().build().launchUrl(context, uri)
        }.onFailure {
            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
