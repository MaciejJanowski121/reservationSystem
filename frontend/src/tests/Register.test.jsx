import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Register from "../pages/Register";
import { BrowserRouter } from "react-router-dom";

global.fetch = jest.fn();
const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("Register", () => {
    beforeEach(() => fetch.mockClear());

    test("sendet formular und zeigt succes", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({
                username: "maciej",
                fullName: "Maciej Janowski",
                email: "maciej@example.com",
                phone: "+491701234567",
            }),
        });

        renderWithRouter(<Register />);

        fireEvent.change(screen.getByLabelText(/Vollständiger Name/i), { target: { value: "Maciej Janowski" } });
        fireEvent.change(screen.getByLabelText(/^E-Mail$/i), { target: { value: "maciej@example.com" } });
        fireEvent.change(screen.getByLabelText(/Telefon/i), { target: { value: "+491701234567" } });
        fireEvent.change(screen.getByLabelText(/Benutzername/i), { target: { value: "maciej" } });
        fireEvent.change(screen.getByLabelText(/^Passwort$/i), { target: { value: "secret1" } });

        fireEvent.click(screen.getByRole("button", { name: /Registrieren/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/auth/register",
                expect.objectContaining({
                    body: JSON.stringify({
                        username: "maciej",
                        password: "secret1",
                        fullName: "Maciej Janowski",
                        email: "maciej@example.com",
                        phone: "+491701234567",
                    }),
                })
            );
            expect(screen.getByRole("status")).toHaveTextContent(/Registrierung erfolgreich/i);
        });
    });
});