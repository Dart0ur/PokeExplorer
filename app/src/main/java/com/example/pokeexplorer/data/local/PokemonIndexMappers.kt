package com.example.pokeexplorer.data.local

import com.example.pokeexplorer.data.model.PokemonListItem

fun PokemonIndexEntity.toDomain(): PokemonListItem =
    PokemonListItem(id = id, name = name, imageUrl = imageUrl)

fun PokemonListItem.toIndexEntity(fetchedAt: Long): PokemonIndexEntity =
    PokemonIndexEntity(id = id, name = name, imageUrl = imageUrl, fetchedAt = fetchedAt)