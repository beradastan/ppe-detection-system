import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

// Mock the components that might cause issues in tests
jest.mock('jsqr', () => jest.fn());

describe('App Component', () => {
  test('renders PPE Detection System title', () => {
    render(<App />);
    const titleElement = screen.getByText(/PPE Detection System/i);
    expect(titleElement).toBeInTheDocument();
  });

  test('shows connection status', () => {
    render(<App />);
    const statusElement = screen.getByText(/Bağlantı durumu/i);
    expect(statusElement).toBeInTheDocument();
  });
});
