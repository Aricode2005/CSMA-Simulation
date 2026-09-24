from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel
import subprocess
import asyncio
import os
import sys
import csv

app = FastAPI()

class LiveStartRequest(BaseModel):
    strategy: str
    useCD: bool
    p: float
    numStations: int
    speedMultiplier: float = 1.0

active_connections: list[WebSocket] = []
live_process = None

@app.websocket("/ws-simulation")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    active_connections.append(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        if websocket in active_connections:
            active_connections.remove(websocket)

async def broadcast_ws(message: str):
    disconnected = []
    for conn in active_connections:
        try:
            await conn.send_text(message)
        except Exception:
            disconnected.append(conn)
    for conn in disconnected:
        if conn in active_connections:
            active_connections.remove(conn)

@app.post("/api/start-live")
async def start_live(req: LiveStartRequest):
    global live_process
    if live_process and live_process.returncode is None:
        try:
            live_process.kill()
        except Exception:
            pass

    compile_result = subprocess.run(
        'javac src/main/java/csma/*.java src/main/java/csma/strategy/*.java',
        shell=True, capture_output=True, text=True
    )
    if compile_result.returncode != 0:
        return JSONResponse(status_code=500, content={"error": "Java compilation failed: " + compile_result.stderr})

    async def run_java_live():
        global live_process
        live_process = await asyncio.create_subprocess_exec(
            "java", "-cp", "src/main/java", "csma.Simulator", "LIVE",
            req.strategy, str(req.useCD).lower(), str(req.p), str(req.numStations), str(req.speedMultiplier),
            stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE
        )
        while True:
            line = await live_process.stdout.readline()
            if not line:
                break
            text = line.decode('utf-8').strip()
            if text.startswith("[WS]"):
                json_data = text[5:].strip()
                await broadcast_ws(json_data)
        await live_process.wait()
        live_process = None

    asyncio.create_task(run_java_live())
    return {"message": f"Simulation started: {req.numStations} stations, {req.strategy}, CD={'ON' if req.useCD else 'OFF'}"}

@app.post("/api/run-batch")
async def run_batch():
    try:
        compile_result = subprocess.run(
            'javac src/main/java/csma/*.java src/main/java/csma/strategy/*.java',
            shell=True, capture_output=True, text=True
        )
        if compile_result.returncode != 0:
            return JSONResponse(status_code=500, content={"error": "Compilation failed: " + compile_result.stderr})

        # Launch Java batch ASYNCHRONOUSLY and stream output to browser
        async def run_batch_stream():
            await broadcast_ws('{"type": "BATCH_LOG", "msg": "Compiling Java code... Done."}')
            await broadcast_ws('{"type": "BATCH_LOG", "msg": "Starting batch simulation engine..."}')
            
            process = await asyncio.create_subprocess_exec(
                "java", "-cp", "src/main/java", "csma.Simulator",
                stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE
            )
            
            while True:
                line = await process.stdout.readline()
                if not line:
                    break
                text = line.decode('utf-8').strip()
                if text:
                    safe = text.replace('\\', '\\\\').replace('"', '\\"')
                    await broadcast_ws('{"type": "BATCH_LOG", "msg": "' + safe + '"}')
            
            await process.wait()
            
            await broadcast_ws('{"type": "BATCH_LOG", "msg": "Java simulation finished. Generating plots with Python..."}')
            
            plot_proc = await asyncio.create_subprocess_exec(
                sys.executable, "plot_results.py",
                stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE
            )
            stdout, _ = await plot_proc.communicate()
            for pline in stdout.decode('utf-8').strip().split('\n'):
                if pline.strip():
                    safe = pline.strip().replace('\\', '\\\\').replace('"', '\\"')
                    await broadcast_ws('{"type": "BATCH_LOG", "msg": "' + safe + '"}')
            
            await broadcast_ws('{"type": "BATCH_DONE", "msg": "All experiments completed! Tables and plots ready."}')
        
        asyncio.create_task(run_batch_stream())
        return {"message": "Batch experiments started. Watch the progress log below..."}
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})

@app.get("/api/csv/{name}")
async def get_csv_data(name: str):
    """Return CSV data as JSON for rendering tables in the UI."""
    file_path = f"{name}.csv"
    if not os.path.exists(file_path):
        return JSONResponse(status_code=404, content={"error": "CSV not found. Run batch first."})
    
    rows = []
    with open(file_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return {"columns": list(rows[0].keys()) if rows else [], "data": rows}

@app.get("/api/plot/{name}")
async def get_plot(name: str):
    file_path = f"{name}.png"
    if os.path.exists(file_path):
        return FileResponse(file_path, media_type="image/png")
    return JSONResponse(status_code=404, content={"error": "Plot not found."})

@app.get("/")
async def root():
    return FileResponse("src/main/resources/static/index.html")

@app.get("/{filename:path}")
async def get_static(filename: str):
    path = f"src/main/resources/static/{filename}"
    if os.path.exists(path):
        return FileResponse(path)
    return JSONResponse(status_code=404, content={"error": "Not found"})

if __name__ == "__main__":
    import uvicorn
    # Render provides the port via the PORT environment variable
    port = int(os.environ.get("PORT", 8080))
    print("=" * 60)
    print("  CSMA/CD Network Simulator - Web Server")
    print(f"  Listening on http://0.0.0.0:{port}")
    print("=" * 60)
    uvicorn.run(app, host="0.0.0.0", port=port)
