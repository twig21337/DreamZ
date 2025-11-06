package com.twig.dreamzversion3.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ✅ Both scopes in one string, space-separated
private const val SCOPE =
    "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/documents"

/** Returns an OAuth access token for the signed-in account. */
suspend fun fetchAccessToken(activity: Activity): String = withContext(Dispatchers.IO) {
    val acct = GoogleSignIn.getLastSignedInAccount(activity)
        ?: error("No signed-in account")
    val account = acct.account ?: error("No Google account bound")
    try {
        GoogleAuthUtil.getToken(activity, account, SCOPE)
    } catch (e: UserRecoverableAuthException) {
        e.intent?.let { intent ->
            activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        throw e
    } catch (e: GoogleAuthException) {
        throw e
    }
}
