package com.example.mobile_app.model.service.module



import com.example.mobile_app.model.service.AccountService
import com.example.mobile_app.model.service.impl.AccountServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    // Questo risolve l'errore [Dagger/MissingBinding]
    // Collega l'interfaccia AccountService alla sua implementazione AccountServiceImpl
    @Binds
    @Singleton
    abstract fun provideAccountService(impl: AccountServiceImpl): AccountService

    // Nota: Ho rimosso provideStorageService perché non abbiamo ancora importato
    // la logica per il salvataggio dei dati (Note/Box), ma solo l'autenticazione.
}