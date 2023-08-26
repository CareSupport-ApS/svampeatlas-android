package com.noque.svampeatlas.view_holders

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemTextInputBinding

class InputTypeViewHolder(private val onTextInputChanged: (view: View, text: String?) -> Unit, binding: ItemTextInputBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private var editText: EditText = binding.textInputItemEditText
    private var titleTextView = binding.textInputItemTitleTextView

    private val textWatcher = object: TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            if (editText.hasFocus()) {
                onTextInputChanged.invoke(itemView, p0.toString())
            }
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }

    init {
        editText.addTextChangedListener(textWatcher)
    }

    fun configure(title: String, placeholder: String?, content: String?) {
        titleTextView.text = title
        editText.hint = placeholder
        editText.setText(content)
    }
}