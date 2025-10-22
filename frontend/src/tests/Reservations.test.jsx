import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import Reservations from "../pages/Reservations";
import { BrowserRouter } from "react-router-dom";

// Globale fetch-Funktion mocken
global.fetch = jest.fn();

// Hilfsfunktion zum Rendern mit Routing-Kontext
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("Reservations", () => {
    beforeEach(() => {
        // Vor jedem Test den Mock zurücksetzen
        fetch.mockClear();
    });

    test("zeigt eine Benutzerreservierung an", async () => {
        // Mock-Antwort für den GET-Request
        fetch.mockResolvedValueOnce({
            ok: true,
            text: async () =>
                JSON.stringify({
                    id: 1,
                    name: "Maciej",
                    reservationTime: "2025-06-09T18:00:00",
                    table: { tableNumber: 7 }
                }),
        });

        renderWithRouter(<Reservations />);

        // Warten, bis die Reservation gerendert ist
        await waitFor(() => {
            expect(screen.getByText(/Maciej/)).toBeInTheDocument();
            expect(
                screen.getByText((_, node) => node.textContent === "Tischnummer: 7")
            ).toBeInTheDocument();
        });
    });

    test("ruft DELETE-API auf, wenn Löschen-Button geklickt wird", async () => {
        // Erste Mock-Antwort: GET für die Anzeige einer Reservation
        fetch.mockResolvedValueOnce({
            ok: true,
            text: async () =>
                JSON.stringify({
                    id: 2,
                    name: "Anna",
                    reservationTime: "2025-06-10T19:00:00",
                    table: { tableNumber: 3 }
                }),
        });

        // Zweite Mock-Antwort: DELETE für das Löschen
        fetch.mockResolvedValueOnce({
            ok: true,
        });

        renderWithRouter(<Reservations />);

        // Sicherstellen, dass der Benutzer angezeigt wird
        await waitFor(() => {
            expect(screen.getByText(/Anna/)).toBeInTheDocument();
        });

        // Löschen-Button finden und klicken
        const deleteButton = screen.getByRole("button", { name: "X" });
        fireEvent.click(deleteButton);

        // Sicherstellen, dass DELETE-Request korrekt abgesetzt wurde
        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/api/reservations/2",
                expect.objectContaining({
                    method: "DELETE",
                    credentials: "include"
                })
            );
        });
    });
});