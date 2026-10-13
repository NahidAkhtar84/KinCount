package com.nahid.kincount

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.nahid.kincount.data.local.KinCountDatabase
import com.nahid.kincount.data.repo.CategoryRepository
import com.nahid.kincount.data.repo.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KinCountApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch {
            container.categoryRepository.seedDefaultsIfEmpty()
        }
    }
}

/**
 * Manual DI root — no Hilt by design (PLAN.md §3).
 * Google auth, the Sheets client, and the sync engine register here in M2+.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: KinCountDatabase =
        Room.databaseBuilder(appContext, KinCountDatabase::class.java, "kincount.db").build()

    val transactionRepository = TransactionRepository(database.transactionDao())
    val categoryRepository = CategoryRepository(database.categoryDao())
}

val Context.appContainer: AppContainer
    get() = (applicationContext as KinCountApp).container
