# Available vs Connected Sources — Quick Comparison

## Side-by-Side Comparison

| Aspect | Available Sources | Connected Sources |
|--------|------------------|-------------------|
| **Endpoint** | `GET /sources/available` | `GET /sources` |
| **Purpose** | List all possible sources app supports | List user's personal connections |
| **Who sees it** | Same list for every user | Different for each user |
| **What includes** | Name, icon, description, required scopes | Account name, item count, last sync, token status |
| **Backend source** | Config or static database | User-specific database |
| **Updated frequency** | Rarely (new sources added to app) | Often (on user actions) |
| **Example** | "Gmail, Slack, Drive, Notion exist" | "User has Gmail (127 items) + Slack (89 items)" |

---

## Visual Comparison

```
AVAILABLE SOURCES (Global)
┌─────────────────────────────────────────┐
│ Same for EVERY user                     │
├─────────────────────────────────────────┤
│ Gmail                                   │
│ - Icon: gmail.png                       │
│ - Description: "Read emails"            │
│ - Scopes: [emails, attachments]         │
│                                         │
│ Slack                                   │
│ - Icon: slack.png                       │
│ - Description: "Sync Slack messages"    │
│ - Scopes: [messages, files]             │
│                                         │
│ Google Drive                            │
│ - Icon: drive.png                       │
│ - Description: "Index documents"        │
│ - Scopes: [files, folders]              │
│                                         │
│ Notion                                  │
│ - Icon: notion.png                      │
│ - Description: "Sync workspace"         │
│ - Scopes: [databases, pages]            │
│                                         │
│ PDF Upload                              │
│ - Icon: pdf.png                         │
│ - Description: "Upload PDFs"            │
│ - Scopes: []                            │
└─────────────────────────────────────────┘
         ↓
    Used to populate list
         ↓


CONNECTED SOURCES (User-Specific)
┌──────────────────────────────────────────────┐
│ DIFFERENT for each user                      │
├──────────────────────────────────────────────┤
│ User John's Connected Sources:               │
│                                              │
│ Gmail                                        │
│ - Account: john@gmail.com                    │
│ - Items: 127                                 │
│ - Last sync: 2024-05-07T14:30:00Z            │
│ - Status: Connected ✓                        │
│                                              │
│ Slack                                        │
│ - Account: Acme Corp Workspace               │
│ - Items: 89                                  │
│ - Last sync: 2024-05-07T14:15:00Z            │
│ - Status: Connected ✓                        │
│                                              │
│ Google Drive                                 │
│ - Account: john@gmail.com                    │
│ - Items: 0                                   │
│ - Last sync: never                           │
│ - Status: Not Connected ✗                    │
│                                              │
│ Notion                                       │
│ - Account: (not connected)                   │
│ - Items: 0                                   │
│ - Last sync: never                           │
│ - Status: Not Connected ✗                    │
│                                              │
│ PDF Upload                                   │
│ - Account: Uploaded Documents                │
│ - Items: 3                                   │
│ - Last sync: 2024-05-06T10:00:00Z            │
│ - Status: Connected ✓                        │
└──────────────────────────────────────────────┘
         ↓
    Shows in Sources tab
         ↓


FRONTEND MERGES BOTH:
┌──────────────────────────────────────────────┐
│ SourcesScreen                                │
├──────────────────────────────────────────────┤
│ [Gmail icon]                                 │
│ Gmail                                        │
│ john@gmail.com · 127 items · synced now      │
│ [Disconnect]  ← Button based on isConnected  │
│                                              │
│ [Slack icon]                                 │
│ Slack                                        │
│ Acme Corp · 89 items · synced 2 hours ago    │
│ [Disconnect]                                 │
│                                              │
│ [Drive icon]                                 │
│ Google Drive                                 │
│ john@gmail.com · 0 items                     │
│ [Connect]  ← Button because NOT connected    │
│                                              │
│ [Notion icon]                                │
│ Notion                                       │
│ Connect your Notion workspace                │
│ [Connect]                                    │
│                                              │
│ [PDF icon]                                   │
│ PDF Upload                                   │
│ Uploaded Documents · 3 items                 │
│ [Upload More]  ← Custom button for PDF       │
└──────────────────────────────────────────────┘
```

---

## API Response Examples

### GET /sources/available
```json
[
  {
    "id": "gmail",
    "name": "Gmail",
    "displayName": "Gmail",
    "icon": "https://example.com/icons/gmail.png",
    "description": "Read your emails and attachments",
    "requiresAuth": true
  },
  {
    "id": "slack",
    "name": "Slack",
    "displayName": "Slack",
    "icon": "https://example.com/icons/slack.png",
    "description": "Sync messages and files from Slack",
    "requiresAuth": true
  }
]
```

### GET /sources
```json
[
  {
    "id": "gmail_user123",
    "type": "GMAIL",
    "sourceId": "gmail",              ← Maps back to available
    "accountName": "john@gmail.com",
    "isConnected": true,
    "itemCount": 127,
    "lastSyncedAt": "2024-05-07T14:30:00Z"
  },
  {
    "id": "slack_user123",
    "type": "SLACK",
    "sourceId": "slack",
    "accountName": "Acme Corp",
    "isConnected": true,
    "itemCount": 89,
    "lastSyncedAt": "2024-05-07T14:15:00Z"
  },
  {
    "id": "drive_user123",
    "type": "GOOGLE_DRIVE",
    "sourceId": "google_drive",
    "accountName": "john@gmail.com",
    "isConnected": false,             ← Not connected yet!
    "itemCount": 0,
    "lastSyncedAt": null
  }
]
```

---

## Key Insight

**The backend returns BOTH lists:**

1. **Available Sources** = "Here are all the sources you can connect to"
2. **Connected Sources** = "Here are the ones you've already connected"

**Frontend combines them:**
- Takes the available sources as the main list
- Looks up each one in connected sources
- If found and `isConnected=true` → show "Disconnect"
- If not found or `isConnected=false` → show "Connect"
- Shows item counts and sync times only if connected

**Why both?**
- Users should see what CAN be connected (available)
- Users should see what IS connected and its status
- Available sources rarely change (app feature management)
- Connected sources change frequently (user actions)

---

## Data Flow

```
User opens app
    ↓
SourcesViewModel requests both:
    ├─ availableSourcesRepo.getAvailableSources()  → GET /sources/available
    └─ connectedSourcesRepo.getConnectedSources()  → GET /sources
    ↓
Backend returns:
    ├─ [Gmail, Slack, Drive, Notion, ...]
    └─ [Gmail (connected), Slack (connected), Drive (not connected), ...]
    ↓
ViewModel merges them:
    For each available source:
      - Find it in connected list
      - If found → show account name, items, sync time, "Disconnect" button
      - If not found → show "Connect" button

    Result: SourceRowData list
    [
      Gmail (connected, 127 items, john@gmail.com),
      Slack (connected, 89 items, Acme Corp),
      Drive (not connected),
      Notion (not connected),
      PDF (connected, 3 items)
    ]
    ↓
UI renders merged list with proper buttons
```

---

## Summary

**Available Sources** = What's possible  
**Connected Sources** = What the user linked  
**Frontend shows** = Merged view with actions  

Both queries are needed!

