package no.nav.amt.deltaker.navenhet

import java.util.UUID

data class NavEnhet(
    val id: UUID,
    val enhetsnummer: String,
    val navn: String,
)
