import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';

// Component helper tests
describe('Utility Functions', () => {
  describe('formatCurrency', () => {
    const formatCurrency = (value) => {
      if (value === null || value === undefined) return '$0.00';
      return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
    };

    test('formats positive numbers correctly', () => {
      expect(formatCurrency(100)).toBe('$100.00');
      expect(formatCurrency(1234.56)).toBe('$1,234.56');
      expect(formatCurrency(1000000)).toBe('$1,000,000.00');
    });

    test('formats zero correctly', () => {
      expect(formatCurrency(0)).toBe('$0.00');
    });

    test('formats negative numbers correctly', () => {
      expect(formatCurrency(-100)).toBe('-$100.00');
    });

    test('handles null and undefined', () => {
      expect(formatCurrency(null)).toBe('$0.00');
      expect(formatCurrency(undefined)).toBe('$0.00');
    });
  });

  describe('formatPercent', () => {
    const formatPercent = (value) => {
      if (value === null || value === undefined) return '0.00%';
      return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
    };

    test('formats positive percentages with plus sign', () => {
      expect(formatPercent(5.25)).toBe('+5.25%');
      expect(formatPercent(0.01)).toBe('+0.01%');
    });

    test('formats negative percentages', () => {
      expect(formatPercent(-3.50)).toBe('-3.50%');
    });

    test('formats zero', () => {
      expect(formatPercent(0)).toBe('+0.00%');
    });

    test('handles null and undefined', () => {
      expect(formatPercent(null)).toBe('0.00%');
      expect(formatPercent(undefined)).toBe('0.00%');
    });
  });

  describe('formatLargeNumber', () => {
    const formatLargeNumber = (num) => {
      if (num === null || num === undefined) return 'N/A';
      if (num >= 1e12) return `$${(num / 1e12).toFixed(2)}T`;
      if (num >= 1e9) return `$${(num / 1e9).toFixed(2)}B`;
      if (num >= 1e6) return `$${(num / 1e6).toFixed(2)}M`;
      return `$${num.toFixed(2)}`;
    };

    test('formats trillions correctly', () => {
      expect(formatLargeNumber(2890000000000)).toBe('$2.89T');
    });

    test('formats billions correctly', () => {
      expect(formatLargeNumber(1500000000)).toBe('$1.50B');
    });

    test('formats millions correctly', () => {
      expect(formatLargeNumber(25000000)).toBe('$25.00M');
    });

    test('formats smaller numbers correctly', () => {
      expect(formatLargeNumber(50000)).toBe('$50000.00');
    });

    test('handles null and undefined', () => {
      expect(formatLargeNumber(null)).toBe('N/A');
      expect(formatLargeNumber(undefined)).toBe('N/A');
    });
  });
});

describe('Data Validation', () => {
  describe('Trade validation', () => {
    const validateTrade = (trade) => {
      const errors = [];
      
      if (!trade.stock_ticker || trade.stock_ticker.length === 0) {
        errors.push('Stock ticker is required');
      }
      
      if (!trade.quantity || trade.quantity <= 0) {
        errors.push('Quantity must be greater than 0');
      }
      
      if (!trade.price || trade.price <= 0) {
        errors.push('Price must be greater than 0');
      }
      
      if (!['BUY', 'SELL'].includes(trade.trade_type)) {
        errors.push('Trade type must be BUY or SELL');
      }
      
      return errors;
    };

    test('validates valid BUY trade', () => {
      const trade = {
        stock_ticker: 'AAPL',
        quantity: 10,
        price: 185.92,
        trade_type: 'BUY'
      };
      expect(validateTrade(trade)).toHaveLength(0);
    });

    test('validates valid SELL trade', () => {
      const trade = {
        stock_ticker: 'MSFT',
        quantity: 5,
        price: 378.91,
        trade_type: 'SELL'
      };
      expect(validateTrade(trade)).toHaveLength(0);
    });

    test('rejects missing ticker', () => {
      const trade = {
        stock_ticker: '',
        quantity: 10,
        price: 100,
        trade_type: 'BUY'
      };
      expect(validateTrade(trade)).toContain('Stock ticker is required');
    });

    test('rejects zero quantity', () => {
      const trade = {
        stock_ticker: 'AAPL',
        quantity: 0,
        price: 100,
        trade_type: 'BUY'
      };
      expect(validateTrade(trade)).toContain('Quantity must be greater than 0');
    });

    test('rejects negative price', () => {
      const trade = {
        stock_ticker: 'AAPL',
        quantity: 10,
        price: -100,
        trade_type: 'BUY'
      };
      expect(validateTrade(trade)).toContain('Price must be greater than 0');
    });

    test('rejects invalid trade type', () => {
      const trade = {
        stock_ticker: 'AAPL',
        quantity: 10,
        price: 100,
        trade_type: 'INVALID'
      };
      expect(validateTrade(trade)).toContain('Trade type must be BUY or SELL');
    });
  });

  describe('User input validation', () => {
    const validateEmail = (email) => {
      const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return re.test(email);
    };

    const validatePassword = (password) => {
      return password && password.length >= 6;
    };

    const validateUsername = (username) => {
      return username && username.length >= 3 && username.length <= 50;
    };

    test('validates correct email', () => {
      expect(validateEmail('user@example.com')).toBe(true);
      expect(validateEmail('test.user@domain.org')).toBe(true);
    });

    test('rejects invalid email', () => {
      expect(validateEmail('invalid')).toBe(false);
      expect(validateEmail('user@')).toBe(false);
      expect(validateEmail('@domain.com')).toBe(false);
    });

    test('validates correct password', () => {
      expect(validatePassword('password123')).toBe(true);
      expect(validatePassword('123456')).toBe(true);
    });

    test('rejects short password', () => {
      expect(validatePassword('12345')).toBe(false);
      expect(validatePassword('')).toBeFalsy();
    });

    test('validates correct username', () => {
      expect(validateUsername('john')).toBe(true);
      expect(validateUsername('testuser123')).toBe(true);
    });

    test('rejects invalid username', () => {
      expect(validateUsername('ab')).toBe(false); // too short
      expect(validateUsername('')).toBe(false);
    });
  });
});
