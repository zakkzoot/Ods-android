# Gmail setup (one-time)

The dashboard reads **unread inbox counts** for `info@outlined-design.com` and
`zakkgray1@gmail.com` over the Gmail API using OAuth. Google requires an OAuth client
that only you can create — there is no way around this. It takes ~5 minutes.

## 1. Create the OAuth client
1. Go to <https://console.cloud.google.com/> and create (or pick) a project.
2. **APIs & Services → Library →** enable **Gmail API**.
3. **APIs & Services → OAuth consent screen:**
   - User type: **External**.
   - Add the scope `.../auth/gmail.readonly`.
   - Under **Test users**, add both addresses (`info@outlined-design.com`,
     `zakkgray1@gmail.com`). In Testing mode only test users can authorize, which is
     fine for a personal app.
4. **APIs & Services → Credentials → Create credentials → OAuth client ID:**
   - Application type: **Android**.
   - Package name: `com.ods.dashboard`.
   - **SHA-1**: from the keystore you build/sign with. For a debug build:
     ```bash
     cd dashboard && ./gradlew signingReport
     # copy the SHA1 under "Variant: debug"
     ```
     (For a release build, use your release keystore's SHA-1 — add both if you use both.)
5. Copy the generated **client ID** (looks like
   `1234567890-abcd…apps.googleusercontent.com`).

## 2. Enter it in the app
1. Open the dashboard → gear (top-right) → **EMAIL (GMAIL)**.
2. Paste the **Google OAuth client ID**.
3. Tap **Connect** next to each address and complete the Google sign-in/consent.
4. Done — the two email tiles now show unread inbox counts and refresh in the
   background every ~15 minutes.

## Notes
- The redirect scheme (`com.ods.dashboard:/oauth2redirect`) is already wired via the
  `appAuthRedirectScheme` manifest placeholder, matching the Android client.
- Tokens (incl. refresh tokens) are stored encrypted on-device per account; nothing is
  committed or sent anywhere except Google.
- If a tile shows "reauth needed", tap **Disconnect** then **Connect** again.
- Scope is read-only (`gmail.readonly`) — the app can only count/read, never send.
