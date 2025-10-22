import { Link, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import '../styles/header.css';

function Header() {
    const location = useLocation();
    const [isLogged, setIsLogged] = useState(false);
    const [role, setRole] = useState(null);

    useEffect(() => {
        const check = async () => {
            try {
                const res = await fetch("http://localhost:8080/auth/auth_check", {
                    credentials: "include",
                });

                if (res.ok) {
                    const data = await res.json();
                    setIsLogged(true);
                    setRole(data.role);
                } else {
                    setIsLogged(false);
                    setRole(null);
                }
            } catch {
                setIsLogged(false);
                setRole(null);
            }
        };

        check();
    }, [location.pathname]);

    return (
        <header className="navbar">
            <div className="navbar-container">
                {/* Logo / Brand */}
                <div className="navbar-logo">
                    <Link to="/">Restaurant</Link>
                </div>

                {/* Links */}
                <nav className="navbar-links">
                    <Link to="/" className={location.pathname === "/" ? "active" : ""}>Home</Link>

                    {!isLogged && (
                        <>
                            <Link to="/register" className={location.pathname === "/register" ? "active" : ""}>
                                Register
                            </Link>
                            <Link to="/login" className={location.pathname === "/login" ? "active" : ""}>
                                Login
                            </Link>
                        </>
                    )}

                    {isLogged && role !== "ROLE_ADMIN" && (
                        <Link to="/myaccount" className={location.pathname === "/myaccount" ? "active" : ""}>
                            My Account
                        </Link>
                    )}

                    {isLogged && role === "ROLE_ADMIN" && (
                        <Link to="/admin" className={location.pathname === "/admin" ? "active" : ""}>
                            Admin Panel
                        </Link>
                    )}
                </nav>
            </div>
        </header>
    );
}

export default Header;