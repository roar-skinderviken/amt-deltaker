package no.nav.amt.deltaker.deltaker.kafka.dto

import no.nav.amt.deltaker.deltaker.getInnsoktDato
import no.nav.amt.deltaker.deltaker.getInnsoktDatoFraImportertDeltaker
import no.nav.amt.deltaker.deltaker.model.Deltaker
import no.nav.amt.deltaker.deltaker.model.getStatustekst
import no.nav.amt.deltaker.deltaker.model.getVisningsnavn
import no.nav.amt.deltaker.deltaker.vurdering.Vurdering
import no.nav.amt.deltaker.navansatt.NavAnsatt
import no.nav.amt.deltaker.navenhet.NavEnhet
import no.nav.amt.lib.models.arrangor.melding.Vurderingstype
import no.nav.amt.lib.models.deltaker.Deltakelsesinnhold
import no.nav.amt.lib.models.deltaker.DeltakerHistorikk
import no.nav.amt.lib.models.deltaker.DeltakerStatus
import no.nav.amt.lib.models.deltaker.deltakelsesmengde.toDeltakelsesmengder
import java.time.LocalDate
import java.util.UUID

data class DeltakerDto(
    private val deltaker: Deltaker,
    private val deltakerhistorikk: List<DeltakerHistorikk>,
    private val vurderinger: List<Vurdering>,
    private val navAnsatt: NavAnsatt?,
    private val navEnhet: NavEnhet?,
    private val forcedUpdate: Boolean?,
) {
    private val sisteEndring = deltakerhistorikk.getSisteEndring()

    private val innsoktDato = deltakerhistorikk.getInnsoktDato()
        ?: throw IllegalStateException("Skal ikke produsere deltaker som mangler vedtak til topic")

    val v1: DeltakerV1Dto
        get() = DeltakerV1Dto(
            id = deltaker.id,
            gjennomforingId = deltaker.deltakerliste.id,
            personIdent = deltaker.navBruker.personident,
            startDato = deltaker.startdato,
            sluttDato = deltaker.sluttdato,
            status = DeltakerV1Dto.DeltakerStatusDto(
                type = deltaker.status.type,
                statusTekst = deltaker.status.type.getStatustekst(),
                aarsak = deltaker.status.aarsak?.type,
                aarsakTekst = deltaker.status.aarsak?.let {
                    DeltakerStatus
                        .Aarsak(type = it.type, beskrivelse = deltaker.status.aarsak?.beskrivelse)
                        .getVisningsnavn()
                },
                opprettetDato = deltaker.status.opprettet,
            ),
            registrertDato = innsoktDato.atStartOfDay(),
            dagerPerUke = deltaker.dagerPerUke,
            prosentStilling = deltaker.deltakelsesprosent,
            endretDato = deltaker.sistEndret,
            kilde = deltaker.kilde,
            innhold = deltaker.deltakelsesinnhold?.toDeltakelsesinnholdDto(),
            deltakelsesmengder = deltakelsesmengderDto(deltaker, deltakerhistorikk),
        )

    val v2: DeltakerV2Dto
        get() = DeltakerV2Dto(
            id = deltaker.id,
            deltakerlisteId = deltaker.deltakerliste.id,
            personalia = DeltakerV2Dto.DeltakerPersonaliaDto(
                personId = deltaker.navBruker.personId,
                personident = deltaker.navBruker.personident,
                navn = DeltakerV2Dto.Navn(
                    fornavn = deltaker.navBruker.fornavn,
                    mellomnavn = deltaker.navBruker.mellomnavn,
                    etternavn = deltaker.navBruker.etternavn,
                ),
                kontaktinformasjon = DeltakerV2Dto.DeltakerKontaktinformasjonDto(
                    telefonnummer = deltaker.navBruker.telefon,
                    epost = deltaker.navBruker.epost,
                ),
                skjermet = deltaker.navBruker.erSkjermet,
                adresse = deltaker.navBruker.adresse,
                adressebeskyttelse = deltaker.navBruker.adressebeskyttelse,
            ),
            status = DeltakerV2Dto.DeltakerStatusDto(
                id = deltaker.status.id,
                type = deltaker.status.type,
                aarsak = deltaker.status.aarsak?.type,
                aarsaksbeskrivelse = deltaker.status.aarsak?.beskrivelse,
                gyldigFra = deltaker.status.gyldigFra,
                opprettetDato = deltaker.status.opprettet,
            ),
            dagerPerUke = deltaker.dagerPerUke,
            prosentStilling = deltaker.deltakelsesprosent?.toDouble(),
            oppstartsdato = deltaker.startdato,
            sluttdato = deltaker.sluttdato,
            innsoktDato = innsoktDato,
            forsteVedtakFattet = deltakerhistorikk.getForsteVedtakFattet(),
            bestillingTekst = deltaker.bakgrunnsinformasjon,
            navKontor = navEnhet?.navn,
            navVeileder = navAnsatt?.toDeltakerNavVeilederDto(),
            deltarPaKurs = deltaker.deltarPaKurs(),
            kilde = deltaker.kilde,
            innhold = deltaker.deltakelsesinnhold,
            historikk = deltakerhistorikk,
            vurderingerFraArrangor = vurderinger.toDto(),
            sistEndret = deltaker.sistEndret,
            sistEndretAv = sisteEndring?.getSistEndretAv(),
            sistEndretAvEnhet = sisteEndring?.getSistEndretAvEnhet(),
            forcedUpdate = forcedUpdate,
            erManueltDeltMedArrangor = deltaker.erManueltDeltMedArrangor,
            oppfolgingsperioder = deltaker.navBruker.oppfolgingsperioder,
        )

    private fun List<Vurdering>.toDto() = this.map {
        no.nav.amt.lib.models.arrangor.melding.Vurdering(
            id = it.id,
            deltakerId = it.deltakerId,
            opprettetAvArrangorAnsattId = it.opprettetAvArrangorAnsattId,
            opprettet = it.gyldigFra,
            vurderingstype = Vurderingstype.valueOf(it.vurderingstype.name),
            begrunnelse = it.begrunnelse,
        )
    }

    private fun Deltakelsesinnhold.toDeltakelsesinnholdDto() = DeltakerV1Dto.DeltakelsesinnholdDto(
        ledetekst = ledetekst,
        innhold = innhold.filter { it.valgt }.map {
            DeltakerV1Dto.InnholdDto(
                tekst = it.tekst,
                innholdskode = it.innholdskode,
            )
        },
    )

    private fun DeltakerHistorikk.getSistEndretAv(): UUID = when (this) {
        is DeltakerHistorikk.Vedtak -> vedtak.sistEndretAv
        is DeltakerHistorikk.Endring -> endring.endretAv
        is DeltakerHistorikk.EndringFraTiltakskoordinator -> endringFraTiltakskoordinator.endretAv
        is DeltakerHistorikk.InnsokPaaFellesOppstart -> data.innsoktAv
        is DeltakerHistorikk.Forslag,
        is DeltakerHistorikk.EndringFraArrangor,
        is DeltakerHistorikk.ImportertFraArena,
        is DeltakerHistorikk.VurderingFraArrangor,
        -> throw IllegalStateException("Siste endring kan ikke være et forslag eller endring fra arrangør")
    }

    private fun DeltakerHistorikk.getSistEndretAvEnhet(): UUID? = when (this) {
        is DeltakerHistorikk.Vedtak -> vedtak.sistEndretAvEnhet
        is DeltakerHistorikk.Endring -> endring.endretAvEnhet
        is DeltakerHistorikk.InnsokPaaFellesOppstart -> data.innsoktAvEnhet
        is DeltakerHistorikk.EndringFraTiltakskoordinator -> null
        is DeltakerHistorikk.Forslag,
        is DeltakerHistorikk.EndringFraArrangor,
        is DeltakerHistorikk.ImportertFraArena,
        is DeltakerHistorikk.VurderingFraArrangor,
        -> throw IllegalStateException("Siste endring kan ikke være et forslag eller endring fra arrangør")
    }

    private fun List<DeltakerHistorikk>.getForsteVedtakFattet(): LocalDate? {
        getInnsoktDatoFraImportertDeltaker()?.let { return it }

        val vedtak = filterIsInstance<DeltakerHistorikk.Vedtak>().map { it.vedtak }
        val forsteVedtak = vedtak.minByOrNull { it.opprettet }

        return forsteVedtak?.fattet?.toLocalDate()
    }

    private fun List<DeltakerHistorikk>.getSisteEndring() = this.firstOrNull {
        it is DeltakerHistorikk.Vedtak || it is DeltakerHistorikk.Endring
    }

    private fun deltakelsesmengderDto(deltaker: Deltaker, historikk: List<DeltakerHistorikk>): List<DeltakerV1Dto.DeltakelsesmengdeDto> {
        val deltakelsesmengder = if (deltaker.deltakerliste.tiltakstype.harDeltakelsesmengde) {
            val mengder = historikk.toDeltakelsesmengder()
            deltaker.startdato?.let { mengder.periode(deltaker.startdato, deltaker.sluttdato) } ?: mengder
        } else {
            emptyList()
        }

        return deltakelsesmengder.map {
            DeltakerV1Dto.DeltakelsesmengdeDto(
                it.deltakelsesprosent,
                it.dagerPerUke,
                it.gyldigFra,
                it.opprettet,
            )
        }
    }
}
