import '../styles/loginAndRegister.css';
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";

function Login() {
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
            const res = await fetch("http://localhost:8080/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ username, password }),
            });

            if (res.ok) {
                const data = await res.json(); // { username, role }
                if (data.role === "ROLE_ADMIN") navigate("/admin");
                else navigate("/myaccount");
            } else {
                const text = await res.text();
                setErrorMsg(text || "Login fehlgeschlagen.");
            }
        } catch (err) {
            setErrorMsg(err.message || "Ein Fehler ist aufgetreten.");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <main className="auth-page" aria-label="Login">
            <section className="auth-card" aria-labelledby="login-title">
                <h1 id="login-title" className="auth-title">Login</h1>

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
                            required
                            aria-invalid={Boolean(errorMsg)}
                        />
                    </div>

                    <div className="field">
                        <label htmlFor="password">Passwort</label>
                        <div className="pwd-wrap">
                            <input
                                id="password"
                                type={showPwd ? "text" : "password"}
                                autoComplete="current-password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                aria-invalid={Boolean(errorMsg)}
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

                    <button className="auth-btn" type="submit" disabled={submitting}>
                        {submitting ? <span className="spinner" aria-hidden="true" /> : null}
                        {submitting ? "Wird eingeloggt…" : "Login"}
                    </button>
                </form>

                <p className="auth-meta">
                    Noch kein Konto?{" "}
                    <Link to="/register" className="auth-link">Jetzt registrieren</Link>
                </p>
            </section>
        </main>
    );
}

export default Login;