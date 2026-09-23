# 🌐 CSMA/CD Network Simulator

![CSMA Simulator](https://img.shields.io/badge/Status-Complete-success.svg)
![Java Engine](https://img.shields.io/badge/Engine-Java-orange.svg)
![FastAPI](https://img.shields.io/badge/Backend-FastAPI-009688.svg)
![WebSockets](https://img.shields.io/badge/Live-WebSockets-blue.svg)

A high-performance, full-stack network simulation application that mathematically models and visually demonstrates the **Carrier Sense Multiple Access with Collision Detection (CSMA/CD)** protocol at the Data-Link Layer.

## ✨ Features

- **Live Real-Time Visualization:** Watch stations sense the channel, transmit frames, detect collisions, and back off in real-time. 
- **WebSocket Integration:** The pure Java discrete-event simulation engine streams "thoughts" and state-changes directly to the browser via WebSockets.
- **Multiple MAC Strategies Supported:**
  - **1-Persistent CSMA:** Transmit immediately when the channel becomes idle.
  - **Non-Persistent CSMA:** Wait a random amount of time if the channel is busy.
  - **p-Persistent CSMA:** Transmit with probability `p` when idle.
  - **CSMA/CD:** Abort transmission early upon collision using a JAM signal, saving valuable channel time.
- **Binary Exponential Backoff (BEB):** Fully implemented collision resolution mechanism (up to 15 retries before dropping the frame).
- **Batch Experimentation:** Run thousands of frames in headless mode instantly to evaluate network throughput, average delay, and collision rates.
- **Automated Plotting:** Generates beautiful Matplotlib graphs and CSV data tables directly on the dashboard.

## 🏗️ Architecture

This project utilizes a modern microservice-style architecture to marry a mathematically perfect simulation engine with a beautiful user interface:

1. **Simulation Engine (Java):** A multithreaded discrete-event simulator. `SimClock`, `Station`, and `Channel` threads synchronize perfectly to simulate microsecond-level propagation delays and vulnerabilities.
2. **Backend Web Server (Python / FastAPI):** Acts as the orchestrator. Serves the UI, handles REST API requests, uses `subprocess` to spawn the Java engine, and bridges Java's `stdout` directly to connected browsers via WebSockets.
3. **Frontend UI (HTML/JS/Bootstrap):** A responsive, single-page dashboard for starting experiments and viewing live logs.
4. **Data Science Script (Python):** `pandas` and `matplotlib` automatically parse the simulation outputs to generate performance graphs.

## 🚀 How to Run Locally

### Prerequisites
- **Java JDK** (8 or newer)
- **Python** (3.9 or newer)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Aricode2005/CSMA-Simulation.git
   cd CSMA-Simulation
   ```
2. Install the required Python dependencies:
   ```bash
   pip install fastapi uvicorn websockets pandas matplotlib seaborn
   # or simply: pip install -r requirements.txt
   ```

### Starting the Server
Run the FastAPI backend:
```bash
python server.py
```
*The server will compile the Java engine automatically.*

Open your browser and navigate to:
**👉 http://localhost:8080**

## 📊 Experiments Included

The dashboard includes a one-click **"Run All Experiments"** button that executes the following academic tests:

1. **Experiment 1 (Effect of Probability `p`):** Evaluates how altering the transmission probability `p` affects network throughput and delay in a fixed 10-station network.
2. **Experiment 2 (Scalability & Strategy Comparison):** Compares all 4 MAC strategies as the network scales from 2 to 30 stations under heavy load, proving mathematically why CSMA/CD is the most efficient protocol.

## 📝 License
This project was developed as a Computer Networks laboratory assignment. Feel free to use it for educational purposes!
