package com.noque.svampeatlas.services

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.noque.svampeatlas.daos.HostsDao
import com.noque.svampeatlas.daos.MushroomsDao
import com.noque.svampeatlas.daos.NotesDao
import com.noque.svampeatlas.daos.SubstratesDao
import com.noque.svampeatlas.daos.UserDao
import com.noque.svampeatlas.daos.VegetationTypeDao
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.models.User
import com.noque.svampeatlas.models.UserRolesTypeConverters
import com.noque.svampeatlas.models.VegetationType

val MIGRATION_12_13 = object: Migration(12,13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE user "
                + " ADD COLUMN roles TEXT")
        database.execSQL("DROP TABLE hosts")
        database.execSQL("CREATE TABLE IF NOT EXISTS `hosts` (`id` INTEGER NOT NULL, `dkName` TEXT, `latinName` TEXT NOT NULL, `probability` INTEGER, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_13_14 = object: Migration(13,14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE substrates ADD COLUMN groupCzName TEXT")
        database.execSQL("ALTER TABLE substrates ADD COLUMN czName TEXT")
        database.execSQL("ALTER TABLE vegetationType ADD COLUMN czName TEXT")
        database.execSQL("ALTER TABLE mushrooms ADD COLUMN vernacularNameCz TEXT")
    }
}

val MIGRATION_14_15 = object: Migration(14,15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE substrates")
        database.execSQL("CREATE TABLE IF NOT EXISTS `substrates` (`id` INTEGER NOT NULL, `dkName` TEXT NOT NULL, `enName` TEXT NOT NULL, `czName` TEXT, `groupDkName` TEXT NOT NULL, `groupEnName` TEXT NOT NULL, `groupCzName` TEXT, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_15_18 = object: Migration(15,18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE hosts")
        database.execSQL("CREATE TABLE IF NOT EXISTS `hosts` (`id` INTEGER NOT NULL, `dkName` TEXT, `latinName` TEXT NOT NULL, `probability` INTEGER, `isUserSelected` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        database.execSQL("DROP TABLE substrates")
        database.execSQL("CREATE TABLE IF NOT EXISTS `substrates` (`id` INTEGER NOT NULL, `dkName` TEXT NOT NULL, `enName` TEXT NOT NULL, `czName` TEXT, `groupDkName` TEXT NOT NULL, `groupEnName` TEXT NOT NULL, `groupCzName` TEXT, `hide` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_18_23 = object: Migration(18,23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE hosts")
        database.execSQL("CREATE TABLE IF NOT EXISTS `hosts` (`id` INTEGER NOT NULL, `dkName` TEXT, `latinName` TEXT NOT NULL, `probability` INTEGER, `isUserSelected` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        database.execSQL("DROP TABLE substrates")
        database.execSQL("CREATE TABLE IF NOT EXISTS `substrates` (`id` INTEGER NOT NULL, `dkName` TEXT NOT NULL, `enName` TEXT NOT NULL, `czName` TEXT, `groupDkName` TEXT NOT NULL, `groupEnName` TEXT NOT NULL, `groupCzName` TEXT, `hide` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        database.execSQL("DROP TABLE vegetationType")
        database.execSQL("CREATE TABLE IF NOT EXISTS `vegetationType` (`id` INTEGER NOT NULL, `dkName` TEXT NOT NULL, `enName` TEXT NOT NULL, `czName` TEXT, PRIMARY KEY(`id`))")
        database.execSQL("ALTER TABLE mushrooms ADD COLUMN isUserFavorite INTEGER NOT NULL DEFAULT 0")
    }
}


@TypeConverters(ImagesConverter::class, RedListDataConverter::class, UserRolesTypeConverters::class, IDsConverter::class, StringsConverter::class, DateConverter::class, LatLngConverter::class)

@Database(entities = [User::class, Substrate::class, VegetationType::class, Host::class, Mushroom::class, NewObservation::class],
    version = 26, autoMigrations = [
        AutoMigration(from = 25, to = 26)
    ])
abstract class Database: RoomDatabase() {
    abstract fun UserDao(): UserDao
    abstract fun SubstratesDao(): SubstratesDao
    abstract fun VegetationTypeDao(): VegetationTypeDao
    abstract fun HostsDao(): HostsDao
    abstract fun mushroomsDao(): MushroomsDao
    abstract fun notesDao(): NotesDao


    companion object {
        fun buildDatabase(context: Context) = Room.databaseBuilder(context,
            com.noque.svampeatlas.services.Database::class.java, "fungal-database.db")
            .addMigrations(MIGRATION_12_13)
            .addMigrations(MIGRATION_13_14)
            .addMigrations(MIGRATION_14_15)
            .addMigrations(MIGRATION_15_18)
            .addMigrations(MIGRATION_18_23)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }
}