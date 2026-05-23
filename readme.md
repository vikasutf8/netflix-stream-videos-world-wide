# How Netflix Streams Video to Millions of Users

## Problem Statement
A movie studio uploads a raw 2-hour movie file to Netflix.

- Raw movie size can be very large (for example, 50 GB).
- Millions of users can stream the same movie at the same time.
- Users watch on mobile, laptop, smart TV, and tablet.
- Internet quality is different for every user (5G, 4G, Wi-Fi, slow networks).

---

## Main Challenges (SSQ)

### 1) Storage
**Problem**
- Raw movie files are huge, and the catalog is massive.

**Solution**
- Store videos in distributed cloud object storage.
- Replicate data across regions for reliability.
- Compress and encode efficiently.

**Technologies**
- AWS S3 / Open Connect Storage
- Distributed storage systems
- Data replication strategies

---

### 2) Streaming
**Problem**
- Millions of concurrent viewers need low-latency playback.

**Solution**
- Use CDN edge delivery close to users.
- Stream short chunks instead of full-file download.

**How it works**
- Video is split into small chunks (2–10 seconds each).
- Player downloads chunks continuously during playback.

**Technologies**
- CDN
- HTTP streaming
- TCP/IP
- Edge servers

---

### 3) Quality Adaptation (ABR)
**Problem**
- Device capability and network speed vary per user.

**Solution**
- Generate multiple quality variants of the same movie.

**Typical renditions**
- 240p (low)
- 480p (SD)
- 720p (HD)
- 1080p (Full HD)
- 4K (Ultra HD)

**Player decision signals**
- Current bandwidth
- Buffer health
- Device/screen capability

This is called **Adaptive Bitrate Streaming (ABR)**.

---

## HLD Image
![HLD](https://github.com/user-attachments/assets/dde85b50-23fd-4e70-91f0-c456c5557acb)

## LLD Image
![LLD](https://github.com/user-attachments/assets/c0e16d6d-fa95-403c-8fd1-6556d69d0a1d)

---

## Complete Netflix Pipeline
#### Step 1: Upload Raw Movie
- Studio uploads the raw movie file to object storage.
- Initial metadata is registered.

#### Step 2: Encode and Compress
- Encoding service creates multiple resolutions and bitrates.
- Common codecs include H.264, H.265, and AV1.

#### Step 3: Chunking
- Encoded outputs are split into small `.ts` segments.
- Example sequence: `movie_chunk_1.ts`, `movie_chunk_2.ts`, `movie_chunk_3.ts`.

#### Step 4: CDN Distribution
- Segments and playlists are pushed to global edge locations.
- Users are served from the nearest available edge.

#### Step 5: Playback Start
- Player requests manifest (`.m3u8`) and initial segments.
- Playback starts quickly from buffered chunks.

#### Step 6: Adaptive Streaming Loop
- Player continuously evaluates network and buffer conditions.
- Quality switches dynamically (for example, 480p ↔ 1080p) without stopping playback.

---

## Important Concepts

### CDN (Content Delivery Network)
Globally distributed servers that reduce latency and improve throughput.

### Buffering
Preloading chunks before playback to avoid interruption.

### Transcoding
Converting one source video into multiple formats and quality levels.

### Adaptive Bitrate Streaming
Dynamic quality switching based on real-time playback conditions.

---

## Final Goal of Netflix
Netflix optimizes for:

- Storage cost
- Streaming speed
- Video quality
- Low latency
- High availability
- Global scalability

Result: smooth playback for millions of concurrent users.

---
## Microservice Ports
- **Content Service**: `6001`
- **Video Service**: `6002`
- **Encoding Service**: `6003`
- **Streaming Service**: `6004`

---
## Docker Compose (Infrastructure Stack)

### Services and Ports
- **MySQL**: `3306`
- **Kafka (KRaft)**: `9092` (internal), `29092` (host)
- **Redis**: `6379`
- **Redis Insight**: `5540` (`http://localhost:5540`)

### Prerequisites
```shell
docker --version
docker compose version
```

### Start All Services
```shell
docker compose up -d
```

### Check Container Status
```shell
docker compose ps
```

### View Logs
```shell
docker compose logs -f
docker compose logs -f mysql
docker compose logs -f kafka
docker compose logs -f redis
docker compose logs -f redis-insight
```

### Connect to Services
**MySQL**
```shell
mysql -h 127.0.0.1 -P 3306 -u appuser -papppassword appdb
```

**Kafka (list topics)**
```shell
kafka-topics --bootstrap-server localhost:29092 --list
```

**Redis**
```shell
redis-cli -h 127.0.0.1 -p 6379 -a redispassword ping
```

**Redis Insight**
```shell
open http://localhost:5540
```

### Health Check
```shell
docker inspect --format='{{.Name}} -> {{.State.Health.Status}}' mysql kafka redis
```

### Common Operations
```shell
docker compose restart kafka
docker compose stop
docker compose down
docker compose down -v
```

### Troubleshooting
```shell
lsof -i :3306
lsof -i :6379
lsof -i :9092
docker compose up -d --force-recreate
docker image prune -f
docker exec -it mysql bash
docker exec -it kafka bash
docker exec -it redis sh
```

### Volumes
```shell
docker volume ls | grep netfiix
docker volume inspect netfiix_mysql-data
```

### Production Reminder
Change all default passwords before deployment and keep credentials in `.env` variables (never commit real secrets).