import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";

function ReservationForm({ setReservation }) {
    const [startTime, setStartTime] = useState(""); // "YYYY-MM-DDTHH:mm"
    const [minutes, setMinutes] = useState(120); // 30..300
    const [tableNumber, setTableNumber] = useState("");
    const [availableTables, setAvailableTables] = useState([]);
    const [formError, setFormError] = useState("");

    const navigate = useNavigate();
    const API = useMemo(
        () => process.env.REACT_APP_API_URL || "http://localhost:8080",
        []
    );

    // --- Geschäftsregeln ---
    const MIN_MIN = 30;
    const MAX_MIN = 300;
    const CLOSING_HOUR = 22; // Reservierungen maximal bis 22:00 Uhr

    // Zeit-Helferfunktionen
    const pad = (n) => (n < 10 ? "0" + n : n);
    const toIsoWithSeconds = (date) =>
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
            date.getDate()
        )}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
            date.getSeconds()
        )}`;

    const parseLocalDateTime = (value) => (value ? new Date(value + ":00") : null);

    // Ermittelt die maximal erlaubte Dauer, sodass Ende <= 22:00 Uhr
    const maxMinutesForStart = (start) => {
        if (!start) return MAX_MIN;
        const latestEnd = new Date(start);
        latestEnd.setHours(CLOSING_HOUR, 0, 0, 0); // 22:00 Uhr desselben Tages
        const diffMs = latestEnd.getTime() - start.getTime();
        const diffMin = Math.floor(diffMs / 60000);
        if (diffMin <= 0) return 0; // zu spät
        return Math.min(MAX_MIN, diffMin);
    };

    // Verfügbare Tische abrufen
    useEffect(() => {
        const fetchAvailable = async () => {
            setFormError("");
            if (!startTime || !minutes) {
                setAvailableTables([]);
                return;
            }

            const start = parseLocalDateTime(startTime);
            if (!start) return;

            const maxAllowed = maxMinutesForStart(start);
            if (maxAllowed < MIN_MIN) {
                setAvailableTables([]);
                return;
            }

            const startISO = toIsoWithSeconds(start);

            try {
                const res = await fetch(
                    `${API}/api/reservations/available?start=${encodeURIComponent(
                        startISO
                    )}&minutes=${minutes}`,
                    { credentials: "include" }
                );
                if (!res.ok) throw new Error("Fehler beim Laden der verfügbaren Tische.");
                const data = await res.json();
                setAvailableTables(Array.isArray(data) ? data : []);
            } catch (e) {
                console.error(e);
                setAvailableTables([]);
            }
        };

        fetchAvailable();
    }, [startTime, minutes, API]);

    // Auswahl zurücksetzen, wenn Tisch nicht mehr verfügbar
    useEffect(() => {
        if (!tableNumber) return;
        const stillAvailable = availableTables.some(
            (t) => String(t.tableNumber) === String(tableNumber)
        );
        if (!stillAvailable) setTableNumber("");
    }, [tableNumber, availableTables]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormError("");

        const start = parseLocalDateTime(startTime);
        if (!start) return setFormError("Bitte Startzeit auswählen.");
        if (start < new Date())
            return setFormError("Reservierungen in der Vergangenheit sind nicht möglich.");

        if (!minutes || minutes < MIN_MIN || minutes > MAX_MIN)
            return setFormError("Die Dauer muss zwischen 30 und 300 Minuten liegen.");

        const maxAllowed = maxMinutesForStart(start);
        if (minutes > maxAllowed) {
            if (maxAllowed < MIN_MIN) {
                return setFormError(
                    "Die letzte Reservierung muss spätestens um 22:00 Uhr enden. Bitte frühere Zeit wählen."
                );
            }
            return setFormError(
                `Für diese Startzeit ist maximal ${maxAllowed} Minuten erlaubt (bis 22:00 Uhr).`
            );
        }

        if (!tableNumber) return setFormError("Bitte Tisch auswählen.");

        const end = new Date(start.getTime() + minutes * 60 * 1000);

        const payload = {
            tableNumber: Number(tableNumber),
            startTime: toIsoWithSeconds(start),
            endTime: toIsoWithSeconds(end),
        };

        try {
            const resp = await fetch(`${API}/api/reservations`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify(payload),
            });

            const txt = await resp.text();
            if (!resp.ok) {
                throw new Error(
                    txt || "Reservierung fehlgeschlagen – bitte versuchen Sie es erneut."
                );
            }

            const data = txt ? JSON.parse(txt) : null;
            setReservation([data]);
            navigate("/reservations/my");
        } catch (err) {
            console.error(err);
            setFormError(`Reservierung fehlgeschlagen: ${err.message}`);
        }
    };

    // Mindestzeit für datetime-local
    const nowLocalForMin = useMemo(() => {
        const d = new Date();
        d.setSeconds(0, 0);
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(
            d.getDate()
        )}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }, []);

    // Dropdown für Dauer (nur gültige Optionen)
    const renderDurationOptions = () => {
        const start = parseLocalDateTime(startTime);
        const maxAllowed = maxMinutesForStart(start);
        const items = [];
        for (let m = MIN_MIN; m <= MAX_MIN; m += 30) {
            const disabled = start && m > maxAllowed;
            const label =
                m < 60
                    ? `${m} Min`
                    : `${Math.floor(m / 60)} Std${m % 60 ? ` ${m % 60} Min` : ""}`;
            items.push(
                <option key={m} value={m} disabled={disabled}>
                    {label}
                    {disabled ? " (bis 22:00 Uhr)" : ""}
                </option>
            );
        }
        return items;
    };

    const start = parseLocalDateTime(startTime);
    const maxAllowedForHint = maxMinutesForStart(start);
    const endPreview =
        start && minutes ? new Date(start.getTime() + minutes * 60000) : null;

    return (
        <form onSubmit={handleSubmit} className="reservation-form">
            {/* Startzeit */}
            <label htmlFor="startTime" style={{ display: "block", marginBottom: 4 }}>
                Beginn der Reservierung
            </label>
            <input
                id="startTime"
                type="datetime-local"
                value={startTime}
                min={nowLocalForMin}
                onChange={(e) => {
                    setStartTime(e.target.value);
                    setFormError("");
                }}
                required
            />
            <small style={{ display: "block", marginTop: 4, color: "#555" }}>
                Reservierungen sind nur bis <strong>22:00 Uhr</strong> am selben Tag
                möglich.
            </small>

            {/* Dauer */}
            <label htmlFor="duration" style={{ display: "block", margin: "12px 0 4px" }}>Dauer</label>
            <select
                id="duration"
                value={minutes}
                onChange={(e) => {
                    setMinutes(Number(e.target.value));
                    setFormError("");
                }}
                required
            >
                {renderDurationOptions()}
            </select>


            {/* Tischauswahl */}
            <label htmlFor="tableNumber" style={{ display: "block", margin: "12px 0 4px" }}>Tisch</label>
            <select
                id="tableNumber"
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                required
                disabled={maxAllowedForHint < MIN_MIN}
            >
                <option value="">Tisch auswählen…</option>
                {availableTables.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                        Tisch {t.tableNumber} ({t.numberOfSeats} Pers.)
                    </option>
                ))}
            </select>

            {/* Vorschau Endezeit */}
            {endPreview && (
                <small style={{ display: "block", marginTop: 6, color: "#555" }}>
                    Ende:{" "}
                    {endPreview.toLocaleTimeString("de-DE", {
                        hour: "2-digit",
                        minute: "2-digit",
                    })}{" "}
                    (spätestens 22:00 Uhr)
                </small>
            )}

            {/* Fehlermeldungen */}
            {formError && (
                <p style={{ color: "#c62828", marginTop: 10 }}>{formError}</p>
            )}

            <button
                type="submit"
                style={{ marginTop: 12 }}
                disabled={maxAllowedForHint < MIN_MIN}
            >
                Reservieren
            </button>
        </form>
    );
}

export default ReservationForm;