import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";

function ReservationForm({ setReservation }) {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
    const [startTime, setStartTime] = useState("");     // "YYYY-MM-DDTHH:mm"
    const [minutes, setMinutes] = useState(120);        // default 2h
    const [tableNumber, setTableNumber] = useState("");
    const [availableTables, setAvailableTables] = useState([]);

    const navigate = useNavigate();
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);

    // Helper: convert Date → "YYYY-MM-DDTHH:mm:ss"
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

    // Parse datetime-local input → Date (adds :00 seconds)
    const parseLocalDateTime = (value) => (value ? new Date(value + ":00") : null);

    // 1️Fetch available tables for (start, minutes)
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

    // 2️⃣ Clear selected table if it's no longer available
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
        if (!start) {
            alert("Please select a start time.");
            return;
        }
        if (start < new Date()) {
            alert(" You cannot book a reservation in the past!");
            return;
        }
        if (!minutes || minutes < 30 || minutes > 300) {
            alert("Duration must be between 30 and 300 minutes.");
            return;
        }
        if (!tableNumber) {
            alert("Please select a table.");
            return;
        }

        const end = new Date(start.getTime() + minutes * 60 * 1000);
        const payload = {
            tableNumber: Number(tableNumber),
            reservation: {
                name,
                email,
                phone,
                startTime: toIsoWithSeconds(start),
                endTime: toIsoWithSeconds(end),
            },
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
            setReservation([data]);
            navigate("/reservations/my");
        } catch (err) {
            console.error(err);
            alert(" Reservation failed: " + err.message);
        }
    };

    // Minimum date/time for input (current moment, rounded to minutes)
    const nowLocalForMin = useMemo(() => {
        const d = new Date();
        d.setSeconds(0, 0);
        const pad = (n) => (n < 10 ? "0" + n : n);
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }, []);

    return (
        <form onSubmit={handleSubmit} className="reservation-form">
            <input
                type="text"
                placeholder="Name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
            />

            <input
                type="email"
                placeholder="E-Mail"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
            />

            <input
                type="tel"
                placeholder="Phone number"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                required
            />

            {/* Start time */}
            <input
                type="datetime-local"
                value={startTime}
                min={nowLocalForMin}
                onChange={(e) => setStartTime(e.target.value)}
                required
            />

            {/* Duration (30–300 minutes, step 30) */}
            <select
                value={minutes}
                onChange={(e) => setMinutes(Number(e.target.value))}
                required
            >
                {[...Array(10)].map((_, i) => {
                    const m = (i + 1) * 30; // 30, 60, …, 300
                    const label =
                        m < 60 ? `${m} minutes` : `${Math.floor(m / 60)} h${m % 60 ? ` ${m % 60} min` : ""}`;
                    return (
                        <option key={m} value={m}>
                            {label}
                        </option>
                    );
                })}
            </select>

            {/* Available tables only */}
            <select
                value={tableNumber}
                onChange={(e) => setTableNumber(e.target.value)}
                required
            >
                <option value="">Select a table...</option>
                {availableTables.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                        Table {t.tableNumber} ({t.numberOfSeats} seats)
                    </option>
                ))}
            </select>

            <button type="submit">Reserve</button>
        </form>
    );
}

export default ReservationForm;