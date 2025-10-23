import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";

function ReservationForm({ setReservation }) {
    const [startTime, setStartTime] = useState("");  // "YYYY-MM-DDTHH:mm"
    const [minutes, setMinutes] = useState(120);     // 30..300
    const [tableNumber, setTableNumber] = useState("");
    const [availableTables, setAvailableTables] = useState([]);

    const navigate = useNavigate();
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);

    // Helper: Date -> "YYYY-MM-DDTHH:mm:ss"
    const toIsoWithSeconds = (date) => {
        const pad = (n) => (n < 10 ? "0" + n : n);
        return (
            date.getFullYear() +
            "-" + pad(date.getMonth() + 1) +
            "-" + pad(date.getDate()) +
            "T" + pad(date.getHours()) +
            ":" + pad(date.getMinutes()) +
            ":" + pad(date.getSeconds())
        );
    };

    const parseLocalDateTime = (value) => (value ? new Date(value + ":00") : null);

    // Fetch available tables for (start, minutes)
    useEffect(() => {
        const fetchAvailable = async () => {
            if (!startTime || !minutes) {
                setAvailableTables([]);
                return;
            }
            const start = parseLocalDateTime(startTime);
            if (!start) return;

            const startISO = toIsoWithSeconds(start);

            try {
                const res = await fetch(
                    `${API}/api/reservations/available?start=${encodeURIComponent(startISO)}&minutes=${minutes}`,
                    { credentials: "include" }
                );
                if (!res.ok) throw new Error("Error loading available tables");

                const data = await res.json();
                setAvailableTables(Array.isArray(data) ? data : []);
            } catch (e) {
                console.error(e);
                setAvailableTables([]);
            }
        };

        fetchAvailable();
    }, [startTime, minutes, API]);

    // Clear the selected table if it disappears from availability
    useEffect(() => {
        if (!tableNumber) return;
        const stillAvailable = availableTables.some(
            (t) => String(t.tableNumber) === String(tableNumber)
        );
        if (!stillAvailable) setTableNumber("");
    }, [tableNumber, availableTables]);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const start = parseLocalDateTime(startTime);
        if (!start) return alert("Wybierz godzinę rozpoczęcia.");
        if (start < new Date()) return alert("Nie możesz zarezerwować czasu w przeszłości.");
        if (!minutes || minutes < 30 || minutes > 300)
            return alert("Czas trwania musi być między 30 a 300 minut.");
        if (!tableNumber) return alert("Wybierz stolik.");

        const end = new Date(start.getTime() + minutes * 60 * 1000);

        // NOWY payload: bez name/email/phone i bez zagnieżdżonego 'reservation'
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

            if (!resp.ok) {
                const txt = await resp.text();
                throw new Error(txt || "Unknown error");
            }

            const data = await resp.json();
            setReservation([data]);  // dostosuj, jeśli oczekujesz innej struktury
            navigate("/reservations/my");
        } catch (err) {
            console.error(err);
            alert("Rezerwacja nie powiodła się: " + err.message);
        }
    };

    // Min dla datetime-local (teraz)
    const nowLocalForMin = useMemo(() => {
        const d = new Date();
        d.setSeconds(0, 0);
        const pad = (n) => (n < 10 ? "0" + n : n);
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }, []);

    return (
        <form onSubmit={handleSubmit} className="reservation-form">
            {/* Start time */}
            <input
                type="datetime-local"
                value={startTime}
                min={nowLocalForMin}
                onChange={(e) => setStartTime(e.target.value)}
                required
            />

            {/* Duration (30–300, step 30) */}
            <select
                value={minutes}
                onChange={(e) => setMinutes(Number(e.target.value))}
                required
            >
                {[...Array(10)].map((_, i) => {
                    const m = (i + 1) * 30;
                    const label = m < 60 ? `${m} min` : `${Math.floor(m / 60)} h${m % 60 ? ` ${m % 60} min` : ""}`;
                    return (
                        <option key={m} value={m}>
                            {label}
                        </option>
                    );
                })}
            </select>

            {/* Only available tables */}
            <select
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                required
            >
                <option value="">Wybierz stolik…</option>
                {availableTables.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                        Stolik {t.tableNumber} ({t.numberOfSeats} os.)
                    </option>
                ))}
            </select>

            <button type="submit">Zarezerwuj</button>
        </form>
    );
}

export default ReservationForm;