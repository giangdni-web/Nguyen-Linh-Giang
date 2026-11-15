package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.model.ConsultationSession
import com.example.model.LegalCategory
import com.example.model.LegalDocumentCatalog
import com.example.ui.AiAnswerState
import com.example.ui.DataUiState
import com.example.ui.components.AdvisorCredentialsCard
import com.example.ui.components.LawCategoryList
import com.example.ui.screens.AppUpdateSettingsCard
import com.example.ui.screens.ConsultationRoomScreen
import com.example.ui.screens.HomeConsultTab
import com.example.ui.theme.MrGiangLawTheme
import com.example.util.AppUpdateStatus
import com.example.util.AppUpdateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mr.Giang Law", appName)
  }

  @Test
  fun `LawCategoryList organizes Civil Criminal and Labor categories and triggers selection`() {
    var selected: LegalCategory? = null
    composeTestRule.setContent {
      MrGiangLawTheme {
        LawCategoryList(
          documents = LegalDocumentCatalog.documents,
          selectedCategory = selected,
          onCategorySelected = { selected = it }
        )
      }
    }

    composeTestRule.onNodeWithTag("law_category_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("law_category_item_civil").assertIsDisplayed()
    composeTestRule.onNodeWithTag("law_category_item_criminal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("law_category_item_labor").assertIsDisplayed()

    composeTestRule.onNodeWithTag("law_category_item_criminal").performClick()
    assertEquals(LegalCategory.CRIMINAL, selected)
  }

  @Test
  fun `AdvisorCredentialsCard displays updated lawyer portrait and opens full portrait dialog`() {
    composeTestRule.setContent {
      MrGiangLawTheme {
        AdvisorCredentialsCard()
      }
    }

    composeTestRule.onNodeWithTag("advisor_portrait_avatar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("advisor_portrait_avatar").performClick()
    composeTestRule.onNodeWithContentDescription("Ảnh đại diện đầy đủ Luật gia Nguyễn Linh Giang").assertIsDisplayed()
  }

  @Test
  fun `ConsultationRoomScreen displays retry action when AI response errors`() {
    var retried = false
    composeTestRule.setContent {
      MrGiangLawTheme {
        ConsultationRoomScreen(
          session = ConsultationSession(
            id = "s1",
            title = "Hỏi đáp hình sự",
            category = LegalCategory.CRIMINAL.key
          ),
          messagesState = DataUiState.Success(emptyList()),
          aiState = AiAnswerState.Error("Mạng tạm gián đoạn"),
          onBack = {},
          onSendQuestion = { _, _ -> },
          onRetryQuestion = { retried = true },
          onMarkResolved = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("retry_ai_question_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("retry_ai_question_button").performClick()
    assertTrue(retried)
  }

  @Test
  fun `HomeConsultTab displays sign-out button and invokes callback`() {
    var signOutRequested = false
    composeTestRule.setContent {
      MrGiangLawTheme {
        HomeConsultTab(
          displayName = "Nguyễn Văn A",
          consultationsState = DataUiState.Success(emptyList()),
          onStartNewConsultation = { _, _ -> },
          onOpenSession = {},
          onDeleteSession = {},
          onRequestSignOut = { signOutRequested = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("home_sign_out_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_sign_out_button").performClick()
    assertTrue(signOutRequested)
  }

  @Test
  fun `AppUpdateSettingsCard triggers check and complete install actions`() {
    var checked = false
    var installed = false
    var autoToggled: Boolean? = null

    composeTestRule.setContent {
      MrGiangLawTheme {
        AppUpdateSettingsCard(
          appUpdateState = AppUpdateUiState(
            versionName = "1.0",
            versionCode = 1,
            autoUpdateEnabled = true,
            status = AppUpdateStatus.UpToDate
          ),
          onToggleAutoUpdate = { autoToggled = it },
          onCheckForUpdate = { checked = true },
          onInstallUpdate = { installed = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("app_update_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auto_update_switch").performClick()
    assertEquals(false, autoToggled)
    composeTestRule.onNodeWithTag("check_app_update_button").performClick()
    assertTrue(checked)
  }

  @Test
  fun `AppUpdateSettingsCard shows restart install button when update is downloaded`() {
    var installed = false
    composeTestRule.setContent {
      MrGiangLawTheme {
        AppUpdateSettingsCard(
          appUpdateState = AppUpdateUiState(
            status = AppUpdateStatus.DownloadedReadyToInstall
          ),
          onToggleAutoUpdate = {},
          onCheckForUpdate = {},
          onInstallUpdate = { installed = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("complete_app_update_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("complete_app_update_button").performClick()
    assertTrue(installed)
  }
}
