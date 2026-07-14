package com.kraftadmin.spi

import com.kraftadmin.enums.ProviderType

interface EntityDiscoverer {
    fun discover(): Set<DiscoveredEntity<*>>
    val provider: ProviderType
}