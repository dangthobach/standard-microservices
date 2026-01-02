# Multi-stage Dockerfile for Angular Frontend
# Stage 1: Build Angular application
# Stage 2: Serve with Nginx

# ================================
# Stage 1: Build
# ================================
FROM node:20-alpine AS builder

# Set working directory
WORKDIR /app

# Step 1: Copy package files first (for dependency caching)
# This layer is CACHED until package.json changes
COPY package*.json ./

# Step 2: Install dependencies (cached layer)
# Use --legacy-peer-deps if there are peer dependency conflicts
RUN npm ci --legacy-peer-deps

# Step 3: Copy application source (only this executes on code changes)
COPY . .

# Build Angular application for production
# --configuration production enables:
# - AOT compilation
# - Tree shaking
# - Minification
# - Source maps disabled
RUN npm run build -- --configuration production

# ================================
# Stage 2: Production Nginx Server
# ================================
FROM nginx:1.25-alpine

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built application from builder stage
COPY --from=builder /app/dist/enterprise-frontend/browser /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
