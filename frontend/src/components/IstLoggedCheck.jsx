import { useEffect, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";

// Komponente zur Prüfung, ob der Benutzer bereits eingeloggt ist
// Wenn ja, erfolgt eine Weiterleitung zur /myaccount-Seite
// Wenn nicht, wird das übergebene Kind-Element (z. B. Login-Formular) gerendert

export default function IstLoggedCheck({ children }) {
    const [isLogged, setIsLogged] = useState(false); // Status: eingeloggt oder nicht
    const [isLoading, setIsLoading] = useState(true); // Wird geprüft?

    const navigate = useNavigate();

    useEffect(() => {
        const checkAuth = async () => {
            try {
                // Anfrage zur Authentifizierungsprüfung
                const res = await fetch("http://localhost:8080/auth/auth_check", {
                    method: "GET",
                    credentials: "include", // JWT-Cookie wird mitgeschickt
                });

                if (res.ok) {
                    setIsLogged(true); // Benutzer ist eingeloggt
                } else {
                    setIsLogged(false); // Benutzer ist nicht eingeloggt
                }
            } catch (error) {
                setIsLogged(false); // Bei Fehler: als nicht eingeloggt behandeln
            } finally {
                setIsLoading(false); // Prüfung abgeschlossen
            }
        };

        checkAuth();
    }, []);

    // Solange geprüft wird: Ladehinweis anzeigen
    if (isLoading) {
        return <p>Anmeldung wird überprüft...</p>;
    }

    // Wenn eingeloggt → weiterleiten zu /myaccount
    if (isLogged) {
        return <Navigate to="/myaccount" />;
    }

    // Wenn nicht eingeloggt → ursprüngliche Komponente rendern
    return children;
}