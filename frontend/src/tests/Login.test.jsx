import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import Login from "../pages/Login";
import { BrowserRouter } from "react-router-dom";

const mockedUsedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => mockedUsedNavigate,
}));

global.fetch = jest.fn();

const renderWithRouter = (ui) => render(<BrowserRouter>{ui}</BrowserRouter>);

describe("Login", () => {
    beforeEach(() => {
        fetch.mockClear();
        mockedUsedNavigate.mockClear();
    });

    test(" rendert Felder und buttons ", () => {
        renderWithRouter(<Login />);
        expect(screen.getByLabelText(/Benutzername/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/^Passwort$/i, { selector: "input" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: /Login/i })).toBeInTheDocument();
    });

    test("Aktualisiert felder", () => {
        renderWithRouter(<Login />);
        const usernameInput = screen.getByLabelText(/Benutzername/i);
        const passwordInput = screen.getByLabelText(/^Passwort$/i, { selector: "input" });

        fireEvent.change(usernameInput, { target: { value: "testuser" } });
        fireEvent.change(passwordInput, { target: { value: "secret" } });

        expect(usernameInput.value).toBe("testuser");
        expect(passwordInput.value).toBe("secret");
    });

    test("sumbit und navigation zum /admin mit ROLE_ADMIN", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ username: "adminuser", role: "ROLE_ADMIN" }),
        });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/Benutzername/i), { target: { value: "adminuser" } });
        fireEvent.change(screen.getByLabelText(/^Passwort$/i, { selector: "input" }), { target: { value: "adminpass" } });

        fireEvent.click(screen.getByRole("button", { name: /Login/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalledWith(
                "http://localhost:8080/auth/login",
                expect.objectContaining({
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({ username: "adminuser", password: "adminpass" }),
                })
            );
            expect(mockedUsedNavigate).toHaveBeenCalledWith("/admin");
        });
    });

    test("submit i nawigacja do /myaccount przy ROLE_USER", async () => {
        fetch.mockResolvedValueOnce({
            ok: true,
            json: async () => ({ username: "regularuser", role: "ROLE_USER" }),
        });

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/Benutzername/i), { target: { value: "regularuser" } });
        fireEvent.change(screen.getByLabelText(/^Passwort$/i, { selector: "input" }), { target: { value: "userpass" } });

        fireEvent.click(screen.getByRole("button", { name: /Login/i }));

        await waitFor(() => {
            expect(fetch).toHaveBeenCalled();
            expect(mockedUsedNavigate).toHaveBeenCalledWith("/myaccount");
        });
    });

    test("zeigt alert wenn falsche anmeldung", async () => {
        const errorText = "Invalid credentials";
        fetch.mockResolvedValueOnce({ ok: false, text: async () => errorText });
        window.alert = jest.fn();

        renderWithRouter(<Login />);

        fireEvent.change(screen.getByLabelText(/Benutzername/i), { target: { value: "failuser" } });
        fireEvent.change(screen.getByLabelText(/^Passwort$/i, { selector: "input" }), { target: { value: "failpass" } });

        fireEvent.click(screen.getByRole("button", { name: /Login/i }));

        await waitFor(() => {
            expect(screen.getByRole("alert")).toHaveTextContent("Invalid credentials");
        });
    });
});