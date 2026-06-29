# FindIt — Lost & Found App

A lost-and-found platform for campuses. Users post lost or found items with photos; the server automatically matches them using perceptual image hashing.

## Demo

[![Watch Demo](https://img.shields.io/badge/Watch%20Demo-YouTube-red?style=for-the-badge&logo=youtube)](https://your-demo-link-here)

> Replace the link above with your actual demo video URL.

## Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)
![Express](https://img.shields.io/badge/Express-000000?style=for-the-badge&logo=express&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

## Project Structure

```
findit/
├── android/        # Android app (Kotlin/Compose)
└── server/         # Node.js API
    ├── src/
    │   ├── index.js
    │   ├── db.js
    │   ├── middleware/authMiddleware.js
    │   ├── routes/
    │   │   ├── auth.js
    │   │   ├── items.js
    │   │   └── matches.js
    │   └── utils/imageHash.js
    └── uploads/    # Stored item images
```

## Getting Started

### Prerequisites

- Node.js 18+
- MySQL 8+
- Android Studio (for the app)

### Backend Setup

```bash
cd server
npm install
```

Create a `.env` file:

```env
PORT=3000
DB_HOST=localhost
DB_PORT=3306
DB_USER=lf_user
DB_PASSWORD=lf_pass123
DB_NAME=lostfound
JWT_SECRET=your_secret_key
```

Initialize the database using `docker/init.sql`, then start the server:

```bash
npm run dev   # development (nodemon)
npm start     # production
```

### Docker (Database)

```bash
cd docker
docker-compose up -d
```

This spins up MySQL and runs `init.sql` to create the schema.

### Android App

1. Open `android/` in Android Studio.
2. Update the base URL in `RetrofitClient.kt` to point to your server.
3. Build and run.

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/register` | No | Register a new user |
| POST | `/auth/login` | No | Login, returns JWT |
| GET | `/items` | No | List open items (filter by `type`, `category`) |
| POST | `/items` | Yes | Post a lost/found item (multipart with image) |
| GET | `/items/:id` | No | Get item by ID |
| GET | `/items/:id/matches` | Yes | Get matches for an item |
| PUT | `/items/:id/resolve` | Yes | Mark item as resolved |
| POST | `/matches/:id/confirm` | Yes | Confirm a match |
| POST | `/matches/:id/reject` | Yes | Reject a match |

## How Matching Works

When an item is posted with an image:
1. A perceptual hash is computed for the image.
2. The server queries open items of the opposite type (`LOST` ↔ `FOUND`) in the same category.
3. If the similarity score is ≥ 60%, a match record is created automatically.

## Database Schema

- `users` — registered users
- `items` — lost/found posts with image hash, category, location, status
- `matches` — links between a lost and a found item with a similarity score
- `messages` — per-match chat (schema ready, not yet wired up)

## Notes

- Images must be jpg, jpeg, png, or webp — max 5MB.
- Change `JWT_SECRET` before deploying.
- The `server/uploads/` folder is git-ignored; create it manually or let the server create it on first run.
