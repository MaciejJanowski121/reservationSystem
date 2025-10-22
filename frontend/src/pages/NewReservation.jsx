import ReservationForm from "../components/ReservationForm";
import '../styles/newreservation.css';
import { useNavigate } from "react-router-dom";

// Seite zum Erstellen einer neuen Tischreservierung
function NewReservation() {
    const navigate = useNavigate();

    return (
        <main className="newreservation-page">
            <section className="newreservation-container">
                {/* Zurück-Button zum Benutzerbereich */}
                <div className="newreservation-header">
                    <button
                        onClick={() => navigate("/myaccount")}
                        className="back-button"
                        type="button"
                    >
                        ← Zurück
                    </button>
                    <h1>Neue Reservierung</h1>
                </div>

                {/* Formular für die Reservierung */}
                <ReservationForm setReservation={() => {}} />
                {/* Hinweis: setReservation hier leer – wird nur benötigt, wenn man nach dem Anlegen etwas anzeigen will */}
            </section>
        </main>
    );
}

export default NewReservation;