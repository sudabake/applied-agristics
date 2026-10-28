# Third-party notices

Gradle Wrapper scripts and binary (gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.jar) originate from Gradle via the official NeoForge MDK, retaining Apache License 2.0 notices. A license copy is included in third_party/gradle-wrapper-LICENSE.txt.

Minecraft, NeoForge, AE2, Mystical Agriculture, Cucumber and GuideME are separate downloaded dependencies. Their JARs are not bundled in this addon. The ME infusion altar and biological altar retain Mystical Agriculture 8.0.28's original model geometry, copyright (c) 2018 BlakeBr0, MIT, with original project pixel textures since Alpha.18. The retained license is bundled as licenses/mysticalagriculture/LICENSE.txt. Other machine and biological item art is original project artwork; editable sources are under art/drafts.

The ME infusion altar calls Mystical Agriculture's native InfusionAltarRenderer at runtime. Its particle emission behavior in InfusionEffects.java is adapted from Mystical Agriculture, copyright (c) 2018 BlakeBr0, MIT License. The license is preserved in third_party/mysticalagriculture/LICENSE.txt and bundled as licenses/mysticalagriculture/LICENSE.txt. AE2's native FluidBlitter is referenced for water display; no AE2 or AdvancedAE texture is copied.

Botany Pots and Bookshelf are optional integration API dependencies; they are not bundled or required for normal operation. The integration follows their public crop/soil/harvest APIs, informed by the locally pinned JDTE greenhouse integration. Minecraft's tinted cross model is referenced for the original biological plant sprite; no Minecraft texture is redistributed.

The vault priority page adapts AE2's priority.json layout, copyright TeamAppliedEnergistics, under LGPL-3.0-or-later. It calls AE2's native NumberEntryWidget and references the installed AE2 UI textures at runtime; those textures are not redistributed. User-edited item PNGs are retained under art/approved.

Historical Alpha.17 essence textures and tint layers used MIT-licensed artwork from https://github.com/rulanup/JDTE, copyright (c) 2026 JDTE contributors. Alpha.18 replaces all of those runtime images with original project artwork from art/drafts/alpha18_original_v01. The historical source revision and MIT notice remain under third_party/jdte, with a notice retained in licenses/jdte/LICENSE.txt; the old JDTE PNGs are not bundled in Alpha.18.
