import "../styles/loginAndRegister.css";
import { Link, useNavigate } from "react-router-dom";
import { useMemo, useState } from "react";

function Register() {
    const [fullName, setFullName] = useState("");
    const [email, setEmail]       = useState("");
    const [phone, setPhone]       = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const [showPwd, setShowPwd] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const navigate = useNavigate();
    const API = useMemo(() => process.env.REACT_APP_API_URL || "http://localhost:8080", []);

    const validEmail = (v) => /\S+@\S+\.\S+/.test(v);

    async function handleSubmit(e) {
        e.preventDefault();
        setErrorMsg("");

        // prosta walidacja frontowa
        if (!validEmail(email)) {
            setErrorMsg("Bitte eine gültige E-Mail-Adresse angeben.");
            return;
        }
        if (password.length < 6) {
            setErrorMsg("Passwort muss mindestens 6 Zeichen haben.");
            return;
        }
        if (username.trim().length < 3) {
            setErrorMsg("Benutzername muss mindestens 3 Zeichen haben.");
            return;
        }

        setSubmitting(true);

        try {
            const res = await fetch(`${API}/auth/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ username, password, fullName, email, phone }),
            });

            if (!res.ok) {
                // Backend może zwrócić JSON lub tekst
                let msg = "Registrierung fehlgeschlagen.";
                try {
                    const data = await res.json();
                    msg = data.message || msg;
                } catch {
                    const text = await res.text();
                    msg = text || msg;
                }
                throw new Error(msg);
            }

            // Odpowiedź z backendu po rejestracji: { username, fullName, email, phone } (wg naszej implementacji)
            const data = await res.json();

            // Zapis kontekstu użytkownika — JWT jest już w httpOnly cookie
            localStorage.setItem(
                "authUser",
                JSON.stringify({
                    username: data.username,
                    fullName: data.fullName,
                    email: data.email,
                    phone: data.phone,
                    role: "ROLE_USER", // domyślnie przy rejestracji
                })
            );

            navigate("/myaccount");
        } catch (err) {
            setErrorMsg(err.message || "Ein Fehler ist aufgetreten.");
        } finally {
            setSubmitting(false);
        }
    }

    const isDisabled =
        submitting ||
        !fullName.trim() ||
        !validEmail(email) ||
        !username.trim() ||
        password.length < 6;

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
                        <label htmlFor="fullName">Vollständiger Name</label>
                        <input
                            id="fullName"
                            type="text"
                            autoComplete="name"
                            value={fullName}
                            onChange={(e) => setFullName(e.target.value)}
                            minLength={2}
                            required
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="email">E-Mail</label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="phone">Telefon (optional)</label>
                        <input
                            id="phone"
                            type="tel"
                            autoComplete="tel"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            placeholder="+49 170 1234567"
                        />
                    </div>

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
                                onClick={() => setShowPwd((v) => !v)}
                                aria-label={showPwd ? "Passwort verbergen" : "Passwort anzeigen"}
                            >
                                {showPwd ? "Verbergen" : "Anzeigen"}
                            </button>
                        </div>
                    </div>

                    <button className="auth-btn" type="submit" disabled={isDisabled}>
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