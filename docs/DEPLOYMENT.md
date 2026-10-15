# Deployment (Heroku)

## Apps
| Purpose  | App                  | URL                                                     |
|----------|----------------------|---------------------------------------------------------|
| Backend  | bebetter-fitness-app | https://bebetter-fitness-app-0caa005ed4cc.herokuapp.com |
| Frontend | bebetter-fitness-app | https://bebetter-fitness-app-0caa005ed4cc.herokuapp.com |

Both apps are owned by Person 1 (billing via student credits). Other team members are collaborators.

## Buildpacks (order matters)
- Backend: 1. https://github.com/lstoll/heroku-buildpack-monorepo, 2. heroku/java
- Frontend: 1. https://github.com/lstoll/heroku-buildpack-monorepo, 2. heroku/nodejs

## Config vars (names only, never commit values)
- Backend: APP_BASE, FRONTEND_URL, DATABASE_URL (set automatically by Heroku Postgres)
- Frontend: APP_BASE, VITE_API_URL (baked in at build time → redeploy after changing it)

## How to deploy
Manual only: Deploy → Manual deploy → branch main → Deploy Branch.
Deploy the backend first, then the frontend. Automatic deploys are disabled.
Eco dynos sleep after 30 minutes without traffic; the first request afterwards takes a few seconds.

## Deployment 1
- Date: 2026-10-15
- Commit: ed4dd20
- Migrations: V1__grundlage, V2__nutzer
- Verified: /health returns 200 online, frontend shows "Backend verbunden", Flyway log ok