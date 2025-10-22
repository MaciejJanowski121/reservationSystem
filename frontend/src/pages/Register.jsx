import '../styles/loginAndRegister.css';
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";

function Register() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPwd, setShowPwd] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    const navigate = useNavigate();

    async function handleSubmit(e) {
        e.preventDefault();
        setErrorMsg("");
        setSubmitting(true);

        try {
            const res = await fetch("http://localhost:8080/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ username, password }),
            });

            if (res.ok) {
                navigate("/myaccount");
            } else {
                // Backend może zwrócić JSON lub tekst
                let msg = "Registrierung fehlgeschlagen.";
                try {
                    const data = await res.json();
                    msg = data.message || msg;
                } catch {
                    const text = await res.text();
                    msg = text || msg;
                }
                setErrorMsg(msg);
            }
        } catch (err) {
            setErrorMsg(err.message || "Ein Fehler ist aufgetreten.");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <main className="auth-page" aria-label="Registrierung">
            <section className="auth-card" aria-labelledby="register-title">
                <h1 id="register-title" className="auth-title">Registrieren</h1>

                {errorMsg && (
                    <div className="auth-alert" role="alert">
                        {errorMsg}
                    </div>
                )}

                <form className="auth-form" onSubmit={handleSubmit} noValidate>
                    <div className="field">
                        <label htmlFor="username">Benutzername</label>
                        <input
                            id="username"
                            type="text"
                            autoComplete="username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            minLength={3}
                            required
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="password">Passwort</label>
                        <div className="pwd-wrap">
                            <input
                                id="password"
                                type={showPwd ? "text" : "password"}
                                autoComplete="new-password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                minLength={6}
                                required
                            />
                            <button
                                type="button"
                                className="pwd-toggle"
                                onClick={() => setShowPwd(v => !v)}
                                aria-label={showPwd ? "Passwort verbergen" : "Passwort anzeigen"}
                            >
                                {showPwd ? "Verbergen" : "Anzeigen"}
                            </button>
                        </div>
                    </div>

                    <button className="auth-btn" type="submit" disabled={submitting}>
                        {submitting ? <span className="spinner" aria-hidden="true" /> : null}
                        {submitting ? "Wird registriert…" : "Registrieren"}
                    </button>
                </form>

                <p className="auth-meta">
                    Bereits ein Konto?{" "}
                    <Link to="/login" className="auth-link">Jetzt einloggen</Link>
                </p>
            </section>
        </main>
    );
}

export default Register;