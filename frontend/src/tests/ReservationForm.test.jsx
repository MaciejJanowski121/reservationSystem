import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import ReservationForm from "../components/ReservationForm";

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

test("zeigt error wenn die reservierung nich akzeptiert wird", async () => {
    global.fetch = jest.fn()
        .mockResolvedValueOnce({
            ok: true,
            json: async () => [{ id: 10, tableNumber: 3, numberOfSeats: 2 }],
        })
        .mockResolvedValueOnce({
            ok: false,
            text: async () => "Kapazität überschritten",
        });

    const setReservation = jest.fn();
    renderWithRouter(<ReservationForm setReservation={setReservation} />);

    fireEvent.change(screen.getByLabelText(/Beginn der Reservierung/i), {
        target: { value: "2025-10-30T18:00" },
    });
    fireEvent.change(screen.getByLabelText(/Dauer/i), {
        target: { value: "120" },
    });

    const tableSelect = screen.getByLabelText(/Tisch/i);
    await waitFor(() =>
        expect(
            within(tableSelect).getByRole("option", { name: /Tisch\s*3/i })
        ).toBeInTheDocument()
    );
    fireEvent.change(tableSelect, { target: { value: "3" } });

    fireEvent.click(screen.getByRole("button", { name: /Reservieren/i }));

    await waitFor(() => {
        expect(fetch).toHaveBeenCalledTimes(2);
        expect(fetch).toHaveBeenCalledWith(
            expect.stringMatching(/\/api\/reservations$/),
            expect.objectContaining({ method: "POST" })
        );
        expect(screen.getByText(/Reservierung fehlgeschlagen/i)).toBeInTheDocument();
        expect(screen.getByText(/Kapazität überschritten/i)).toBeInTheDocument();
    });
});