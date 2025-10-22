import { useNavigate } from "react-router-dom";

function LogoutButton({ className }) {
    const navigate = useNavigate(); // Navigation nach dem Logout

    // Funktion zum Abmelden des Benutzers
    const handleLogout = async () => {
        try {
            // Anfrage an die Logout-Route im Backend
            const res = await fetch("http://localhost:8080/auth/logout", {
                method: "POST",
                credentials: "include", // Cookie mit JWT wird mitgesendet
            });

            if (res.ok) {
                // Erfolgreicher Logout → Weiterleitung zur Login-Seite
                navigate("/login");
            } else {
                alert("Logout fehlgeschlagen");
            }
        } catch (err) {
            console.error(err);
        }
    };

    return (
        // Button zum Ausloggen, Klasse wird per Prop übergeben
        <button onClick={handleLogout} className={className}>
            Logout
        </button>
    );
}

export default LogoutButton;