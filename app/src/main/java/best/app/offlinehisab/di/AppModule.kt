package best.app.offlinehisab.di

import android.content.Context
import androidx.room.Room
import best.app.offlinehisab.data.db.AppDatabase
import best.app.offlinehisab.data.repo.Repo

const val DB_NAME = "hisab-db"
class AppModule {
    companion object {
        @Volatile
        private var db: AppDatabase? = null

        @Volatile
        private var repo: Repo? = null


        /**
         * Provide a singleton AppDatabase instance (thread-safe).
         */
        fun provideDb(context: Context): AppDatabase {
            return db ?: synchronized(this) {
                db ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // optionally add migrations here
                    // .addMigrations(...)
                    .build().also { db = it }
            }
        }


        /**
         * Provide repository bound to current db instance.
         * Repo is recreated if DB was destroyed & recreated.
         */
        fun provideRepo(context: Context): Repo {
            return repo ?: synchronized(this) {
                repo ?: Repo(provideDb(context)).also { repo = it }
            }
        }

        /**
         * Close and clear DB + Repo singletons. Call this BEFORE replacing DB files on disk.
         * After restore/copy, call provideDb/provideRepo again to recreate instances.
         */
        fun destroyInstance() {
            synchronized(this) {
                try {
                    db?.close()
                } catch (t: Throwable) {
                    // ignore or log
                } finally {
                    db = null
                    repo = null
                }
            }
        }
    }
}