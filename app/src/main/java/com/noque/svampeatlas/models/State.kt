package com.noque.svampeatlas.models

sealed class State<T>(val item: T? = null) {
    class Items<T>(val items: T): State<T>(items)
    class Empty<T> : State<T>()
    class Loading<T> : State<T>()
    class Error<T>(val error: AppError): State<T>()
}

sealed class State2<T>(val item: T? = null) {
    class Items<T>(val items: T): State2<T>(items)
    class Empty<T> : State2<T>()
    class Loading<T> : State2<T>()
    class Error<T>(val error: AppError2): State2<T>()
}