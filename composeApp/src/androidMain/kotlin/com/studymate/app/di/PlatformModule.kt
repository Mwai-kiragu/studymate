package com.studymate.app.di

import com.studymate.app.data.DatabaseDriverFactory
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory(get()).createDriver() }
}
