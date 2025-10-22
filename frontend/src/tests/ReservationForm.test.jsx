import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ReservationForm from "../components/ReservationForm"; // Komponente zum Testen
import { BrowserRouter } from "react-router-dom";

// Hilfsfunktion für das Rendern mit Routing-Kontext
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("ReservationForm", () => {
    beforeEach(() => {
        // Vor jedem Test die globale fetch-Funktion mocken
        global.fetch = jest.fn();
    });

    afterEach(() => {
        // Nach jedem Test alle Mocks zurücksetzen
        jest.resetAllMocks();
    });

    test("sendet Reservierung ab und setzt Formular zurück", async () => {
        // Testdaten für eine Beispielreservierung
        const mockReservation = {
            id: 1,
            name: "Maciej",
            email: "maciej@example.com",
            phone: "123456789",
            reservationTime: "2025-06-10T18:00",
            table: { tableNumber: 3 }
        };

        // Mock-Funktion für das Setzen der Reservierung
        const mockSetReservation = jest.fn();

        // Simuliere erfolgreichen fetch-Aufruf mit Rückgabe der Reservierung
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => mockReservation
        });

        // Komponente rendern mit übergebener Callback-Funktion
        renderWithRouter(<ReservationForm setReservation={mockSetReservation} />);

        // Felder mit Beispieldaten ausfüllen
        fireEvent.change(screen.getByPlaceholderText("Name"), {
            target: { value: mockReservation.name }
        });
        fireEvent.change(screen.getByPlaceholderText("E-Mail"), {
            target: { value: mockReservation.email }
        });
        fireEvent.change(screen.getByPlaceholderText("Telefonnummer"), {
            target: { value: mockReservation.phone }
        });
        fireEvent.change(screen.getByRole("combobox"), {
            target: { value: String(mockReservation.table.tableNumber) }
        });
        fireEvent.change(screen.getByDisplayValue(""), {
            target: { value: mockReservation.reservationTime }
        });

        // Klick auf den Button zum Absenden des Formulars
        fireEvent.click(screen.getByRole("button", { name: /reservieren/i }));

        // Warten, bis fetch aufgerufen und die Callback-Funktion ausgeführt wurde
        await waitFor(() => {
            // Überprüfung des fetch-Aufrufs mit korrektem Payload
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/api/reservations",
                expect.objectContaining({
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({
                        tableNumber: String(mockReservation.table.tableNumber),
                        reservation: {
                            name: mockReservation.name,
                            email: mockReservation.email,
                            phone: mockReservation.phone,
                            reservationTime: mockReservation.reservationTime
                        }
                    })
                })
            );

            // Überprüfung, ob die Callback-Funktion mit den Daten aufgerufen wurde
            expect(mockSetReservation).toHaveBeenCalledWith([mockReservation]);
        });
    });
});