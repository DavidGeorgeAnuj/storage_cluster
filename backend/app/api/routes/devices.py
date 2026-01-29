from fastapi import APIRouter
from app.core.database import SessionLocal
from app.services.registration import register_device
import app.models

router = APIRouter()

@router.post("/devices/register")
def register_device_endpoint(payload: dict):
    db = SessionLocal()
    try:
        result = register_device(
            db=db,
            user_id= payload["user_id"],
            device_name = payload["device_name"],
            storage_capacity= payload["storage_capacity"],
            available_storage=payload["available_storage"],
            fingerprint = payload["fingerprint"],
        )
        return result
    finally:
        db.close()
