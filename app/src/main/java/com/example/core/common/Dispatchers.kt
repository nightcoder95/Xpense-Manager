package com.example.core.common

import kotlinx.coroutines.CoroutineDispatcher

data class AppDispatchers(val io: CoroutineDispatcher, val default: CoroutineDispatcher)
