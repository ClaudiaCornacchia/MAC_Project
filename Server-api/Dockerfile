# 1. Use a lightweight Node base image (Alpine version to save space)
FROM node:18-alpine

# 2. Set the working directory inside the container
WORKDIR /app

# 3. Copy dependency files first (to leverage Docker cache)
# We copy both package.json and package-lock.json (if it exists)
COPY package*.json ./

# 4. Install dependencies
# 'npm ci' is faster and safer than 'npm install' for build environments
# --only=production avoids installing dev tools (e.g., test, eslint)
RUN npm ci --only=production

# 5. Copy the rest of the source code into the container
COPY . .

# 6. Cloud Run injects the PORT variable (usually 8080)
# Exposing the port is documentation for Docker, but helpful
EXPOSE 8080

# 7. Command to start the server
# Ensure your package.json has a "start" script, or modify this line
CMD ["npm", "start"]