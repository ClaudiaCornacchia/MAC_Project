const express = require('express');
const QRCode = require('qrcode');
const admin = require('firebase-admin');
const { getStorage } = require('firebase-admin/storage');

// Initializza Firebase Admin SDK
// If we are in Cloud Run, use the default credentials
if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    admin.initializeApp({
        credential: admin.credential.applicationDefault(), // Usa l'identità del server Cloud Run
        storageBucket: 'mac-project-480111.firebasestorage.app'
    });
} else {
    // Fallback for local testing with explicit file
    try {
        const serviceAccount = require('./serviceAccountKey.json');
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            storageBucket: 'mac-project-480111.firebasestorage.app'
        });
    } catch (e) {
        console.error("I can't find credentials neither automatic nor local file.");
    }
}

const app = express();
app.use(express.json());

app.post('/generate-qr', async (req, res) => {
  try {
    const { boxId } = req.body; // Android sends us the ID
    
    if (!boxId) return res.status(400).send('Missing boxId');

    // 1. GENERATE THE QR (As Buffer in memory)
    // Advice: In the QR don't put just the ID, put a Deep Link!
    // Example: "boxapp://open?id=xyz" so if you scan it, the app opens.
    const qrData = `boxapp://box/${boxId}`; 
    const qrBuffer = await QRCode.toBuffer(qrData);

    // 2. UPLOAD TO FIREBASE STORAGE
    const bucket = getStorage().bucket();
    const file = bucket.file(`qrcodes/${boxId}.png`);
    
    await file.save(qrBuffer, {
      metadata: { contentType: 'image/png' },
      public: true // Or manage access tokens
    });

    // 3. GET THE PUBLIC URL
    // (In Firebase Storage public files have a standard format)
    const publicUrl = `https://storage.googleapis.com/${bucket.name}/qrcodes/${boxId}.png`;

    // 4. RESPOND TO ANDROID
    res.json({ qrCodeUrl: publicUrl });

  } catch (error) {
    console.error(error);
    res.status(500).send('Error generating QR');
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`QR server running on port ${port}`));