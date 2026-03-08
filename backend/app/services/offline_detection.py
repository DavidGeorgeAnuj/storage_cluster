from datetime import datetime, timezone, timedelta
from sqlalchemy.orm import Session

from app.models.device import Device
from app.models.chunk_replication import ChunkReplication
from app.core.connection_manager import manager

HEARTBEAT_TIMEOUT = timedelta(seconds=13)


def mark_offline_devices(db: Session):
    cutoff = datetime.now(timezone.utc) - HEARTBEAT_TIMEOUT

    devices = (
        db.query(Device)
        .filter(Device.last_heartbeat < cutoff)
        .filter(Device.status == "ONLINE")
        .all()
    )

    for device in devices:
        print(f"Marking device {device.device_id} as OFFLINE")

        device.status = "OFFLINE"

        # mark all replicas on that device as LOST
        replicas = (
            db.query(ChunkReplication)
            .filter(
                ChunkReplication.device_id == device.device_id,
                ChunkReplication.replica_status == "ACTIVE"
            )
            .all()
        )

        for r in replicas:
            print(f"Replica lost: chunk {r.chunk_id} on device {device.device_id}")
            r.replica_status = "LOST"

        # close websocket if still connected
        manager.disconnect(device.device_id)

    if devices:
        db.commit()