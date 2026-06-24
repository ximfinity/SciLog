package com.scilog.app.core.math

import com.scilog.app.domain.model.MedicationType
import kotlin.math.pow

/**
 * Two-compartment pharmacokinetic model with first-order subcutaneous absorption (RK4).
 *
 * State: A1 = SC depot (mg), A2 = central compartment (mg), A3 = peripheral compartment (mg)
 *   dA1/dt = -ka·A1
 *   dA2/dt =  ka·A1 − (K10 + K12)·A2 + K21·A3
 *   dA3/dt =  K12·A2 − K21·A3
 *
 * Serum concentration C(t) = A2(t)/V2   [mg/L]
 * Active drug amount   M(t) = A2(t)      [mg]
 *
 * Bioavailability F is applied at dosing: F·dose is loaded into A1.
 */
object TwoCompartmentPKEngine {

    // ── PK parameter sets ─────────────────────────────────────────────────────

    /**
     * All parameters for one drug.
     *
     * Tirzepatide sources:
     *   - FDA NDA 213292 CDER Clinical Pharmacology Review (2022): CL=0.0642 L/h, V_ss=10.3 L, F=0.80
     *   - Thomas MK et al. N Engl J Med. 2021;385:503-515 (first-in-human pop-PK)
     *   - Frias JP et al. Lancet. 2021;398:143-155 (SURPASS-1)
     *   - Terminal t½ = 5 days (label); Q calibrated to reproduce β = ln2/5 /day
     *
     * Semaglutide sources:
     *   - Kapitza C et al. Eur J Clin Pharmacol. 2015;71:1393-1405 (pop-PK, 2-compartment)
     *   - FDA NDA 209637 CDER Review (2017): F=0.89, t½ ≈ 7 days (empirical)
     *   - Lau J et al. J Med Chem. 2015;58:7370-80 (albumin binding; explains high V2)
     *   Note: pop-PK model gives terminal t½ ≈ 10-11 days; empirical label value 7 days
     *   reflects assay sensitivity (Marbury T et al. Clin Pharmacokinet. 2021;60:493-507).
     */
    data class PKParameters(
        val ka: Double,             // /day — first-order absorption rate from SC depot
        val cl: Double,             // L/day — systemic clearance from central compartment
        val v2: Double,             // L — central compartment volume of distribution
        val v3: Double,             // L — peripheral compartment volume of distribution
        val q: Double,              // L/day — inter-compartmental clearance
        val bioavailability: Double, // 0–1 — subcutaneous bioavailability F
        val displayName: String,
        val labelHalfLifeDays: Double,  // half-life quoted on FDA label (informational)
        val maxDoseMg: Double           // maximum labeled dose for this medication
    ) {
        val k10: Double get() = cl / v2   // /day — first-order elimination from central
        val k12: Double get() = q  / v2   // /day — central → peripheral
        val k21: Double get() = q  / v3   // /day — peripheral → central
        val vss: Double get() = v2 + v3   // L — volume at steady state

        /**
         * Returns a copy with CL, V2, V3, Q scaled allometrically to [weightKg].
         * Standard population-PK allometric exponents: CL ∝ BW^0.75, V ∝ BW^1.0.
         * Reference weight is 70 kg per published popPK literature.
         */
        fun scaledForWeight(weightKg: Double): PKParameters {
            val wt = weightKg / 70.0
            return copy(
                cl = cl * wt.pow(0.75),
                v2 = v2 * wt,
                v3 = v3 * wt,
                q  = q  * wt.pow(0.75)
            )
        }
    }

    /** Pre-built parameter sets indexed by medication type. */
    object Params {
        /**
         * Tirzepatide — dual GIP/GLP-1 receptor agonist (Mounjaro®/Zepbound®).
         *   ka  = 0.984 /day  (FDA pop-PK; T½_abs ≈ 17 h; Tmax ~1-2 days SC)
         *   CL  = 1.541 L/day (FDA CDER: 0.0642 L/h, typical 94 kg patient)
         *   V2  = 3.06  L     (FDA pop-PK central volume)
         *   V3  = 7.24  L     (FDA pop-PK; V_ss = 10.3 L − V2)
         *   Q   = 10.27 L/day (calibrated: gives β = ln2/5 d⁻¹, i.e. t½_terminal = 5 d)
         *   F   = 0.80        (FDA label)
         */
        val TIRZEPATIDE = PKParameters(
            ka              = 0.984,
            cl              = 1.541,
            v2              = 3.06,
            v3              = 7.24,
            q               = 10.27,
            bioavailability = 0.80,
            displayName     = "Tirzepatide",
            labelHalfLifeDays = 5.0,
            maxDoseMg       = 15.0   // Zepbound/Mounjaro: 2.5→5→7.5→10→12.5→15 mg
        )

        /**
         * Semaglutide — once-weekly GLP-1 receptor agonist (Ozempic®).
         *   ka  = 0.787 /day  (Kapitza 2015; T½_abs ≈ 21 h; Tmax 24-72 h SC)
         *   CL  = 0.792 L/day (Kapitza 2015: 0.033 L/h)
         *   V2  = 7.87  L     (Kapitza 2015; larger central volume due to 98% albumin binding)
         *   V3  = 4.16  L     (Kapitza 2015)
         *   Q   = 1.219 L/day (Kapitza 2015: 0.0508 L/h)
         *   F   = 0.89        (FDA NDA 209637 label)
         */
        val SEMAGLUTIDE = PKParameters(
            ka              = 0.787,
            cl              = 0.792,
            v2              = 7.87,
            v3              = 4.16,
            q               = 1.219,
            bioavailability = 0.89,
            displayName     = "Semaglutide",
            labelHalfLifeDays = 7.0,
            maxDoseMg       = 2.4   // Wegovy max; Ozempic diabetes max is 1.0 mg
        )

        fun forMedication(type: MedicationType): PKParameters = when (type) {
            MedicationType.TIRZEPATIDE -> TIRZEPATIDE
            else                       -> SEMAGLUTIDE
        }
    }

    // ── Internal ODE state ────────────────────────────────────────────────────

    private data class State(val a1: Double, val a2: Double, val a3: Double) {
        operator fun plus(d: State)  = State(a1 + d.a1, a2 + d.a2, a3 + d.a3)
        operator fun times(s: Double) = State(a1 * s,   a2 * s,   a3 * s)
    }

    private fun deriv(s: State, p: PKParameters) = State(
        a1 = -p.ka * s.a1,
        a2 =  p.ka * s.a1 - (p.k10 + p.k12) * s.a2 + p.k21 * s.a3,
        a3 =  p.k12 * s.a2 - p.k21 * s.a3
    )

    private fun rk4(s: State, p: PKParameters, dt: Double): State {
        val k1 = deriv(s, p)
        val k2 = deriv(s + k1 * (dt / 2), p)
        val k3 = deriv(s + k2 * (dt / 2), p)
        val k4 = deriv(s + k3 * dt, p)
        return s + (k1 + k2 * 2.0 + k3 * 2.0 + k4) * (dt / 6.0)
    }

    // ── Public data types ─────────────────────────────────────────────────────

    /**
     * A single point on the simulated curve.
     *
     * [concMgL]  — serum concentration C = A2/V2  (mg/L) — used by the full PK chart
     * [amountMg] — active drug in central compartment A2   (mg)  — used by the dashboard
     */
    data class PKPoint(
        val timestampMs: Long,
        val concMgL: Double,
        val amountMg: Double
    )

    data class SimResult(
        val actualPoints:    List<PKPoint>,
        val projectedPoints: List<PKPoint>,
        val cMaxSS:          Double,   // mg/L
        val cMinSS:          Double,   // mg/L
        val currentConcMgL:  Double,
        val currentAmountMg: Double    // A2 at "now" in mg
    )

    // ── Simulation ────────────────────────────────────────────────────────────

    /**
     * Integrate from the first dose to the end of the last projected dose + one cycle.
     *
     * Points with timestampMs ≤ nowMs → [SimResult.actualPoints] (solid line).
     * Points after nowMs → [SimResult.projectedPoints] (dashed line).
     *
     * Dose is multiplied by [PKParameters.bioavailability] before entering depot A1.
     */
    fun simulate(
        historicalDoses:  List<Pair<Long, Double>>,
        projectedDoses:   List<Pair<Long, Double>>,
        nowMs: Long,
        params: PKParameters = Params.TIRZEPATIDE,
        stepMinutes: Int = 30
    ): SimResult {
        val allDoses = (historicalDoses + projectedDoses).sortedBy { it.first }
        if (allDoses.isEmpty()) return SimResult(emptyList(), emptyList(), 0.0, 0.0, 0.0, 0.0)

        val stepMs   = stepMinutes * 60_000L
        val stepDays = stepMinutes / (24.0 * 60.0)
        val startMs  = allDoses.first().first
        val endMs    = (projectedDoses.lastOrNull()?.first ?: nowMs) + 7L * 86_400_000L

        var state    = State(0.0, 0.0, 0.0)
        val actual   = mutableListOf<PKPoint>()
        val projected = mutableListOf<PKPoint>()
        val queue    = ArrayDeque(allDoses)

        var tMs = startMs
        while (tMs <= endMs) {
            while (queue.isNotEmpty() && queue.first().first <= tMs) {
                val (_, dose) = queue.removeFirst()
                state = state.copy(a1 = state.a1 + dose * params.bioavailability)
            }
            val pt = PKPoint(tMs, state.a2 / params.v2, state.a2)
            if (tMs <= nowMs) actual.add(pt) else projected.add(pt)
            state = rk4(state, params, stepDays)
            tMs  += stepMs
        }

        val cur = actual.lastOrNull() ?: projected.firstOrNull()
        return SimResult(actual, projected, 0.0, 0.0, cur?.concMgL ?: 0.0, cur?.amountMg ?: 0.0)
    }

    // ── Steady-state estimation ───────────────────────────────────────────────

    /**
     * Numerically estimate Cmax_SS and Cmin_SS (mg/L) by simulating [nDoses] weekly
     * doses at [cycleDays] interval, then reading the extremes in the final cycle.
     *
     * Returns (cMaxSS, cMinSS) in mg/L.
     */
    fun steadyState(
        doseMg: Double,
        cycleDays: Double    = 7.0,
        params: PKParameters = Params.TIRZEPATIDE,
        nDoses: Int          = 8
    ): Pair<Double, Double> {
        val stepDays = 0.5 / 24.0
        val stepMs   = (stepDays * 86_400_000.0).toLong()
        val endMs    = (nDoses * cycleDays * 86_400_000.0).toLong()
        val ssStart  = ((nDoses - 1) * cycleDays * 86_400_000.0).toLong()

        val queue = ArrayDeque((0 until nDoses).map { i ->
            (i * cycleDays * 86_400_000.0).toLong() to (doseMg * params.bioavailability)
        })

        var state = State(0.0, 0.0, 0.0)
        var cMax  = 0.0
        var cMin  = Double.MAX_VALUE
        var tMs   = 0L

        while (tMs <= endMs) {
            while (queue.isNotEmpty() && queue.first().first <= tMs) {
                state = state.copy(a1 = state.a1 + queue.removeFirst().second)
            }
            if (tMs >= ssStart) {
                val c = state.a2 / params.v2
                if (c > cMax) cMax = c
                if (c < cMin) cMin = c
            }
            state = rk4(state, params, stepDays)
            tMs  += stepMs
        }
        return cMax to (if (cMin == Double.MAX_VALUE) 0.0 else cMin)
    }

    /**
     * Returns the 1-indexed dose number at which simulated Cmax reaches ≥90% of
     * the true steady-state Cmax (estimated at nDoses=16).
     */
    fun dosesUntilSteadyState(
        doseMg: Double,
        params: PKParameters,
        intervalDays: Double = 7.0
    ): Int {
        val trueSSCmax = steadyState(doseMg, intervalDays, params, nDoses = 16).first
        if (trueSSCmax <= 0) return 0
        for (n in 1..16) {
            val cMaxN = steadyState(doseMg, intervalDays, params, nDoses = n).first
            if (cMaxN >= 0.9 * trueSSCmax) return n
        }
        return 16
    }
}
