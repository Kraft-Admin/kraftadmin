package com.kraftadmin.repository

import com.kraftadmin.domain.event.Venue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VenueRepository : JpaRepository<Venue, String> {
}