import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, useNavigate } from "react-router-dom";
import AdminPanel from "../pages/AdminPanel";

// Mock für useNavigate – wir überschreiben die Standardfunktion
jest.mock("react-router-dom", () => {
    const original = jest.requireActual("react-router-dom");
    return {
        ...original,
        useNavigate: jest.fn()
    };
});

describe("AdminPanel", () => {
    const mockNavigate = jest.fn();

    beforeEach(() => {
        // Vor jedem Test: useNavigate liefert unser mockNavigate zurück
        useNavigate.mockReturnValue(mockNavigate);
    });

    afterEach(() => {
        // Nach jedem Test: alle Mocks zurücksetzen
        jest.clearAllMocks();
    });

    test("zeigt Admin-Inhalte, wenn die Rolle ROLE_ADMIN ist", async () => {
        render(
            <MemoryRouter>
                <AdminPanel username="Maciej" role="ROLE_ADMIN" />
            </MemoryRouter>
        );

        // Wir warten auf die Darstellung des Inhalts
        await waitFor(() => {
            expect(screen.getByText("Admin Panel")).toBeInTheDocument();

            expect(
                screen.getByText((content, element) =>
                    element.textContent === "Angemeldet als: Maciej"
                )
            ).toBeInTheDocument();
        });
    });

    test("leitet weiter nach /myaccount, wenn Rolle nicht ROLE_ADMIN ist", () => {
        render(
            <MemoryRouter>
                <AdminPanel username="Maciej" role="ROLE_USER" />
            </MemoryRouter>
        );

        // Erwartung: automatische Weiterleitung
        expect(mockNavigate).toHaveBeenCalledWith("/myaccount");
    });
});