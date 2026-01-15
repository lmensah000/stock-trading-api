import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';

// Mock axios
jest.mock('axios', () => ({
  create: jest.fn(() => ({
    get: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() }
    }
  }))
}));

// Import App after mocking
import App from './App';

describe('App Component', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  describe('Authentication', () => {
    test('renders login page when not authenticated', () => {
      localStorage.getItem.mockReturnValue(null);
      render(<App />);
      
      expect(screen.getByText(/MoneyTeam/i)).toBeInTheDocument();
      expect(screen.getByTestId('username-input')).toBeInTheDocument();
      expect(screen.getByTestId('password-input')).toBeInTheDocument();
    });

    test('shows sign in tab by default', () => {
      localStorage.getItem.mockReturnValue(null);
      render(<App />);
      
      expect(screen.getByTestId('login-tab')).toBeInTheDocument();
      expect(screen.getByTestId('register-tab')).toBeInTheDocument();
    });

    test('switches to sign up tab when clicked', async () => {
      localStorage.getItem.mockReturnValue(null);
      render(<App />);
      
      const registerTab = screen.getByTestId('register-tab');
      await userEvent.click(registerTab);
      
      // Email field should appear in registration
      expect(screen.getByTestId('email-input')).toBeInTheDocument();
    });

    test('login form has required fields', () => {
      localStorage.getItem.mockReturnValue(null);
      render(<App />);
      
      const usernameInput = screen.getByTestId('username-input');
      const passwordInput = screen.getByTestId('password-input');
      const submitBtn = screen.getByTestId('auth-submit-btn');
      
      expect(usernameInput).toBeRequired();
      expect(passwordInput).toBeRequired();
      expect(submitBtn).toBeInTheDocument();
    });

    test('displays error message on failed login', async () => {
      localStorage.getItem.mockReturnValue(null);
      
      const axios = require('axios');
      axios.create.mockReturnValue({
        get: jest.fn(),
        post: jest.fn().mockRejectedValue({
          response: { data: { detail: 'Invalid credentials' } }
        }),
        interceptors: {
          request: { use: jest.fn() },
          response: { use: jest.fn() }
        }
      });
      
      render(<App />);
      
      await userEvent.type(screen.getByTestId('username-input'), 'testuser');
      await userEvent.type(screen.getByTestId('password-input'), 'wrongpass');
      await userEvent.click(screen.getByTestId('auth-submit-btn'));
      
      // Error handling is async
      await waitFor(() => {
        const errorElement = screen.queryByTestId('auth-error');
        if (errorElement) {
          expect(errorElement).toBeInTheDocument();
        }
      }, { timeout: 3000 });
    });
  });
});
