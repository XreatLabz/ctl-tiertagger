# Minecraft 1.21.10 Mod Compatibility Notes

Scope: validated compatibility behavior for CTL TierTagger on Minecraft 1.21.10.

## 1) Declared support boundary
- Project support range is `>=1.21 <=1.21.11`.
- Runtime gate is enforced by `VersionSupport.requireSupportedOrThrow()`.
- Metadata gate is enforced by `fabric.mod.json`.

## 2) Upstream changes relevant at 1.21.10

### Mapping rename: `Entity#getWorld` → `Entity#getEntityWorld`
- Status in this mod: **not directly used** in current code paths.
- Action: no patch needed unless future code introduces direct calls.

### Player label render signature churn
- Status in this mod: **used** via `EntityRendererMixin` label injection.
- Action: maintain compatibility at mixin seam for both legacy and render-state signatures.
- Impacted path: `src/client/java/com/ctltierlist/tiertagger/client/mixin/EntityRendererMixin.java`.

### Render API churn (1.21.9/1.21.10 boundary)
- Status in this mod: world render event APIs are **not used**.
- Action: none needed for feature behavior currently implemented.

### Identifier usage rules
- Status in this mod: uses `Identifier.of(...)` (already aligned).
- Action: keep avoiding direct `new Identifier(...)`.

## 3) Compatibility seam policy
- Keep version-sensitive calls centralized in:
  - `ClientCompatBridge`
  - `ClientCompatBridge121`
  - `CompatBridgeFactory`
- Avoid scattering version checks in feature logic.

## 4) 1.21.10 regression checklist
- Client startup passes runtime version gate.
- Keybind registration works (`G`/`Y`).
- Nametag tier rendering works (mixin target resolves, no signature misses).
- Search screen and skin widget path works.
- Logs contain no mixin target or descriptor errors.
