# Glacier & Garnish: Thermal Logistics API

A professional backend system for managing the complete thermal lifecycle of high-end ice sculpture
installations. The API transforms live environmental data into actionable catering logistics, ensuring
ice assets remain structurally sound and visually pristine for the duration of any event.

---

## Running the Application

```bash
# Option 1: Demo mode (no API key required, uses synthetic weather data)
mvn spring-boot:run

# Option 2: Live weather data from OpenWeatherMap
WEATHER_API_KEY=your_key_here mvn spring-boot:run
```

**Access Points**
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:thermaldb`)

The application seeds three demo Ice Projects on startup (London, Dubai, New York).

---

## API Overview

### Ice Projects (`/api/v1/projects`)

| Method | Path | Description |
|---|---|---|
| POST | `/projects` | Create a new Ice Project |
| GET | `/projects` | List all projects (optional `?status=` filter) |
| GET | `/projects/{id}` | Get project by ID |
| PUT | `/projects/{id}` | Full project update |
| PATCH | `/projects/{id}/status` | Update lifecycle status |
| DELETE | `/projects/{id}` | Delete project |
| POST | `/projects/{id}/equipment` | Add cooling equipment |
| DELETE | `/projects/{id}/equipment/{eqId}` | Remove cooling equipment |

### Thermal Calculations (`/api/v1/thermal`)

| Method | Path | Description |
|---|---|---|
| GET | `/thermal/{id}/calculate` | Full melt calculation (live weather) |
| GET | `/thermal/{id}/calculate/override` | Calculation with manual temperature override |
| GET | `/thermal/{id}/melt-rate` | Quick melt rate summary |

### Installation Schedule (`/api/v1/schedule`)

| Method | Path | Description |
|---|---|---|
| GET | `/schedule/{id}` | Generate installation schedule |
| POST | `/schedule/{id}/regenerate` | Regenerate with latest forecast |

### Alerts (`/api/v1/alerts`)

| Method | Path | Description |
|---|---|---|
| POST | `/alerts/check/{id}` | Check and generate temperature warnings |
| GET | `/alerts` | List all alerts |
| GET | `/alerts/unacknowledged` | List unacknowledged alerts |
| GET | `/alerts/{id}` | Get alert by ID |
| GET | `/alerts/project/{id}` | Alerts for a project |
| GET | `/alerts/project/{id}/recent` | Alerts in last 24 hours |
| PUT | `/alerts/{id}/acknowledge` | Acknowledge alert |

---

## Melt Rate Algorithm

The thermal calculation engine is grounded in **Newton's Law of Cooling** applied to a
convective heat transfer model.

### Physics Model

```
Heat transfer rate:   Q̇ = h · A · (T_eff - T_ice)   [Watts]
Melt rate:            ṁ = Q̇ / L_f                    [kg/s]
```

| Symbol | Value | Description |
|---|---|---|
| `h` | 10 W/m²·K | Convective heat transfer coefficient (free convection, still air) |
| `A` | derived | Effective sculpture surface area [m²] |
| `T_eff` | variable | Effective ambient temperature after cooling adjustments [°C] |
| `T_ice` | 0°C | Ice melting point |
| `L_f` | 334,000 J/kg | Latent heat of fusion for water ice |
| `ρ_ice` | 917 kg/m³ | Density of ice |

### Surface Area Estimation

Sculptures are approximated as a **sphere of equivalent volume** (conservative upper bound
for surface exposure):

```
A = 4π · (3V / 4π)^(2/3)
```

Where `V` is the sculpture volume in m³ (1 liter = 0.001 m³).

### Effective Temperature

The effective ambient temperature accounts for all active cooling sources:

```
T_eff = T_ambient - ΔT_venue - ΔT_fans - ΔT_dry_ice - ΔT_drip_trays
```

| Source | Reduction |
|---|---|
| Venue AC (INDOOR_COOLED) | Configurable offset (default: 8°C) |
| Circulation Fan | −2.5°C per unit |
| Dry Ice Booster | −7.0°C per unit |
| Insulated Drip Tray | −0.5°C per unit |

Effective temperature is clamped to a minimum of 0.1°C (ice melts at any temperature above 0°C).

### Ice Type Multiplier

| Type | Multiplier | Rationale |
|---|---|---|
| Clear Ice | 0.85× | Higher density, air-free structure reduces surface porosity |
| White Ice | 1.00× | Baseline. Standard cloudy ice with trapped air bubbles |

### Threshold Definitions

| Threshold | Trigger | Description |
|---|---|---|
| **Structural Failure** | 40% mass lost | Sculpture loses structural integrity; removal is mandatory |
| **Peak Clarity End** | 10% mass lost | End of the period where ice is visually pristine |

### Melt Time Formulas

```
Total melt time            = M_initial / ṁ_kg_per_hour
Time to structural failure = (M_initial × 0.40) / ṁ_kg_per_hour
Peak clarity window        = (M_initial × 0.10) / ṁ_kg_per_hour
```

### Example Calculation

For a **250-liter clear ice sculpture** in a **25°C ambient** indoor venue with **2 fans + 2 dry ice boosters**:

```
V    = 0.250 m³
A    = 4π(3×0.25/4π)^(2/3) ≈ 0.381 m²
ρ_ice = 917 kg/m³ → M_initial = 917 × 0.250 = 229.25 kg

T_eff = 25 - 5 - 14 = 6°C  (2 fans×2.5 + 2 boosters×7)

Q̇ = 10 × 0.381 × 6 = 22.86 W
ṁ = 22.86 / 334,000 = 6.84×10⁻⁵ kg/s = 0.246 kg/h

Adjusted for clear ice: 0.246 × 0.85 = 0.209 kg/h

Time to structural failure = (229.25 × 0.40) / 0.209 ≈ 438.7 hours
Peak clarity window        = (229.25 × 0.10) / 0.209 ≈ 109.7 hours
```

---

## Alert Severity Matrix

| Severity | Temperature Delta | Recommended Action |
|---|---|---|
| LOW | +1–3°C | Monitor conditions |
| MEDIUM | +3–5°C | Review schedule, activate additional fans |
| HIGH | +5–8°C | Deploy emergency dry ice boosters |
| CRITICAL | +8°C or more | Immediate schedule revision or replacement ice |

---

## Installation Schedule Logic

The scheduler determines all time anchors from the moment of installation:

1. **Setup arrival** = Event start − (setup lead time + 30 min buffer)
2. **Installation** = Event start − setup lead time
3. **Peak clarity end** = Installation + peak clarity window hours
4. **Structural failure** = Installation + time-to-failure hours
5. **Mandatory teardown** = Structural failure − safety buffer (default: 30 min)

**Setup Lead Times by Sculpture Volume**

| Volume | Lead Time |
|---|---|
| < 50 L | 30 minutes |
| 50–200 L | 60 minutes |
| 200–500 L | 90 minutes |
| > 500 L | 120 minutes |

---

## Weather API Configuration

The application integrates with **OpenWeatherMap** (free tier supported).

```yaml
# application.yml
weather:
  api:
    key: ${WEATHER_API_KEY:demo-key}
    base-url: https://api.openweathermap.org/data/2.5
```

When `WEATHER_API_KEY` is not set (or equals `demo-key`), the application operates in
**demo mode**: it returns synthetic latitude-adjusted temperature data so all endpoints
remain fully functional without an API key.

Get a free API key at: https://openweathermap.org/api

---