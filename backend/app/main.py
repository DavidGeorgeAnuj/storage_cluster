from fastapi import FastAPI, WebSocket
from app.api.ws.devices import device_ws
import asyncio
from contextlib import asynccontextmanager

from app.core.scheduler import offline_monitor_loop
from app.core.repair_loop import repair_loop   # <-- add this

from app.api.routes.devices import router as device_router
from app.api.routes.files import router as file_router
from app.api.routes import chunks


@asynccontextmanager
async def lifespan(app: FastAPI):

    offline_task = asyncio.create_task(offline_monitor_loop())
    repair_task = asyncio.create_task(repair_loop())

    print("Startup: Background loops started.")

    yield

    offline_task.cancel()
    repair_task.cancel()

    try:
        await offline_task
    except asyncio.CancelledError:
        print("Offline monitor stopped.")

    try:
        await repair_task
    except asyncio.CancelledError:
        print("Repair loop stopped.")


app = FastAPI(lifespan=lifespan)
app.include_router(device_router)
app.include_router(chunks.router)
app.include_router(file_router)

@app.websocket("/ws/device")
async def ws_service(websocket : WebSocket):
    await device_ws(websocket)
    