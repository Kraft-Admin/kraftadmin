package com.kraftadmin.spi

import com.kraftadmin.enums.ProviderType

data class DiscoveredEntity<T : Any>(
    val entityClass: Class<T>,
    val provider: ProviderType
)