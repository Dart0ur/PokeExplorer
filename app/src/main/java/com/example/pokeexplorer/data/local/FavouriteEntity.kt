package com.example.pokeexplorer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "favourites",
    primaryKeys = ["profileId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("profileId")],
)
data class FavouriteEntity(
    val profileId: Long = 1,
    val id: Int,
    val name: String,
    val imageUrl: String,
    val addedAt: Long,
)
