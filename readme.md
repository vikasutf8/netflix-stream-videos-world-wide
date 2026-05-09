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
