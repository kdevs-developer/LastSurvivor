Optional sprite overrides (Phase 9)
===================================
Every sprite is drawn by code (game/render/PlaceholderSprites.kt) unless a PNG with the matching
name is in this folder. Drop in real art one file at a time; missing files keep the generated art.

All sprites face RIGHT (+X) and are drawn centred on the entity, then rotated. Use transparent PNGs.

floor.png                         seamless floor tile, any size (256 recommended)
player_<weapon>_<frame>.png       weapon = pistol, smg, shotgun, rifle; frame 0 = standing, 1-2 = walk steps; 96x96
zombie_<type>_<frame>.png         type = walker, runner, tank, toxic, brute, mother
                                  frame 0-3 = walk cycle, 4 = attack; square, size = radius * 3.2
                                  (walker 83, runner 76, tank 128, toxic 89, brute 224, mother 198)
decal_blood_0..3.png              floor blood splatters, 96x96
decal_goo_0..1.png                toxic goo splatters, 96x96
decal_scorch.png                  explosion scorch mark, 96x96
decor_0..3.png                    floor decoration: crack, oil stain, rubble, road marking; 128x128

The weapon barrel tip should sit about WeaponType.muzzleOffset pixels in front of the centre
(pistol 36, SMG 42, shotgun 52, rifle 52 at 96 px) so the muzzle flash lines up.
