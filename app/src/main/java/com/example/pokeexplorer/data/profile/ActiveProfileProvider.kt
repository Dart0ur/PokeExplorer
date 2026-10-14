package com.example.pokeexplorer.data.profile

import kotlinx.coroutines.flow.Flow

interface ActiveProfileProvider {
    val activeProfileId: Flow<Long>
}