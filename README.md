# Taska — a smart task manager for focus and productivity

Taska is an Android task management app with a modern design, real-time synchronization, and a focus on focusing throughout the day. The project is built in Kotlin using Firebase and Material Design 3.

Currently, three key modules have been implemented:
Authorization, User Profile, and Focus (Day Planner).

---

## 📸 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="screenshots/profile_fragment_screenshots.jpg" width="240"/>
        <br/>
        <sub>User profile - data, status and settings</sub>
      </td>
      <td align="center">
        <img src="screenshots/focus_fragment_screenshots.jpg" width="240"/>
        <br/>
        <sub>Daily planner with timeline and priorities</sub>
      </td>
      <td align="center">
        <img src="screenshots/NewSpaceCreateFragment.png" width="240"/>
        <br/>
        <sub>Fragment of creating a new space</sub>
      </td>
      <td align="center">
        <img src="screenshots/clear_focus_fragment_screenshots.jpg" width="240"/>
        <br/>
        <sub>Minimalist concentration mode</sub>
      </td>
    </tr>
  </table>
</div>

---

## 🧭 User Flow

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="design/user_flow/user_flow.png" width="720"/>
        <br/>
        <sub>User flow diagram — the full path of user actions inside the app</sub>
      </td>
    </tr>
  </table>
</div>

🔄 Data Flow Diagram

This diagram describes how data flows between the user, application
screens, local storage, and Firebase services.
```mermaid    
    flowchart LR
        U[User]
        SYS[AndroidSystem]
        CAM[CameraQR]
        EXT[ExternalServices]

        FS[(Firestore)]
        AUTH[(FirebaseAuth)]
        SP[(SharedPrefs)]
        LC[(LocalCache)]

        SA[StartActivity]
        LA[LoginActivity]
        MA[MainActivity]
        PF[ProfileFragment]
        FF[FocusFragment]
        SF[SpaceFragment]

        SYS -->|InsetsTheme| SA
        U -->|BackStart| SA
        SA -->|UIAnimations| U
        SA -->|Navigate| LA
        SA -->|FinishApp| SYS

        U -->|GoogleSignIn| LA
        LA -->|OAuthIntent| AUTH
        AUTH -->|TokenAccount| LA
        LA -->|CreateUpdateUser| FS
        LA -->|SaveUserId| SP
        LA -->|Navigate| MA
        LA -->|ErrorsLoading| U

        AUTH -->|CurrentUser| PF
        FS -->|UserPremiumSettings| PF
        SP -->|LocalSettings| PF
        U -->|ProfileActions| PF
        PF -->|UpdateSettings| FS
        PF -->|SaveSettings| SP
        PF -->|ThemeLanguage| SYS
        PF -->|Navigate| MA
        PF -->|UIUpdates| U

        FS -->|TasksRealtime| FF
        SYS -->|CurrentTime| FF
        U -->|SelectEditAdd| FF
        FF -->|CreateUpdateTask| FS
        FF -->|DeleteTask| FS
        FF -->|TimelineDialogs| U

        FS -->|SpacesMembersTasksInvites| SF
        AUTH -->|CurrentUser| SF
        LC -->|CachedSpaces| SF
        CAM -->|QRData| SF
        EXT -->|AttachmentsDeadlines| SF

        U -->|CreateSpace| SF
        U -->|OpenSpace| SF
        U -->|EditTreeRolesComments| SF
        U -->|UploadFiles| SF

        SF -->|CreateUpdateSpace| FS
        SF -->|UpdateMembersRoles| FS
        SF -->|CreateMoveDeleteTasks| FS
        SF -->|CreateInvites| FS
        SF -->|RealtimeChanges| U

        SF -->|CacheSpaces| LC
        SF -->|SyncOffline| FS
        SF -->|ConflictsNotifications| U
        SF -->|TreeProgressBoard| U
```

## 🚀 Current Functionality

### 🔐 Authorization
- Login via Google (Firebase Auth)
- Create and store user profile in Firestore
- Manage user sessions
- Automatic data synchronization

### 👤 User Profile (`ProfileFragment`)
- Display user data:
  - Avatar, name, email
  - UID
  - Status (Free / Premium)
- Settings:
  - App language (RU / EN)
  - Light / dark theme
  - Notifications
  - Additional settings (sound, vibration, date format)
- Quick actions:
  - Edit profile
  - Go to statistics (under development)
  - Security settings
- Premium block:
  - Subscription status display
  - Premium expiration date
  - Animation for Premium users

**Technical:**
- Real-time data updates via Firestore listeners
- Local saving settings for offline work
- Material Design 3
- Smooth element animations

### 🎯 Focus / Day Planner (`FocusFragment`)
- Day timeline by hour
- Current time display with animation
- Create tasks with time
- Edit and delete tasks
- Task priorities:
  - Low
  - Medium
  - High
- Color-coded priorities
- Day navigation:
  - Infinite scrolling
  - Current day highlight
  - Animated transitions

**Visual features:**
- Priority gradients
- Smooth color transitions
- Current time pulsing animation
- Adaptive task cards

---

## 🛠 Tech stack

- **Language:** Kotlin
- **Architecture:** MVVM
- **Database:** Firebase Firestore
- **Authentication:** Firebase Auth
- **UI:** XML + Material Components
- **Async:** Kotlin Coroutines
- **Images:** Glide

---

## 📁 Data Structure (Firestore)

- `users` — user data
- `tasks` — user tasks
- `user_settings` — settings
- `premium_subscriptions` — subscription information

---

## 📌 Upcoming Plans

- 🔔 Notifications and Reminders
- 📊 Statistics and Analytics
- 🧩 Spaces for Teams
- 📴 Full Offline Mode
- 🧪 Unit and Integration Tests

---

## 🧠 Project Idea

Taska combines:
- Personal planning
- Time management through a visual timeline
- In the future, teamwork and a Git-like task system

The project's goal is to create a productivity ecosystem: from personal tasks to team projects.

---

## 📄 License

The project is distributed under the **Apache License 2.0**.

---

**Taska** is more than just a to-do list. It's a tool for mindful time management.
