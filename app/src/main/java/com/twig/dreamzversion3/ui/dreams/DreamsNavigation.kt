package com.twig.dreamzversion3.ui.dreams

object DreamsDestinations {
    const val LIST_ROUTE = "dreams/list"
    const val ADD_ROUTE = "dreams/add"
    const val DREAM_ID_ARG = "dreamId"
    const val EDIT_ROUTE = "dreams/edit/{$DREAM_ID_ARG}"
    const val SNACKBAR_RESULT_KEY = "dreams_snackbar_result"

    fun editRoute(dreamId: String): String = "dreams/edit/$dreamId"
}
