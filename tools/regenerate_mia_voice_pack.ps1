$ErrorActionPreference = 'Stop'

$voiceStudio = 'http://127.0.0.1:3900/generate'
$voiceStudioOutput = 'D:\Voice Studio\VoiceStudio\data\outputs'
$ffmpeg = 'D:\Voice Studio\VoiceStudio\data\media_tools\shims\ffmpeg.exe'
$curl = 'C:\Windows\System32\curl.exe'
$targetDir = Join-Path $PSScriptRoot '..\app\src\main\assets\coach_audio\mia'

# Every cue shares one profile, seed, and rate, so the coach remains recognisably Mia
# throughout a whole guided workout rather than changing pace or colour between cues.
$cueText = [ordered]@{
    welcome = 'Hola, I am Coach Mia. Strong energy, beautiful control. Let us make this workout count.'
    new_exercise = 'New move, champ. Set your position, own your form, and let us go.'
    set_opening_1 = 'Ready, strong one. Breathe in, brace strong, and let us begin.'
    set_opening_2 = 'Vamos, we start now. Strong and steady.'
    set_opening_3 = 'Vamos, move with confidence. You have this.'
    set_opening_4 = 'Get ready, champ. You are stronger than you think.'
    go = 'Vamos! Let us go, strong one.'
    set_complete_1 = 'Beautiful work, champ. You earned this rest. Breathe deep, then we go again.'
    set_complete_2 = 'Strong set. Take your recovery, stay focused, and come back ready.'
    set_complete_3 = 'That set is done. Excellent focus. Recover with purpose, then we rise again.'
    exercise_complete_next = 'Excellent work on that exercise. Reset in your cooldown, then we own the next one.'
    exercise_complete_final = 'You finished that exercise. That is strong work. Be proud.'
    one_minute = 'One minute left. Breathe deep. You are nearly ready.'
    thirty_seconds = 'Thirty seconds left. Stay loose, stay ready, you have this.'
    twenty_seconds = 'Twenty seconds. Shake it out, reset your focus, and get ready.'
    ten_seconds = 'Ten seconds. Eyes up, strong posture, vamos.'
    recovery_guidance = 'Recover with purpose. Breathe deeply, stay loose, and come back ready, champ.'
    count_5 = 'Five.'
    count_4 = 'Four.'
    count_3 = 'Three.'
    count_2 = 'Two.'
    count_1 = 'One.'
}

foreach ($cue in $cueText.GetEnumerator()) {
    $form = @{
        text = $cue.Value
        language = 'English'
        profile_id = 'e0e6fad1'
        instruct = 'female, young adult, moderate pitch'
        speed = '0.98'
        seed = '42'
        engine = 'omnivoice'
        effect_preset = 'broadcast'
        wav_bits = '16'
    }
    # VoiceStudio returns raw WAV bytes. Let curl discard those bytes; its local output
    # directory is the stable hand-off point for this single-threaded render sequence.
    & $curl -sS -X POST $voiceStudio -F ("text=" + $cue.Value) -F 'language=English' `
        -F 'profile_id=e0e6fad1' -F 'instruct=female, young adult, moderate pitch' `
        -F 'speed=0.98' -F 'seed=42' -F 'engine=omnivoice' -F 'effect_preset=broadcast' `
        -F 'wav_bits=16' -o NUL
    if ($LASTEXITCODE -ne 0) { throw "VoiceStudio could not render $($cue.Key)." }
    $source = Get-ChildItem $voiceStudioOutput -Filter '*.wav' -File |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
    if ([string]::IsNullOrWhiteSpace($source)) { throw "VoiceStudio did not record an audio file for $($cue.Key)." }
    $destination = Join-Path $targetDir ($cue.Key + '.ogg')
    & $ffmpeg -hide_banner -loglevel error -y -i $source -c:a libvorbis -q:a 4 $destination
    if ($LASTEXITCODE -ne 0) { throw "Could not encode $($cue.Key)." }
    Write-Output "Regenerated $($cue.Key)"
}
