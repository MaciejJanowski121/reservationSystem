import { render, screen } from '@testing-library/react';
import App from './App';

test('renders welcome message', () => {
    render(<App />);
    const element = screen.getByText(/Willkommen in unserem Restaurant/i);
    expect(element).toBeInTheDocument();
});