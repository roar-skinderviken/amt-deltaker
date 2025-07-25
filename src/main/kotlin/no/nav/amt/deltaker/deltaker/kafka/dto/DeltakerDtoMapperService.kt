package no.nav.amt.deltaker.deltaker.kafka.dto

import no.nav.amt.deltaker.deltaker.DeltakerHistorikkService
import no.nav.amt.deltaker.deltaker.model.Deltaker
import no.nav.amt.deltaker.deltaker.model.Kilde
import no.nav.amt.deltaker.deltaker.vurdering.VurderingRepository
import no.nav.amt.deltaker.navansatt.NavAnsattService
import no.nav.amt.deltaker.navenhet.NavEnhetService
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk

class DeltakerDtoMapperService(
    private val navAnsattService: NavAnsattService,
    private val navEnhetService: NavEnhetService,
    private val deltakerHistorikkService: DeltakerHistorikkService,
    private val vurderingRepository: VurderingRepository,
) {
    suspend fun tilDeltakerDto(deltaker: Deltaker, forcedUpdate: Boolean? = false): DeltakerDto {
        val deltakerhistorikk = deltakerHistorikkService.getForDeltaker(deltaker.id)
        val vurderinger = vurderingRepository.getForDeltaker(deltaker.id)

        if (deltaker.kilde == Kilde.KOMET &&
            deltakerhistorikk.filterIsInstance<DeltakerHistorikk.Vedtak>().isEmpty() &&
            deltakerhistorikk.filterIsInstance<DeltakerHistorikk.InnsokPaaFellesOppstart>().isEmpty()
        ) {
            throw IllegalStateException(
                "Deltaker med kilde ${Kilde.KOMET} må ha minst et vedtak eller være søkt in for å produseres til topic",
            )
        }

        val navEnhet = deltaker.navBruker.navEnhetId
            ?.let { navEnhetService.hentEllerOpprettNavEnhet(it) }

        val navAnsatt = deltaker.navBruker.navVeilederId
            ?.let { navAnsattService.hentEllerOpprettNavAnsatt(it) }

        return DeltakerDto(
            deltaker,
            deltakerhistorikk,
            vurderinger,
            navAnsatt,
            navEnhet,
            forcedUpdate,
        )
    }
}
