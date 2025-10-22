import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Register from "../pages/Register";
import { BrowserRouter } from "react-router-dom";

// Wir mocken die globale fetch-Funktion, um API-Aufrufe zu simulieren
global.fetch = jest.fn();

// Hilfsfunktion zum Rendern mit Router-Kontext
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("Register-Komponente", () => {
    // Vor jedem Test setzen wir den fetch-Mock zurück
    beforeEach(() => {
        fetch.mockClear();
    });

    test("zeigt Eingabefelder und Button an", () => {
        renderWithRouter(<Register />);
        // Überprüft, ob die Eingabefelder und der Button sichtbar sind
        expect(screen.getByLabelText(/benutzername/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/passwort/i)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /registrieren/i })).toBeInTheDocument();
    });

    test("sendet Formular ab und navigiert bei Erfolg", async () => {
        // Simuliere eine erfolgreiche Registrierung (HTTP 200 OK)
        fetch.mockResolvedValueOnce({ ok: true });

        renderWithRouter(<Register />);

        // Fülle die Eingabefelder aus
        fireEvent.change(screen.getByLabelText(/benutzername/i), {
            target: { value: "maciej" },
        });
        fireEvent.change(screen.getByLabelText(/passwort/i), {
            target: { value: "secret" },
        });

        // Klicke auf den Button zum Registrieren
        fireEvent.click(screen.getByRole("button", { name: /registrieren/i }));

        // Warten auf den Aufruf der fetch-Funktion
        await waitFor(() => {
            expect(fetch).toHaveBeenCalledTimes(1);
        });
    });

    test("zeigt Fehlermeldung bei fehlgeschlagener Registrierung", async () => {
        // Simuliere eine fehlgeschlagene Registrierung mit Fehlermeldung
        fetch.mockResolvedValueOnce({
            ok: false,
            json: async () => ({ message: "Benutzer existiert bereits" }),
        });

        // Mock für window.alert
        window.alert = jest.fn();

        renderWithRouter(<Register />);

        // Eingabe von Benutzerdaten
        fireEvent.change(screen.getByLabelText(/benutzername/i), {
            target: { value: "maciej" },
        });
        fireEvent.change(screen.getByLabelText(/passwort/i), {
            target: { value: "secret" },
        });

        // Klicke auf Registrieren
        fireEvent.click(screen.getByRole("button", { name: /registrieren/i }));

        // Überprüfung, ob die Fehlermeldung angezeigt wird
        await waitFor(() => {
            expect(window.alert).toHaveBeenCalledWith("Benutzer existiert bereits");
        });
    });
});