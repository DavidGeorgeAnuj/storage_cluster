from fastapi import WebSocket, WebSocketDisconnect, status
from sqlalchemy.orm import Session
from datetime import datetime
import logging
from app.models.device import Device
from app.core.database import SessionLocal
from app.core.connection_manager import manager 
from app.models.chunk_replication import ChunkReplication
import asyncio 

logger = logging.getLogger("uvicorn")
async def device_ws(ws: WebSocket):
    logger.info("WS: Incoming connection")
    await ws.accept()
    logger.info("WS: Accepted connection")

    db: Session = SessionLocal()
    current_device_id = None

    try:
        logger.info("WS: Waiting for register payload...")
        payload = await ws.receive_json()
        logger.info(f"WS: Received payload: {payload}")

        if payload.get("type") != "register":
            logger.warning("WS: First message was not 'register'. Closing.")
            await ws.close(code=status.WS_1008_POLICY_VIOLATION)
            return

        fingerprint = payload.get("fingerprint")
        logger.info(f"WS: Fingerprint received: {fingerprint}")

        device = db.query(Device).filter(
            Device.device_fingerprint == fingerprint
        ).first()

        if not device:
            logger.warning("WS: Device not found in DB. Closing.")
            await ws.close(
                code=status.WS_1008_POLICY_VIOLATION,
                reason="Device not registered"
            )
            return

        logger.info(f"WS: Device found. ID={device.device_id}, Status={device.status}")

        if device.status != "ONLINE":
            logger.info(f"WS: Device {device.device_id} transitioning to ONLINE")
            device.status = "ONLINE"
            device.last_seen = datetime.utcnow()
            db.commit()

        current_device_id = device.device_id

        logger.info(f"WS: Registering device {current_device_id} with connection manager")
        await manager.connect(current_device_id, ws)

        logger.info(f"WS: Sending ready to device {current_device_id}")
        await ws.send_json({
            "type": "ready",
            "device_id": current_device_id,
        })

        logger.info("WS: Entering main loop")

        while True:
            logger.info(f"WS: Waiting for message from {current_device_id}")
            msg = await ws.receive_json()
            logger.info(f"WS: Message received from {current_device_id}: {msg}")

            msg_type = msg.get("type")

            if msg_type == "CHUNK_STORED_SUCCESS":
                chunk_id = msg.get("chunk_id")
                logger.info(
                    f"WS: CHUNK_STORED_SUCCESS received. Device={current_device_id}, Chunk={chunk_id}"
                )
                db.rollback()
                replication_entry = db.query(ChunkReplication).filter(
                    ChunkReplication.chunk_id == chunk_id,
                    ChunkReplication.device_id == current_device_id
                ).first()

                if replication_entry:
                    logger.info("WS: Replication entry found. Marking ACTIVE.")
                    replication_entry.replica_status = "ACTIVE"
                    db.commit()
                    logger.info(
                        f"WS: Chunk {chunk_id} is now ACTIVE on Device {current_device_id}"
                    )
                else:
                    logger.warning(
                        f"WS: No replication entry found for Device={current_device_id}, Chunk={chunk_id}"
                    )
            elif msg_type == "CHUNK_STORE_FAILED":
                chunk_id = msg.get("chunk_id")
                print(f"Chunk_id:{chunk_id} storage failed")

            else:
                logger.warning(
                    f"WS: Unknown message type from {current_device_id}: {msg_type}"
                )
    except asyncio.TimeoutError:
        await ws.send_json({"type": "ping"})
    except WebSocketDisconnect:
        logger.info(f"WS: Device {current_device_id} disconnected.")

    except Exception as e:
        logger.exception(f"WS: Unexpected error for device {current_device_id}: {e}")

    finally:
        logger.info(f"WS: Cleaning up connection for {current_device_id}")

        if current_device_id:
            manager.disconnect(current_device_id)
            logger.info(f"WS: Disconnected {current_device_id} from manager")

        db.close()
        logger.info("WS: DB session closed")