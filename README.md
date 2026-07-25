# Europe Flight Deals 🇪🇺✈️

A Java automation script that scans **India → Europe** round-trip flights and surfaces anything **under ₹20,000 INR**.

Built for travellers who obsessively check fares and occasionally spot those rare sub-₹20k deals to Europe.

## How it works

1. Reads your Amadeus API credentials from `application.properties`
2. Searches round-trip fares from major Indian airports (DEL, BOM, BLR, etc.)
3. Checks 22+ European destinations (Paris, Amsterdam, Budapest, Warsaw, etc.)
4. Converts all prices to INR and filters deals under your threshold (default: ₹20,000)
5. Prints ranked results with Google Flights booking links

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **Free Amadeus API account** → [developers.amadeus.com/register](https://developers.amadeus.com/register)

## Quick start

```bash
# 1. Clone / copy this project
cd europe-flight-deals

# 2. Configure API keys
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties with your Amadeus API key & secret

# 3. Build
mvn clean package -DskipTests

# 4. Run
java -jar target/europe-flight-deals-1.0.0.jar
```

Or run directly with Maven:

```bash
mvn compile exec:java -Dexec.mainClass="com.travel.flightdeals.FlightDealsApp"
```

## Sample output

```
========================================================================
  EUROPE FLIGHT DEALS — India → Europe under ₹20000
========================================================================
  Origins      : DEL, BOM, BLR, MAA, HYD, CCU, AMD
  Destinations : 22 European airports
  Departure    : ~2026-08-24 (+14 day return)
========================================================================

Found 2 deal(s) under ₹20,000:

 1. ₹18,450 | DEL → BUD (Budapest) | 2026-08-24 → 2026-09-07 | W6 | 1 stop(s)
    Book: https://www.google.com/travel/flights?q=...

 2. ₹19,200 | BOM → WAW (Warsaw) | 2026-08-24 → 2026-09-07 | LO | 1 stop(s)
    Book: https://www.google.com/travel/flights?q=...
```

## Configuration

Edit `src/main/resources/application.properties`:

| Key | Default | Description |
|-----|---------|-------------|
| `price.maxInr` | `20000` | Maximum price threshold in INR |
| `search.departureDaysAhead` | `30` | Search departures N days from today |
| `search.returnDaysAfterDeparture` | `14` | Round-trip length in days |
| `search.originAirports` | `DEL,BOM,BLR,...` | Comma-separated origin IATA codes |
| `search.europeAirports` | *(empty = all)* | Restrict to specific European airports |

## Automate daily checks

### macOS / Linux (cron)

```bash
# Run every morning at 8 AM
0 8 * * * cd /path/to/europe-flight-deals && java -jar target/europe-flight-deals-1.0.0.jar >> ~/flight-deals.log 2>&1
```

### GitHub Actions (free, runs in cloud)

Create `.github/workflows/daily-flight-scan.yml`:

```yaml
name: Daily Europe Flight Scan
on:
  schedule:
    - cron: '0 3 * * *'  # 8:30 AM IST
  workflow_dispatch:

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build
        run: mvn -q package -DskipTests
      - name: Run scan
        env:
          AMADEUS_KEY: ${{ secrets.AMADEUS_KEY }}
          AMADEUS_SECRET: ${{ secrets.AMADEUS_SECRET }}
        run: |
          cat > src/main/resources/application.properties <<EOF
          amadeus.api.key=$AMADEUS_KEY
          amadeus.api.secret=$AMADEUS_SECRET
          amadeus.api.baseUrl=https://test.api.amadeus.com
          price.maxInr=20000
          EOF
          java -jar target/europe-flight-deals-1.0.0.jar
```

Add `AMADEUS_KEY` and `AMADEUS_SECRET` as GitHub repo secrets.

## Tips for finding sub-₹20k Europe fares

- **Eastern Europe** (Budapest, Warsaw, Prague) is often ₹3–5k cheaper than Paris/London
- **Turkish Airlines / Qatar / Air Arabia** frequently have flash sales
- Search **4–8 weeks ahead** — last-minute Europe rarely drops this low
- Try **DEL and BOM** — they usually have the most competitive pricing
- Run the script **daily** — these deals appear and vanish within hours

## Project structure

```
europe-flight-deals/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/travel/flightdeals/
    │   │   ├── FlightDealsApp.java          # Main entry point
    │   │   ├── config/AppConfig.java        # Loads application.properties
    │   │   ├── model/                       # FlightDeal, Airport records
    │   │   ├── service/AmadeusFlightService.java  # API integration
    │   │   └── util/                        # JSON parsing, currency conversion
    │   └── resources/
    │       ├── application.properties.example
    │       └── logback.xml
    └── test/java/                           # Unit tests
```

## API notes

- Uses [Amadeus Self-Service API](https://developers.amadeus.com/) (free test tier: ~2,000 calls/month)
- Test environment (`test.api.amadeus.com`) returns real-ish data; switch to `api.amadeus.com` for production
- The script requests `currencyCode=INR` directly from Amadeus when possible
- Rate limiting: 250ms delay between searches to stay within API limits

## License

MIT — use freely for your travel hacking adventures.
