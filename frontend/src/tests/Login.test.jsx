import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Login from "../pages/Login";
import { BrowserRouter } from "react-router-dom";

// Wir mocken die Navigationsfunktion von react-router
const mockedUsedNavigate = jest.fn();

// Wir überschreiben useNavigate durch einen Mock
jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => mockedUsedNavigate,
}));

// Wir mocken fetch, um API-Aufrufe zu simulieren
global.fetch = jest.fn();

// Hilfsfunktion zum Rendern mit Router
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("Login-Komponente", () => {
    // Vor jedem Test setzen wir die Mocks zurück
    beforeEach(() => {
        fetch.mockClear();
        mockedUsedNavigate.mockClear();
    });

    test("zeigt Eingabefelder und Login-Button an", () => {
        renderWithRouter(<Login />);
        expect(screen.getByLabelText(/benutzername/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/passwort/i)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /login/i })).toBeInTheDocument();
    });

    test("aktualisiert Eingabewerte bei Eingabe", () => {
        renderWithRouter(<Login />);
        const usernameInput = screen.getByLabelText(/benutzername/i);
        const passwordInput = screen.getByLabelText(/passwort/i);

        fireEvent.change(usernameInput, { target: { value: "testuser" } });
        fireEvent.change(passwordInput, { target: { value: "secret" } });

        expect(usernameInput.value).toBe("testuser");
        expect(passwordInput.value).toBe("secret");
    });

    test("sendet Formular und navigiert zu /admin bei ROLE_ADMIN", async () => {
        // Simulierte API-Antwort für Admin
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ username: "adminuser", role: "ROLE_ADMIN" }),
        });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/benutzername/i), {
            target: { value: "adminuser" },
        });
        fireEvent.change(screen.getByLabelText(/passwort/i), {
            target: { value: "adminpass" },
        });

        fireEvent.click(screen.getByRole("button", { name: /login/i }));

        await waitFor(() => {
            // Überprüfung des fetch-Aufrufs
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/auth/login",
                expect.objectContaining({
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ username: "adminuser", password: "adminpass" }),
                })
            );

            // Admin wird weitergeleitet
            expect(mockedUsedNavigate).toHaveBeenCalledWith("/admin");
        });
    });

    test("sendet Formular und navigiert zu /myaccount bei ROLE_USER", async () => {
        // Simulierte API-Antwort für normalen Benutzer
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ username: "regularuser", role: "ROLE_USER" }),
        });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/benutzername/i), {
            target: { value: "regularuser" },
        });
        fireEvent.change(screen.getByLabelText(/passwort/i), {
            target: { value: "userpass" },
        });

        fireEvent.click(screen.getByRole("button", { name: /login/i }));

        await waitFor(() => {
            // Benutzer wird weitergeleitet
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/auth/login",
                expect.objectContaining({
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ username: "regularuser", password: "userpass" }),
                })
            );

            expect(mockedUsedNavigate).toHaveBeenCalledWith("/myaccount");
        });
    });

    test("zeigt alert bei fehlgeschlagenem Login", async () => {
        const errorText = "Invalid credentials";
        fetch.mockResolvedValueOnce({
            ok: false,
            text: async () => errorText,
        });

        // Wir mocken window.alert, um die Anzeige zu überprüfen
        window.alert = jest.fn();

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/benutzername/i), {
            target: { value: "failuser" },
        });
        fireEvent.change(screen.getByLabelText(/passwort/i), {
            target: { value: "failpass" },
        });

        fireEvent.click(screen.getByRole("button", { name: /login/i }));

        await waitFor(() => {
            // Überprüfung, ob alert aufgerufen wurde
            expect(window.alert).toHaveBeenCalledWith(errorText);
        });
    });
});