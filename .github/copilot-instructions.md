# Copilot Instructions – Phone Cluster Secure Storage System

## Project Overview
This project implements a **secure, zero-trust phone-cluster storage system** where:
- User files are **encrypted on the client**
- Encrypted chunks are **distributed across a cluster of Android phones**
- A **central coordination server** manages metadata, replication, and routing
- **Only the user device can decrypt data**
- Retrieval-Augmented Generation (RAG) is performed **entirely on the user device**

The system is designed as a **personal, self-hosted alternative to cloud storage**, using unused phones as storage nodes.

---

## System Components

### 1. User Device (Android – User Mode)
Responsibilities:
- Encrypt files before upload (AES-GCM)
- Chunk files into fixed-size blocks
- Generate and store **local vector embeddings** for RAG
- Perform cosine similarity search locally
- Decrypt retrieved data
- Provide Google-Drive-like UI

Security:
- Encryption keys never leave the user device
- Server and storage phones are zero-trust

---

### 2. Storage Phones (Android – Storage Node Mode)
Responsibilities:
- Maintain persistent outbound connection to server
- Store encrypted chunks
- Respond to store/retrieve commands
- Send periodic heartbeats

Assumptions:
- Honest-but-curious nodes
- Malicious storage nodes are outside current scope
- Nodes cannot decrypt or forge data (AES-GCM)

---

### 3. Coordination Server (FastAPI)
Responsibilities:
- Maintain WebSocket connections to storage nodes
- Track node liveness via heartbeats
- Maintain chunk → node mappings
- Assign replicas for fault tolerance
- Coordinate upload and download workflows

Security Model:
- Server never sees plaintext
- Server never stores encryption keys
- Server stores metadata only

---

## Communication Model

### REST APIs (Data Plane)
Used for:
- User file upload (encrypted)
- User file download requests
- Metadata and system status

REST is **stateless** and user-initiated.

### WebSockets (Control Plane)
Used for:
- Persistent storage node connections
- Heartbeats
- Server → phone commands
- Chunk retrieval coordination

All storage nodes initiate **outbound WebSocket connections**.

---

## Core Server Endpoints

### WebSocket
- `/ws/node`  
  Persistent connection for storage phones  
  Handles registration, heartbeats, and commands

### REST
- `POST /upload` – User uploads encrypted chunks
- `GET /download/{file_id}` – User requests retrieval
- `GET /status` – Cluster health and metadata

---

## Encryption Design

- AES-GCM for confidentiality + integrity
- Encryption happens **before upload**
- Storage nodes store ciphertext only
- Any tampering causes decryption failure
- Availability attacks are mitigated via replication

---

## RAG Design (Client-Side Only)

- Only **text-based RAG** is supported
- Files are converted to embeddings locally
- Embeddings stored on user device
- Vector search uses cosine similarity
- Server and storage nodes are NOT involved in RAG

Out of scope:
- Server-side RAG
- Cloud vector databases
- Homomorphic encryption
- Secure multi-party computation

---

## Threat Model (Explicit Assumptions)

In scope:
- Curious but honest storage nodes
- Network failures
- Phone churn

Out of scope:
- Malicious storage nodes
- Byzantine behavior
- Active data deletion attacks
- Global adversaries

---

## Development Guidelines for Copilot

- Do NOT implement encryption on server
- Do NOT store keys outside user device
- Prefer FastAPI async patterns
- Keep WebSocket logic minimal and event-driven
- Avoid tight coupling between RAG and server logic
- Treat storage nodes as unreliable but non-adversarial

---

## Future Work (Not Implemented Now)

- Image / video RAG
- Erasure coding
- Proof of retrievability
- Malicious node detection
- Public multi-user shared clusters
- Incentive or reputation systems

---

## Guiding Principle

> **Privacy first, simplicity second, performance third**

If a design choice risks exposing plaintext or keys, it is rejected.
