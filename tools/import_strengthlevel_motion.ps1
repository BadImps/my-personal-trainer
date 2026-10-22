<#
 Imports the neutral animated WebP loops used by the exercise library.  Sources are the
 locally retained Strength Level motion source, never coach portraits or generic stick figures.
 Each key is the Android resource name and each value is the verified source loop slug.
#>
param(
    [string]$SourceRoot = 'D:\Strenght Level\strengthlevel-exercise-media\source-pose-loops',
    [string]$Destination = "$PSScriptRoot\..\app\src\main\res\drawable-nodpi"
)

$loops = [ordered]@{
    # Direct and closely-equivalent neutral movement guides for the remaining catalogue entries.
    'sl_assisted_pull_up' = 'neutral-grip-pull-ups'
    'sl_backpack_curl' = 'dumbbell-curl'
    'sl_backpack_floor_press' = 'dumbbell-floor-press'
    'sl_backpack_row' = 'dumbbell-row'
    'sl_barbell_rollout' = 'ab-wheel-rollout'
    'sl_cable_pallof_press' = 'cable-woodchopper'
    'sl_captains_chair_knee_raise' = 'hanging-knee-raise'
    'sl_chair_triceps_dip' = 'bench-dips'
    'sl_close_grip_floor_press' = 'close-grip-dumbbell-bench-press'
    'sl_counter_triceps_press' = 'incline-push-up'
    'sl_doorway_curl_hold' = 'cable-bicep-curl'
    'sl_drag_curl' = 'barbell-curl'
    'sl_dumbbell_dead_bug' = 'lying-leg-raise'
    'sl_dumbbell_march' = 'farmers-walk'
    'sl_dumbbell_step_up' = 'dumbbell-lunge'
    'sl_elliptical_trainer' = 'bodyweight-squat'
    'sl_household_overhead_triceps_extension' = 'dumbbell-tricep-extension'
    'sl_isometric_towel_curl' = 'cable-bicep-curl'
    'sl_landmine_anti_rotation_hold' = 'landmine-press'
    'sl_landmine_rotation' = 'russian-twist'
    'sl_ninety_ninety_hip_switch' = 'bodyweight-squat'
    'sl_prone_snow_angel' = 'dumbbell-reverse-fly'
    'sl_prone_y_raise' = 'dumbbell-incline-y-raise'
    'sl_rotary_torso_machine' = 'russian-twist'
    'sl_rowing_machine' = 'seated-cable-row'
    'sl_seated_cable_press' = 'chest-press'
    'sl_shoulder_wall_slide' = 'cable-lateral-raise'
    'sl_side_plank' = 'plank'
    'sl_squat_to_press' = 'dumbbell-thruster'
    'sl_stationary_bike' = 'bodyweight-squat'
    'sl_step_up' = 'dumbbell-lunge'
    'sl_suitcase_carry' = 'farmers-walk'
    'sl_table_row_hold' = 'inverted-row'
    'sl_towel_pull_apart' = 'cable-reverse-fly'
    'sl_treadmill_incline_walk' = 'farmers-walk'
    'sl_triceps_push_up_hold' = 'diamond-push-ups'
    'sl_water_bottle_hammer_curl' = 'hammer-curl'
    'sl_water_bottle_lateral_raise' = 'dumbbell-lateral-raise'
    'sl_water_bottle_overhead_extension' = 'dumbbell-tricep-extension'
    'sl_wall_sit' = 'bodyweight-squat'
    'sl_worlds_greatest_stretch' = 'lunge'
    'sl_band_pull_apart' = 'cable-reverse-fly'
    'sl_bird_dog' = 'plank'
    'sl_brisk_march' = 'farmers-walk'
    'sl_cat_cow' = 'back-extension'
    'sl_dead_bug' = 'lying-leg-raise'
}

if (!(Test-Path -LiteralPath $SourceRoot)) { throw "Strength Level loop source was not found: $SourceRoot" }
New-Item -ItemType Directory -Force -Path $Destination | Out-Null

foreach ($entry in $loops.GetEnumerator()) {
    $source = Join-Path $SourceRoot ($entry.Value + '.webp')
    if (!(Test-Path -LiteralPath $source)) { throw "Missing verified source loop: $source" }
    Copy-Item -LiteralPath $source -Destination (Join-Path $Destination ($entry.Key + '.webp')) -Force
}

# Plank Shoulder Tap needs its own cue: the source collection only has a generic plank,
# which incorrectly reads as a hip motion. Build a short neutral loop from the approved
# Strength Level shoulder-tap plate, mirrored between contacts to show opposite hand →
# opposite shoulder while the torso remains stable.
$shoulderTapPoster = Join-Path $PSScriptRoot '..\app\src\main\res\drawable\sl_plank_shoulder_tap.png'
$shoulderTapOutput = Join-Path $Destination 'sl_plank_shoulder_tap.webp'
$ffmpeg = (Get-Command ffmpeg -ErrorAction Stop).Source
if (!(Test-Path -LiteralPath $shoulderTapPoster)) { throw "Missing Shoulder Tap plate: $shoulderTapPoster" }
& $ffmpeg -y -loop 1 -t 1.25 -i $shoulderTapPoster -loop 1 -t 1.25 -i $shoulderTapPoster `
    -filter_complex '[1:v]hflip[mirror];[0:v]scale=800:800,setsar=1[first];[mirror]scale=800:800,setsar=1[second];[first][second]concat=n=2:v=1:a=0,fps=12' `
    -c:v libwebp_anim -q:v 82 -loop 0 $shoulderTapOutput
if ($LASTEXITCODE -ne 0) { throw "Could not create corrected Plank Shoulder Tap loop." }

Write-Host "Imported $($loops.Count) neutral Strength Level WebP loops and the corrected Plank Shoulder Tap loop."
