#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <time.h>
#include <Preferences.h>
#include <math.h>

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#include "MAX30105.h"
#include "heartRate.h"
#include "MPU6050.h"

/* =========================================================
   HEALTH APP - ESP32 + MAX30102 + MPU6050 + OLED + FIREBASE
   - Giu logic goc cua ban:
     + MAX30102 do nhip tim/SpO2 trong 60 giay
     + MPU6050 dem buoc chan
     + MPU6050 nhan dien giac ngu
     + Firebase Realtime Database qua REST API
   - Ghép them tu file OLED:
     + OLED SSD1306 128x64
     + Lat thiet bi de bat man
     + Nghieng trai/phai de doi trang
     + Tu tat man sau timeout
   ========================================================= */

/* ================= I2C ================= */
#define SDA_PIN 8
#define SCL_PIN 6

/* ================= OLED SSD1306 ================= */
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define SCREEN_ADDRESS 0x3C   // neu OLED khong len, thu doi thanh 0x3D

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

/* ================= SENSORS ================= */
MAX30105 particleSensor;
MPU6050 mpu(0x68);
Preferences prefs;

/* ================= WIFI + FIREBASE ================= */
const char* WIFI_SSID = "Thinh";
const char* WIFI_PASS = "25212345";

const char* FIREBASE_DB_URL = "https://health-monitoring-system-aed2c-default-rtdb.firebaseio.com";

// ID thiết bị phải trùng với ID bạn nhập trong app
const char* DEVICE_ID = "device_001";

// Mã ghép hiện tại sẽ được đọc từ Firebase
String currentPairCode = "555555";

// UID tài khoản đang ghép với thiết bị
String activeUserUid = "";

// Kiểm tra ownerUid định kỳ
unsigned long lastOwnerUidCheck = 0;
const unsigned long OWNER_UID_CHECK_INTERVAL = 75000UL; // 30 giây

// NTP - Asia/Bangkok / Viet Nam = UTC+7
const long GMT_OFFSET_SEC = 7 * 3600;
const int DAYLIGHT_OFFSET_SEC = 0;
/* ===== WIFI NON-BLOCKING ===== */
unsigned long lastWiFiReconnectAttempt = 0;
const unsigned long WIFI_RECONNECT_INTERVAL = 200000; // thử kết nối lại mỗi 15 giây
bool ntpSynced = false;

/* ================= MAX30102 ================= */
long irValue;
long redValue;

/* ===== MAX30102 CONFIG FOR HEART RATE SENSITIVITY ===== */

// LED mạnh hơn để tín hiệu IR rõ hơn
// Nếu tín hiệu bị nhiễu hoặc quá sáng, giảm xuống 0x5F
const byte MAX_LED_BRIGHTNESS = 0x5F;

// Không dùng sampleAverage quá cao.
// Average cao làm mượt nhưng bắt nhịp chậm hơn.
const byte MAX_SAMPLE_AVERAGE = 4;

// MAX30102 dùng Red + IR
const byte MAX_LED_MODE = 2;

// Tăng sample rate để bắt đỉnh nhịp tốt hơn.
// 100Hz ổn, 200Hz nhạy hơn.
const int MAX_SAMPLE_RATE = 200;

// Pulse width lớn giúp tín hiệu ADC rõ hơn
const int MAX_PULSE_WIDTH = 411;

// ADC range cao hơn để tránh bão hòa khi tăng LED
const int MAX_ADC_RANGE = 16384;

float beatsPerMinute = 0;
float beatAvg = 0;

#define RATE_SIZE 10
byte rates[RATE_SIZE];
byte rateSpot = 0;

long lastBeat = 0;

bool measuring = false;
unsigned long startTimeMillis = 0;
time_t sessionStartUnix = 0;
const unsigned long VITAL_MEASURE_DURATION_MS = 35000UL; // 60 giay

/* ===== Buffer SpO2 ===== */
#define SPO2_SAMPLES 100
uint32_t redBuffer[SPO2_SAMPLES];
uint32_t irBuffer[SPO2_SAMPLES];
int bufferIndex = 0;

float spo2 = 0;

/* ===== Gia tri gan nhat de hien thi OLED ===== */
float latestHeartRate = 0;
float latestSpo2 = 0;
time_t latestVitalTime = 0;

/* ================= MPU6050 STEP COUNTER ================= */
int16_t ax, ay, az;
long steps = 0;

float accelMagnitude = 0;

// Gia tốc nền, thường quanh 1.0g do trọng lực
float stepBaseline = 1.0f;

// Dao động thật sau khi loại bỏ nền trọng lực
float stepDynamic = 0.0f;

// Trạng thái đang ở đỉnh của một xung bước
bool stepPeakState = false;

unsigned long lastStepSampleTime = 0;
unsigned long lastCandidateStepTime = 0;

// Số xung đang chờ xác nhận là đi bộ thật
uint8_t pendingStepCount = 0;

// Đã xác nhận đang đi bộ hay chưa
bool walkingConfirmed = false;

// Lấy mẫu mỗi 40ms ~ 25Hz
const unsigned long STEP_SAMPLE_INTERVAL = 40;

// Khoảng cách hợp lệ giữa hai bước
// 380ms ~ tối đa khoảng 157 bước/phút
const unsigned long STEP_MIN_INTERVAL = 320;

// Nếu quá 1.1s không có bước tiếp theo thì coi như ngắt nhịp đi bộ
const unsigned long STEP_MAX_INTERVAL = 1100;

// Nếu lâu không có nhịp bước thì reset trạng thái đang đi bộ
const unsigned long WALK_RESET_TIMEOUT = 1800;

// Cần ít nhất 3 xung có nhịp giống bước chân mới bắt đầu cộng bước
const uint8_t WALK_CONFIRM_STEPS = 3;

// Ngưỡng phát hiện xung bước sau khi lọc nền
// Nếu đếm thiếu bước: giảm 0.30 xuống 0.26
// Nếu đếm thừa bước: tăng 0.30 lên 0.35
const float STEP_HIGH_THRESHOLD = 0.24f;

// Ngưỡng reset để sẵn sàng bắt bước tiếp theo
const float STEP_LOW_THRESHOLD = 0.10f;

// Loại bỏ xung quá mạnh, thường là chạm/lắc/va đập
const float STEP_MAX_DYNAMIC = 0.70f;

// Biên độ gia tốc hợp lệ
const float STEP_MAX_VALID_MAG = 1.90f;
const float STEP_MIN_VALID_MAG = 0.65f;

/* ================= STEP PERSISTENCE ================= */
String currentDayKey = "";
unsigned long lastDayCheckMillis = 0;
const unsigned long DAY_CHECK_INTERVAL = 200000; // kiem tra doi ngay moi 60s

/*
unsigned long lastStepFirebaseSync = 0;
const unsigned long STEP_SYNC_INTERVAL = 800; // giu nhu code goc
*/
unsigned long lastStepFirebaseSync = 0;

// Chi gui Firebase moi 1 phut neu so buoc thay doi > 20
const unsigned long STEP_SYNC_INTERVAL = 120000;
const long STEP_SYNC_MIN_DELTA = 20;

long lastUploadedSteps = 0;
/* ================= SLEEP DETECTION ================= */
enum SleepState {
  AWAKE,
  CANDIDATE_SLEEP,
  SLEEPING,
  TEMP_WAKE
};

SleepState sleepState = AWAKE;

// Lay mau sleep rieng de khong anh huong loop goc
unsigned long lastSleepSampleMillis = 0;
const unsigned long SLEEP_SAMPLE_INTERVAL_MS = 200;

// Threshold sleep
const float SLEEP_DELTA_THRESHOLD = 0.03f;

// ===== Fast test timings =====
// Khi test on co the doi sang moc that ben duoi
const unsigned long TO_CANDIDATE_MS  = 50000;  // yen 30s -> candidate
const unsigned long TO_SLEEP_MS      = 30000;  // yen 90s -> ngu that
const unsigned long TO_TEMP_WAKE_MS  = 5000;  // active 10s -> thuc tam
const unsigned long BACK_TO_SLEEP_MS = 15000;  // yen 15s sau thuc tam -> ngu lai
const unsigned long END_SLEEP_MS     = 30000;  // thuc lien tuc 30s -> ket thuc ngu

// ===== Real timings later =====
// const unsigned long TO_CANDIDATE_MS  = 10UL * 60UL * 1000UL;
// const unsigned long TO_SLEEP_MS      = 30UL * 60UL * 1000UL;
// const unsigned long TO_TEMP_WAKE_MS  = 3UL  * 60UL * 1000UL;
// const unsigned long BACK_TO_SLEEP_MS = 5UL  * 60UL * 1000UL;
// const unsigned long END_SLEEP_MS     = 15UL * 60UL * 1000UL;

float prevSleepAccMag = 0.0f;
bool hasPrevSleepSample = false;
int activeStreak = 0;
int quietStreak = 0;

unsigned long quietStartMs = 0;
unsigned long activeStartMs = 0;
unsigned long tempWakeStartMs = 0;

time_t sleepStartUnix = 0;
time_t sleepEndUnix = 0;
unsigned long totalSleepMs = 0;
int wakeCount = 0;

time_t lastCompletedSleepStartUnix = 0;
time_t lastCompletedSleepEndUnix = 0;
unsigned long lastCompletedTotalSleepMs = 0;
int lastCompletedWakeCount = 0;

/* ================= OLED PAGE CONTROL ================= */
int currentPage = 0;

bool oledAvailable = false;
bool oledOn = false;
unsigned long lastInteraction = 0;
const unsigned long SCREEN_TIMEOUT = 120000;   // 10 giay

unsigned long lastOledRefresh = 0;
const unsigned long OLED_REFRESH_INTERVAL = 500;

/* ================= TILT CHANGE PAGE ================= */
enum TiltState {
  TILT_NEUTRAL,
  TILT_LEFT_HELD,
  TILT_RIGHT_HELD
};

TiltState tiltState = TILT_NEUTRAL;

const float TILT_THRESHOLD = 0.45f;
const float RETURN_NEUTRAL_THRESHOLD = 0.18f;
unsigned long lastTiltAction = 0;
const unsigned long TILT_COOLDOWN = 600;

/* ================= FLIP TO WAKE ================= */
const float FACE_UP_THRESHOLD = 0.75f;
const float FACE_DOWN_THRESHOLD = -0.75f;

enum FacePose {
  FACE_UNKNOWN,
  FACE_UP,
  FACE_DOWN
};

enum FlipWakeStage {
  FLIP_WAIT_START,
  FLIP_WAIT_RETURN
};

FacePose stablePose = FACE_UNKNOWN;
FacePose lastRawPose = FACE_UNKNOWN;
FacePose restPose = FACE_UNKNOWN;
FacePose startPose = FACE_UNKNOWN;

unsigned long rawPoseChangedAt = 0;
const unsigned long POSE_STABLE_MS = 120;

FlipWakeStage flipStage = FLIP_WAIT_START;
unsigned long flipStageStart = 0;
const unsigned long FLIP_SEQUENCE_TIMEOUT = 2500;

unsigned long lastWakeAction = 0;
const unsigned long WAKE_COOLDOWN = 1200;

/* ================= FUNCTION DECLARATIONS ================= */
void connectWiFi();
void handleWiFiAutoReconnect();
void syncTimeNTP();
time_t getUnixTime();
String getDateKey();

bool firebasePatch(const String& path, const String& json);
bool firebasePut(const String& path, const String& json);
bool firebaseGetString(const String& path, String& response);

String cleanFirebaseString(String value);
bool hasLinkedUser();
String userBasePath();
bool loadPairCodeFromFirebase();
bool loadOwnerUidFromFirebase();
void syncOwnerUidIfNeeded();

void createProfileIfNeeded();
void sendMeasurementToFirebase(float hrAvg, float spo2Avg, time_t startTs, time_t endTs);
//void updateStepsToFirebase();
bool updateStepsToFirebase();
void syncStepsToFirebaseIfNeeded();
void sendSleepSessionToFirebase(time_t startTs, time_t endTs, unsigned long sleepMs, int wakes);

void saveStepsToNVS();
void loadStepsFromNVS();
void loadTodayStepsFromFirebase();
void initStepPersistence();
void checkNewDayAndResetSteps();

void resetData();
void calculateSpO2();
void countSteps();

void updateSleepDetection();
String sleepStateToString(SleepState s);
String formatDuration(unsigned long ms);
String formatUnixTimestamp(time_t ts);
String formatUnixTimeHHMM(time_t ts);
String getCurrentTimeHHMM();
String timeAgoText(time_t ts);
unsigned long getVitalMeasureRemainingSeconds();
String getMeasuringText();
void printSleepFinalResult();
void resetSleepSessionTracking();

void initOLED();
void updateOLEDDataAndRender();
void renderOLEDPage();
void handleGestureControl();
void handleFlipWake(float Az, unsigned long now);
void handlePageTilt(float Ax, unsigned long now);

/* ================= RESET DU LIEU MAX30102 ================= */
void resetData()
{
  rateSpot = 0;
  beatAvg = 0;
  beatsPerMinute = 0;
  lastBeat = 0;
  bufferIndex = 0;
  spo2 = 0;

  for (int i = 0; i < RATE_SIZE; i++)
    rates[i] = 0;
}

/* ================= TIME / FORMAT HELPERS ================= */
String sleepStateToString(SleepState s)
{
  switch (s)
  {
    case AWAKE: return "AWAKE";
    case CANDIDATE_SLEEP: return "CANDIDATE_SLEEP";
    case SLEEPING: return "SLEEPING";
    case TEMP_WAKE: return "TEMP_WAKE";
    default: return "UNKNOWN";
  }
}

String formatDuration(unsigned long ms)
{
  unsigned long sec = ms / 1000UL;
  unsigned long h = sec / 3600UL;
  unsigned long m = (sec % 3600UL) / 60UL;
  unsigned long s = sec % 60UL;

  char buf[20];
  sprintf(buf, "%02lu:%02lu:%02lu", h, m, s);
  return String(buf);
}

String formatUnixTimestamp(time_t ts)
{
  if (ts < 100000)
    return "--";

  struct tm timeinfo;
  localtime_r(&ts, &timeinfo);

  char buf[25];
  strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", &timeinfo);
  return String(buf);
}

String formatUnixTimeHHMM(time_t ts)
{
  if (ts < 100000)
    return "--:--";

  struct tm timeinfo;
  localtime_r(&ts, &timeinfo);

  char buf[8];
  strftime(buf, sizeof(buf), "%H:%M", &timeinfo);
  return String(buf);
}

String getCurrentTimeHHMM()
{
  time_t nowTs = getUnixTime();
  return formatUnixTimeHHMM(nowTs);
}

String timeAgoText(time_t ts)
{
  if (ts < 100000)
    return "chua co DL";

  time_t nowTs = getUnixTime();
  if (nowTs < ts || nowTs < 100000)
    return "vua xong";

  unsigned long diff = (unsigned long)(nowTs - ts);

  if (diff < 60)
    return String(diff) + "s truoc";

  if (diff < 3600)
    return String(diff / 60) + "p truoc";

  if (diff < 86400)
    return String(diff / 3600) + "h truoc";

  return String(diff / 86400) + "d truoc";
}

void resetSleepSessionTracking()
{
  quietStartMs = 0;
  activeStartMs = 0;
  tempWakeStartMs = 0;
  sleepStartUnix = 0;
  sleepEndUnix = 0;
  totalSleepMs = 0;
  wakeCount = 0;
}

void printSleepFinalResult()
{
  Serial.println();
  Serial.println("========== KET QUA GIAC NGU ==========");
  Serial.print("Sleep Start : ");
  Serial.println(formatUnixTimestamp(lastCompletedSleepStartUnix));

  Serial.print("Sleep End   : ");
  Serial.println(formatUnixTimestamp(lastCompletedSleepEndUnix));

  Serial.print("Total Sleep : ");
  Serial.println(formatDuration(lastCompletedTotalSleepMs));

  Serial.print("Wake Count  : ");
  Serial.println(lastCompletedWakeCount);
  Serial.println("======================================");
  Serial.println();
}

/* ================= OLED ICONS ================= */
void drawHeartIcon(int x, int y) {
  display.fillCircle(x + 3, y + 3, 3, SSD1306_WHITE);
  display.fillCircle(x + 9, y + 3, 3, SSD1306_WHITE);
  display.fillTriangle(x, y + 5, x + 12, y + 5, x + 6, y + 13, SSD1306_WHITE);
}

void drawDropIcon(int x, int y) {
  display.fillTriangle(x + 5, y, x, y + 8, x + 10, y + 8, SSD1306_WHITE);
  display.fillCircle(x + 5, y + 10, 4, SSD1306_WHITE);
}

void drawFootIcon(int x, int y) {
  display.fillRoundRect(x + 2, y + 4, 8, 12, 3, SSD1306_WHITE);
  display.fillCircle(x + 2, y + 2, 1, SSD1306_WHITE);
  display.fillCircle(x + 5, y + 1, 1, SSD1306_WHITE);
  display.fillCircle(x + 8, y + 2, 1, SSD1306_WHITE);
  display.fillCircle(x + 10, y + 4, 1, SSD1306_WHITE);
}

void drawMoonIcon(int x, int y) {
  display.fillCircle(x + 7, y + 7, 6, SSD1306_WHITE);
  display.fillCircle(x + 10, y + 5, 6, SSD1306_BLACK);
}

void drawWifiIcon(int x, int y, bool on) {
  if (!on) {
    display.drawLine(x, y, x + 10, y + 10, SSD1306_WHITE);
    display.drawLine(x + 10, y, x, y + 10, SSD1306_WHITE);
    return;
  }

  display.drawCircle(x + 5, y + 9, 1, SSD1306_WHITE);
  display.drawCircle(x + 5, y + 9, 4, SSD1306_WHITE);
  display.drawCircle(x + 5, y + 9, 7, SSD1306_WHITE);
}

void drawCard(int x, int y, int w, int h) {
  display.drawRoundRect(x, y, w, h, 6, SSD1306_WHITE);
}

int textXCentered(const String &text, int textSize) {
  int width = text.length() * 6 * textSize;
  int x = (SCREEN_WIDTH - width) / 2;
  if (x < 0) x = 0;
  return x;
}

void drawCenteredText(const String &text, int y, int textSize) {
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(textSize);
  display.setCursor(textXCentered(text, textSize), y);
  display.print(text);
}

/* ================= OLED HELPERS ================= */
int getDisplayHeartRate()
{
  if (measuring && beatAvg > 0)
    return (int)(beatAvg + 0.5f);

  if (latestHeartRate > 0)
    return (int)(latestHeartRate + 0.5f);

  return 0;
}

int getDisplaySpo2()
{
  if (measuring && spo2 > 0)
    return (int)(spo2 + 0.5f);

  if (latestSpo2 > 0)
    return (int)(latestSpo2 + 0.5f);

  return 0;
}

String getDisplaySleepStart()
{
  if (sleepState == SLEEPING || sleepState == TEMP_WAKE)
    return formatUnixTimeHHMM(sleepStartUnix);

  return formatUnixTimeHHMM(lastCompletedSleepStartUnix);
}

String getDisplaySleepEnd()
{
  if (sleepState == SLEEPING || sleepState == TEMP_WAKE)
    return "dang ngu";

  return formatUnixTimeHHMM(lastCompletedSleepEndUnix);
}

String getDisplayTotalSleep()
{
  if (sleepState == SLEEPING || sleepState == TEMP_WAKE)
  {
    time_t nowTs = getUnixTime();
    if (sleepStartUnix > 100000 && nowTs > sleepStartUnix)
      return formatDuration((unsigned long)(nowTs - sleepStartUnix) * 1000UL).substring(0, 5);

    return "00:00";
  }

  if (lastCompletedTotalSleepMs > 0)
    return formatDuration(lastCompletedTotalSleepMs).substring(0, 5);

  return "--:--";
}
String getDisplaySleepStatus()
{
  switch (sleepState)
  {
    case AWAKE:
      return "Dang thuc";

    case CANDIDATE_SLEEP:
      return "Cho ngu";

    case SLEEPING:
      return "Dang ngu";

    case TEMP_WAKE:
      return "Thuc tam";

    default:
      return "--";
  }
}

void turnOnOLED() {
  if (!oledAvailable) return;
  oledOn = true;
  currentPage = 0;
  lastInteraction = millis();
  lastOledRefresh = 0;
  display.ssd1306_command(SSD1306_DISPLAYON);
  Serial.println("OLED ON");
}

void resetFlipWake() {
  flipStage = FLIP_WAIT_START;
  startPose = FACE_UNKNOWN;
  flipStageStart = 0;
}

void turnOffOLED() {
  if (!oledAvailable) {
    oledOn = false;
    return;
  }

  oledOn = false;
  display.clearDisplay();
  display.display();
  display.ssd1306_command(SSD1306_DISPLAYOFF);
  tiltState = TILT_NEUTRAL;
  resetFlipWake();
  restPose = FACE_UNKNOWN;
  stablePose = FACE_UNKNOWN;
  lastRawPose = FACE_UNKNOWN;
  Serial.println("OLED OFF");
}

void showPairingPage()
{
  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);

  display.setTextSize(1);
  display.setCursor(0, 0);
  display.print("CHUA LIEN KET");

  display.setCursor(0, 16);
  display.print("ID:");

  display.setCursor(24, 16);
  display.print(DEVICE_ID);

  display.setCursor(0, 32);
  display.print("MA:");

  display.setCursor(24, 32);
  display.print(currentPairCode);

  display.setCursor(0, 52);
  display.print(WiFi.status() == WL_CONNECTED ? "WiFi: OK" : "WiFi: OFF");

  display.display();
}

void showMainDashboard() {
  int displayHr = getDisplayHeartRate();
  int displaySp = getDisplaySpo2();
  String displaySleep = getDisplayTotalSleep();
  bool wifiOk = WiFi.status() == WL_CONNECTED;

  display.clearDisplay();
  display.setTextColor(SSD1306_WHITE);

  display.setTextSize(2);
  display.setCursor(2, 0);
  display.print(getCurrentTimeHHMM());

  drawWifiIcon(113, 0, wifiOk);

  drawCard(0, 20, 62, 20);
  drawCard(66, 20, 62, 20);
  drawCard(0, 44, 62, 20);
  drawCard(66, 44, 62, 20);

  drawHeartIcon(4, 23);
  display.setTextSize(1);
  display.setCursor(20, 23);
  display.print("HR");
  display.setCursor(20, 31);
  if (displayHr > 0) display.print(displayHr);
  else display.print("--");

  drawDropIcon(70, 23);
  display.setCursor(86, 23);
  display.print("SpO2");
  display.setCursor(86, 31);
  if (displaySp > 0) {
    display.print(displaySp);
    display.print("%");
  } else {
    display.print("--");
  }

  drawFootIcon(4, 47);
  display.setCursor(20, 47);
  display.print("Step");
  display.setCursor(20, 55);
  display.print(steps);

  drawMoonIcon(70, 47);
  display.setCursor(86, 47);
  display.print("Sleep");
  display.setCursor(86, 55);
  display.print(displaySleep);

  display.display();
}

unsigned long getVitalMeasureRemainingSeconds()
{
  if (!measuring)
    return 0;

  unsigned long elapsed = millis() - startTimeMillis;

  if (elapsed >= VITAL_MEASURE_DURATION_MS)
    return 0;

  unsigned long remainingMs = VITAL_MEASURE_DURATION_MS - elapsed;

  // Lam tron len de hien 60s, 59s, ..., 1s
  return (remainingMs + 999) / 1000;
}

String getMeasuringText()
{
  return "dang do: " + String(getVitalMeasureRemainingSeconds()) + "s";
}
void showHeartPage() {
  int displayHr = getDisplayHeartRate();

  display.clearDisplay();
  drawHeartIcon(58, 2);

  drawCenteredText("Heart Rate", 14, 1);

  if (displayHr > 0)
    drawCenteredText(String(displayHr) + " BPM", 28, 2);
  else
    drawCenteredText("-- BPM", 28, 2);

  if (measuring)
  drawCenteredText(getMeasuringText(), 52, 1);
else
  drawCenteredText(timeAgoText(latestVitalTime), 52, 1);

  display.display();
}

void showSpo2Page() {
  int displaySp = getDisplaySpo2();

  display.clearDisplay();
  drawDropIcon(58, 2);

  drawCenteredText("SpO2", 14, 1);

  if (displaySp > 0)
    drawCenteredText(String(displaySp) + "%", 28, 2);
  else
    drawCenteredText("--%", 28, 2);

  if (measuring)
  drawCenteredText(getMeasuringText(), 52, 1);
else
  drawCenteredText(timeAgoText(latestVitalTime), 52, 1);

  display.display();
}

void showStepsPage() {
  display.clearDisplay();
  drawFootIcon(58, 2);

  drawCenteredText("Steps", 14, 1);
  drawCenteredText(String(steps), 28, 2);
  drawCenteredText("Today", 52, 1);

  display.display();
}

void showSleepPage() {
  display.clearDisplay();

  // Icon nhỏ hơn và đẩy lên cao để có thêm chỗ cho trạng thái
  drawMoonIcon(4, 0);

  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);

  // Tiêu đề
  display.setCursor(24, 2);
  display.print("Sleep");

  // Dòng trạng thái hiện tại
  String statusLine = "TT: " + getDisplaySleepStatus();

  // Các dòng dữ liệu
  String line1 = "Ngu : " + getDisplaySleepStart();
  String line2 = "Thuc: " + getDisplaySleepEnd();
  String line3 = "Tong: " + getDisplayTotalSleep();

  display.setCursor(0, 16);
  display.print(statusLine);

  display.setCursor(0, 28);
  display.print(line1);

  display.setCursor(0, 40);
  display.print(line2);

  display.setCursor(0, 52);
  display.print(line3);

  display.display();
}

void renderOLEDPage()
{
  if (!oledAvailable || !oledOn)
    return;

  if (!hasLinkedUser())
  {
    showPairingPage();
    return;
  }

  switch (currentPage) {
    case 0: showMainDashboard(); break;
    case 1: showHeartPage(); break;
    case 2: showSpo2Page(); break;
    case 3: showStepsPage(); break;
    case 4: showSleepPage(); break;
    default:
      currentPage = 0;
      showMainDashboard();
      break;
  }
}

void updateOLEDDataAndRender()
{
  if (!oledOn)
    return;

  unsigned long now = millis();
  if (now - lastOledRefresh >= OLED_REFRESH_INTERVAL)
  {
    lastOledRefresh = now;
    renderOLEDPage();
  }
}

FacePose detectFacePose(float Az) {
  if (Az >= FACE_UP_THRESHOLD) return FACE_UP;
  if (Az <= FACE_DOWN_THRESHOLD) return FACE_DOWN;
  return FACE_UNKNOWN;
}

const char* facePoseToString(FacePose p) {
  switch (p) {
    case FACE_UP: return "UP";
    case FACE_DOWN: return "DOWN";
    default: return "UNKNOWN";
  }
}

void handleFlipWake(float Az, unsigned long now) {
  if (oledOn) return;
  if (now - lastWakeAction < WAKE_COOLDOWN) return;

  FacePose rawPose = detectFacePose(Az);

  // Loc on dinh pose
  if (rawPose != lastRawPose) {
    lastRawPose = rawPose;
    rawPoseChangedAt = now;
  }

  if (rawPose != FACE_UNKNOWN && (now - rawPoseChangedAt >= POSE_STABLE_MS)) {
    stablePose = rawPose;
  }

  if (stablePose == FACE_UNKNOWN)
    return;

  // Sua loi cu: khong bat dau chuoi lat chi vi thiet bi dang nam yen UP/DOWN.
  // Man hinh tat -> ghi nho tu the nghi ban dau, chi khi doi sang mat doi dien moi bat dau tinh chuoi.
  if (flipStage == FLIP_WAIT_START) {
    if (restPose == FACE_UNKNOWN) {
      restPose = stablePose;
      return;
    }

    if (stablePose != restPose) {
      startPose = restPose;
      flipStage = FLIP_WAIT_RETURN;
      flipStageStart = now;

      Serial.print("FLIP HALF: ");
      Serial.print(facePoseToString(startPose));
      Serial.print(" -> ");
      Serial.println(facePoseToString(stablePose));
    }

    return;
  }

  if (flipStage == FLIP_WAIT_RETURN) {
    if (now - flipStageStart > FLIP_SEQUENCE_TIMEOUT) {
      Serial.println("FLIP TIMEOUT -> RESET");
      resetFlipWake();
      restPose = stablePose;
      return;
    }

    if (stablePose == startPose) {
      Serial.println("FULL FLIP DETECTED -> BAT MAN");
      turnOnOLED();
      lastWakeAction = now;
      resetFlipWake();
      restPose = FACE_UNKNOWN;
    }
  }
}

void handlePageTilt(float Ax, unsigned long now) {
  if (!oledOn) return;

  if (now - lastTiltAction < TILT_COOLDOWN) return;

  switch (tiltState) {
    case TILT_NEUTRAL:
      if (Ax >= TILT_THRESHOLD) {
        tiltState = TILT_RIGHT_HELD;
      } else if (Ax <= -TILT_THRESHOLD) {
        tiltState = TILT_LEFT_HELD;
      }
      break;

    case TILT_RIGHT_HELD:
      if (fabs(Ax) <= RETURN_NEUTRAL_THRESHOLD) {
        currentPage++;
        if (currentPage > 4) currentPage = 0;

        tiltState = TILT_NEUTRAL;
        lastTiltAction = now;
        lastInteraction = now;
        lastOledRefresh = 0;

        Serial.println("NGHIENG PHAI -> TRANG SAU");
      }
      break;

    case TILT_LEFT_HELD:
      if (fabs(Ax) <= RETURN_NEUTRAL_THRESHOLD) {
        currentPage--;
        if (currentPage < 0) currentPage = 4;

        tiltState = TILT_NEUTRAL;
        lastTiltAction = now;
        lastInteraction = now;
        lastOledRefresh = 0;

        Serial.println("NGHIENG TRAI -> TRANG TRUOC");
      }
      break;
  }
}

void handleGestureControl() {
  if (!oledAvailable) return;

  int16_t gx, gy, gz;
  mpu.getAcceleration(&gx, &gy, &gz);

  float Ax = gx / 16384.0f;
  float Az = gz / 16384.0f;

  unsigned long now = millis();

  handleFlipWake(Az, now);
  handlePageTilt(Ax, now);

  if (oledOn && (now - lastInteraction >= SCREEN_TIMEOUT)) {
    turnOffOLED();
  }
}

void initOLED()
{
  if (!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
    Serial.println("OLED not found! Bo qua OLED, code van chay cac phan khac.");
    return;
  }

  oledAvailable = true;
  display.clearDisplay();
  display.display();
  turnOffOLED();

  Serial.println("OLED san sang");
}

/* ================= WIFI ================= */
void connectWiFi()
{
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);

  Serial.print("Dang ket noi WiFi");

  unsigned long startAttempt = millis();
  const unsigned long WIFI_CONNECT_TIMEOUT = 8000; // chỉ chờ tối đa 8 giây

  while (WiFi.status() != WL_CONNECTED &&
         millis() - startAttempt < WIFI_CONNECT_TIMEOUT)
  {
    delay(300);
    Serial.print(".");
  }

  Serial.println();

  if (WiFi.status() == WL_CONNECTED)
  {
    Serial.print("WiFi OK, IP: ");
    Serial.println(WiFi.localIP());
  }
  else
  {
    Serial.println("Khong co WiFi -> tiep tuc chay offline.");
  }
}
void handleWiFiAutoReconnect()
{
  if (WiFi.status() == WL_CONNECTED)
  {
    // Nếu vừa kết nối lại được WiFi mà chưa đồng bộ NTP thì đồng bộ lại
    if (!ntpSynced)
    {
      syncTimeNTP();
    }
    return;
  }

  unsigned long now = millis();

  if (now - lastWiFiReconnectAttempt < WIFI_RECONNECT_INTERVAL)
    return;

  lastWiFiReconnectAttempt = now;

  Serial.println("WiFi mat ket noi -> thu ket noi lai...");
  WiFi.disconnect();
  WiFi.begin(WIFI_SSID, WIFI_PASS);
}

/* ================= NTP TIME ================= */
void syncTimeNTP()
{
  if (WiFi.status() != WL_CONNECTED)
  {
    Serial.println("Khong co WiFi -> bo qua dong bo NTP.");
    ntpSynced = false;
    return;
  }

  configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET_SEC, "pool.ntp.org", "time.nist.gov");

  Serial.print("Dong bo thoi gian NTP");
  struct tm timeinfo;
  int retry = 0;

  // Giảm thời gian chờ NTP để không làm hệ thống bị đứng lâu
  while (!getLocalTime(&timeinfo) && retry < 8)
  {
    delay(300);
    Serial.print(".");
    retry++;
  }

  Serial.println();

  if (retry >= 8)
  {
    ntpSynced = false;
    Serial.println("Khong dong bo duoc NTP.");
  }
  else
  {
    ntpSynced = true;
    Serial.print("Thoi gian hien tai: ");
    Serial.println(&timeinfo, "%Y-%m-%d %H:%M:%S");
  }
}
time_t getUnixTime()
{
  time_t now;
  time(&now);
  return now;
}

String getDateKey()
{
  struct tm timeinfo;

  if (!getLocalTime(&timeinfo))
  {
    return "1970-01-01";
  }

  char buf[11];
  strftime(buf, sizeof(buf), "%Y-%m-%d", &timeinfo);

  return String(buf);
}
/* ================= FIREBASE REST ================= */
bool firebasePatch(const String& path, const String& json)
{
  if (WiFi.status() != WL_CONNECTED)
    return false;

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  String url = String(FIREBASE_DB_URL) + "/" + path + ".json";

  if (!http.begin(client, url))
  {
    Serial.println("HTTP begin PATCH that bai");
    return false;
  }

  http.addHeader("Content-Type", "application/json");
  int httpCode = http.sendRequest("PATCH", json);

  Serial.print("[PATCH] ");
  Serial.print(path);
  Serial.print(" -> HTTP ");
  Serial.println(httpCode);

  if (httpCode > 0)
  {
    String payload = http.getString();
    if (payload.length())
      Serial.println(payload);
  }
  else
  {
    Serial.print("PATCH loi: ");
    Serial.println(http.errorToString(httpCode));
  }

  http.end();
  return httpCode >= 200 && httpCode < 300;
}

bool firebasePut(const String& path, const String& json)
{
  if (WiFi.status() != WL_CONNECTED)
    return false;

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  String url = String(FIREBASE_DB_URL) + "/" + path + ".json";

  if (!http.begin(client, url))
  {
    Serial.println("HTTP begin PUT that bai");
    return false;
  }

  http.addHeader("Content-Type", "application/json");
  int httpCode = http.PUT(json);

  Serial.print("[PUT] ");
  Serial.print(path);
  Serial.print(" -> HTTP ");
  Serial.println(httpCode);

  if (httpCode > 0)
  {
    String payload = http.getString();
    if (payload.length())
      Serial.println(payload);
  }
  else
  {
    Serial.print("PUT loi: ");
    Serial.println(http.errorToString(httpCode));
  }

  http.end();
  return httpCode >= 200 && httpCode < 300;
}

bool firebaseGetString(const String& path, String& response)
{
  response = "";

  if (WiFi.status() != WL_CONNECTED)
    return false;

  WiFiClientSecure client;
  client.setInsecure();

  HTTPClient http;
  String url = String(FIREBASE_DB_URL) + "/" + path + ".json";

  if (!http.begin(client, url))
  {
    Serial.println("HTTP begin GET that bai");
    return false;
  }

  int httpCode = http.GET();

  Serial.print("[GET] ");
  Serial.print(path);
  Serial.print(" -> HTTP ");
  Serial.println(httpCode);

  if (httpCode > 0)
  {
    response = http.getString();
  }
  else
  {
    Serial.print("GET loi: ");
    Serial.println(http.errorToString(httpCode));
  }

  http.end();
  return httpCode >= 200 && httpCode < 300;
}
String cleanFirebaseString(String value)
{
  value.trim();

  if (value == "null")
    return "";

  if (value.length() >= 2 && value[0] == '"' && value[value.length() - 1] == '"')
  {
    value = value.substring(1, value.length() - 1);
  }

  value.replace("\\\"", "\"");
  value.trim();

  return value;
}

bool hasLinkedUser()
{
  return activeUserUid.length() > 0;
}

String userBasePath()
{
  return "users/" + activeUserUid;
}
bool loadPairCodeFromFirebase()
{
  if (WiFi.status() != WL_CONNECTED)
    return false;

  String path = "devices/" + String(DEVICE_ID) + "/pairCode";
  String response;

  if (!firebaseGetString(path, response))
  {
    Serial.println("Khong doc duoc pairCode.");
    return false;
  }

  String newPairCode = cleanFirebaseString(response);

  if (newPairCode.length() > 0 && newPairCode != currentPairCode)
  {
    currentPairCode = newPairCode;

    Serial.print("Cap nhat ma ghep moi tu Firebase: ");
    Serial.println(currentPairCode);

    lastOledRefresh = 0;
  }

  return currentPairCode.length() > 0;
}

bool loadOwnerUidFromFirebase()
{
  if (WiFi.status() != WL_CONNECTED)
    return false;

  String path = "devices/" + String(DEVICE_ID) + "/ownerUid";
  String response;

  if (!firebaseGetString(path, response))
  {
    Serial.println("Khong doc duoc ownerUid cua thiet bi.");
    return false;
  }

  String newOwnerUid = cleanFirebaseString(response);

  if (newOwnerUid != activeUserUid)
  {
    Serial.print("ownerUid thay doi: ");
    Serial.print(activeUserUid);
    Serial.print(" -> ");
    Serial.println(newOwnerUid);

    activeUserUid = newOwnerUid;

    if (activeUserUid.length() > 0)
    {
      Serial.print("Thiet bi da lien ket voi UID: ");
      Serial.println(activeUserUid);

      if (currentDayKey.length() == 0)
      {
        currentDayKey = getDateKey();
      }

      if (currentDayKey != "1970-01-01")
      {
        loadTodayStepsFromFirebase();
        lastUploadedSteps = steps;
        lastStepFirebaseSync = millis();
      }
    }
    else
    {
      Serial.println("Thiet bi chua duoc lien ket tai khoan.");
    }
  }

  return activeUserUid.length() > 0;
}

void syncOwnerUidIfNeeded()
{
  if (WiFi.status() != WL_CONNECTED)
    return;

  unsigned long now = millis();

  if (now - lastOwnerUidCheck < OWNER_UID_CHECK_INTERVAL)
    return;

  lastOwnerUidCheck = now;
  loadPairCodeFromFirebase();
  loadOwnerUidFromFirebase();
}

/* ================= PROFILE ================= */
void createProfileIfNeeded()
{

}

/* ================= DO TIM/SPO2 -> FIREBASE ================= */
void sendMeasurementToFirebase(float hrAvg, float spo2Avg, time_t startTs, time_t endTs)
{
  if (WiFi.status() != WL_CONNECTED)
    return;

  if (!hasLinkedUser())
  {
    Serial.println("Chua lien ket tai khoan -> khong gui ket qua do.");
    return;
  }

  String currentPath = userBasePath() + "/current";
  String measurePath = userBasePath() + "/measurements/" + String((unsigned long)endTs);

  String currentJson = "{";
  currentJson += "\"heartRate\":" + String(hrAvg, 1) + ",";
  currentJson += "\"spo2\":" + String(spo2Avg, 1) + ",";
  currentJson += "\"lastMeasureTime\":" + String((unsigned long)endTs) + ",";
  currentJson += "\"lastVitalMeasureTime\":" + String((unsigned long)endTs);
  currentJson += "}";

  String measureJson = "{";
  measureJson += "\"heartRateAvg\":" + String(hrAvg, 1) + ",";
  measureJson += "\"spo2Avg\":" + String(spo2Avg, 1) + ",";
  measureJson += "\"startTime\":" + String((unsigned long)startTs) + ",";
  measureJson += "\"endTime\":" + String((unsigned long)endTs) + ",";
  measureJson += "\"status\":\"completed\"";
  measureJson += "}";

  Serial.println("Dang gui ket qua do len Firebase...");
  firebasePatch(currentPath, currentJson);
  firebasePut(measurePath, measureJson);
}

/* ================= STEPS -> FIREBASE ================= */
/*
void updateStepsToFirebase()
{
  if (WiFi.status() != WL_CONNECTED)
    return;

  time_t nowTs = getUnixTime();

  String currentPath = "users/" + String(USER_UID) + "/current";
  String stepsHistoryPath = "users/" + String(USER_UID) + "/steps_history/" + currentDayKey;

  String currentJson = "{";
  currentJson += "\"steps\":" + String(steps);
  if (nowTs > 100000)
  {
    currentJson += ",\"lastMeasureTime\":" + String((unsigned long)nowTs);
    currentJson += ",\"lastStepUpdateTime\":" + String((unsigned long)nowTs);
  }
  currentJson += "}";

  String dayJson = "{";
  dayJson += "\"totalSteps\":" + String(steps) + ",";
  dayJson += "\"updatedAt\":" + String((unsigned long)nowTs);
  dayJson += "}";

  firebasePatch(currentPath, currentJson);
  firebasePatch(stepsHistoryPath, dayJson);
}
*/
bool updateStepsToFirebase()
{
  if (WiFi.status() != WL_CONNECTED)
    return false;

  if (!hasLinkedUser())
  {
    Serial.println("Chua lien ket tai khoan -> khong gui steps.");
    return false;
  }

  time_t nowTs = getUnixTime();

  String currentPath = userBasePath() + "/current";

  String currentJson = "{";
  currentJson += "\"steps\":" + String(steps);

  if (nowTs > 100000)
  {
    currentJson += ",\"lastStepUpdateTime\":" + String((unsigned long)nowTs);
  }

  currentJson += "}";

  bool currentOk = firebasePatch(currentPath, currentJson);

  if (nowTs < 100000)
  {
    Serial.println("Chua co thoi gian hop le -> khong ghi steps_history.");
    return currentOk;
  }

  currentDayKey = getDateKey();

  if (currentDayKey == "1970-01-01")
  {
    Serial.println("Ngay 1970-01-01 khong hop le -> khong ghi steps_history.");
    return currentOk;
  }

  String dayPath = userBasePath() + "/steps_history/" + currentDayKey;
  String recordKey = String((unsigned long)nowTs);
  String recordPath = dayPath + "/records/" + recordKey;

  String dayJson = "{";
  dayJson += "\"totalSteps\":" + String(steps) + ",";
  dayJson += "\"updatedAt\":" + String((unsigned long)nowTs);
  dayJson += "}";

  bool dayOk = firebasePatch(dayPath, dayJson);

  String recordJson = "{";
  recordJson += "\"steps\":" + String(steps) + ",";
  recordJson += "\"updatedAt\":" + String((unsigned long)nowTs);
  recordJson += "}";

  bool recordOk = firebasePut(recordPath, recordJson);

  if (currentOk && dayOk && recordOk)
  {
    Serial.print("Da them ban ghi steps moi: ");
    Serial.print(recordKey);
    Serial.print(" | steps = ");
    Serial.println(steps);
  }

  return currentOk && dayOk && recordOk;
}

void syncStepsToFirebaseIfNeeded()
{
  unsigned long nowMs = millis();

  // Chi kiem tra moi 1 phut
  if (nowMs - lastStepFirebaseSync < STEP_SYNC_INTERVAL)
    return;

  lastStepFirebaseSync = nowMs;

  long changedSteps = steps - lastUploadedSteps;
  if (changedSteps < 0)
    changedSteps = -changedSteps;

  // Yeu cau: so buoc thay doi > 20 moi gui
  if (changedSteps <= STEP_SYNC_MIN_DELTA)
  {
    Serial.print("Chua gui steps Firebase. Thay doi moi: ");
    Serial.println(changedSteps);
    return;
  }

  Serial.print("Du dieu kien gui steps Firebase. Thay doi: ");
  Serial.println(changedSteps);

  bool ok = updateStepsToFirebase();

  if (ok)
  {
    lastUploadedSteps = steps;
  }
}
/* ================= SLEEP -> FIREBASE ================= */
void sendSleepSessionToFirebase(time_t startTs, time_t endTs, unsigned long sleepMs, int wakes)
{
  if (WiFi.status() != WL_CONNECTED)
    return;

  if (!hasLinkedUser())
  {
    Serial.println("Chua lien ket tai khoan -> khong gui sleep session.");
    return;
  }

  if (startTs < 100000 || endTs <= startTs)
    return;

  String currentPath = userBasePath() + "/current/sleep";
  String sessionPath = userBasePath() + "/sleep_sessions/" + String((unsigned long)endTs);

  unsigned long totalSec = sleepMs / 1000UL;

  String sleepJson = "{";
  sleepJson += "\"startTime\":" + String((unsigned long)startTs) + ",";
  sleepJson += "\"endTime\":" + String((unsigned long)endTs) + ",";
  sleepJson += "\"totalSleepSeconds\":" + String(totalSec) + ",";
  sleepJson += "\"wakeCount\":" + String(wakes) + ",";
  sleepJson += "\"status\":\"completed\"";
  sleepJson += "}";

  firebasePatch(currentPath, sleepJson);
  firebasePut(sessionPath, sleepJson);
}

/* ================= NVS - STEPS ================= */
void saveStepsToNVS()
{
  prefs.putLong("steps", steps);
  prefs.putString("dayKey", currentDayKey);
}

void loadStepsFromNVS()
{
  steps = prefs.getLong("steps", 0);
  currentDayKey = prefs.getString("dayKey", "");
}

void loadTodayStepsFromFirebase()
{
  if (!hasLinkedUser())
  {
    Serial.println("Chua lien ket tai khoan -> khong tai steps tu Firebase.");
    return;
  }

  if (currentDayKey.length() == 0 || currentDayKey == "1970-01-01")
  {
    Serial.println("currentDayKey khong hop le -> khong tai steps.");
    return;
  }

  String path = userBasePath() + "/steps_history/" + currentDayKey + "/totalSteps";
  String response;

  if (firebaseGetString(path, response))
  {
    response.trim();

    if (response != "null" && response.length() > 0)
    {
      long fbSteps = response.toInt();
      steps = fbSteps;
      saveStepsToNVS();

      Serial.print("Da tai tong buoc hom nay tu Firebase: ");
      Serial.println(steps);
    }
    else
    {
      steps = 0;
      saveStepsToNVS();
      Serial.println("Hom nay chua co du lieu steps tren Firebase, khoi tao = 0");
    }
  }
  else
  {
    Serial.println("Khong doc duoc steps tu Firebase, dung du lieu NVS neu co.");
  }
}

void initStepPersistence()
{
  prefs.begin("health-app", false);

  String todayKey = getDateKey();
  loadStepsFromNVS();

  Serial.print("Ngay hom nay: ");
  Serial.println(todayKey);

  Serial.print("NVS dayKey: ");
  Serial.println(currentDayKey);

  Serial.print("NVS steps: ");
  Serial.println(steps);

  if (currentDayKey != todayKey)
  {
    currentDayKey = todayKey;
    steps = 0;
    saveStepsToNVS();

    Serial.println("Ngay moi hoac chua co du lieu NVS -> reset steps = 0");
  }

  //loadTodayStepsFromFirebase();
  //updateStepsToFirebase();
  loadTodayStepsFromFirebase();

if (updateStepsToFirebase())
{
  lastUploadedSteps = steps;
}

lastStepFirebaseSync = millis();
}

void checkNewDayAndResetSteps()
{
  if (millis() - lastDayCheckMillis < DAY_CHECK_INTERVAL)
    return;

  lastDayCheckMillis = millis();

  String todayKey = getDateKey();

  if (todayKey != currentDayKey)
  {
    Serial.println("Da sang ngay moi -> reset tong buoc cho ngay moi");

    currentDayKey = todayKey;
    /*steps = 0;
    saveStepsToNVS();

    updateStepsToFirebase();

    Serial.print("Ngay moi: ");
    Serial.println(currentDayKey);*/
    steps = 0;
saveStepsToNVS();

lastUploadedSteps = 0;

if (updateStepsToFirebase())
{
  lastUploadedSteps = steps;
}

lastStepFirebaseSync = millis();

Serial.print("Ngay moi: ");
Serial.println(currentDayKey);
  }
}

/* ================= TINH SPO2 ================= */
void calculateSpO2()
{
  long redMax = 0, redMin = 2147483647;
  long irMax = 0, irMin = 2147483647;

  double redSum = 0;
  double irSum = 0;

  for (int i = 0; i < SPO2_SAMPLES; i++)
  {
    if (redBuffer[i] > redMax) redMax = redBuffer[i];
    if (redBuffer[i] < redMin) redMin = redBuffer[i];

    if (irBuffer[i] > irMax) irMax = irBuffer[i];
    if (irBuffer[i] < irMin) irMin = irBuffer[i];

    redSum += redBuffer[i];
    irSum += irBuffer[i];
  }

  float redAC = redMax - redMin;
  float irAC = irMax - irMin;

  float redDC = redSum / (float)SPO2_SAMPLES;
  float irDC = irSum / (float)SPO2_SAMPLES;

  if (redDC <= 0 || irDC <= 0 || irAC <= 0 || redAC <= 0)
  {
    spo2 = 0;
    return;
  }

  float R = (redAC / redDC) / (irAC / irDC);
  float newSpO2 = 110.0 - 25.0 * R;

  if (newSpO2 > 100) newSpO2 = 100;
  if (newSpO2 < 0) newSpO2 = 0;

  if (spo2 == 0)
    spo2 = newSpO2;
  else
    spo2 = 0.7 * spo2 + 0.3 * newSpO2;
}

/* ================= DEM BUOC ================= */
void countSteps()
{
  unsigned long now = millis();

  // Không đọc MPU quá dày, 25Hz là đủ cho bước chân
  if (now - lastStepSampleTime < STEP_SAMPLE_INTERVAL)
    return;

  lastStepSampleTime = now;

  // Nếu lâu không có nhịp bước thì reset trạng thái xác nhận đi bộ
  if (lastCandidateStepTime > 0 &&
      now - lastCandidateStepTime > WALK_RESET_TIMEOUT)
  {
    pendingStepCount = 0;
    walkingConfirmed = false;
    lastCandidateStepTime = 0;
  }

  mpu.getAcceleration(&ax, &ay, &az);

  float Ax = ax / 16384.0f;
  float Ay = ay / 16384.0f;
  float Az = az / 16384.0f;

  accelMagnitude = sqrt(Ax * Ax + Ay * Ay + Az * Az);

  // Loại bỏ mẫu bất thường do va chạm/lắc mạnh
  if (accelMagnitude > STEP_MAX_VALID_MAG ||
      accelMagnitude < STEP_MIN_VALID_MAG)
  {
    stepPeakState = false;
    pendingStepCount = 0;
    walkingConfirmed = false;
    lastCandidateStepTime = 0;

    Serial.print("Step reject impact | Mag=");
    Serial.println(accelMagnitude, 2);
    return;
  }

  // Lọc nền trọng lực.
  // stepBaseline bám chậm theo tư thế cảm biến.
  stepBaseline = 0.98f * stepBaseline + 0.02f * accelMagnitude;

  // Dao động thật sau khi loại bỏ nền
  stepDynamic = accelMagnitude - stepBaseline;
  if (stepDynamic < 0)
    stepDynamic = 0;

  // Xung quá mạnh thường là chạm/lắc/va đập, không phải bước chân
  if (stepDynamic > STEP_MAX_DYNAMIC)
  {
    stepPeakState = false;
    pendingStepCount = 0;
    walkingConfirmed = false;
    lastCandidateStepTime = 0;

    Serial.print("Step reject shake | Mag=");
    Serial.print(accelMagnitude, 2);
    Serial.print(" | Dyn=");
    Serial.println(stepDynamic, 2);
    return;
  }

  // Phát hiện một xung có khả năng là bước
  if (!stepPeakState && stepDynamic > STEP_HIGH_THRESHOLD)
  {
    unsigned long interval = 0;

    if (lastCandidateStepTime > 0)
      interval = now - lastCandidateStepTime;

    // Xung đầu tiên: chỉ ghi nhận ứng viên, chưa cộng steps
    if (lastCandidateStepTime == 0 || interval > STEP_MAX_INTERVAL)
    {
      pendingStepCount = 1;
      walkingConfirmed = false;
      lastCandidateStepTime = now;
      stepPeakState = true;

      Serial.print("Step candidate 1/");
      Serial.print(WALK_CONFIRM_STEPS);
      Serial.print(" | Mag=");
      Serial.print(accelMagnitude, 2);
      Serial.print(" | Dyn=");
      Serial.println(stepDynamic, 2);
      return;
    }

    // Xung quá nhanh, thường là rung/chạm
    if (interval < STEP_MIN_INTERVAL)
    {
      stepPeakState = true;

      Serial.print("Step reject too fast | interval=");
      Serial.print(interval);
      Serial.print(" | Dyn=");
      Serial.println(stepDynamic, 2);
      return;
    }

    // Xung có nhịp hợp lệ
    if (interval >= STEP_MIN_INTERVAL && interval <= STEP_MAX_INTERVAL)
    {
      lastCandidateStepTime = now;
      stepPeakState = true;

      if (!walkingConfirmed)
      {
        pendingStepCount++;

        Serial.print("Step candidate ");
        Serial.print(pendingStepCount);
        Serial.print("/");
        Serial.print(WALK_CONFIRM_STEPS);
        Serial.print(" | interval=");
        Serial.print(interval);
        Serial.print(" | Dyn=");
        Serial.println(stepDynamic, 2);

        // Đủ 3 nhịp hợp lệ thì xác nhận đang đi bộ
        if (pendingStepCount >= WALK_CONFIRM_STEPS)
        {
          steps += pendingStepCount;
          walkingConfirmed = true;
          pendingStepCount = 0;

          saveStepsToNVS();

          Serial.print("WALK CONFIRMED -> Steps: ");
          Serial.println(steps);
        }

        return;
      }

      // Nếu đã xác nhận đang đi bộ thì cộng từng bước tiếp theo
      steps++;
      saveStepsToNVS();

      Serial.print("Steps: ");
      Serial.print(steps);
      Serial.print(" | Mag=");
      Serial.print(accelMagnitude, 2);
      Serial.print(" | Dyn=");
      Serial.print(stepDynamic, 2);
      Serial.print(" | interval=");
      Serial.println(interval);
    }
  }

  // Khi dao động giảm xuống thấp thì cho phép bắt xung tiếp theo
  if (stepPeakState && stepDynamic < STEP_LOW_THRESHOLD)
  {
    stepPeakState = false;
  }
}

/* ================= SLEEP DETECTION ================= */
void updateSleepDetection()
{
  unsigned long nowMs = millis();

  if (nowMs - lastSleepSampleMillis < SLEEP_SAMPLE_INTERVAL_MS)
    return;

  lastSleepSampleMillis = nowMs;

  int16_t sax, say, saz;
  mpu.getAcceleration(&sax, &say, &saz);

  float Ax = sax / 16384.0f;
  float Ay = say / 16384.0f;
  float Az = saz / 16384.0f;

  float accMag = sqrt(Ax * Ax + Ay * Ay + Az * Az);

  float delta = 0.0f;
  if (hasPrevSleepSample)
  {
    delta = fabs(accMag - prevSleepAccMag);
  }
  else
  {
    hasPrevSleepSample = true;
  }
  prevSleepAccMag = accMag;

  bool sampleActive = delta >= SLEEP_DELTA_THRESHOLD;
  bool sampleQuiet = !sampleActive;

  if (sampleActive)
  {
    activeStreak = min(activeStreak + 1, 100);
    quietStreak = 0;
  }
  else
  {
    quietStreak = min(quietStreak + 1, 100);
    activeStreak = 0;
  }

  bool isQuiet = quietStreak >= 2;
  bool isActive = activeStreak >= 2;

  switch (sleepState)
  {
    case AWAKE:
      if (isQuiet)
      {
        if (quietStartMs == 0)
          quietStartMs = nowMs;

        if (nowMs - quietStartMs >= TO_CANDIDATE_MS)
        {
          sleepState = CANDIDATE_SLEEP;
          Serial.println(">>> SLEEP: VAO CANDIDATE_SLEEP");
        }
      }
      else if (isActive)
      {
        quietStartMs = 0;
      }
      break;

    case CANDIDATE_SLEEP:
      if (isActive)
      {
        sleepState = AWAKE;
        quietStartMs = 0;
        Serial.println(">>> SLEEP: HUY CANDIDATE, QUAY VE AWAKE");
      }
      else if (isQuiet)
      {
        if (nowMs - quietStartMs >= TO_SLEEP_MS)
        {
          sleepState = SLEEPING;
          sleepStartUnix = getUnixTime();
          activeStartMs = 0;
          tempWakeStartMs = 0;
          wakeCount = 0;

          Serial.println(">>> SLEEP START");
          Serial.print("Sleep Start = ");
          Serial.println(formatUnixTimestamp(sleepStartUnix));
        }
      }
      break;

    case SLEEPING:
      if (isActive)
      {
        if (activeStartMs == 0)
          activeStartMs = nowMs;

        unsigned long activeDuration = nowMs - activeStartMs;

        if (activeDuration >= TO_TEMP_WAKE_MS)
        {
          tempWakeStartMs = activeStartMs;
          sleepState = TEMP_WAKE;
          quietStartMs = 0;

          Serial.println(">>> THUC TAM");
        }
      }
      else if (isQuiet)
      {
        activeStartMs = 0;
      }
      break;

    case TEMP_WAKE:
      if (isQuiet)
      {
        if (quietStartMs == 0)
          quietStartMs = nowMs;

        unsigned long quietDuration = nowMs - quietStartMs;

        if (quietDuration >= BACK_TO_SLEEP_MS)
        {
          wakeCount++;
          sleepState = SLEEPING;
          activeStartMs = 0;
          tempWakeStartMs = 0;

          Serial.println(">>> NGU LAI, TIEP TUC PHIEN NGU");
          Serial.print("Wake Count = ");
          Serial.println(wakeCount);
        }
      }
      else if (isActive)
      {
        quietStartMs = 0;

        if (tempWakeStartMs > 0)
        {
          unsigned long wakeDuration = nowMs - tempWakeStartMs;

          if (wakeDuration >= END_SLEEP_MS)
          {
            sleepEndUnix = getUnixTime();

            if (sleepStartUnix > 100000 && sleepEndUnix > sleepStartUnix)
              totalSleepMs = (unsigned long)(sleepEndUnix - sleepStartUnix) * 1000UL;
            else
              totalSleepMs = 0;

            lastCompletedSleepStartUnix = sleepStartUnix;
            lastCompletedSleepEndUnix = sleepEndUnix;
            lastCompletedTotalSleepMs = totalSleepMs;
            lastCompletedWakeCount = wakeCount;

            Serial.println(">>> SLEEP END");
            printSleepFinalResult();

            sendSleepSessionToFirebase(
              lastCompletedSleepStartUnix,
              lastCompletedSleepEndUnix,
              lastCompletedTotalSleepMs,
              lastCompletedWakeCount
            );

            sleepState = AWAKE;
            resetSleepSessionTracking();
          }
        }
      }
      break;
  }
}
void configureMAX30102()
{
  particleSensor.setup(
    MAX_LED_BRIGHTNESS,
    MAX_SAMPLE_AVERAGE,
    MAX_LED_MODE,
    MAX_SAMPLE_RATE,
    MAX_PULSE_WIDTH,
    MAX_ADC_RANGE
  );

  particleSensor.setPulseAmplitudeRed(MAX_LED_BRIGHTNESS);
  particleSensor.setPulseAmplitudeIR(MAX_LED_BRIGHTNESS);
  particleSensor.setPulseAmplitudeGreen(0);
}
/* ================= SETUP ================= */
void setup()
{
  Serial.begin(115200);
  delay(1000);

  Wire.begin(SDA_PIN, SCL_PIN);

  initOLED();

 connectWiFi();
syncTimeNTP();

// Đọc tài khoản đang ghép với thiết bị
loadPairCodeFromFirebase();
loadOwnerUidFromFirebase();

initStepPersistence();

  // MAX30102
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST))
  {
    Serial.println("Khong tim thay MAX30102");
    while (1);
  }

  configureMAX30102();

  // MPU6050
  mpu.initialize();

  if (mpu.testConnection())
    Serial.println("MPU6050 san sang");
  else
    Serial.println("MPU6050 loi");

  Serial.println("Dat ngon tay vao cam bien...");
  Serial.println("Sleep detection da duoc kich hoat.");
  Serial.println("OLED: lat UP/DOWN/UP hoac DOWN/UP/DOWN de bat man, nghieng trai/phai de doi trang.");
}

/* ================= LOOP ================= */
void loop()
{
  handleWiFiAutoReconnect();



  checkNewDayAndResetSteps();

  // Dem buoc luon luon
  countSteps();

// Dong bo steps len Firebase moi 1 phut neu thay doi > 20 buoc
syncStepsToFirebaseIfNeeded();

  // Theo doi giac ngu luon luon
  updateSleepDetection();

  // Dieu khien OLED bang MPU6050
  handleGestureControl();
  updateOLEDDataAndRender();

  // Doc MAX30102
  irValue = particleSensor.getIR();
  redValue = particleSensor.getRed();

  // Bat dau do khi co tay
  if (irValue > 50000 && !measuring)
  {
    measuring = true;
    startTimeMillis = millis();
    sessionStartUnix = getUnixTime();
    resetData();

    Serial.println("Bat dau do 60 giay...");

    if (oledOn)
    {
      currentPage = 1;
      lastInteraction = millis();
      lastOledRefresh = 0;
    }
  }

  if (measuring)
  {
    if (irValue < 50000)
    {
      Serial.println("Da rut tay - Huy lan do!");
      measuring = false;
      resetData();
      delay(10);
      return;
    }

    // Luu mau SpO2
    redBuffer[bufferIndex] = redValue;
    irBuffer[bufferIndex] = irValue;
    bufferIndex++;

    if (bufferIndex >= SPO2_SAMPLES)
    {
      calculateSpO2();
      bufferIndex = 0;
    }

    // Thuat toan nhip tim
    if (checkForBeat(irValue))
    {
      long delta = millis() - lastBeat;
      lastBeat = millis();

      beatsPerMinute = 60 / (delta / 1000.0);

      if (beatsPerMinute > 40 && beatsPerMinute < 200)
      {
        rates[rateSpot++] = (byte)beatsPerMinute;
        rateSpot %= RATE_SIZE;

        float sumRate = 0;
        int validRateCount = 0;

        for (byte i = 0; i < RATE_SIZE; i++)
        {
          if (rates[i] > 0)
          {
            sumRate += rates[i];
            validRateCount++;
          }
        }

        if (validRateCount > 0)
          beatAvg = sumRate / validRateCount;
        else
          beatAvg = 0;

        Serial.print("Thoi gian: ");
        Serial.print((millis() - startTimeMillis) / 1000);
        Serial.print(" s | BPM hien tai: ");
        Serial.print(beatsPerMinute);

        Serial.print(" | BPM TB: ");
        Serial.print(beatAvg);

        Serial.print(" | SpO2: ");
        Serial.print(spo2, 1);
        Serial.println(" %");
      }
    }

    // Sau 60 giay
    if (millis() - startTimeMillis >= VITAL_MEASURE_DURATION_MS)
    {
      time_t endUnix = getUnixTime();

      Serial.println("=================================");
      Serial.print("Nhip tim trung binh: ");
      Serial.print(beatAvg);
      Serial.println(" BPM");

      Serial.print("SpO2: ");
      Serial.print(spo2, 1);
      Serial.println(" %");
      Serial.println("=================================");

      latestHeartRate = beatAvg;
      latestSpo2 = spo2;
      latestVitalTime = endUnix;

      if (WiFi.status() == WL_CONNECTED && endUnix > 100000)
      {
        sendMeasurementToFirebase(beatAvg, spo2, sessionStartUnix, endUnix);
      }
      else
      {
        Serial.println("Khong gui Firebase duoc vi mat WiFi hoac chua co NTP time.");
      }

      measuring = false;
      resetData();
      Serial.println("Co the do lai.");

      if (oledOn)
      {
        currentPage = 0;
        lastInteraction = millis();
        lastOledRefresh = 0;
      }
    }
      // Định kỳ đọc ownerUid để biết thiết bị đang thuộc tài khoản nào
  syncOwnerUidIfNeeded();
  }

  if (measuring)
  delay(2);
else
  delay(10);
}
