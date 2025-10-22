import { useState, useEffect } from "react";
import '../styles/reservations.css';
import { useNavigate } from "react-router-dom";

function Reservations() {
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchReservations = async () => {
            try {
                const res = await fetch("http://localhost:8080/api/reservations/userReservations", {
                    credentials: "include"
                });
                if (!res.ok) throw new Error(`HTTP-Fehler: ${res.status}`);

                const text = await res.text();
                if (text) {
                    const data = JSON.parse(text);
                    setReservations(data.id ? [data] : []);
                } else {
                    setReservations([]);
                }
            } catch (err) {
                console.error("Fehler beim Laden:", err);
                setReservations([]);
            } finally {
                setLoading(false);
            }
        };

        fetchReservations();
    }, []);

    const deleteReservation = async (id) => {
        try {
            const res = await fetch(`http://localhost:8080/api/reservations/${id}`, {
                method: "DELETE",
                credentials: "include"
            });
            if (!res.ok) throw new Error(`Löschen fehlgeschlagen: ${res.status}`);
            setReservations([]);
        } catch (err) {
            console.error("Fehler beim Löschen:", err);
        }
    };

    return (
        <main className="reservations-page" aria-label="Deine Reservierungen">
            <section className="reservations-container">
                <div className="reservations-header">
                    <button
                        onClick={() => navigate("/myaccount")}
                        className="reservations-back-button"
                        type="button"
                    >
                        ← Zurück
                    </button>
                    <h1>Deine Reservierungen</h1>
                </div>

                {loading ? (
                    <div className="skeleton-list" aria-busy="true" aria-live="polite">
                        <div className="skeleton-row" />
                        <div className="skeleton-row" />
                    </div>
                ) : reservations.length === 0 ? (
                    <p className="empty-state">Keine Reservierungen vorhanden.</p>
                ) : (
                    <ul className="reservation-list">
                        {reservations.map((r) => {
                            const start = r.startTime ? new Date(r.startTime) : null;
                            const end = r.endTime ? new Date(r.endTime) : null;

                            const dateStr = start
                                ? start.toLocaleDateString("de-DE")
                                : "Unbekannt";
                            const timeStr = start && end
                                ? `${start.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })} – ${end.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })}`
                                : "Unbekannt";

                            return (
                                <li key={r.id} className="res-card">
                                    <div className="reservation-info">
                                        <div><strong>Name:</strong> {r.name}</div>
                                        <div><strong>Tischnummer:</strong> {r.table?.tableNumber ?? "?"}</div>
                                        <div><strong>Datum:</strong> {dateStr}</div>
                                        <div><strong>Uhrzeit:</strong> {timeStr}</div>
                                    </div>
                                    <button
                                        className="delete-btn"
                                        type="button"
                                        onClick={() => deleteReservation(r.id)}
                                        aria-label={`Reservierung von ${r.name} löschen`}
                                    >
                                        Löschen
                                    </button>
                                </li>
                            );
                        })}
                    </ul>
                )}
            </section>
        </main>
    );
}

export default Reservations;