# React Frontend - MoneyTeam Trading

Modern Robinhood-style trading interface built with React and TailwindCSS.

## 📁 Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── App.js              # Main application (1200+ lines)
│   ├── App.css             # Custom styles
│   ├── index.js            # Entry point
│   ├── index.css           # Tailwind imports
│   ├── setupTests.js       # Test configuration
│   ├── App.test.js         # Component tests
│   ├── utils.test.js       # Utility tests
│   └── components.test.js  # Component logic tests
├── package.json
├── tailwind.config.js
├── postcss.config.js
└── .env
```

## 🚀 Running

```bash
# Install dependencies
yarn install

# Start development server
yarn start

# Build for production
yarn build

# Run tests
yarn test
```

## ⚙️ Configuration

Edit `.env`:

```env
# For Python backend
REACT_APP_BACKEND_URL=http://localhost:8001

# For Java backend
REACT_APP_BACKEND_URL=http://localhost:8080
```

## 🎨 Features

### Pages/Views
- **Dashboard** - Portfolio summary, search, market movers
- **Portfolio** - Holdings table, P&L tracking
- **Watchlist** - Track favorite stocks
- **History** - Trade history log

### Components
- **AuthScreen** - Login/Register forms
- **Sidebar** - Navigation menu
- **StockCard** - Stock price display
- **StockDetailModal** - Full stock info with charts
- **Trade Form** - Buy/Sell interface

### UI Features
- 🌙 Dark theme (Robinhood-style)
- 📈 Interactive charts (Recharts)
- 🟢🔴 Color-coded gains/losses
- 📱 Responsive design
- 🔔 Toast notifications
- ⚡ Real-time updates

## 🧪 Testing

```bash
# Run all tests
yarn test

# Run with coverage
yarn test -- --coverage

# Run specific test
yarn test App.test.js
```

**49 frontend tests covering:**
- Component rendering
- Utility functions
- Data validation
- Portfolio calculations
- Authentication state

## 📦 Dependencies

- **react** - UI framework
- **axios** - HTTP client
- **recharts** - Charts library
- **lucide-react** - Icons
- **tailwindcss** - Styling

## 🎯 Test IDs

All interactive elements have `data-testid` attributes:

```
login-tab, register-tab
username-input, email-input, password-input
auth-submit-btn
nav-dashboard, nav-portfolio, nav-watchlist, nav-history
stock-card-{ticker}
buy-btn, sell-btn
quantity-input, confirm-trade-btn
add-watchlist-input, add-watchlist-btn
```

## 🔗 API Integration

The frontend automatically connects to the backend URL specified in `.env`:

```javascript
const API_URL = process.env.REACT_APP_BACKEND_URL;
```

All API calls use the `/api` prefix and include JWT token in headers.
