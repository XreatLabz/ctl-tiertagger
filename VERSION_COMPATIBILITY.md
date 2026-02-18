# Version Compatibility Guide

## Supported Minecraft Versions

The mod currently supports **Minecraft 1.21 through 1.21.11** (inclusive):
- ✅ Minecraft 1.21
- ✅ Minecraft 1.21.1
- ✅ Minecraft 1.21.2
- ✅ Minecraft 1.21.3
- ✅ Minecraft 1.21.4
- ✅ Minecraft 1.21.5
- ✅ Minecraft 1.21.6
- ✅ Minecraft 1.21.7
- ✅ Minecraft 1.21.8
- ✅ Minecraft 1.21.9
- ✅ Minecraft 1.21.10
- ✅ Minecraft 1.21.11

## Single Source of Truth

Support boundaries are intentionally aligned across all compatibility gates:
- Runtime gate: `VersionSupport.MIN_SUPPORTED` / `VersionSupport.MAX_SUPPORTED`
- Metadata gate: `fabric.mod.json` → `"minecraft": ">=1.21 <=1.21.11"`
- Documentation: this file and versioned docs

If you change support bounds, update all three together.

## Runtime Compatibility Gate (Fail-fast)

On client init, the mod validates the running Minecraft version with:
- `VersionSupport.requireSupportedOrThrow()`

Behavior:
- Supported version: startup continues
- Unsupported version: clear error is logged and initialization aborts

## Compatibility Matrix (Pinned Scope)

This matrix tracks upstream API drift we care about and whether this mod currently touches it.

| Upstream change | Version boundary | Used in this mod now? | Bridge / seam action | Impacted files |
| --- | --- | --- | --- | --- |
| `Entity#getWorld` → `Entity#getEntityWorld` rename | 1.21.9+ mappings | **No** direct usage in current code paths | None required right now | N/A |
| Player label render signature churn (`AbstractClientPlayerEntity` → `PlayerEntityRenderState`) | 1.21.4+ family (still relevant through 1.21.9/1.21.10/1.21.11) | **Yes** (`EntityRendererMixin`) | Keep mixin seam version-safe (legacy + state signatures) | `src/client/java/com/ctltierlist/tiertagger/client/mixin/EntityRendererMixin.java` |
| Render API churn (world rendering event suite removed / redesigned) | 1.21.9/1.21.10 | **No** (mod does not use world render events) | None; continue using existing nametag mixin path | N/A |
| HUD API deprecation path (`HudRenderCallback` deprecated) | 1.21.5+ onward | **No** (mod does not use HUD callback APIs) | None required | N/A |
| Identifier creation rules (`new Identifier(...)` discouraged/invalid) | Modern 1.21.x mappings/docs | **Yes** (`Identifier.of(...)` already used) | Keep current `Identifier.of(...)` usage | `src/client/java/com/ctltierlist/tiertagger/client/util/SkinLoader.java`, `src/client/java/com/ctltierlist/tiertagger/version/compat/ClientCompatBridge121.java` |
| Keybinding category API moved to `KeyBinding.Category` | 1.21.9+ / 1.21.11 runtime target in this project | **Yes** | Keep existing reflection bridge split by detected version | `src/client/java/com/ctltierlist/tiertagger/version/compat/ClientCompatBridge121.java` |
| `PlayerSkinWidget` constructor/model loader type drift | 1.21.x internal type naming churn | **Yes** (search screen skin widget path) | Keep creation centralized in client bridge | `src/client/java/com/ctltierlist/tiertagger/version/compat/ClientCompatBridge121.java`, `src/client/java/com/ctltierlist/tiertagger/client/util/SkinLoader.java` |

## Compatibility Architecture (Current)

The project’s compatibility seam remains:
- `ClientCompatBridge`
- `ClientCompatBridge121`
- `CompatBridgeFactory`

Feature logic should keep calling the seam; avoid scattered version checks.

## Build Baseline and Intent

- Build baseline remains 1.21.1 Yarn/Fabric coordinates in `gradle.properties`
- Runtime support range is still explicitly bounded to 1.21–1.21.11
- Patch support is validated through bridge/mixin seams plus smoke tests

## Verification Checklist

Minimum verification for compatibility updates:
1. Search for unstable direct usages:
   - `getWorld(`
   - deprecated HUD callbacks
   - `new Identifier(`
2. Project diagnostics and build:
   - LSP/project diagnostics
   - `./gradlew clean build`
3. Runtime smoke versions:
   - 1.21 (lower bound)
   - 1.21.1 (baseline)
   - 1.21.9 or 1.21.10 (API churn boundary)
   - 1.21.11 (upper bound)

## Future Version Policy

For 1.21.x updates beyond 1.21.11 or 1.22+:
1. Rebuild this matrix first (docs gate)
2. Patch only proven drift points in bridge/mixin seams
3. Align bounds in runtime + metadata + docs together
4. Re-run full verification checklist
