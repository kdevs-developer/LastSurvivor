# Release build rules (Phase 10).
#
# The app needs no special keep rules: it uses no reflection, no JSON/serialisation libraries and
# no code loaded by name. Enum names are saved in DataStore (WeaponType, UpgradeTrack, CharacterSkin,
# GoreLevel); R8 keeps enum name() values, and the defaults in proguard-android-optimize.txt keep
# values()/valueOf(), so saved data stays readable after shrinking.

# Readable crash traces from release builds (upload mapping.txt to Play Console).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
