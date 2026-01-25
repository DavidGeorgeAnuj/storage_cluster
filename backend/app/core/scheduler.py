import asyncio
from app.core.database import SessionLocal
from app.services.offline_detection import mark_offline_devices
import app.models  # register models

async def offline_monitor_loop():
    while True:
        await asyncio.sleep(30)

        db = SessionLocal()
        try:
            mark_offline_devices(db)
        except Exception as e:
            print("Offline detection error:", e)
            db.rollback()
        finally:
            db.close()
