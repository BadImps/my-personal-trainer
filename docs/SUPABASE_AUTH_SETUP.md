# Supabase authentication setup

My Personal Trainer uses Supabase only for authentication. Personal and fitness information remains in private Android storage and is not represented in the hosted schema.

## Mobile configuration

Add these values to the developer machine's uncommitted `local.properties`:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Only the publishable/anon key belongs in the Android build. Never place a service-role key in the app or repository.

## Database

Apply `supabase/migrations/202610240001_account_registry.sql` with the Supabase CLI or SQL editor. The migration creates one identity registry row per Auth user, enables RLS, denies anonymous writes and reads, and permits authenticated users to read only their own row.

## Authentication settings

In Authentication settings:

1. Enable email/password sign-up.
2. Require email confirmation before sign-in.
3. Set minimum password length to 6 and require uppercase/lowercase letters, digits, and symbols. The app applies the same policy and explains every requirement while the member types.
4. Set email OTP length to 6 and expiry to 10 minutes.
5. Configure custom SMTP before production. Supabase's trial mailer is rate-limited and best-effort.
6. Add `com.badimps.personaltrainer://auth/verified` to allowed redirect URLs.

## Email templates

The confirmation template should retain `{{ .ConfirmationURL }}`. The recovery template must show the OTP:

```html
<h2>Reset your My Personal Trainer password</h2>
<p>Enter this six-digit code in the app:</p>
<p style="font-size:32px;font-weight:700;letter-spacing:8px">{{ .Token }}</p>
<p>This code expires in 10 minutes. If you did not request it, ignore this email.</p>
```

Supabase supports email OTP lengths from 6–10 digits. A four-digit recovery code is intentionally not used because it is unsupported and substantially weaker against guessing attacks.

Password recovery uses three separate screens: email request, six individual OTP fields, and new-password creation. The request response deliberately does not reveal whether an email is registered, which prevents account enumeration.

## Privacy boundary

Supabase receives the account email, password hash managed by Supabase Auth, authentication identifiers, and Auth audit metadata. Username, profile photo, gender/body context, measurements, workouts, endurance routes, nutrition, schedules, progress, coach choice, and dialogue preferences remain on the phone.
