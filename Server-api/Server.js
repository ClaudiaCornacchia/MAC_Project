const express = require('express');
const QRCode = require('qrcode');
const admin = require('firebase-admin');
const { getStorage } = require('firebase-admin/storage');

admin.initializeApp({
    // If you are running this code in Cloud Run, it will use the default service account.
    // If you are running this code locally, it will look for the GOOGLE_APPLICATION_CREDENTIALS variable on your PC.
    storageBucket: 'mac-project-480111.firebasestorage.app'
});

const app = express();
app.use(express.json());

app.post('/generate-qr', async (req, res) => {
  try {
    // 1. CHECK SECURITY, take the token from the header
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
       return res.status(401).send('Unauthorized: No token provided');
    }
    const idToken = authHeader.split('Bearer ')[1];

    // 2. VERIFY TOKEN, if invalid, throw error
    const decodedToken = await admin.auth().verifyIdToken(idToken);

    const { boxId } = req.body; // Android sends us the ID
    if (!boxId) return res.status(400).send('Missing boxId');

    // 3. GENERATE THE QR (As Buffer in memory)
    const qrData = `boxapp://box/${boxId}`; 
    const qrBuffer = await QRCode.toBuffer(qrData);

    // 4. UPLOAD TO FIREBASE STORAGE
    const bucket = getStorage().bucket();
    const file = bucket.file(`qrcodes/${boxId}.png`);
    
    await file.save(qrBuffer, {
      metadata: { contentType: 'image/png' },
      public: true // Or manage access tokens
    });

    // 5. GET THE PUBLIC URL
    // (In Firebase Storage public files have a standard format)
    const publicUrl = `https://storage.googleapis.com/${bucket.name}/qrcodes/${boxId}.png`;

    // 6. RESPOND TO ANDROID
    res.json({ qrCodeUrl: publicUrl });

  } catch (error) {
    console.error("Security Error:", error.message);
    res.status(403).send('Unauthorized: Invalid token')
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`QR server running on port ${port}`));