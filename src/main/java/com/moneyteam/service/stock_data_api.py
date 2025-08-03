from flask import Flask, request, jsonify
import yfinance as yf
import json

app = Flask(__name__)

def format_dollar(value):
    """Format values as dollar amounts with commas."""
    if value is None:
        return "N/A"
    return f"${value:,.2f}"


def format_percentage(value):
    """Format values as percentages."""
    if value is None:
        return "N/A"
    return f"{value * 100:.2f}%"


def fetch_stock_data(ticker):
    try:
        # Fetch stock data using yfinance
        stock = yf.Ticker(ticker)

        # Extract key metrics
        info = stock.info
        if not info:
            raise ValueError("Stock data unavailable. The ticker might be mistyped or delisted.")

        key_metrics = {
            "Stock Name": info.get("shortName", "N/A"),
            "P/E Ratio": info.get("trailingPE"),
            "P/B Ratio": info.get("priceToBook"),
            "Market Cap": info.get("marketCap"),
            "Beta": info.get("beta"),
            "Dividend Yield": info.get("dividendYield"),
            "Earnings Per Share (EPS)": info.get("trailingEps"),
        }

        # Format dollar amounts and percentages
        formatted_metrics = {
            "Stock Name": key_metrics["Stock Name"],
            "P/E Ratio": key_metrics["P/E Ratio"],
            "P/B Ratio": key_metrics["P/B Ratio"],
            "Market Cap": format_dollar(key_metrics["Market Cap"]),
            "Beta": key_metrics["Beta"],
            "Dividend Yield": format_percentage(key_metrics["Dividend Yield"]),
            "Earnings Per Share (EPS)": format_dollar(key_metrics["Earnings Per Share (EPS)"]),
        }

        # Extract financial statements
        financial_statements = {
            "Total Revenue": stock.financials.loc["Total Revenue"].iloc[0] if "Total Revenue" in stock.financials.index else None,
            "Net Income": stock.financials.loc["Net Income"].iloc[0] if "Net Income" in stock.financials.index else None,
            "Total Assets": stock.balance_sheet.loc["Total Assets"].iloc[0] if "Total Assets" in stock.balance_sheet.index else None,
            "Total Liabilities": stock.balance_sheet.loc["Total Liabilities"].iloc[0] if "Total Liabilities" in stock.balance_sheet.index else None,
        }

        # Format financial data
        formatted_statements = {key: format_dollar(value) for key, value in financial_statements.items()}

        # Combine data into a single dictionary
        result = {
            "Key Metrics": formatted_metrics,
            "Financial Statements": formatted_statements,
        }

        # Save data as JSON
        output_file = f"{ticker}_data.json"
        with open(output_file, "w") as json_file:
            json.dump(result, json_file, indent=4)

        print(f"\nData for {ticker} saved to {output_file}")
        return result

    except Exception as e:
        print(f"Error fetching data for {ticker}: {str(e)}")
        return None


if __name__ == "__main__":
    ticker_symbol = input("Enter stock ticker symbol: ").upper()
    stock_data = fetch_stock_data(ticker_symbol)

    if stock_data:
        print("\nKey Metrics:")
        for key, value in stock_data["Key Metrics"].items():
            print(f"{key}: {value}")

        print("\nFinancial Statements:")
        for key, value in stock_data["Financial Statements"].items():
            print(f"{key}: {value}")

# @app.route('/api/stock/<ticker>', methods=['GET'])
# def get_stock_data(ticker):
#     try:
#         stock_data = fetch_stock_data(ticker)
#         return jsonify(stock_data), 200
#     except ValueError as e:
#         return jsonify({"error": str(e)}), 404