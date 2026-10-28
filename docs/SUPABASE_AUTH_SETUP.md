# Supabase authentication setup

My Personal Trainer uses Supabase for authentication and the optional Gym Lobby. Personal workout,
route, nutrition, schedule, body, and health information remains in private Android storage. The
Gym Lobby stores only a member's display name, level, Momentum Points, Apex lane, earned badge
names, buddy requests, invitations, lobby membership, and leaderboard ordering.

## Mobile configuration

Add these values to the developer machine's uncommitted `local.properties`:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Only the publishable/anon key belongs in the Android build. Never place a service-role key in the app or repository.

## Database

Apply `supabase/migrations/202610240001_account_registry.sql` and then
`supabase/migrations/202610280001_gym_lobby.sql` with the Supabase CLI or SQL editor. The Gym Lobby
migration enables RLS, denies direct anonymous/authenticated table access, and exposes only narrow
authenticated security-definer functions for publishing the public summary and managing requests.

## Authentication settings

In Authentication settings:

1. Enable email/password sign-up.
2. Turn **Confirm email** off. Registration signs the member into the app immediately; Supabase
   still stores the email and password hash securely in Auth, without sending a verification email.
3. Set minimum password length to 10 and require uppercase/lowercase letters, digits, and symbols. The app applies the same policy and explains every requirement while the member types.
4. Add `com.badimps.personaltrainer://auth/verified` to allowed redirect URLs only if email confirmation is enabled again later.

## Password recovery email template

Password recovery still sends an email. Its template must show the OTP:

```html
<h2>Reset your My Personal Trainer password</h2>
<p>Enter this six-digit code in the app:</p>
<p style="font-size:32px;font-weight:700;letter-spacing:8px">{{ .Token }}</p>
<p>This code expires in 10 minutes. If you did not request it, ignore this email.</p>
```

Supabase supports email OTP lengths from 6–10 digits. A four-digit recovery code is intentionally not used because it is unsupported and substantially weaker against guessing attacks. Configure custom SMTP before releasing password recovery broadly; Supabase's trial mailer is rate-limited and best-effort.

Password recovery uses three separate screens: email request, six individual OTP fields, and new-password creation. The request response deliberately does not reveal whether an email is registered, which prevents account enumeration.

The app adds device-side throttling to Supabase's server limits: recovery email resends wait at least 60 seconds, and five failed login or recovery-code attempts pause that action for 15 minutes. After a successful password reset, the temporary recovery session is revoked and replaced with a fresh password-authenticated session.

Password reset, session revocation, and sign-out operate only on authentication credentials. They never erase local workouts, endurance routes, nutrition, schedules, body information, progress, achievements, or coach preferences.

## Privacy boundary

Supabase receives the account email, password hash managed by Supabase Auth, authentication
identifiers, Auth audit metadata, and the optional Gym Lobby fields listed above. Profile photo,
gender/body context, measurements, workouts, endurance routes, nutrition, schedules, detailed
progress history, coach choice, and dialogue preferences remain on the phone.
