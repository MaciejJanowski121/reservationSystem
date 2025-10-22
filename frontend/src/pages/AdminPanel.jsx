import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import LogoutButton from "../components/LogoutButton";
import '../styles/adminpanel.css';

// AdminPanel-Komponente – nur für Nutzer:innen mit ROLE_ADMIN zugänglich
function AdminPanel({ username, role }) {
    const navigate = useNavigate();

    useEffect(() => {
        // Wenn der Nutzer keine Admin-Rolle hat, zur normalen Benutzerseite umleiten
        if (role !== "ROLE_ADMIN") {
            navigate("/myaccount");
        }
    }, [role]);

    return (
        <div className="admin-container">
            <div className="admin-box">
                <h1>Admin Panel</h1>
                <p>Willkommen im Adminbereich.</p>
                <p>Angemeldet als: <strong>{username}</strong></p>

                <div className="admin-actions">
                    {/* Weiterleitung zur Reservierungsliste */}
                    <button
                        onClick={() => navigate("/admin/reservations")}
                        className="account-btn account-btn--primary"
                    >
                        Alle Reservierungen anzeigen
                    </button>

                    {/* Logout-Button mit rotem Stil */}
                    <LogoutButton className="account-btn account-btn--danger" />
                </div>
            </div>
        </div>
    );
}

export default AdminPanel;