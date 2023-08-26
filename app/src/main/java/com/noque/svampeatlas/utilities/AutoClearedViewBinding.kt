package com.noque.svampeatlas.utilities

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AutoClearedViewBinding<T : ViewBinding>( val fragment: Fragment,
                                               val viewBindingFactory: (View) -> T, val onDestroyView: ((T?) -> Unit)?) :
    ReadOnlyProperty<Fragment, T> {

    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            val viewLifecycleOwnerObserver = Observer<LifecycleOwner?> { owner ->
                if (owner == null) {
                    onDestroyView?.invoke(binding)
                    binding = null
                }
            }
            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observeForever(viewLifecycleOwnerObserver)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.removeObserver(viewLifecycleOwnerObserver)
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding

        if (binding != null && binding.root === thisRef.view) {
            return binding
        }

        val view = thisRef.view

        @Suppress("FoldInitializerAndIfToElvis")
        if (view == null) {
            throw IllegalStateException("Should not attempt to get bindings when the Fragment's view is null.")
        }

        return viewBindingFactory(view).also { this.binding = it }
    }
}

fun <T : ViewBinding> Fragment.autoClearedViewBinding(viewBindingFactory: (View) -> T, onDestroyView: ((T?) -> Unit)? = null) =
    AutoClearedViewBinding(this, viewBindingFactory, onDestroyView)