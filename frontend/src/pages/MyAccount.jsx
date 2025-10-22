import '../styles/myaccount.css';
import { Link } from "react-router-dom";
import LogoutButton from "../components/LogoutButton";

// Benutzerbereich für eingeloggte Nutzer:innen (nicht für Admins)
function MyAccount({ username, role }) {
    return (
        <div className="account-container">
            <div className="account-box">
                {/* Kopfbereich mit Avatar und Begrüßung */}
                <div className="account-header">
                    <div className="account-avatar">
                        {username?.charAt(0).toUpperCase()}
                    </div>
                    <h1>{username}</h1>
                    <p className="account-subtitle">
                        Willkommen in deinem Benutzerbereich
                    </p>
                </div>

                {/* Nur anzeigen, wenn keine Admin-Rolle */}
                {role !== "ROLE_ADMIN" && (
                    <div className="account-actions">
                        {/* Link zur Seite für neue Reservierung */}
                        <Link to="/reservations/new" className="account-btn account-btn--gold">
                            Neue Reservierung
                        </Link>

                        {/* Link zur Übersicht eigener Reservierungen */}
                        <Link to="/reservations/my" className="account-btn account-btn--blue">
                            Meine Reservierungen
                        </Link>

                        {/* Link zum Ändern des Passworts */}
                        <Link to="/changePassword" className="account-btn account-btn--blue">
                            Passwort ändern
                        </Link>

                        {/* Logout-Button */}
                        <LogoutButton className="account-btn account-btn--danger" />
                    </div>
                )}
            </div>
        </div>
    );
}

export default MyAccount;