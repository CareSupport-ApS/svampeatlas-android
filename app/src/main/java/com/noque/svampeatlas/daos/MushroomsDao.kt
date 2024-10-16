package com.noque.svampeatlas.daos

import androidx.room.*
import com.noque.svampeatlas.models.Mushroom



@Dao
interface MushroomsDao {

    @Query("SELECT * FROM mushrooms WHERE isUserFavorite = 1")
    suspend fun getFavorites(): Array<Mushroom>

    @Query("SELECT * FROM mushrooms WHERE id = :id LIMIT 1")
    suspend fun getMushroom(id: Int): Mushroom?

    @Query("SELECT * FROM mushrooms WHERE fullName LIKE :fullName OR _vernacularNameDK LIKE :fullName OR (fullName LIKE :genus || '%'  AND taxonName LIKE :taxonName || '%') ORDER BY probability DESC")
    suspend fun searchMushrooms_dk(fullName: String, taxonName: String, genus: String): Array<Mushroom>

    @Query("SELECT * FROM mushrooms WHERE fullName LIKE :fullName OR vernacularNameEn LIKE :fullName OR (fullName LIKE :genus || '%'  AND taxonName LIKE :taxonName || '%') ORDER BY probability DESC")
    suspend fun searchMushrooms_en(fullName: String, taxonName: String, genus: String): Array<Mushroom>

    @Query("SELECT * FROM mushrooms WHERE fullName LIKE :fullName OR vernacularNameCz LIKE :fullName OR (fullName LIKE :genus || '%'  AND taxonName LIKE :taxonName || '%') ORDER BY probability DESC")
    suspend fun searchMushrooms_cz(fullName: String, taxonName: String, genus: String): Array<Mushroom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavorites(vararg mushroom: Mushroom)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMushrooms(vararg mushrooms: Mushroom)

    @Delete
    suspend fun deleteMushroom(mushroom: Mushroom)
}