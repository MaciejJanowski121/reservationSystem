import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

function ReservationForm({ setReservation }) {
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
    const [startTime, setStartTime] = useState("");
    const [tableNumber, setTableNumber] = useState("");
    const [availableTables, setAvailableTables] = useState([]);

    const navigate = useNavigate();

    // 🔹 helper: lokalny czas (bez UTC przesunięć)
    const formatLocalDateTime = (date) => {
        const pad = (n) => (n < 10 ? "0" + n : n);
        return (
            date.getFullYear() +
            "-" +
            pad(date.getMonth() + 1) +
            "-" +
            pad(date.getDate()) +
            "T" +
            pad(date.getHours()) +
            ":" +
            pad(date.getMinutes()) +
            ":" +
            pad(date.getSeconds())
        );
    };

    // 🔹 pobieranie dostępnych stolików (zawsze 2h)
    useEffect(() => {
        const fetchAvailableTables = async () => {
            if (!startTime) return;

            const start = new Date(startTime);

            try {
                const res = await fetch(
                    `http://localhost:8080/api/reservations/available?start=${formatLocalDateTime(start)}`,
                    { credentials: "include" }
                );
                if (!res.ok) throw new Error("Fehler beim Laden der verfügbaren Tische");

                const data = await res.json();
                setAvailableTables(data);
            } catch (err) {
                console.error("Fehler beim Abrufen:", err);
                setAvailableTables([]);
            }
        };

        fetchAvailableTables();
    }, [startTime]);

    const handleSubmit = async (e) => {
        e.preventDefault();

        const start = new Date(startTime);
        if (start < new Date()) {
            alert("❌ Du kannst keine Reservierung in der Vergangenheit anlegen!");
            return;
        }

        const newReservation = {
            tableNumber,
            reservation: { name, email, phone, startTime }
            // ⬆️ backend liczy endTime = start + 2h
        };

        try {
            const response = await fetch("http://localhost:8080/api/reservations", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify(newReservation),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error("Fehler beim Senden der Reservierung: " + errorText);
            }

            const data = await response.json();
            setReservation([data]);
            navigate("/reservations/my");
        } catch (err) {
            console.error("Fehler beim Hinzufügen:", err);
            alert("❌ Reservierung fehlgeschlagen: " + err.message);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="reservation-form">
            <input type="text" placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} required />
            <input type="email" placeholder="E-Mail" value={email} onChange={(e) => setEmail(e.target.value)} required />
            <input type="tel" placeholder="Telefonnummer" value={phone} onChange={(e) => setPhone(e.target.value)} required />

            {/* 🔹 Startzeit = wybór daty i godziny */}
            <input
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
            />

            {/* 🔹 tylko dostępne stoliki */}
            <select value={tableNumber} onChange={(e) => setTableNumber(e.target.value)} required>
                <option value="">Wähle einen Tisch...</option>
                {availableTables.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                        Tisch {t.tableNumber} ({t.numberOfSeats} Personen)
                    </option>
                ))}
            </select>

            <button type="submit">Reservieren</button>
        </form>
    );
}

export default ReservationForm;