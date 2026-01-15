import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { LineChart, Line, XAxis, YAxis, ResponsiveContainer, Tooltip, AreaChart, Area } from 'recharts';
import { 
  TrendingUp, TrendingDown, Search, Home, Briefcase, Eye, Clock, 
  Settings, LogOut, User, DollarSign, ArrowUpRight, ArrowDownRight,
  Plus, Minus, X, Menu, Bell, ChevronRight, BarChart2, Activity
} from 'lucide-react';
import './App.css';

const API_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8001';

// API Client
const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Format helpers
const formatCurrency = (value) => {
  if (value === null || value === undefined) return '$0.00';
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
};

const formatPercent = (value) => {
  if (value === null || value === undefined) return '0.00%';
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;
};

const formatLargeNumber = (num) => {
  if (num === null || num === undefined) return 'N/A';
  if (num >= 1e12) return `$${(num / 1e12).toFixed(2)}T`;
  if (num >= 1e9) return `$${(num / 1e9).toFixed(2)}B`;
  if (num >= 1e6) return `$${(num / 1e6).toFixed(2)}M`;
  return formatCurrency(num);
};

// Auth Screen Component
function AuthScreen({ onLogin }) {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const endpoint = isLogin ? '/api/auth/login' : '/api/auth/register';
      const payload = isLogin 
        ? { username, password }
        : { username, password, email };
      
      const response = await api.post(endpoint, payload);
      localStorage.setItem('token', response.data.access_token);
      localStorage.setItem('user', JSON.stringify(response.data.user));
      onLogin(response.data.user);
    } catch (err) {
      setError(err.response?.data?.detail || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-robin-darker flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8 slide-up">
          <div className="flex items-center justify-center gap-2 mb-2">
            <div className="w-10 h-10 bg-robin-green rounded-full flex items-center justify-center">
              <TrendingUp size={24} className="text-white" />
            </div>
            <h1 className="text-3xl font-bold text-white">MoneyTeam</h1>
          </div>
          <p className="text-robin-text">Your gateway to smart investing</p>
        </div>

        <div className="bg-robin-card rounded-2xl p-8 shadow-2xl border border-robin-border fade-in">
          <div className="flex mb-6 bg-robin-darker rounded-lg p-1">
            <button
              onClick={() => setIsLogin(true)}
              className={`flex-1 py-2 rounded-md transition-all ${
                isLogin ? 'bg-robin-green text-white' : 'text-robin-text hover:text-white'
              }`}
              data-testid="login-tab"
            >
              Sign In
            </button>
            <button
              onClick={() => setIsLogin(false)}
              className={`flex-1 py-2 rounded-md transition-all ${
                !isLogin ? 'bg-robin-green text-white' : 'text-robin-text hover:text-white'
              }`}
              data-testid="register-tab"
            >
              Sign Up
            </button>
          </div>

          {error && (
            <div className="bg-robin-red/20 border border-robin-red text-robin-red px-4 py-2 rounded-lg mb-4 text-sm" data-testid="auth-error">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-robin-text text-sm mb-2">Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-robin-darker border border-robin-border rounded-lg px-4 py-3 text-white focus:border-robin-green focus:outline-none transition-colors"
                placeholder="Enter username"
                required
                data-testid="username-input"
              />
            </div>

            {!isLogin && (
              <div className="fade-in">
                <label className="block text-robin-text text-sm mb-2">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-robin-darker border border-robin-border rounded-lg px-4 py-3 text-white focus:border-robin-green focus:outline-none transition-colors"
                  placeholder="Enter email"
                  required
                  data-testid="email-input"
                />
              </div>
            )}

            <div>
              <label className="block text-robin-text text-sm mb-2">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-robin-darker border border-robin-border rounded-lg px-4 py-3 text-white focus:border-robin-green focus:outline-none transition-colors"
                placeholder="Enter password"
                required
                data-testid="password-input"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-robin-green hover:bg-green-600 text-white font-semibold py-3 rounded-lg transition-all transform hover:scale-[1.02] disabled:opacity-50 disabled:transform-none"
              data-testid="auth-submit-btn"
            >
              {loading ? 'Processing...' : isLogin ? 'Sign In' : 'Create Account'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

// Sidebar Component
function Sidebar({ activeView, setActiveView, user, onLogout }) {
  const navItems = [
    { id: 'dashboard', icon: Home, label: 'Dashboard' },
    { id: 'portfolio', icon: Briefcase, label: 'Portfolio' },
    { id: 'watchlist', icon: Eye, label: 'Watchlist' },
    { id: 'history', icon: Clock, label: 'History' },
  ];

  return (
    <div className="w-64 bg-robin-card border-r border-robin-border h-screen fixed left-0 top-0 flex flex-col">
      <div className="p-6 border-b border-robin-border">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-robin-green rounded-full flex items-center justify-center pulse-green">
            <TrendingUp size={20} className="text-white" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white">MoneyTeam</h1>
            <p className="text-robin-text text-xs">Trading Platform</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 p-4">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => setActiveView(item.id)}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg mb-2 transition-all ${
              activeView === item.id
                ? 'bg-robin-green/20 text-robin-green'
                : 'text-robin-text hover:bg-robin-darker hover:text-white'
            }`}
            data-testid={`nav-${item.id}`}
          >
            <item.icon size={20} />
            <span className="font-medium">{item.label}</span>
          </button>
        ))}
      </nav>

      <div className="p-4 border-t border-robin-border">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 bg-robin-darker rounded-full flex items-center justify-center">
            <User size={20} className="text-robin-text" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-white font-medium truncate">{user?.username}</p>
            <p className="text-robin-text text-xs truncate">{user?.email}</p>
          </div>
        </div>
        <button
          onClick={onLogout}
          className="w-full flex items-center justify-center gap-2 px-4 py-2 text-robin-red hover:bg-robin-red/10 rounded-lg transition-colors"
          data-testid="logout-btn"
        >
          <LogOut size={18} />
          <span>Sign Out</span>
        </button>
      </div>
    </div>
  );
}

// Stock Card Component
function StockCard({ stock, onClick }) {
  const isPositive = stock.change >= 0;

  return (
    <div
      onClick={onClick}
      className="bg-robin-card border border-robin-border rounded-xl p-4 hover:border-robin-green/50 transition-all cursor-pointer group"
      data-testid={`stock-card-${stock.ticker}`}
    >
      <div className="flex justify-between items-start mb-2">
        <div>
          <h3 className="text-white font-semibold group-hover:text-robin-green transition-colors">
            {stock.ticker}
          </h3>
          <p className="text-robin-text text-sm truncate max-w-[120px]">{stock.name}</p>
        </div>
        <div className={`flex items-center gap-1 ${isPositive ? 'text-robin-green' : 'text-robin-red'}`}>
          {isPositive ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
          <span className="text-sm font-medium">{formatPercent(stock.change_percent)}</span>
        </div>
      </div>
      <div className="flex justify-between items-end">
        <span className="text-white text-lg font-bold">{formatCurrency(stock.price)}</span>
        <span className={`text-sm ${isPositive ? 'text-robin-green' : 'text-robin-red'}`}>
          {isPositive ? '+' : ''}{formatCurrency(stock.change)}
        </span>
      </div>
    </div>
  );
}

// Stock Detail Modal
function StockDetailModal({ ticker, onClose, onTrade }) {
  const [quote, setQuote] = useState(null);
  const [fundamentals, setFundamentals] = useState(null);
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tradeMode, setTradeMode] = useState(null);
  const [quantity, setQuantity] = useState('');
  const [chartPeriod, setChartPeriod] = useState('1mo');

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [quoteRes, fundRes, chartRes] = await Promise.all([
          api.get(`/api/stocks/quote/${ticker}`),
          api.get(`/api/stocks/fundamentals/${ticker}`),
          api.get(`/api/stocks/chart/${ticker}?period=${chartPeriod}`)
        ]);
        setQuote(quoteRes.data);
        setFundamentals(fundRes.data);
        setChartData(chartRes.data.data || []);
      } catch (err) {
        console.error('Error fetching stock data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [ticker, chartPeriod]);

  const handleTrade = async () => {
    if (!quantity || parseFloat(quantity) <= 0) return;
    
    try {
      await onTrade({
        stock_ticker: ticker,
        quantity: parseFloat(quantity),
        price: quote.price,
        trade_type: tradeMode
      });
      setTradeMode(null);
      setQuantity('');
    } catch (err) {
      console.error('Trade error:', err);
    }
  };

  const isPositive = quote?.change >= 0;

  return (
    <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div 
        className="bg-robin-card rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto border border-robin-border"
        onClick={(e) => e.stopPropagation()}
      >
        {loading ? (
          <div className="p-8 flex items-center justify-center">
            <div className="animate-spin w-8 h-8 border-2 border-robin-green border-t-transparent rounded-full"></div>
          </div>
        ) : (
          <>
            {/* Header */}
            <div className="p-6 border-b border-robin-border sticky top-0 bg-robin-card z-10">
              <div className="flex justify-between items-start">
                <div>
                  <div className="flex items-center gap-3 mb-1">
                    <h2 className="text-2xl font-bold text-white">{ticker}</h2>
                    <span className="bg-robin-darker px-3 py-1 rounded-full text-robin-text text-sm">
                      {fundamentals?.sector || 'N/A'}
                    </span>
                  </div>
                  <p className="text-robin-text">{quote?.name}</p>
                </div>
                <button
                  onClick={onClose}
                  className="text-robin-text hover:text-white p-2 hover:bg-robin-darker rounded-lg transition-colors"
                  data-testid="close-modal"
                >
                  <X size={24} />
                </button>
              </div>

              <div className="mt-4 flex items-baseline gap-4">
                <span className="text-4xl font-bold text-white">{formatCurrency(quote?.price)}</span>
                <span className={`text-xl font-semibold ${isPositive ? 'text-robin-green' : 'text-robin-red'}`}>
                  {isPositive ? '+' : ''}{formatCurrency(quote?.change)} ({formatPercent(quote?.change_percent)})
                </span>
              </div>
            </div>

            {/* Chart */}
            <div className="p-6 border-b border-robin-border">
              <div className="flex gap-2 mb-4">
                {['1d', '5d', '1mo', '3mo', '1y'].map((period) => (
                  <button
                    key={period}
                    onClick={() => setChartPeriod(period)}
                    className={`px-3 py-1 rounded-lg text-sm transition-colors ${
                      chartPeriod === period
                        ? 'bg-robin-green text-white'
                        : 'bg-robin-darker text-robin-text hover:text-white'
                    }`}
                  >
                    {period.toUpperCase()}
                  </button>
                ))}
              </div>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData}>
                    <defs>
                      <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor={isPositive ? '#00C805' : '#FF5000'} stopOpacity={0.3}/>
                        <stop offset="95%" stopColor={isPositive ? '#00C805' : '#FF5000'} stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="date" hide />
                    <YAxis domain={['auto', 'auto']} hide />
                    <Tooltip
                      contentStyle={{
                        backgroundColor: '#1E2124',
                        border: '1px solid #2F3336',
                        borderRadius: '8px',
                        color: '#fff'
                      }}
                      formatter={(value) => [formatCurrency(value), 'Price']}
                    />
                    <Area
                      type="monotone"
                      dataKey="close"
                      stroke={isPositive ? '#00C805' : '#FF5000'}
                      fill="url(#colorPrice)"
                      strokeWidth={2}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Key Stats */}
            <div className="p-6 border-b border-robin-border">
              <h3 className="text-lg font-semibold text-white mb-4">Key Statistics</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">Open</p>
                  <p className="text-white font-semibold">{formatCurrency(quote?.open_price)}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">Day High</p>
                  <p className="text-white font-semibold">{formatCurrency(quote?.day_high)}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">Day Low</p>
                  <p className="text-white font-semibold">{formatCurrency(quote?.day_low)}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">Volume</p>
                  <p className="text-white font-semibold">{quote?.volume?.toLocaleString()}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">Market Cap</p>
                  <p className="text-white font-semibold">{formatLargeNumber(fundamentals?.market_cap)}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">P/E Ratio</p>
                  <p className="text-white font-semibold">{fundamentals?.pe_ratio?.toFixed(2) || 'N/A'}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">52W High</p>
                  <p className="text-white font-semibold">{formatCurrency(quote?.high_52_week)}</p>
                </div>
                <div className="bg-robin-darker rounded-lg p-3">
                  <p className="text-robin-text text-sm">52W Low</p>
                  <p className="text-white font-semibold">{formatCurrency(quote?.low_52_week)}</p>
                </div>
              </div>
            </div>

            {/* Fundamentals */}
            <div className="p-6 border-b border-robin-border">
              <h3 className="text-lg font-semibold text-white mb-4">Fundamental Analysis</h3>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                <div>
                  <p className="text-robin-text text-sm">EPS</p>
                  <p className="text-white font-semibold">{formatCurrency(fundamentals?.eps)}</p>
                </div>
                <div>
                  <p className="text-robin-text text-sm">P/B Ratio</p>
                  <p className="text-white font-semibold">{fundamentals?.pb_ratio?.toFixed(2) || 'N/A'}</p>
                </div>
                <div>
                  <p className="text-robin-text text-sm">Dividend Yield</p>
                  <p className="text-white font-semibold">{fundamentals?.dividend_yield ? formatPercent(fundamentals.dividend_yield * 100) : 'N/A'}</p>
                </div>
                <div>
                  <p className="text-robin-text text-sm">Beta</p>
                  <p className="text-white font-semibold">{fundamentals?.beta?.toFixed(2) || 'N/A'}</p>
                </div>
                <div>
                  <p className="text-robin-text text-sm">Profit Margin</p>
                  <p className="text-white font-semibold">{fundamentals?.profit_margin ? formatPercent(fundamentals.profit_margin * 100) : 'N/A'}</p>
                </div>
                <div>
                  <p className="text-robin-text text-sm">ROE</p>
                  <p className="text-white font-semibold">{fundamentals?.roe ? formatPercent(fundamentals.roe * 100) : 'N/A'}</p>
                </div>
              </div>
            </div>

            {/* Trade Actions */}
            <div className="p-6">
              {!tradeMode ? (
                <div className="flex gap-4">
                  <button
                    onClick={() => setTradeMode('BUY')}
                    className="flex-1 bg-robin-green hover:bg-green-600 text-white font-semibold py-3 rounded-lg transition-all"
                    data-testid="buy-btn"
                  >
                    Buy {ticker}
                  </button>
                  <button
                    onClick={() => setTradeMode('SELL')}
                    className="flex-1 bg-robin-red hover:bg-red-600 text-white font-semibold py-3 rounded-lg transition-all"
                    data-testid="sell-btn"
                  >
                    Sell {ticker}
                  </button>
                </div>
              ) : (
                <div className="slide-up">
                  <h4 className={`text-lg font-semibold mb-4 ${tradeMode === 'BUY' ? 'text-robin-green' : 'text-robin-red'}`}>
                    {tradeMode} {ticker}
                  </h4>
                  <div className="flex gap-4 items-end">
                    <div className="flex-1">
                      <label className="block text-robin-text text-sm mb-2">Shares</label>
                      <input
                        type="number"
                        value={quantity}
                        onChange={(e) => setQuantity(e.target.value)}
                        className="w-full bg-robin-darker border border-robin-border rounded-lg px-4 py-3 text-white focus:border-robin-green focus:outline-none"
                        placeholder="0"
                        min="0.01"
                        step="0.01"
                        data-testid="quantity-input"
                      />
                    </div>
                    <div className="text-right">
                      <p className="text-robin-text text-sm mb-1">Estimated Total</p>
                      <p className="text-white text-xl font-bold">
                        {formatCurrency(parseFloat(quantity || 0) * quote?.price)}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-4 mt-4">
                    <button
                      onClick={() => setTradeMode(null)}
                      className="flex-1 bg-robin-darker hover:bg-robin-border text-white font-semibold py-3 rounded-lg transition-all"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleTrade}
                      disabled={!quantity || parseFloat(quantity) <= 0}
                      className={`flex-1 font-semibold py-3 rounded-lg transition-all disabled:opacity-50 ${
                        tradeMode === 'BUY' 
                          ? 'bg-robin-green hover:bg-green-600 text-white'
                          : 'bg-robin-red hover:bg-red-600 text-white'
                      }`}
                      data-testid="confirm-trade-btn"
                    >
                      Confirm {tradeMode}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// Dashboard View
function DashboardView({ portfolio, positions, watchlist, movers, onStockClick, onAddToWatchlist }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);

  const handleSearch = async (query) => {
    setSearchQuery(query);
    if (query.length < 1) {
      setSearchResults([]);
      return;
    }
    
    setSearching(true);
    try {
      const response = await api.get(`/api/stocks/quote/${query.toUpperCase()}`);
      setSearchResults([response.data]);
    } catch (err) {
      setSearchResults([]);
    } finally {
      setSearching(false);
    }
  };

  return (
    <div className="space-y-6 fade-in">
      {/* Portfolio Summary */}
      <div className="bg-gradient-to-br from-robin-card to-robin-darker rounded-2xl p-6 border border-robin-border">
        <div className="flex justify-between items-start mb-6">
          <div>
            <p className="text-robin-text text-sm mb-1">Total Portfolio Value</p>
            <h2 className="text-4xl font-bold text-white">{formatCurrency(portfolio?.total_value)}</h2>
            <div className={`flex items-center gap-2 mt-2 ${portfolio?.total_gain_loss >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
              {portfolio?.total_gain_loss >= 0 ? <TrendingUp size={20} /> : <TrendingDown size={20} />}
              <span className="font-semibold">
                {portfolio?.total_gain_loss >= 0 ? '+' : ''}{formatCurrency(portfolio?.total_gain_loss)} ({formatPercent(portfolio?.total_gain_loss_percent)})
              </span>
            </div>
          </div>
          <div className="text-right">
            <p className="text-robin-text text-sm">Cash Available</p>
            <p className="text-2xl font-bold text-white">{formatCurrency(portfolio?.cash_balance)}</p>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4">
          <div className="bg-robin-darker/50 rounded-xl p-4">
            <div className="flex items-center gap-2 text-robin-text mb-1">
              <Briefcase size={16} />
              <span className="text-sm">Positions</span>
            </div>
            <p className="text-2xl font-bold text-white">{portfolio?.positions_count || 0}</p>
          </div>
          <div className="bg-robin-darker/50 rounded-xl p-4">
            <div className="flex items-center gap-2 text-robin-text mb-1">
              <DollarSign size={16} />
              <span className="text-sm">Cost Basis</span>
            </div>
            <p className="text-2xl font-bold text-white">{formatCurrency(portfolio?.total_cost)}</p>
          </div>
          <div className="bg-robin-darker/50 rounded-xl p-4">
            <div className="flex items-center gap-2 text-robin-text mb-1">
              <Activity size={16} />
              <span className="text-sm">Today's Return</span>
            </div>
            <p className={`text-2xl font-bold ${portfolio?.total_gain_loss >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
              {formatPercent(portfolio?.total_gain_loss_percent)}
            </p>
          </div>
        </div>
      </div>

      {/* Search */}
      <div className="relative">
        <div className="bg-robin-card rounded-xl border border-robin-border p-4">
          <div className="flex items-center gap-3">
            <Search size={20} className="text-robin-text" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="Search stocks by symbol..."
              className="flex-1 bg-transparent text-white placeholder-robin-text focus:outline-none"
              data-testid="stock-search"
            />
            {searching && <div className="animate-spin w-5 h-5 border-2 border-robin-green border-t-transparent rounded-full"></div>}
          </div>
        </div>
        
        {searchResults.length > 0 && (
          <div className="absolute top-full left-0 right-0 mt-2 bg-robin-card border border-robin-border rounded-xl shadow-2xl z-20 overflow-hidden">
            {searchResults.map((stock) => (
              <div
                key={stock.ticker}
                className="flex items-center justify-between p-4 hover:bg-robin-darker cursor-pointer transition-colors"
                onClick={() => {
                  onStockClick(stock.ticker);
                  setSearchQuery('');
                  setSearchResults([]);
                }}
              >
                <div>
                  <p className="text-white font-semibold">{stock.ticker}</p>
                  <p className="text-robin-text text-sm">{stock.name}</p>
                </div>
                <div className="text-right">
                  <p className="text-white font-semibold">{formatCurrency(stock.price)}</p>
                  <p className={`text-sm ${stock.change >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
                    {formatPercent(stock.change_percent)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Positions */}
      {positions.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Briefcase size={20} />
            Your Positions
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {positions.map((position) => (
              <div
                key={position.id}
                onClick={() => onStockClick(position.stock_ticker)}
                className="bg-robin-card border border-robin-border rounded-xl p-4 hover:border-robin-green/50 cursor-pointer transition-all"
                data-testid={`position-${position.stock_ticker}`}
              >
                <div className="flex justify-between items-start mb-3">
                  <div>
                    <h4 className="text-white font-semibold">{position.stock_ticker}</h4>
                    <p className="text-robin-text text-sm">{position.total_quantity} shares</p>
                  </div>
                  <div className={`px-2 py-1 rounded ${position.unrealized_pnl >= 0 ? 'bg-robin-green/20 text-robin-green' : 'bg-robin-red/20 text-robin-red'}`}>
                    {formatPercent(position.unrealized_pnl_percent)}
                  </div>
                </div>
                <div className="flex justify-between items-end">
                  <div>
                    <p className="text-robin-text text-xs">Market Value</p>
                    <p className="text-white font-bold">{formatCurrency(position.market_value)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-robin-text text-xs">P&L</p>
                    <p className={`font-bold ${position.unrealized_pnl >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
                      {position.unrealized_pnl >= 0 ? '+' : ''}{formatCurrency(position.unrealized_pnl)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Market Movers */}
      <div>
        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
          <BarChart2 size={20} />
          Market Movers
        </h3>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {movers.slice(0, 10).map((stock) => (
            <StockCard key={stock.ticker} stock={stock} onClick={() => onStockClick(stock.ticker)} />
          ))}
        </div>
      </div>

      {/* Watchlist Preview */}
      {watchlist.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
            <Eye size={20} />
            Watchlist
          </h3>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
            {watchlist.slice(0, 5).map((stock) => (
              <StockCard key={stock.ticker} stock={stock} onClick={() => onStockClick(stock.ticker)} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// Portfolio View
function PortfolioView({ portfolio, positions, onStockClick }) {
  return (
    <div className="space-y-6 fade-in">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-white">Portfolio</h2>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-robin-card border border-robin-border rounded-xl p-5">
          <p className="text-robin-text text-sm mb-1">Total Value</p>
          <p className="text-2xl font-bold text-white">{formatCurrency(portfolio?.total_value)}</p>
        </div>
        <div className="bg-robin-card border border-robin-border rounded-xl p-5">
          <p className="text-robin-text text-sm mb-1">Cash Balance</p>
          <p className="text-2xl font-bold text-white">{formatCurrency(portfolio?.cash_balance)}</p>
        </div>
        <div className="bg-robin-card border border-robin-border rounded-xl p-5">
          <p className="text-robin-text text-sm mb-1">Total Gain/Loss</p>
          <p className={`text-2xl font-bold ${portfolio?.total_gain_loss >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
            {portfolio?.total_gain_loss >= 0 ? '+' : ''}{formatCurrency(portfolio?.total_gain_loss)}
          </p>
        </div>
        <div className="bg-robin-card border border-robin-border rounded-xl p-5">
          <p className="text-robin-text text-sm mb-1">Return</p>
          <p className={`text-2xl font-bold ${portfolio?.total_gain_loss_percent >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
            {formatPercent(portfolio?.total_gain_loss_percent)}
          </p>
        </div>
      </div>

      {/* Positions Table */}
      <div className="bg-robin-card border border-robin-border rounded-xl overflow-hidden">
        <div className="p-4 border-b border-robin-border">
          <h3 className="text-lg font-semibold text-white">Holdings</h3>
        </div>
        {positions.length === 0 ? (
          <div className="p-8 text-center">
            <Briefcase size={48} className="text-robin-text mx-auto mb-4" />
            <p className="text-robin-text">No positions yet</p>
            <p className="text-robin-text text-sm">Start trading to build your portfolio</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-robin-darker">
                <tr>
                  <th className="text-left p-4 text-robin-text font-medium">Symbol</th>
                  <th className="text-right p-4 text-robin-text font-medium">Shares</th>
                  <th className="text-right p-4 text-robin-text font-medium">Avg Cost</th>
                  <th className="text-right p-4 text-robin-text font-medium">Current Price</th>
                  <th className="text-right p-4 text-robin-text font-medium">Market Value</th>
                  <th className="text-right p-4 text-robin-text font-medium">P&L</th>
                  <th className="text-right p-4 text-robin-text font-medium">Return</th>
                </tr>
              </thead>
              <tbody>
                {positions.map((position) => (
                  <tr
                    key={position.id}
                    onClick={() => onStockClick(position.stock_ticker)}
                    className="border-t border-robin-border hover:bg-robin-darker cursor-pointer transition-colors"
                    data-testid={`portfolio-row-${position.stock_ticker}`}
                  >
                    <td className="p-4">
                      <span className="text-white font-semibold">{position.stock_ticker}</span>
                    </td>
                    <td className="p-4 text-right text-white">{position.total_quantity}</td>
                    <td className="p-4 text-right text-white">{formatCurrency(position.average_price)}</td>
                    <td className="p-4 text-right text-white">{formatCurrency(position.current_price)}</td>
                    <td className="p-4 text-right text-white font-semibold">{formatCurrency(position.market_value)}</td>
                    <td className={`p-4 text-right font-semibold ${position.unrealized_pnl >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
                      {position.unrealized_pnl >= 0 ? '+' : ''}{formatCurrency(position.unrealized_pnl)}
                    </td>
                    <td className={`p-4 text-right font-semibold ${position.unrealized_pnl_percent >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
                      {formatPercent(position.unrealized_pnl_percent)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

// Watchlist View
function WatchlistView({ watchlist, onStockClick, onRemoveFromWatchlist, onAddToWatchlist }) {
  const [addingTicker, setAddingTicker] = useState('');
  const [adding, setAdding] = useState(false);

  const handleAdd = async () => {
    if (!addingTicker.trim()) return;
    setAdding(true);
    try {
      await onAddToWatchlist(addingTicker.trim().toUpperCase());
      setAddingTicker('');
    } catch (err) {
      console.error('Error adding to watchlist:', err);
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="space-y-6 fade-in">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-white">Watchlist</h2>
      </div>

      {/* Add to Watchlist */}
      <div className="bg-robin-card border border-robin-border rounded-xl p-4">
        <div className="flex gap-4">
          <input
            type="text"
            value={addingTicker}
            onChange={(e) => setAddingTicker(e.target.value.toUpperCase())}
            placeholder="Enter stock symbol (e.g., AAPL)"
            className="flex-1 bg-robin-darker border border-robin-border rounded-lg px-4 py-3 text-white focus:border-robin-green focus:outline-none"
            onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
            data-testid="add-watchlist-input"
          />
          <button
            onClick={handleAdd}
            disabled={adding || !addingTicker.trim()}
            className="bg-robin-green hover:bg-green-600 text-white font-semibold px-6 py-3 rounded-lg transition-all disabled:opacity-50 flex items-center gap-2"
            data-testid="add-watchlist-btn"
          >
            <Plus size={20} />
            Add
          </button>
        </div>
      </div>

      {/* Watchlist Items */}
      {watchlist.length === 0 ? (
        <div className="bg-robin-card border border-robin-border rounded-xl p-8 text-center">
          <Eye size={48} className="text-robin-text mx-auto mb-4" />
          <p className="text-robin-text">Your watchlist is empty</p>
          <p className="text-robin-text text-sm">Add stocks to track their performance</p>
        </div>
      ) : (
        <div className="bg-robin-card border border-robin-border rounded-xl overflow-hidden">
          <table className="w-full">
            <thead className="bg-robin-darker">
              <tr>
                <th className="text-left p-4 text-robin-text font-medium">Symbol</th>
                <th className="text-left p-4 text-robin-text font-medium">Name</th>
                <th className="text-right p-4 text-robin-text font-medium">Price</th>
                <th className="text-right p-4 text-robin-text font-medium">Change</th>
                <th className="text-right p-4 text-robin-text font-medium">% Change</th>
                <th className="text-center p-4 text-robin-text font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {watchlist.map((stock) => (
                <tr
                  key={stock.ticker}
                  className="border-t border-robin-border hover:bg-robin-darker transition-colors"
                  data-testid={`watchlist-row-${stock.ticker}`}
                >
                  <td 
                    className="p-4 cursor-pointer"
                    onClick={() => onStockClick(stock.ticker)}
                  >
                    <span className="text-white font-semibold hover:text-robin-green transition-colors">
                      {stock.ticker}
                    </span>
                  </td>
                  <td className="p-4 text-robin-text">{stock.name}</td>
                  <td className="p-4 text-right text-white font-semibold">{formatCurrency(stock.price)}</td>
                  <td className={`p-4 text-right font-semibold ${stock.change >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
                    {stock.change >= 0 ? '+' : ''}{formatCurrency(stock.change)}
                  </td>
                  <td className={`p-4 text-right font-semibold ${stock.change_percent >= 0 ? 'text-robin-green' : 'text-robin-red'}`}>
                    {formatPercent(stock.change_percent)}
                  </td>
                  <td className="p-4 text-center">
                    <button
                      onClick={() => onRemoveFromWatchlist(stock.ticker)}
                      className="text-robin-red hover:bg-robin-red/10 p-2 rounded-lg transition-colors"
                      data-testid={`remove-watchlist-${stock.ticker}`}
                    >
                      <X size={18} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// History View
function HistoryView({ trades }) {
  return (
    <div className="space-y-6 fade-in">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-white">Trade History</h2>
      </div>

      {trades.length === 0 ? (
        <div className="bg-robin-card border border-robin-border rounded-xl p-8 text-center">
          <Clock size={48} className="text-robin-text mx-auto mb-4" />
          <p className="text-robin-text">No trades yet</p>
          <p className="text-robin-text text-sm">Your completed trades will appear here</p>
        </div>
      ) : (
        <div className="bg-robin-card border border-robin-border rounded-xl overflow-hidden">
          <table className="w-full">
            <thead className="bg-robin-darker">
              <tr>
                <th className="text-left p-4 text-robin-text font-medium">Date</th>
                <th className="text-left p-4 text-robin-text font-medium">Symbol</th>
                <th className="text-center p-4 text-robin-text font-medium">Type</th>
                <th className="text-right p-4 text-robin-text font-medium">Shares</th>
                <th className="text-right p-4 text-robin-text font-medium">Price</th>
                <th className="text-right p-4 text-robin-text font-medium">Total</th>
                <th className="text-center p-4 text-robin-text font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {trades.map((trade) => (
                <tr key={trade.id} className="border-t border-robin-border" data-testid={`trade-row-${trade.id}`}>
                  <td className="p-4 text-robin-text">
                    {new Date(trade.execution_date).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </td>
                  <td className="p-4">
                    <span className="text-white font-semibold">{trade.stock_ticker}</span>
                  </td>
                  <td className="p-4 text-center">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      trade.trade_type === 'BUY' 
                        ? 'bg-robin-green/20 text-robin-green' 
                        : 'bg-robin-red/20 text-robin-red'
                    }`}>
                      {trade.trade_type}
                    </span>
                  </td>
                  <td className="p-4 text-right text-white">{trade.quantity}</td>
                  <td className="p-4 text-right text-white">{formatCurrency(trade.price)}</td>
                  <td className="p-4 text-right text-white font-semibold">{formatCurrency(trade.total_value)}</td>
                  <td className="p-4 text-center">
                    <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                      trade.status === 'EXECUTED' 
                        ? 'bg-robin-green/20 text-robin-green'
                        : trade.status === 'CANCELLED'
                        ? 'bg-robin-red/20 text-robin-red'
                        : 'bg-yellow-500/20 text-yellow-500'
                    }`}>
                      {trade.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// Main App Component
function App() {
  const [user, setUser] = useState(null);
  const [activeView, setActiveView] = useState('dashboard');
  const [portfolio, setPortfolio] = useState(null);
  const [positions, setPositions] = useState([]);
  const [watchlist, setWatchlist] = useState([]);
  const [trades, setTrades] = useState([]);
  const [movers, setMovers] = useState([]);
  const [selectedStock, setSelectedStock] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);

  // Check for existing session
  useEffect(() => {
    const token = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (token && savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);
  }, []);

  // Fetch data when user is logged in
  const fetchData = useCallback(async () => {
    if (!user) return;
    
    try {
      const [portfolioRes, positionsRes, watchlistRes, tradesRes, moversRes] = await Promise.all([
        api.get('/api/portfolio/summary'),
        api.get('/api/portfolio/positions'),
        api.get('/api/watchlist'),
        api.get('/api/trades'),
        api.get('/api/market/movers')
      ]);
      
      setPortfolio(portfolioRes.data);
      setPositions(positionsRes.data);
      setWatchlist(watchlistRes.data.stocks || []);
      setTrades(tradesRes.data);
      setMovers(moversRes.data.movers || []);
    } catch (err) {
      console.error('Error fetching data:', err);
      if (err.response?.status === 401) {
        handleLogout();
      }
    }
  }, [user]);

  useEffect(() => {
    fetchData();
    // Refresh data every 30 seconds
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, [fetchData]);

  const handleLogin = (userData) => {
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    setPortfolio(null);
    setPositions([]);
    setWatchlist([]);
    setTrades([]);
  };

  const handleTrade = async (tradeData) => {
    try {
      await api.post('/api/trades', tradeData);
      showNotification(`${tradeData.trade_type} order executed for ${tradeData.quantity} shares of ${tradeData.stock_ticker}`, 'success');
      fetchData();
      setSelectedStock(null);
    } catch (err) {
      showNotification(err.response?.data?.detail || 'Trade failed', 'error');
      throw err;
    }
  };

  const handleAddToWatchlist = async (ticker) => {
    try {
      await api.post('/api/watchlist', { stock_ticker: ticker });
      showNotification(`${ticker} added to watchlist`, 'success');
      fetchData();
    } catch (err) {
      showNotification(err.response?.data?.detail || 'Failed to add to watchlist', 'error');
      throw err;
    }
  };

  const handleRemoveFromWatchlist = async (ticker) => {
    try {
      await api.delete(`/api/watchlist/${ticker}`);
      showNotification(`${ticker} removed from watchlist`, 'success');
      fetchData();
    } catch (err) {
      showNotification(err.response?.data?.detail || 'Failed to remove from watchlist', 'error');
    }
  };

  const showNotification = (message, type = 'info') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-robin-darker flex items-center justify-center">
        <div className="animate-spin w-12 h-12 border-4 border-robin-green border-t-transparent rounded-full"></div>
      </div>
    );
  }

  if (!user) {
    return <AuthScreen onLogin={handleLogin} />;
  }

  return (
    <div className="min-h-screen bg-robin-darker">
      <Sidebar 
        activeView={activeView} 
        setActiveView={setActiveView} 
        user={user} 
        onLogout={handleLogout} 
      />

      <main className="ml-64 p-8">
        {activeView === 'dashboard' && (
          <DashboardView
            portfolio={portfolio}
            positions={positions}
            watchlist={watchlist}
            movers={movers}
            onStockClick={setSelectedStock}
            onAddToWatchlist={handleAddToWatchlist}
          />
        )}
        {activeView === 'portfolio' && (
          <PortfolioView
            portfolio={portfolio}
            positions={positions}
            onStockClick={setSelectedStock}
          />
        )}
        {activeView === 'watchlist' && (
          <WatchlistView
            watchlist={watchlist}
            onStockClick={setSelectedStock}
            onRemoveFromWatchlist={handleRemoveFromWatchlist}
            onAddToWatchlist={handleAddToWatchlist}
          />
        )}
        {activeView === 'history' && (
          <HistoryView trades={trades} />
        )}
      </main>

      {/* Stock Detail Modal */}
      {selectedStock && (
        <StockDetailModal
          ticker={selectedStock}
          onClose={() => setSelectedStock(null)}
          onTrade={handleTrade}
        />
      )}

      {/* Notification */}
      {notification && (
        <div className={`fixed bottom-6 right-6 px-6 py-4 rounded-xl shadow-2xl border z-50 slide-up ${
          notification.type === 'success' 
            ? 'bg-robin-green/20 border-robin-green text-robin-green'
            : notification.type === 'error'
            ? 'bg-robin-red/20 border-robin-red text-robin-red'
            : 'bg-robin-card border-robin-border text-white'
        }`} data-testid="notification">
          {notification.message}
        </div>
      )}
    </div>
  );
}

export default App;
