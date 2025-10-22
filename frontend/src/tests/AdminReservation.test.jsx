import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import AdminReservations from "../pages/AdminReservations";
import { BrowserRouter } from "react-router-dom";

// Wir mocken den globalen fetch-Aufruf, um kontrollierte API-Antworten zu simulieren
global.fetch = jest.fn();

// Hilfsfunktion zum Rendern des Components mit React Router
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("AdminReservations", () => {
    // Vor jedem Test wird der fetch-Mock zurückgesetzt
    beforeEach(() => {
        fetch.mockClear();
    });

    test("zeigt eine Liste von Reservierungen an", async () => {
        // Wir simulieren eine erfolgreiche API-Antwort mit einer Testreservierung
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => [
                {
                    id: 1,
                    name: "Maciej Janowski",
                    reservationTime: "2025-06-08T20:30:00",
                    table: { tableNumber: 5 }
                }
            ]
        });

        // Komponente wird mit Router gerendert
        renderWithRouter(<AdminReservations />);

        // Überschrift wird sofort überprüft
        expect(screen.getByText(/Alle Reservierungen/i)).toBeInTheDocument();

        // Wir warten, bis die Daten geladen und dargestellt sind
        await waitFor(() => {
            // Name aus der Reservierung wird angezeigt
            expect(screen.getByText(/Maciej Janowski/i)).toBeInTheDocument();

            // Tisch-Nummer wird korrekt angezeigt
            expect(
                screen.getByText((_, node) => node.textContent === "Tisch: 5")
            ).toBeInTheDocument();
        });
    });
});