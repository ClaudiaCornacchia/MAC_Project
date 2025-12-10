# MAC_Project
Nella directory Server-api
npm install express qrcode firebase-admin

- Per creare l'immagine (chiamata qr-server) Docker eseguire:
     docker build -t qr-server .
- Per eseguire il container localmente:
    docker run -p 3000:3000 qr-server