# How Netflix Streams Video to Millions of Users

## Problem Statement

A movie studio uploads a raw 2-hour movie file to Netflix.

- Raw movie size = 50 GB
- Millions of users stream the same movie simultaneously
- Users watch on:
  - Mobile
  - Laptop
  - Smart TV
  - Tablet
- Every user has different internet bandwidth:
  - 5G
  - 4G
  - Wi-Fi
  - Slow internet connections

---

# Main Challenges (SSQ)

## 1. Storage

### Problem
Storing huge raw movie files for thousands of movies and shows.

### Netflix Solution
- Store movies in distributed cloud storage
- Replicate data across multiple regions
- Compress and encode files efficiently

### Technologies
- AWS S3 / Open Connect Storage
- Distributed File Systems
- Data Replication

---

## 2. Streaming

### Problem
10 million users watching the same movie at the same time.

### Netflix Solution
- Use CDN (Content Delivery Network)
- Store movie copies near users globally
- Deliver chunks instead of full file download

### How Streaming Works
Movie is divided into:
- Small chunks (2–10 seconds)

Player downloads chunks continuously while playing.

### Technologies
- CDN
- HTTP Streaming
- TCP/IP
- Edge Servers

---

## 3. Quality Adaptation

### Problem
Every user has different internet speed and device capability.

### Netflix Solution
Netflix creates multiple versions of the same movie:

| Quality | Resolution | Internet Required |
|----------|-------------|------------------|
| 240p | Low | Slow Internet |
| 480p | SD | 3G/4G |
| 720p | HD | Moderate |
| 1080p | Full HD | Fast Wi-Fi |
| 4K | Ultra HD | High-Speed Fiber |

Player automatically switches quality based on:
- Bandwidth
- Device screen size
- Network stability

This is called:

## Adaptive Bitrate Streaming (ABR)

# HLD Imaga
<img width="1446" height="1162" alt="image" src="https://github.com/user-attachments/assets/dde85b50-23fd-4e70-91f0-c456c5557acb" />


# LLD  Images
<img width="1019" height="841" alt="image" src="https://github.com/user-attachments/assets/c0e16d6d-fa95-403c-8fd1-6556d69d0a1d" />



---

# Complete Netflix Pipeline

## Step 1 — Upload Raw Movie
Studio uploads:
- 50 GB raw movie

---

## Step 2 — Encoding & Compression
Netflix converts movie into:
- Multiple resolutions
- Multiple bitrates
- Different formats

Example:
- 240p
- 480p
- 720p
- 1080p
- 4K

Compression codecs:
- H.264
- H.265
- AV1

---

## Step 3 — Chunking
Movie split into small chunks:
- 2–10 seconds each

Example:
movie_chunk_1.ts
movie_chunk_2.ts
movie_chunk_3.ts

---

## Step 4 — CDN Distribution
Chunks copied to global edge servers.

Goal:
Serve content from nearest location.

---

## Step 5 — User Plays Movie
Player requests:
- Manifest file
- Initial chunks

---

## Step 6 — Adaptive Streaming
Player continuously checks:
- Internet speed
- Buffer health
- Device performance

Then switches quality dynamically.

Example:
- Slow network → 480p
- Fast Wi-Fi → 1080p

Without stopping the movie.

---

# Core System Design Components

## Backend Services
- Encoding Service
- Metadata Service
- Recommendation Service
- Streaming Service
- Authentication Service

---

## Infrastructure
- CDN
- Edge Servers
- Distributed Storage
- Load Balancers
- Monitoring Systems

---

# Important Concepts

## CDN (Content Delivery Network)
Servers distributed globally to reduce latency.

---

## Buffering
Preloading chunks before playback.

---

## Transcoding
Converting raw movie into multiple formats and qualities.

---

## Adaptive Bitrate Streaming
Dynamic quality switching based on bandwidth.

---

# Final Goal of Netflix

Netflix optimizes:

- Storage Cost
- Streaming Speed
- Video Quality
- Low Latency
- High Availability
- Global Scalability

To stream videos smoothly to millions of users simultaneously.
EOF


# Docker compose 
# ╔══════════════════════════════════════════════════════════════════╗
# ║              🎬  NETFIIX — Infrastructure Stack                  ║
# ║         MySQL · Kafka (KRaft) · Redis · Redis Insight            ║
# ╚══════════════════════════════════════════════════════════════════╝


# ──────────────────────────────────────────
# 📦  SERVICES & PORTS
# ──────────────────────────────────────────

# Service         Image                        Port(s)
# ─────────────── ──────────────────────────── ──────────────────────
# mysql           mysql:8.0                    3306
# kafka           confluentinc/cp-kafka:7.6.0  9092 (internal)
#                                              29092 (host)
# redis           redis:7.2-alpine             6379
# redis-insight   redis/redisinsight:latest    5540  → http://localhost:5540


# ──────────────────────────────────────────
# ✅  PREREQUISITES
# ──────────────────────────────────────────

# Make sure these are installed before running:

docker --version          # Docker >= 24.x
docker compose version    # Docker Compose >= 2.x


# ──────────────────────────────────────────
# 🚀  START — spin up all services
# ──────────────────────────────────────────

docker compose up -d
# -d → detached mode (runs in background)


# ──────────────────────────────────────────
# 🔍  STATUS — check running containers
# ──────────────────────────────────────────

docker compose ps
# shows: container name, image, status, ports


# ──────────────────────────────────────────
# 📋  LOGS — view service output
# ──────────────────────────────────────────

docker compose logs -f              # all services (follow mode)
docker compose logs -f mysql        # MySQL only
docker compose logs -f kafka        # Kafka only
docker compose logs -f redis        # Redis only
docker compose logs -f redis-insight


# ──────────────────────────────────────────
# 🔌  CONNECT — credentials & access
# ──────────────────────────────────────────

# MySQL
mysql -h 127.0.0.1 -P 3306 -u appuser -papppassword appdb
#   root password : rootpassword
#   app user      : appuser / apppassword
#   database      : appdb

# Kafka  (list topics from host machine)
kafka-topics --bootstrap-server localhost:29092 --list
#   internal (container) : kafka:9092
#   external (host)      : localhost:29092

# Redis CLI
redis-cli -h 127.0.0.1 -p 6379 -a redispassword ping
# expected response → PONG

# Redis Insight (GUI)
open http://localhost:5540
#   Host     : redis
#   Port     : 6379
#   Password : redispassword


# ──────────────────────────────────────────
# 🏥  HEALTH — verify all containers healthy
# ──────────────────────────────────────────

docker inspect --format='{{.Name}} → {{.State.Health.Status}}' \
  mysql kafka redis
# expected → /mysql → healthy
#            /kafka → healthy
#            /redis → healthy


# ──────────────────────────────────────────
# 🔄  COMMON OPERATIONS
# ──────────────────────────────────────────

# Restart a single service
docker compose restart kafka

# Stop all services (keeps volumes/data)
docker compose stop

# Stop + remove containers (keeps volumes/data)
docker compose down

# ⚠️  Stop + wipe ALL data (volumes deleted)
docker compose down -v


# ──────────────────────────────────────────
# 🛠️  TROUBLESHOOTING
# ──────────────────────────────────────────

# Port already in use?
lsof -i :3306     # find what's using MySQL port
lsof -i :6379     # find what's using Redis port
lsof -i :9092     # find what's using Kafka port

# Force recreate containers (picks up config changes)
docker compose up -d --force-recreate

# Remove dangling images to free disk space
docker image prune -f

# Enter a running container shell
docker exec -it mysql bash
docker exec -it kafka bash
docker exec -it redis sh       # alpine → sh not bash


# ──────────────────────────────────────────
# 📁  VOLUMES — where data lives
# ──────────────────────────────────────────

docker volume ls | grep netfiix
# netfiix_mysql-data    → MySQL databases
# netfiix_redis-data    → Redis AOF snapshots
# netfiix_kafka-data    → Kafka topic partitions

# Inspect a volume path on disk
docker volume inspect netfiix_mysql-data


# ──────────────────────────────────────────
# ⚠️  PRODUCTION REMINDERS
# ──────────────────────────────────────────

# Change ALL default passwords before deploying:
#   MYSQL_ROOT_PASSWORD  : rootpassword   ← change this
#   MYSQL_PASSWORD       : apppassword    ← change this
#   Redis --requirepass  : redispassword  ← change this
#
# Use a .env file and reference via ${VAR_NAME} in docker-compose.yml
# Never commit real credentials to git







# Git Branching Strategy

## Main Branches

### `stream-dev`
- Primary development branch.
- All new features, bug fixes, and enhancements are created from this branch.
- After completion, changes must be merged back into `stream-dev`.

### `stream-test`
- Testing and QA integration branch.
- `stream-dev` is always merged into `stream-test` for validation and testing.

### `stream-master`
- Production/stable release branch.
- Only verified and tested code from `stream-test` should be merged into `stream-master`.

---

## Development Flow

```text
stream-dev
   └── feature/fix branches
           └── merge back into stream-dev
                   └── merge into stream-test
                           └── merge into stream-master