
package com.example.healthapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.healthapp.data.model.UserProfileModel
import com.example.healthapp.ui.components.BottomNavBar
import com.example.healthapp.ui.components.BottomTab
import com.example.healthapp.ui.navigation.Screen
import com.example.healthapp.ui.theme.*
import com.example.healthapp.viewmodel.AuthViewModel
import com.example.healthapp.viewmodel.ProfileViewModel
import java.io.File
import java.util.Calendar
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val userProfile by profileViewModel.userProfile.collectAsState()
    val devicePairState by profileViewModel.devicePairState.collectAsState()
    val isUploadingImage by profileViewModel.isUploadingImage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showEditProfile by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalType?>(null) }
    var showDevicePairDialog by remember { mutableStateOf(false) }
    var showLinkedDeviceDialog by remember { mutableStateOf(false) }
    var showImagePickerSheet by remember { mutableStateOf(false) }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { profileViewModel.updateProfileImage(context, it) }
    }

    // Camera picker launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Save bitmap to a temp file, then pass its URI to ViewModel
            val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
            tempFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            val uri = android.net.Uri.fromFile(tempFile)
            profileViewModel.updateProfileImage(context, uri)
        }
    }

    devicePairState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            profileViewModel.clearDevicePairMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cá nhân",
                        color = PrimaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        },
        bottomBar = {
            BottomNavBar(
                selected = BottomTab.PROFILE,
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ProfileHeader(
                    profile = userProfile,
                    isUploadingImage = isUploadingImage,
                    onEditClick = { showEditProfile = true },
                    onAvatarClick = { showImagePickerSheet = true }
                )
            }


            item {
                PhysicalStatsRow(profile = userProfile)
            }

            item {
                PersonalGoalsSection(
                    profile = userProfile,
                    onEditGoal = { goalToEdit = it }
                )
            }

            item {
                SettingsSection(
                    onDeviceClick = {
                        profileViewModel.clearDevicePairState()
                        if (userProfile.linkedDeviceId.isBlank()) {
                            showDevicePairDialog = true
                        } else {
                            showLinkedDeviceDialog = true
                        }
                    },
                    onLogoutClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    if (showEditProfile) {
        EditProfileDialog(
            profile = userProfile,
            onDismiss = { showEditProfile = false },
            onSave = {
                profileViewModel.saveUserProfile(it)
                showEditProfile = false
            }
        )
    }

    if (showDevicePairDialog) {
        DevicePairDialog(
            isLoading = devicePairState.isLoading,
            errorMessage = devicePairState.errorMessage,
            onDismiss = {
                showDevicePairDialog = false
                profileViewModel.clearDevicePairState()
            },
            onPair = { deviceId, pairCode ->
                profileViewModel.pairDevice(
                    deviceId = deviceId,
                    pairCode = pairCode,
                    onSuccess = {
                        showDevicePairDialog = false
                    }
                )
            }
        )
    }

    if (showLinkedDeviceDialog) {
        LinkedDeviceDialog(
            deviceId = userProfile.linkedDeviceId,
            isLoading = devicePairState.isLoading,
            errorMessage = devicePairState.errorMessage,
            onDismiss = {
                showLinkedDeviceDialog = false
                profileViewModel.clearDevicePairState()
            },
            onUnlink = {
                profileViewModel.unlinkDevice(
                    deviceId = userProfile.linkedDeviceId,
                    onSuccess = {
                        showLinkedDeviceDialog = false
                    }
                )
            }
        )
    }

    goalToEdit?.let { goalType ->
        val goalValue = when (goalType) {
            GoalType.Steps -> userProfile.stepGoal
            GoalType.Calories -> userProfile.calorieGoal
            GoalType.Sleep -> userProfile.sleepGoalSeconds
        }

        val title = when (goalType) {
            GoalType.Steps -> "Mục tiêu bước chân"
            GoalType.Calories -> "Mục tiêu calories"
            GoalType.Sleep -> "Mục tiêu giấc ngủ"
        }

        val unit = when (goalType) {
            GoalType.Steps -> "bước"
            GoalType.Calories -> "kcal"
            GoalType.Sleep -> "giờ"
        }

        val currentText = when (goalType) {
            GoalType.Sleep -> secondsToHoursText(goalValue)
            else -> goalValue.takeIf { it > 0L }?.toString() ?: ""
        }

        EditGoalDialog(
            title = title,
            unit = unit,
            currentValue = currentText,
            onDismiss = { goalToEdit = null },
            onSave = { input ->
                val fields = when (goalType) {
                    GoalType.Steps -> input.toLongOrNull()?.let {
                        mapOf("stepGoal" to it)
                    }

                    GoalType.Calories -> input.toLongOrNull()?.let {
                        mapOf("calorieGoal" to it)
                    }

                    GoalType.Sleep -> input.toDoubleOrNull()?.let {
                        mapOf("sleepGoalSeconds" to (it * 3600.0).toLong())
                    }
                }

                if (fields != null) {
                    profileViewModel.updateUserProfileFields(fields)
                }

                goalToEdit = null
            }
        )
    }

    // Image Picker Bottom Sheet
    if (showImagePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImagePickerSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Chọn ảnh đại diện",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Camera option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showImagePickerSheet = false
                            cameraLauncher.launch(null)
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                PrimaryColor.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            "Chụp ảnh",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Sử dụng camera để chụp ảnh mới",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Gallery option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showImagePickerSheet = false
                            galleryLauncher.launch("image/*")
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Color(0xFF6366F1).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            "Thư viện ảnh",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Chọn ảnh từ bộ sưu tập của bạn",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Remove photo option (only show when a photo exists)
                if (userProfile.profileImageUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showImagePickerSheet = false
                                profileViewModel.updateUserProfileFields(
                                    mapOf("profileImageUri" to "")
                                )
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color.Red.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                "Xóa ảnh đại diện",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.Red
                            )
                            Text(
                                "Quay lại ảnh đại diện mặc định",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profile: UserProfileModel,
    isUploadingImage: Boolean = false,
    onEditClick: () -> Unit,
    onAvatarClick: () -> Unit = {}
) {
    val displayName = profile.name.ifBlank { "Chưa cập nhật" }
    val hasProfileImage = profile.profileImageUri.isNotBlank() &&
            File(profile.profileImageUri).exists()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            // Avatar image or placeholder
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8EAF0))
                    .border(3.dp, PrimaryColor.copy(alpha = 0.15f), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (hasProfileImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(profile.profileImageUri))
                            .crossfade(300)
                            .build(),
                        contentDescription = "Ảnh đại diện",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Avatar placeholder",
                        tint = Color(0xFFB0B5C0),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Loading overlay while uploading
            if (isUploadingImage) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                }
            }

            // Camera button overlay
            IconButton(
                onClick = onAvatarClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .background(PrimaryColor, CircleShape)
                    .border(2.dp, SurfaceColor, CircleShape)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Đổi ảnh đại diện",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            displayName,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TextOnSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(50.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    "Chỉnh sửa",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(
                onClick = { /* Share */ },
                shape = RoundedCornerShape(50.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D0D0)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    "Chia sẻ",
                    color = TextOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PhysicalStatsRow(profile: UserProfileModel) {
    val ageText = calculateAgeText(profile.yearOfBirth)
    val heightText = profile.height.takeIf { it > 0 }?.toString() ?: "--"
    val weightText = profile.weight.takeIf { it > 0 }?.toString() ?: "--"
    val genderDisplay = genderDisplay(profile.gender)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "TUỔI", value = ageText, modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(OutlineVariant)
        )

        StatItem(
            label = genderDisplay.label,
            value = genderDisplay.value,
            icon = genderDisplay.icon,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(OutlineVariant)
        )

        StatItem(
            label = "CHIỀU CAO",
            value = heightText,
            unit = "cm",
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(OutlineVariant)
        )

        StatItem(
            label = "CÂN NẶNG",
            value = weightText,
            unit = "kg",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatItem(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    unit: String? = null,
    icon: ImageVector? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1
                )

                if (unit != null) {
                    Text(
                        " $unit",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalGoalsSection(
    profile: UserProfileModel,
    onEditGoal: (GoalType) -> Unit
) {
    val stepGoalText = profile.stepGoal.takeIf { it > 0L }?.let { formatNumber(it) } ?: "--"
    val calorieGoalText = profile.calorieGoal.takeIf { it > 0L }?.let { formatNumber(it) } ?: "--"
    val sleepGoalText = profile.sleepGoalSeconds.takeIf { it > 0L }?.let { formatSleepHours(it) } ?: "--"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Mục tiêu cá nhân",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        GoalCard(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            color = SecondaryColor,
            bg = Color(0xFFA0F399).copy(alpha = 0.3f),
            title = "Bước chân",
            value = stepGoalText,
            unit = "bước",
            onEdit = { onEditGoal(GoalType.Steps) }
        )

        GoalCard(
            icon = Icons.Default.Whatshot,
            color = Color(0xFFF97316),
            bg = Color(0xFFFFEDD5),
            title = "Calories",
            value = calorieGoalText,
            unit = "kcal",
            onEdit = { onEditGoal(GoalType.Calories) }
        )

        GoalCard(
            icon = Icons.Default.NightsStay,
            color = Color(0xFF6366F1),
            bg = Color(0xFFE0E7FF),
            title = "Giấc ngủ",
            value = sleepGoalText,
            unit = "giờ",
            onEdit = { onEditGoal(GoalType.Sleep) }
        )
    }
}

@Composable
fun GoalCard(
    icon: ImageVector,
    color: Color,
    bg: Color,
    title: String,
    value: String,
    unit: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "Mục tiêu: ",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1
                )

                Text(
                    value,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1
                )

                Text(
                    " $unit",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

enum class GoalType {
    Steps,
    Calories,
    Sleep
}

private data class GenderDisplay(
    val label: String,
    val value: String?,
    val icon: ImageVector?
)

private fun genderDisplay(rawGender: String): GenderDisplay {
    val gender = rawGender.trim().lowercase(Locale.getDefault())

    return when (gender) {
        "nam", "male", "m" -> GenderDisplay("NAM", null, Icons.Default.Male)
        "nu", "nữ", "female", "f" -> GenderDisplay("NỮ", null, Icons.Default.Female)
        "khac", "khác", "other" -> GenderDisplay("GIỚI TÍNH", "Khác", null)
        else -> GenderDisplay(
            "GIỚI TÍNH",
            rawGender.takeIf { it.isNotBlank() } ?: "--",
            null
        )
    }
}

private fun calculateAgeText(yearOfBirth: Int): String {
    if (yearOfBirth <= 0) return "--"

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val age = (currentYear - yearOfBirth).coerceAtLeast(0)

    return age.toString()
}

private fun formatNumber(value: Long): String {
    return "%,d".format(value).replace(",", ".")
}

private fun formatSleepHours(seconds: Long): String {
    val hours = seconds / 3600.0

    if (hours <= 0.0) return "--"

    return if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", hours)
    }
}

private fun secondsToHoursText(seconds: Long): String {
    if (seconds <= 0L) return ""

    val hours = seconds / 3600.0

    return if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", hours)
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfileModel,
    onDismiss: () -> Unit,
    onSave: (UserProfileModel) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var gender by remember { mutableStateOf(profile.gender) }
    var height by remember { mutableStateOf(profile.height.takeIf { it > 0 }?.toString() ?: "") }
    var weight by remember { mutableStateOf(profile.weight.takeIf { it > 0 }?.toString() ?: "") }
    var yearOfBirth by remember {
        mutableStateOf(profile.yearOfBirth.takeIf { it > 0 }?.toString() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chỉnh sửa hồ sơ")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ tên") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Giới tính") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = yearOfBirth,
                    onValueChange = { yearOfBirth = it },
                    label = { Text("Năm sinh") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Chiều cao (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Cân nặng (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updated = profile.copy(
                        createdAt = profile.createdAt.takeIf { it > 0L }
                            ?: System.currentTimeMillis(),
                        name = name.trim(),
                        gender = gender.trim(),
                        yearOfBirth = yearOfBirth.toIntOrNull()
                            ?: profile.yearOfBirth,
                        height = height.toIntOrNull()
                            ?: profile.height,
                        weight = weight.toIntOrNull()
                            ?: profile.weight
                    )

                    onSave(updated)
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun EditGoalDialog(
    title: String,
    unit: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var valueText by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text("Giá trị ($unit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(valueText.trim())
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun DevicePairDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPair: (deviceId: String, pairCode: String) -> Unit
) {
    var deviceId by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Kết nối thiết bị",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nhập ID thiết bị và mã ghép đang hiển thị trên màn OLED.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("ID thiết bị") },
                    placeholder = { Text("Ví dụ: device_001") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pairCode,
                    onValueChange = { pairCode = it },
                    label = { Text("Mã ghép") },
                    placeholder = { Text("Ví dụ: 842193") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPair(deviceId, pairCode)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Kết nối")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun LinkedDeviceDialog(
    deviceId: String,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onUnlink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Thiết bị đã liên kết",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "UID thiết bị: ${deviceId.ifBlank { "--" }}",
                    fontSize = 14.sp
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onUnlink,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Hủy liên kết")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun SettingsSection(
    onDeviceClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Cài đặt",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, OutlineVariant, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
        ) {
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                label = "Thông báo & Nhắc nhở"
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = OutlineVariant
            )

            SettingsItem(
                icon = Icons.Outlined.Lock,
                label = "Quyền riêng tư"
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = OutlineVariant
            )

            SettingsItem(
                icon = Icons.Outlined.Devices,
                label = "Thiết bị",
                onClick = onDeviceClick
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogoutClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Đăng xuất",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            label,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}