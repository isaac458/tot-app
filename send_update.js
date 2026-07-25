const admin = require('firebase-admin');
const serviceAccount = require('./firebase-key.json');

const version = process.argv[2] || 'جديد';

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const messaging = admin.messaging();

async function sendUpdateNotification() {
  try {
    const usersSnapshot = await db.collection('users').get();
    const tokens = [];
    usersSnapshot.forEach(doc => {
      const data = doc.data();
      if (data.fcmToken) {
        tokens.push(data.fcmToken);
      }
    });

    if (tokens.length === 0) {
      console.log('No FCM tokens found.');
      process.exit(0);
    }

    const payload = {
      data: {
        type: 'update_available',
        version: version
      }
    };

    const chunks = [];
    for (let i = 0; i < tokens.length; i += 500) {
      chunks.push(tokens.slice(i, i + 500));
    }

    for (const chunk of chunks) {
      const response = await messaging.sendEachForMulticast({
        tokens: chunk,
        data: payload.data
      });
      console.log(`[+] Notification sent: ${response.successCount} successes, ${response.failureCount} failures.`);
    }
    
    console.log('[+] Update notification sent successfully to all users!');
    process.exit(0);
  } catch (error) {
    console.error('[-] Error sending notification:', error);
    process.exit(1);
  }
}

sendUpdateNotification();
