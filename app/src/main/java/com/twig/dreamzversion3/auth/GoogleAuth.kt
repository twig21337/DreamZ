package com.twig.dreamzversion3.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

// Drive FILE scope (visible files your app creates)
private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
// Docs scope (to edit Google Docs content)
private const val DOCS_SCOPE = "https://www.googleapis.com/auth/documents"

fun buildGoogleSignInClient(context: Context): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        // ask for BOTH scopes
        .requestScopes(
            Scope(DRIVE_FILE_SCOPE),
            Scope(DOCS_SCOPE)
        )
        .build()
    return GoogleSignIn.getClient(context, gso)
}

fun getLastAccount(context: Context) =
    GoogleSignIn.getLastSignedInAccount(context)
