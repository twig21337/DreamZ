package com.twig.dreamzversion3.dreamsigns

import android.net.Uri

object DreamSignsDestinations {
    const val HOME_ROUTE = "dream_signs/home"
    const val IGNORED_ROUTE = "dream_signs/ignored"
    const val SIGN_KEY_ARG = "signKey"
    const val DETAIL_ROUTE = "dream_signs/detail/{$SIGN_KEY_ARG}"

    fun detailRoute(signKey: String): String = "dream_signs/detail/${Uri.encode(signKey)}"
}
