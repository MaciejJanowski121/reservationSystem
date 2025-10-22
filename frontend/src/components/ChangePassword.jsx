import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import '../styles/changepassword.css';

function ChangePassword() {
    const navigate = useNavigate();

    // Zustände für das alte und neue Passwort
    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");

    // Funktion zum Absenden des Passwortänderungsformulars
    const handlePasswordChange = async (e) => {
        e.preventDefault();

        try {
            // Anfrage an das Backend zur Passwortänderung
            const response = await fetch("http://localhost:8080/user/change-password", {
                method: "PUT",
                credentials: "include", // Cookie (JWT) wird mitgesendet
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    oldPassword: oldPassword,
                    newPassword: newPassword
                })
            });

            if (response.ok) {
                console.log("Passwort wurde geändert");
                // Nach erfolgreicher Änderung zur Benutzerseite navigieren
                navigate("/myaccount");
            } else {
                console.log("Passwort wurde nicht geändert – ein Fehler ist aufgetreten");
            }
        } catch (error) {
            console.error("Fehler beim Ändern des Passworts:", error);
        }
    };

    return (
        <div className="password-change-container">
            <form className="password-change-form" onSubmit={handlePasswordChange}>
                {/* Zurück-Button zur vorherigen Seite */}
                <button
                    type="button"
                    onClick={() => navigate("/myaccount")}
                    className="back-button"
                >
                    ← Zurück
                </button>

                <h1>Passwort Ändern</h1>

                {/* Eingabefeld für das alte Passwort */}
                <input
                    type="password"
                    placeholder="Altes Passwort"
                    onChange={(e) => setOldPassword(e.target.value)}
                    required
                />

                {/* Eingabefeld für das neue Passwort */}
                <input
                    type="password"
                    placeholder="Neues Passwort"
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                />

                {/* Absenden des Formulars */}
                <button type="submit" className="password-change">Passwort Ändern</button>
            </form>
        </div>
    );
}

export default ChangePassword;