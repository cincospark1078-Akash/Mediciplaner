package com.mediciplanner.app.ui.screens.onboarding

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mediciplanner.app.MediciApplication
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.FemalePrimary
import com.mediciplanner.app.core.theme.MalePrimary
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.theme.NeutralPrimary
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.ui.components.BouncingButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PersonalizationUiState(
    val userName: String = "",
    val userMobile: String = "",
    val userEmail: String = "",
    val userAddress: String = "",
    val userProfilePicPath: String = "",
    val greetingPreference: String = "universal",
    val customGreeting: String = "",
    val selectedThemeMode: AppThemeMode = AppThemeMode.NEUTRAL
)

class PersonalizationViewModel(
    private val themePreferences: ThemePreferences,
    initialTheme: AppThemeMode = AppThemeMode.NEUTRAL
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PersonalizationUiState(selectedThemeMode = initialTheme)
    )
    val uiState: StateFlow<PersonalizationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedName = themePreferences.userNameFlow.first()
            val savedMobile = themePreferences.userMobileFlow.first()
            val savedEmail = themePreferences.userEmailFlow.first()
            val savedAddress = themePreferences.userAddressFlow.first()
            val savedPicPath = themePreferences.userProfilePicPathFlow.first()
            val savedGreeting = themePreferences.greetingPreferenceFlow.first()
            val savedCustom = themePreferences.customGreetingFlow.first()

            _uiState.value = _uiState.value.copy(
                userName = if (_uiState.value.userName.isEmpty()) savedName else _uiState.value.userName,
                userMobile = if (_uiState.value.userMobile.isEmpty()) savedMobile else _uiState.value.userMobile,
                userEmail = if (_uiState.value.userEmail.isEmpty()) savedEmail else _uiState.value.userEmail,
                userAddress = if (_uiState.value.userAddress.isEmpty()) savedAddress else _uiState.value.userAddress,
                userProfilePicPath = if (_uiState.value.userProfilePicPath.isEmpty()) savedPicPath else _uiState.value.userProfilePicPath,
                greetingPreference = savedGreeting,
                customGreeting = if (_uiState.value.customGreeting.isEmpty()) savedCustom.take(30) else _uiState.value.customGreeting
            )
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(userName = name)
    }

    fun updateMobile(mobile: String) {
        _uiState.value = _uiState.value.copy(userMobile = mobile)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(userEmail = email)
    }

    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(userAddress = address)
    }

    fun updateProfilePicPath(path: String) {
        _uiState.value = _uiState.value.copy(userProfilePicPath = path)
    }

    fun updateGreeting(preference: String) {
        _uiState.value = _uiState.value.copy(greetingPreference = preference)
    }

    fun updateCustomGreeting(greeting: String) {
        _uiState.value = _uiState.value.copy(customGreeting = greeting.take(30))
    }

    fun updateTheme(mode: AppThemeMode) {
        _uiState.value = _uiState.value.copy(selectedThemeMode = mode)
    }

    fun savePersonalization(onCompleted: (String, String, AppThemeMode) -> Unit) {
        val state = _uiState.value
        val trimmedName = state.userName.trim()
        val trimmedMobile = state.userMobile.trim()
        val trimmedEmail = state.userEmail.trim()
        val trimmedAddress = state.userAddress.trim()
        val profilePicPath = state.userProfilePicPath
        val greeting = state.greetingPreference
        val trimmedCustomGreeting = state.customGreeting.trim().take(30)
        val mode = state.selectedThemeMode
        onCompleted(trimmedName, greeting, mode)
        viewModelScope.launch {
            val themeGender = when (mode) {
                AppThemeMode.MALE -> "male"
                AppThemeMode.FEMALE -> "female"
                AppThemeMode.NEUTRAL -> "neutral"
            }
            themePreferences.savePersonalization(
                userName = trimmedName,
                greetingPreference = greeting,
                themeGender = themeGender,
                customGreeting = trimmedCustomGreeting,
                userMobile = trimmedMobile,
                userEmail = trimmedEmail,
                userAddress = trimmedAddress,
                userProfilePicPath = profilePicPath
            )
        }
    }

    fun skipWithDefaults(onCompleted: () -> Unit) {
        onCompleted()
        viewModelScope.launch {
            themePreferences.savePersonalization(
                userName = "",
                greetingPreference = "universal",
                themeGender = "neutral",
                customGreeting = ""
            )
        }
    }
}

class PersonalizationViewModelFactory(
    private val themePreferences: ThemePreferences,
    private val initialTheme: AppThemeMode
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PersonalizationViewModel(themePreferences, initialTheme) as T
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(
    currentTheme: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onSaveAndContinue: (userName: String, greetingPreference: String, themeMode: AppThemeMode) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalizationViewModel? = null,
    isEditMode: Boolean = false,
    onNavigateBack: (() -> Unit)? = null
) {
    if (isEditMode && onNavigateBack != null) {
        BackHandler { onNavigateBack() }
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val app = context.applicationContext as? MediciApplication
    val actualViewModel: PersonalizationViewModel = viewModel ?: if (app != null) {
        viewModel(factory = PersonalizationViewModelFactory(app.themePreferences, currentTheme))
    } else {
        viewModel(factory = PersonalizationViewModelFactory(ThemePreferences(context), currentTheme))
    }

    val uiState by actualViewModel.uiState.collectAsStateWithLifecycle()

    val profilePicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val profileDir = File(context.filesDir, "profile_pics")
                if (!profileDir.exists()) {
                    profileDir.mkdirs()
                }
                val fileName = "${UUID.randomUUID()}.jpg"
                val destFile = File(profileDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0L) {
                    actualViewModel.updateProfilePicPath(destFile.absolutePath)
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .testTag("personalization_screen"),
        containerColor = MediciTheme.colors.surface,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MediciTheme.colors.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BouncingButton(
                    onClick = {
                        actualViewModel.savePersonalization { name, greeting, mode ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_profile_updated),
                                Toast.LENGTH_SHORT
                            ).show()
                            onSaveAndContinue(name, greeting, mode)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("personalization_get_started_button")
                ) {
                    Text(
                        text = if (isEditMode) {
                            stringResource(R.string.personalization_save_changes)
                        } else {
                            stringResource(R.string.personalization_start_button)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = {
                        if (isEditMode && onNavigateBack != null) {
                            onNavigateBack()
                        } else {
                            actualViewModel.skipWithDefaults {
                                onSkip()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("personalization_skip_button")
                ) {
                    Text(
                        text = if (isEditMode) {
                            stringResource(R.string.settings_cancel_btn)
                        } else {
                            stringResource(R.string.personalization_skip_button)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Screen Header: Title (Bold 24.sp), Subtitle (14.sp), Small Note (12.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.personalization_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MediciTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personalization_title")
                )

                Text(
                    text = stringResource(R.string.personalization_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MediciTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personalization_subtitle")
                )

                Text(
                    text = stringResource(R.string.personalization_note),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MediciTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personalization_note")
                )
            }

            // Profile Avatar (80.dp) above the fields
            val avatarChangeDesc = stringResource(R.string.profile_avatar_change_desc)
            val avatarInitial = uiState.userName.trim().firstOrNull()?.let { ch ->
                if (ch.isLowerCase()) ch.uppercaseChar() else ch
            }?.toString()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = avatarChangeDesc
                        }
                        .testTag("profile_avatar_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MediciTheme.colors.primary, CircleShape)
                            .border(2.dp, MediciTheme.colors.primaryContainer, CircleShape)
                            .clickable { profilePicLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val picFile = if (uiState.userProfilePicPath.isNotBlank()) {
                            File(uiState.userProfilePicPath)
                        } else {
                            null
                        }
                        if (picFile != null && picFile.exists()) {
                            AsyncImage(
                                model = picFile,
                                contentDescription = avatarChangeDesc,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .testTag("profile_avatar_image")
                            )
                        } else if (!avatarInitial.isNullOrEmpty()) {
                            Text(
                                text = avatarInitial,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.onPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("profile_avatar_initial")
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = avatarChangeDesc,
                                tint = MediciTheme.colors.onPrimary,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("profile_avatar_icon")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .background(MediciTheme.colors.card, CircleShape)
                            .border(1.5.dp, MediciTheme.colors.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MediciTheme.colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Section 1: Personal Details (Name, Mobile, Email, Address)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.card
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalization_name_label_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MediciTheme.colors.primaryContainer, CircleShape)
                                .testTag("personalization_name_icon"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.testTag("personalization_name_label_text")
                        ) {
                            Text(
                                text = stringResource(R.string.personalization_name_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "(${stringResource(R.string.personalization_name_optional)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.userName,
                        onValueChange = { actualViewModel.updateName(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.personalization_name_placeholder),
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalization_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary
                        )
                    )

                    // Mobile Number (Numeric keyboard)
                    OutlinedTextField(
                        value = uiState.userMobile,
                        onValueChange = { actualViewModel.updateMobile(it) },
                        label = {
                            Text(
                                text = stringResource(R.string.profile_mobile_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.profile_mobile_placeholder),
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = stringResource(R.string.profile_mobile_label),
                                tint = MediciTheme.colors.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_mobile_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary,
                            focusedLabelColor = MediciTheme.colors.primary,
                            unfocusedLabelColor = MediciTheme.colors.textSecondary
                        )
                    )

                    // Email (Email keyboard)
                    OutlinedTextField(
                        value = uiState.userEmail,
                        onValueChange = { actualViewModel.updateEmail(it) },
                        label = {
                            Text(
                                text = stringResource(R.string.profile_email_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.profile_email_placeholder),
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = stringResource(R.string.profile_email_label),
                                tint = MediciTheme.colors.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary,
                            focusedLabelColor = MediciTheme.colors.primary,
                            unfocusedLabelColor = MediciTheme.colors.textSecondary
                        )
                    )

                    // Address (Multiline)
                    OutlinedTextField(
                        value = uiState.userAddress,
                        onValueChange = { actualViewModel.updateAddress(it) },
                        label = {
                            Text(
                                text = stringResource(R.string.profile_address_label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.profile_address_placeholder),
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.profile_address_label),
                                tint = MediciTheme.colors.primary
                            )
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_address_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MediciTheme.colors.primary,
                            unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                            focusedTextColor = MediciTheme.colors.textPrimary,
                            unfocusedTextColor = MediciTheme.colors.textPrimary,
                            focusedLabelColor = MediciTheme.colors.primary,
                            unfocusedLabelColor = MediciTheme.colors.textSecondary
                        )
                    )
                }
            }

            // Section 2: Greeting Preference
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.card
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.personalization_greeting_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("greeting_preference_label")
                    )

                    // 5 Inclusive Options: Muslim, Universal, Hindu, English, Custom
                    val greetingOptions = listOf(
                        Triple("salam", stringResource(R.string.greeting_option_salam), "greeting_chip_salam"),
                        Triple("universal", stringResource(R.string.greeting_option_universal), "greeting_chip_universal"),
                        Triple("namaskar", stringResource(R.string.greeting_option_namaskar), "greeting_chip_namaskar"),
                        Triple("hello", stringResource(R.string.greeting_option_hello), "greeting_chip_hello"),
                        Triple("custom", stringResource(R.string.greeting_option_custom), "greeting_chip_custom")
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        greetingOptions.forEach { (key, label, tag) ->
                            val isSelected = uiState.greetingPreference == key ||
                                    (uiState.greetingPreference.isBlank() && key == "universal")

                            FilterChip(
                                selected = isSelected,
                                onClick = { actualViewModel.updateGreeting(key) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MediciTheme.colors.primaryContainer,
                                    selectedLabelColor = MediciTheme.colors.primary,
                                    selectedLeadingIconColor = MediciTheme.colors.primary,
                                    containerColor = MediciTheme.colors.surface,
                                    labelColor = MediciTheme.colors.textPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) MediciTheme.colors.primary else MediciTheme.colors.outlineVariant
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag(tag)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.greetingPreference == "custom",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = uiState.customGreeting,
                            onValueChange = { input ->
                                actualViewModel.updateCustomGreeting(input.take(30))
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.personalization_custom_greeting_label),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingText = {
                                Text(
                                    text = "${uiState.customGreeting.length}/30",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MediciTheme.colors.textSecondary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("personalization_custom_greeting_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MediciTheme.colors.primary,
                                unfocusedBorderColor = MediciTheme.colors.outlineVariant,
                                focusedTextColor = MediciTheme.colors.textPrimary,
                                unfocusedTextColor = MediciTheme.colors.textPrimary,
                                focusedLabelColor = MediciTheme.colors.primary,
                                unfocusedLabelColor = MediciTheme.colors.textSecondary
                            )
                        )
                    }
                }
            }

            // Section 3: App Theme
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MediciTheme.colors.card
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MediciTheme.colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.personalization_theme_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("app_theme_label")
                        )
                    }

                    // Three selectable cards (পুরুষ, মহিলা, সাধারণ)
                    val themeCards = listOf(
                        ThemeCardItem(
                            mode = AppThemeMode.MALE,
                            title = stringResource(R.string.theme_male_title),
                            previewColor = MalePrimary,
                            tag = "theme_card_male"
                        ),
                        ThemeCardItem(
                            mode = AppThemeMode.FEMALE,
                            title = stringResource(R.string.theme_female_title),
                            previewColor = FemalePrimary,
                            tag = "theme_card_female"
                        ),
                        ThemeCardItem(
                            mode = AppThemeMode.NEUTRAL,
                            title = stringResource(R.string.theme_neutral_title),
                            previewColor = NeutralPrimary,
                            tag = "theme_card_neutral"
                        )
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        themeCards.forEach { item ->
                            val isSelected = uiState.selectedThemeMode == item.mode

                            val animatedBorderColor by animateColorAsState(
                                targetValue = if (isSelected) MediciTheme.colors.primary else MediciTheme.colors.outlineVariant,
                                animationSpec = tween(250),
                                label = "border_color"
                            )

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        actualViewModel.updateTheme(item.mode)
                                        onThemeSelected(item.mode) // Apply theme immediately to screen
                                    }
                                    .testTag(item.tag),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MediciTheme.colors.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MediciTheme.colors.surface
                                    }
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = animatedBorderColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Theme color preview circle
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(item.previewColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MediciTheme.colors.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MediciTheme.colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MediciTheme.colors.primary,
                                                    shape = CircleShape
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.theme_current_tag),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MediciTheme.colors.onPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FADE EFFECT: At the bottom of the Box (just above the fixed buttons), overlay a Box with a vertical gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MediciTheme.colors.surface.copy(alpha = 0f),
                            MediciTheme.colors.surface
                        )
                    )
                )
                .testTag("scroll_fade_effect")
        )
    }
}
}

private data class ThemeCardItem(
    val mode: AppThemeMode,
    val title: String,
    val previewColor: Color,
    val tag: String
)
