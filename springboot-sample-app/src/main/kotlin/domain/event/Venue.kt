package com.kraftadmin.domain.event

import com.kraftadmin.annotations.KraftAdminCustomAction
import com.kraftadmin.domain.base.BaseEntity
import jakarta.persistence.Entity

@Entity
class Venue(
    var name: String = "",
    var capacity: Int = 0,
    var address: String = ""
) : BaseEntity()

