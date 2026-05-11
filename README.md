# AutoBridge Mod — Minecraft 1.8.9 Forge

A **legit ninja-bridge assist** mod for Minecraft 1.8.9. It automatically sneaks
when your feet reach the edge of a block — exactly what a skilled player does
manually when ninja-bridging. You still place blocks yourself.

---

## How It Works (Legit Logic)

The mod mimics the **ninja bridging** technique:

1. You walk **backwards** over a void, holding a block in your hand.
2. When your feet get within ~0.22 blocks of the edge (with tiny random
   variance so it never looks perfectly mechanical), the mod presses **Sneak**
   for you — preventing a fall just like a human would.
3. You right-click to place the block yourself. The mod never auto-places.
4. The moment you step back onto a safe block, sneak is released naturally.

**What the mod does NOT do (keeping it legit):**
- ❌ No auto block placement
- ❌ No camera/rotation manipulation
- ❌ No fixed mechanical timing patterns (random jitter is applied)
- ✅ Only controls sneak state, just like a human fingertip on Shift

---

## Controls

| Action | Default Key | Where to change |
|---|---|---|
| Toggle AutoBridge ON/OFF | `V` | Options → Controls → **AutoBridge** |

When toggled, a chat message confirms the state:
- `[AutoBridge] AutoBridge ON` (green)
- `[AutoBridge] AutoBridge OFF` (red)

---

## Installation

### Requirements
- Minecraft **1.8.9**
- **Minecraft Forge** for 1.8.9 — download from https://files.minecraftforge.net

### Steps
1. Install Minecraft Forge 1.8.9 if you haven't already.
2. Place `AutoBridge-1.0.0.jar` into your `.minecraft/mods/` folder.
3. Launch Minecraft with the Forge 1.8.9 profile.
4. Go to **Options → Controls** and scroll to the **AutoBridge** category.
5. Rebind the toggle key to whatever you prefer.

---

## Building from Source

### Prerequisites
- JDK 8
- Gradle (wrapper included)

### Steps
```bash
# 1. Clone / download the project
cd AutoBridge

# 2. Set up the Forge workspace
./gradlew setupDecompWorkspace

# 3. Build the jar
./gradlew build

# Output: build/libs/AutoBridge-1.0.0.jar
```

---

## Usage Tips

- Hold **blocks** in your hand (the mod only activates when you're holding a
  placeable block).
- Walk **backwards** toward the edge as you would when ninja-bridging.
- Right-click as normal to place blocks; the auto-sneak handles the edge safety.
- You can rebind the toggle key in **Options → Controls → AutoBridge**.

---

## File Structure

```
AutoBridge/
├── build.gradle
└── src/main/
    ├── java/com/autobridge/mod/
    │   ├── AutoBridgeMod.java        ← Forge mod entry point
    │   ├── KeyBindings.java          ← Keybind registration
    │   └── AutoBridgeHandler.java    ← Core edge-detection & sneak logic
    └── resources/
        ├── mcmod.info
        └── assets/autobridge/lang/
            └── en_US.lang            ← Key binding display names
```
