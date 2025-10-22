import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import '../styles/adminreservations.css';

function AdminReservations() {
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        fetch("http://localhost:8080/api/reservations/all", {
            credentials: "include",
        })
            .then(res => res.json())
            .then(data => setReservations(data || []))
            .catch(err => console.error("Fehler beim Abrufen der Reservierungen:", err))
            .finally(() => setLoading(false));
    }, []);

    const formatDateTime = (start, end) => {
        const startDate = new Date(start);
        const endDate = new Date(end);

        const d = startDate.toLocaleDateString("de-DE", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric"
        });

        const t1 = startDate.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" });
        const t2 = endDate.toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" });

        return `${d} – ${t1} bis ${t2} Uhr`;
    };
    const deleteReservation = (id) => {
        fetch(`http://localhost:8080/api/reservations/${id}`, {
            method: "DELETE",
            credentials: "include"
        })
            .then(() => setReservations(prev => prev.filter(r => r.id !== id)))
            .catch(err => console.error("Fehler beim Löschen der Reservierung:", err));
    };

    return (
        <main className="admin-resv-page" aria-label="Alle Reservierungen">
            <section className="admin-resv-card">
                <div className="admin-resv-header">
                    <button onClick={() => navigate("/admin")} className="back-button" type="button">
                        ← Zurück
                    </button>
                    <h1 className="admin-resv-title">Alle Reservierungen</h1>
                </div>

                {loading ? (
                    <div className="skeleton-list" aria-busy="true" aria-live="polite">
                        <div className="skeleton-row" />
                        <div className="skeleton-row" />
                        <div className="skeleton-row" />
                    </div>
                ) : reservations.length === 0 ? (
                    <p className="empty-state">Keine Reservierungen gefunden.</p>
                ) : (
                    <ul className="admin-reservation-list">
                        {reservations.map(res => (
                            <li key={res.id} className="res-row">
                                <div className="reservation-info">
                                    <div><strong>Name:</strong> {res.name}</div>
                                    <div><strong>Tisch:</strong> {res.table?.tableNumber ?? "?"}</div>
                                    <div><strong>Zeit:</strong> {formatDateTime(res.startTime, res.endTime)}</div>
                                </div>

                                <button
                                    type="button"
                                    className="delete-btn"
                                    onClick={() => deleteReservation(res.id)}
                                    aria-label={`Reservierung von ${res.name} löschen`}
                                >
                                    Löschen
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </main>
    );
}

export default AdminReservations;