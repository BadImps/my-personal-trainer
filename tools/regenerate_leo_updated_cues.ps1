$ErrorActionPreference = 'Stop'
$generate = 'http://127.0.0.1:3900/generate'
$outputs = 'D:\Voice Studio\VoiceStudio\data\outputs'
$curl = 'C:\Windows\System32\curl.exe'
$ffmpeg = 'D:\Voice Studio\VoiceStudio\data\media_tools\shims\ffmpeg.exe'
$target = Join-Path $PSScriptRoot '..\app\src\main\assets\coach_audio\leo'
$cues = [ordered]@{
    new_exercise = 'New exercise, bro. Lock in your position and move with real intent.'
    set_opening_4 = 'Get set, champ. You are ready to build.'
}
foreach ($cue in $cues.GetEnumerator()) {
    & $curl -sS -X POST $generate -F ("text=" + $cue.Value) -F 'language=English' `
        -F 'profile_id=6f3a0413' -F 'instruct=male, young adult, low pitch' `
        -F 'speed=0.98' -F 'seed=108' -F 'engine=omnivoice' -F 'effect_preset=broadcast' `
        -F 'wav_bits=16' -o NUL
    if ($LASTEXITCODE -ne 0) { throw "VoiceStudio could not render $($cue.Key)." }
    $source = Get-ChildItem $outputs -Filter '*.wav' -File |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
    & $ffmpeg -hide_banner -loglevel error -y -i $source -c:a libvorbis -q:a 4 (Join-Path $target ($cue.Key + '.ogg'))
    if ($LASTEXITCODE -ne 0) { throw "Could not encode $($cue.Key)." }
    Write-Output "Regenerated $($cue.Key)"
}
