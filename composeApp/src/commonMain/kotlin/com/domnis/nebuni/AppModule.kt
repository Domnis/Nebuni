package com.domnis.nebuni

import org.koin.dsl.module

val appModule = module {
    single { AppState() }
}