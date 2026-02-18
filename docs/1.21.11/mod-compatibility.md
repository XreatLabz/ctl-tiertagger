# Minecraft 1.21.11 Mod Compatibility Notes

Scope: validated compatibility behavior for CTL TierTagger on Minecraft 1.21.11.

## 1) Declared support boundary
- Project support range is `>=1.21.0 <=1.21.11`.
- Runtime gate is enforced by `VersionSupport.requireSupportedOrThrow()`.
- Metadata gate is enforced by `fabric.mod.json`.

## 2) Upstream changes relevant at 1.21.11

### Keybinding category API shift
- Status in this mod: **used**.
- Action: maintain bridge handling for category object creation (`KeyBinding.Category`) while preserving legacy constructor path for older 1.21.x.
- Impacted path: `src/client/java/com/ctltierlist/tiertagger/version/compat/ClientCompatBridge121.java`.

### Player label render signature churn
- Status in this mod: **used** via `EntityRendererMixin` label injection.
- Action: maintain compatibility at mixin seam for both legacy and render-state signatures.
- Impacted path: `src/client/java/com/ctltierlist/tiertagger/client/mixin/EntityRendererMixin.java`.

### `PlayerSkinWidget` constructor/model loader drift
- Status in this mod: **used** in search profile UI.
- Action: keep construction centralized in compat bridge.
- Impacted paths:
  - `src/client/java/com/ctltierlist/tiertagger/version/compat/ClientCompatBridge121.java`
  - `src/client/java/com/ctltierlist/tiertagger/client/util/SkinLoader.java`

### Identifier usage rules
- Status in this mod: uses `Identifier.of(...)` (already aligned).
- Action: keep avoiding direct `new Identifier(...)`.

## 3) Compatibility seam policy
- Keep version-sensitive calls centralized in:
  - `ClientCompatBridge`
  - `ClientCompatBridge121`
  - `CompatBridgeFactory`
- Avoid scattering version checks in feature logic.

## 4) 1.21.11 regression checklist
- Client startup passes runtime version gate.
- Keybind registration works (`G`/`Y`) with the new category path.
- Nametag tier rendering works (mixin target resolves, no signature misses).
- Search screen and skin widget path works.
- Logs contain no mixin target or descriptor errors.
