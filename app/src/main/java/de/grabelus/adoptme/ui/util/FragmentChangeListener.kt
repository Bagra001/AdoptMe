package de.grabelus.adoptme.ui.util

import androidx.fragment.app.Fragment

interface FragmentChangeListener {
    fun replaceFragment(fragment: Fragment)
}