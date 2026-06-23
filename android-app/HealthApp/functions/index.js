const { onValueCreated } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onMeasurementCreated = onValueCreated(
  "/users/{uid}/measurements/{measurementId}",
  async (event) => {
    const uid = event.params.uid;
    const measurementId = event.params.measurementId;
    const measurement = event.data.val();

    if (!measurement) return;

    const heartRate = Number(measurement.heartRateAvg || 0);
    const spo2 = Number(measurement.spo2Avg || 0);
    const endTime = Number(
      measurement.endTime || Math.floor(Date.now() / 1000)
    );

    let title = "";
    let message = "";
    let type = "health";
    let level = "warning";
    let targetScreen = "home";

    if (spo2 > 0 && spo2 < 95) {
      title = "SpO2 cần chú ý";
      message = `Có lần SpO2 là ${Math.round(spo2)}%, nên đo lại sau vài phút.`;
      targetScreen = "spo2_detail";
    } else if (heartRate > 100) {
      title = "Nhịp tim cao hơn bình thường";
      message = `Có lần nhịp tim là ${Math.round(heartRate)} bpm, nên nghỉ ngơi và theo dõi thêm.`;
      targetScreen = "heart_rate_detail";
    } else if (heartRate > 0 && heartRate < 60) {
      title = "Nhịp tim thấp hơn bình thường";
      message = `Có lần nhịp tim là ${Math.round(heartRate)} bpm, nên theo dõi thêm.`;
      targetScreen = "heart_rate_detail";
    } else {
      return;
    }

    const tokensSnap = await admin
      .database()
      .ref(`/users/${uid}/fcmTokens`)
      .get();

    if (!tokensSnap.exists()) {
      console.log("No FCM tokens for user:", uid);
      return;
    }

    const tokens = [];

    tokensSnap.forEach((child) => {
      const token = child.child("token").val();
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) {
      console.log("Token list empty for user:", uid);
      return;
    }

    const notificationRef = admin
      .database()
      .ref(`/users/${uid}/notifications`)
      .push();

    const notificationData = {
      type,
      level,
      title,
      message,
      createdAt: endTime,
      isRead: false,
      targetScreen,
      source: "measurement",
      sourceId: measurementId
    };

    await notificationRef.set(notificationData);

    const response = await admin.messaging().sendEachForMulticast({
      tokens: tokens,
      notification: {
        title: title,
        body: message
      },
      data: {
        title: title,
        body: message,
        type: type,
        level: level,
        targetScreen: targetScreen,
        notificationId: notificationRef.key || "",
        createdAt: String(endTime),
        source: "measurement",
        sourceId: measurementId
      }
    });

    console.log(
      "Push sent:",
      response.successCount,
      "success,",
      response.failureCount,
      "failed"
    );
  }
);